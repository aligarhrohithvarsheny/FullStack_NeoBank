package com.neo.springapp.controller;

import com.neo.springapp.model.Account;
import com.neo.springapp.model.CurrentAccount;
import com.neo.springapp.model.SalaryAccount;
import com.neo.springapp.repository.AccountRepository;
import com.neo.springapp.repository.CurrentAccountRepository;
import com.neo.springapp.repository.SalaryAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/admin/signature-management")
@CrossOrigin(origins = "*")
public class SignatureManagementController {

    private static final String UPLOAD_DIR = "uploads/signatures/";

    // Track which accounts have had signature forms downloaded (key: "accountType_accountId")
    private static final ConcurrentHashMap<String, FormDownloadRecord> formDownloadTracker = new ConcurrentHashMap<>();

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private SalaryAccountRepository salaryAccountRepository;

    @Autowired
    private CurrentAccountRepository currentAccountRepository;

    // Inner class to track form downloads
    private static class FormDownloadRecord {
        String accountNumber;
        String accountType;
        Long accountId;
        LocalDateTime downloadedAt;

        FormDownloadRecord(String accountNumber, String accountType, Long accountId) {
            this.accountNumber = accountNumber;
            this.accountType = accountType;
            this.accountId = accountId;
            this.downloadedAt = LocalDateTime.now();
        }
    }

    /**
     * Check if an uploaded image contains a signature (has handwritten marks).
     * 
     * Strategy: A scanned signature form will always have printed text (dark pixels).
     * So we can't just check total darkness. Instead we:
     * 1) Divide the image into a grid of blocks
     * 2) Printed text produces UNIFORM dark blocks (spread across lines evenly)
     * 3) A handwritten signature produces CONCENTRATED variable ink in the lower half 
     *    (signature box area) with irregular stroke patterns
     * 4) We look for "signature-like" regions: dense ink clusters that are NOT in
     *    uniform horizontal lines (i.e., hand-drawn strokes vs printed text rows)
     * 5) Additionally check the bottom 40% of the image specifically (where "Sign Here" box is)
     */
    private Map<String, Object> analyzeSignaturePresence(MultipartFile file) {
        Map<String, Object> analysis = new HashMap<>();
        analysis.put("signed", false);
        analysis.put("confidence", 0);
        analysis.put("message", "");

        String contentType = file.getContentType();
        if (contentType != null && contentType.equals("application/pdf")) {
            analysis.put("signed", true);
            analysis.put("confidence", 50);
            analysis.put("message", "PDF uploaded - manual verification required by manager");
            analysis.put("requiresManualCheck", true);
            return analysis;
        }

        try (InputStream is = file.getInputStream()) {
            BufferedImage image = ImageIO.read(is);
            if (image == null) {
                analysis.put("message", "Cannot read image file. Please upload a valid image.");
                return analysis;
            }

            int width = image.getWidth();
            int height = image.getHeight();
            int totalPixels = width * height;

            // === PHASE 1: Analyze the signature zone (bottom 40% of image) ===
            // The downloaded form has the "Sign Here" box in the lower portion
            int sigZoneStartY = (int)(height * 0.55);
            int sigZoneEndY = (int)(height * 0.85);
            int sigZonePixels = width * (sigZoneEndY - sigZoneStartY);
            int sigZoneDark = 0;
            int sigZoneInk = 0;

            for (int y = sigZoneStartY; y < sigZoneEndY; y++) {
                for (int x = 0; x < width; x++) {
                    int rgb = image.getRGB(x, y);
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;
                    int brightness = (r + g + b) / 3;
                    if (brightness < 100) sigZoneDark++;
                    if (brightness < 50) sigZoneInk++;
                }
            }

            double sigZoneDarkPct = (double) sigZoneDark / sigZonePixels * 100;
            double sigZoneInkPct = (double) sigZoneInk / sigZonePixels * 100;

            // === PHASE 2: Stroke variation analysis ===
            // Divide image into horizontal strips in the signature zone
            // Printed text = dark pixels spread uniformly across rows
            // Handwritten sig = non-uniform dark pixel distribution (strokes vary)
            int numStrips = 20;
            int stripHeight = Math.max(1, (sigZoneEndY - sigZoneStartY) / numStrips);
            double[] stripDarkness = new double[numStrips];
            int stripIdx = 0;

            for (int s = 0; s < numStrips; s++) {
                int sStart = sigZoneStartY + s * stripHeight;
                int sEnd = Math.min(sStart + stripHeight, sigZoneEndY);
                int dark = 0;
                int total = 0;
                for (int y = sStart; y < sEnd; y++) {
                    for (int x = 0; x < width; x++) {
                        int rgb = image.getRGB(x, y);
                        int brightness = ((rgb >> 16 & 0xFF) + (rgb >> 8 & 0xFF) + (rgb & 0xFF)) / 3;
                        if (brightness < 80) dark++;
                        total++;
                    }
                }
                stripDarkness[s] = total > 0 ? (double) dark / total * 100 : 0;
            }

            // Calculate variation (std dev) in strip darkness
            double mean = 0;
            for (double v : stripDarkness) mean += v;
            mean /= numStrips;
            double variance = 0;
            for (double v : stripDarkness) variance += (v - mean) * (v - mean);
            variance /= numStrips;
            double strokeVariation = Math.sqrt(variance);

            // Count how many strips have significant ink (above minimal threshold)
            int stripsWithInk = 0;
            for (double v : stripDarkness) {
                if (v > 0.5) stripsWithInk++;
            }

            // === PHASE 3: Check for dense ink clusters (signature strokes) ===
            // Divide signature zone into small blocks, look for concentrated dark regions
            int blockSize = Math.max(8, Math.min(width, height) / 40);
            int blocksX = width / blockSize;
            int sigBlocksY = (sigZoneEndY - sigZoneStartY) / blockSize;
            int denseBlocks = 0;
            int totalBlocks = 0;

            for (int by = 0; by < sigBlocksY; by++) {
                for (int bx = 0; bx < blocksX; bx++) {
                    int bStartX = bx * blockSize;
                    int bStartY = sigZoneStartY + by * blockSize;
                    int blockDark = 0;
                    int blockTotal = blockSize * blockSize;

                    for (int py = bStartY; py < bStartY + blockSize && py < sigZoneEndY; py++) {
                        for (int px = bStartX; px < bStartX + blockSize && px < width; px++) {
                            int rgb = image.getRGB(px, py);
                            int brightness = ((rgb >> 16 & 0xFF) + (rgb >> 8 & 0xFF) + (rgb & 0xFF)) / 3;
                            if (brightness < 70) blockDark++;
                        }
                    }

                    totalBlocks++;
                    double blockDarkPct = (double) blockDark / blockTotal * 100;
                    // A block with >15% dark pixels suggests ink strokes
                    if (blockDarkPct > 15) denseBlocks++;
                }
            }

            double denseBlockPct = totalBlocks > 0 ? (double) denseBlocks / totalBlocks * 100 : 0;

            // === DECISION LOGIC ===
            // A blank scanned form has: uniform strip darkness, low stroke variation,
            // very few dense blocks in the signature zone, and the "Sign Here" text is very light/dashed
            //
            // A signed form has: non-uniform strip darkness (signature strokes vary),
            // higher variation, more dense blocks from handwriting

            boolean hasSignatureStrokes = false;
            String reason = "";

            // Check 1: Dense ink blocks in signature zone (handwriting creates concentrated dark clusters)
            if (denseBlockPct >= 3.0) {
                hasSignatureStrokes = true;
                reason = "Concentrated ink strokes detected in signature area";
            }
            // Check 2: High strip variation + some ink (signature = non-uniform)
            else if (strokeVariation > 1.5 && stripsWithInk >= 3) {
                hasSignatureStrokes = true;
                reason = "Variable ink pattern detected (handwriting-like strokes)";
            }
            // Check 3: Significant ink in signature zone specifically
            else if (sigZoneInkPct > 1.0) {
                hasSignatureStrokes = true;
                reason = "Significant ink marks detected in signature area";
            }

            // Build result
            analysis.put("sigZoneDarkPct", Math.round(sigZoneDarkPct * 100.0) / 100.0);
            analysis.put("sigZoneInkPct", Math.round(sigZoneInkPct * 100.0) / 100.0);
            analysis.put("strokeVariation", Math.round(strokeVariation * 100.0) / 100.0);
            analysis.put("denseBlockPct", Math.round(denseBlockPct * 100.0) / 100.0);
            analysis.put("stripsWithInk", stripsWithInk);

            if (!hasSignatureStrokes) {
                analysis.put("signed", false);
                analysis.put("confidence", 92);
                analysis.put("message", "Document appears to be UNSIGNED. No handwritten signature marks detected in the signature area. The form has printed text but no signature. Please sign the form in the 'Sign Here' box before uploading.");
                analysis.put("darkPercentage", Math.round(sigZoneDarkPct * 100.0) / 100.0);
                analysis.put("inkPercentage", Math.round(sigZoneInkPct * 100.0) / 100.0);
            } else {
                int confidence = Math.min(99, (int)(denseBlockPct * 10 + strokeVariation * 5));
                confidence = Math.max(confidence, 60);
                analysis.put("signed", true);
                analysis.put("confidence", confidence);
                analysis.put("message", "Signature detected: " + reason);
                analysis.put("darkPercentage", Math.round(sigZoneDarkPct * 100.0) / 100.0);
                analysis.put("inkPercentage", Math.round(sigZoneInkPct * 100.0) / 100.0);
            }

        } catch (Exception e) {
            analysis.put("message", "Could not analyze image: " + e.getMessage());
        }

        return analysis;
    }

    /**
     * Verify that the uploaded document is for the correct account
     * by checking form download records.
     */
    private Map<String, Object> verifyAccountMatch(String accountType, Long accountId) {
        Map<String, Object> result = new HashMap<>();
        String key = accountType + "_" + accountId;

        FormDownloadRecord record = formDownloadTracker.get(key);
        if (record == null) {
            result.put("matched", false);
            result.put("message", "Signature form was NOT downloaded for this account. You must first download the signature form (Step 1) before uploading. This ensures the correct form is used for the correct account.");
            return result;
        }

        // Verify it's the same account type and ID
        if (!record.accountType.equals(accountType) || !record.accountId.equals(accountId)) {
            result.put("matched", false);
            result.put("message", "Account mismatch! The downloaded form was for a different account. Please download the correct form for this account first.");
            return result;
        }

        result.put("matched", true);
        result.put("accountNumber", record.accountNumber);
        result.put("downloadedAt", record.downloadedAt.toString());
        result.put("message", "Account matched. Form was downloaded for account: " + record.accountNumber);
        return result;
    }

    /**
     * Look up account by account number across all account types
     */
    @GetMapping("/lookup/{accountNumber}")
    public ResponseEntity<?> lookupAccount(@PathVariable String accountNumber) {
        Map<String, Object> result = new HashMap<>();

        // Check Savings Account
        Account savings = accountRepository.findByAccountNumber(accountNumber);
        if (savings != null) {
            result.put("accountType", "savings");
            result.put("accountId", savings.getId());
            result.put("accountNumber", savings.getAccountNumber());
            result.put("name", savings.getName());
            result.put("phone", savings.getPhone());
            result.put("aadharNumber", savings.getAadharNumber());
            result.put("pan", savings.getPan());
            result.put("address", savings.getAddress());
            result.put("customerId", savings.getCustomerId());
            result.put("status", savings.getStatus());
            result.put("signatureCopyPath", savings.getSignatureCopyPath());
            result.put("signatureVerified", savings.getSignatureVerified());
            result.put("signatureUploadedAt", savings.getSignatureUploadedAt());
            result.put("signatureVerifiedBy", savings.getSignatureVerifiedBy());
            result.put("signatureVerifiedAt", savings.getSignatureVerifiedAt());
            return ResponseEntity.ok(result);
        }

        // Check Salary Account
        SalaryAccount salary = salaryAccountRepository.findByAccountNumber(accountNumber);
        if (salary != null) {
            result.put("accountType", "salary");
            result.put("accountId", salary.getId());
            result.put("accountNumber", salary.getAccountNumber());
            result.put("name", salary.getEmployeeName());
            result.put("phone", salary.getMobileNumber());
            result.put("aadharNumber", salary.getAadharNumber());
            result.put("pan", salary.getPanNumber());
            result.put("address", salary.getAddress());
            result.put("customerId", salary.getCustomerId());
            result.put("companyName", salary.getCompanyName());
            result.put("designation", salary.getDesignation());
            result.put("status", salary.getStatus());
            result.put("signatureCopyPath", salary.getSignatureCopyPath());
            result.put("signatureVerified", salary.getSignatureVerified());
            result.put("signatureUploadedAt", salary.getSignatureUploadedAt());
            result.put("signatureVerifiedBy", salary.getSignatureVerifiedBy());
            result.put("signatureVerifiedAt", salary.getSignatureVerifiedAt());
            return ResponseEntity.ok(result);
        }

        // Check Business/Current Account
        Optional<CurrentAccount> businessOpt = currentAccountRepository.findByAccountNumber(accountNumber);
        if (businessOpt.isPresent()) {
            CurrentAccount business = businessOpt.get();
            result.put("accountType", "business");
            result.put("accountId", business.getId());
            result.put("accountNumber", business.getAccountNumber());
            result.put("name", business.getOwnerName());
            result.put("businessName", business.getBusinessName());
            result.put("phone", business.getMobile());
            result.put("aadharNumber", business.getAadharNumber());
            result.put("pan", business.getPanNumber());
            result.put("address", business.getShopAddress());
            result.put("customerId", business.getCustomerId());
            result.put("status", business.getStatus());
            result.put("signatureCopyPath", business.getSignatureCopyPath());
            result.put("signatureVerified", business.getSignatureVerified());
            result.put("signatureUploadedAt", business.getSignatureUploadedAt());
            result.put("signatureVerifiedBy", business.getSignatureVerifiedBy());
            result.put("signatureVerifiedAt", business.getSignatureVerifiedAt());
            return ResponseEntity.ok(result);
        }

        return ResponseEntity.status(404).body(Map.of("error", "Account not found with number: " + accountNumber));
    }

    /**
     * Upload signature copy for an account.
     * Validates: 1) Form was downloaded for this account first (account match)
     *            2) Document is actually signed (signature detection)
     */
    @PostMapping("/upload/{accountType}/{accountId}")
    public ResponseEntity<?> uploadSignature(
            @PathVariable String accountType,
            @PathVariable Long accountId,
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No file provided"));
        }

        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.startsWith("image/") && !contentType.equals("application/pdf"))) {
            return ResponseEntity.badRequest().body(Map.of("error", "Only image files and PDFs are allowed"));
        }

        // === VALIDATION 1: Verify account match (form must be downloaded first) ===
        Map<String, Object> matchResult = verifyAccountMatch(accountType, accountId);
        boolean accountMatched = (boolean) matchResult.get("matched");
        if (!accountMatched) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", matchResult.get("message"));
            errorResponse.put("validationFailed", "ACCOUNT_MATCH");
            errorResponse.put("accountMatchPassed", false);
            errorResponse.put("signatureDetected", false);
            return ResponseEntity.badRequest().body(errorResponse);
        }

        // === VALIDATION 2: Check if document is actually signed ===
        Map<String, Object> signatureAnalysis = analyzeSignaturePresence(file);
        boolean isSigned = (boolean) signatureAnalysis.get("signed");
        if (!isSigned) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", signatureAnalysis.get("message"));
            errorResponse.put("validationFailed", "SIGNATURE_DETECTION");
            errorResponse.put("accountMatchPassed", true);
            errorResponse.put("signatureDetected", false);
            errorResponse.put("confidence", signatureAnalysis.get("confidence"));
            if (signatureAnalysis.containsKey("darkPercentage")) {
                errorResponse.put("darkPercentage", signatureAnalysis.get("darkPercentage"));
                errorResponse.put("inkPercentage", signatureAnalysis.get("inkPercentage"));
            }
            return ResponseEntity.badRequest().body(errorResponse);
        }

        try {
            // Create directory
            String subDir = UPLOAD_DIR + accountType + "/";
            Path uploadPath = Paths.get(subDir);
            Files.createDirectories(uploadPath);

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String filename = "signature_" + accountType + "_" + accountId + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;
            Path filePath = uploadPath.resolve(filename);

            // Save file
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            String savedPath = subDir + filename;
            boolean requiresManualCheck = signatureAnalysis.containsKey("requiresManualCheck") && (boolean) signatureAnalysis.get("requiresManualCheck");

            // Update the account with signature path
            switch (accountType) {
                case "savings":
                    Account savings = accountRepository.findById(accountId).orElse(null);
                    if (savings == null) return ResponseEntity.status(404).body(Map.of("error", "Savings account not found"));
                    savings.setSignatureCopyPath(savedPath);
                    savings.setSignatureUploadedAt(LocalDateTime.now());
                    savings.setSignatureVerified(false);
                    savings.setSignatureVerifiedBy(null);
                    savings.setSignatureVerifiedAt(null);
                    accountRepository.save(savings);
                    break;
                case "salary":
                    SalaryAccount salary = salaryAccountRepository.findById(accountId).orElse(null);
                    if (salary == null) return ResponseEntity.status(404).body(Map.of("error", "Salary account not found"));
                    salary.setSignatureCopyPath(savedPath);
                    salary.setSignatureUploadedAt(LocalDateTime.now());
                    salary.setSignatureVerified(false);
                    salary.setSignatureVerifiedBy(null);
                    salary.setSignatureVerifiedAt(null);
                    salaryAccountRepository.save(salary);
                    break;
                case "business":
                    CurrentAccount business = currentAccountRepository.findById(accountId).orElse(null);
                    if (business == null) return ResponseEntity.status(404).body(Map.of("error", "Business account not found"));
                    business.setSignatureCopyPath(savedPath);
                    business.setSignatureUploadedAt(LocalDateTime.now());
                    business.setSignatureVerified(false);
                    business.setSignatureVerifiedBy(null);
                    business.setSignatureVerifiedAt(null);
                    currentAccountRepository.save(business);
                    break;
                default:
                    return ResponseEntity.badRequest().body(Map.of("error", "Invalid account type: " + accountType));
            }

            // Build success response with validation details
            Map<String, Object> successResponse = new HashMap<>();
            successResponse.put("message", "Signature uploaded successfully");
            successResponse.put("path", savedPath);
            successResponse.put("uploadedAt", LocalDateTime.now().toString());
            successResponse.put("accountMatchPassed", true);
            successResponse.put("signatureDetected", true);
            successResponse.put("signatureConfidence", signatureAnalysis.get("confidence"));
            successResponse.put("accountNumber", matchResult.get("accountNumber"));
            if (requiresManualCheck) {
                successResponse.put("requiresManualCheck", true);
                successResponse.put("manualCheckMessage", "PDF document uploaded. Manager must manually verify the signature.");
            }

            return ResponseEntity.ok(successResponse);

        } catch (IOException e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to upload signature: " + e.getMessage()));
        }
    }

    /**
     * Verify signature for an account (manager action)
     */
    @PostMapping("/verify/{accountType}/{accountId}")
    public ResponseEntity<?> verifySignature(
            @PathVariable String accountType,
            @PathVariable Long accountId,
            @RequestBody Map<String, String> body) {

        String verifiedBy = body.getOrDefault("verifiedBy", "Admin");

        switch (accountType) {
            case "savings":
                Account savings = accountRepository.findById(accountId).orElse(null);
                if (savings == null) return ResponseEntity.status(404).body(Map.of("error", "Savings account not found"));
                if (savings.getSignatureCopyPath() == null) return ResponseEntity.badRequest().body(Map.of("error", "No signature uploaded yet"));
                savings.setSignatureVerified(true);
                savings.setSignatureVerifiedBy(verifiedBy);
                savings.setSignatureVerifiedAt(LocalDateTime.now());
                accountRepository.save(savings);
                return ResponseEntity.ok(Map.of("message", "Signature verified successfully"));
            case "salary":
                SalaryAccount salary = salaryAccountRepository.findById(accountId).orElse(null);
                if (salary == null) return ResponseEntity.status(404).body(Map.of("error", "Salary account not found"));
                if (salary.getSignatureCopyPath() == null) return ResponseEntity.badRequest().body(Map.of("error", "No signature uploaded yet"));
                salary.setSignatureVerified(true);
                salary.setSignatureVerifiedBy(verifiedBy);
                salary.setSignatureVerifiedAt(LocalDateTime.now());
                salaryAccountRepository.save(salary);
                return ResponseEntity.ok(Map.of("message", "Signature verified successfully"));
            case "business":
                CurrentAccount business = currentAccountRepository.findById(accountId).orElse(null);
                if (business == null) return ResponseEntity.status(404).body(Map.of("error", "Business account not found"));
                if (business.getSignatureCopyPath() == null) return ResponseEntity.badRequest().body(Map.of("error", "No signature uploaded yet"));
                business.setSignatureVerified(true);
                business.setSignatureVerifiedBy(verifiedBy);
                business.setSignatureVerifiedAt(LocalDateTime.now());
                currentAccountRepository.save(business);
                return ResponseEntity.ok(Map.of("message", "Signature verified successfully"));
            default:
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid account type: " + accountType));
        }
    }

    /**
     * View/download the uploaded signature image
     */
    @GetMapping("/view/{accountType}/{accountId}")
    public ResponseEntity<?> viewSignature(
            @PathVariable String accountType,
            @PathVariable Long accountId) {

        String signaturePath = null;

        switch (accountType) {
            case "savings":
                Account savings = accountRepository.findById(accountId).orElse(null);
                if (savings != null) signaturePath = savings.getSignatureCopyPath();
                break;
            case "salary":
                SalaryAccount salary = salaryAccountRepository.findById(accountId).orElse(null);
                if (salary != null) signaturePath = salary.getSignatureCopyPath();
                break;
            case "business":
                CurrentAccount business = currentAccountRepository.findById(accountId).orElse(null);
                if (business != null) signaturePath = business.getSignatureCopyPath();
                break;
            default:
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid account type"));
        }

        if (signaturePath == null) {
            return ResponseEntity.status(404).body(Map.of("error", "No signature found for this account"));
        }

        try {
            Path path = Paths.get(signaturePath);
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists()) {
                return ResponseEntity.status(404).body(Map.of("error", "Signature file not found on disk"));
            }

            String contentType = Files.probeContentType(path);
            if (contentType == null) contentType = "application/octet-stream";

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + path.getFileName().toString() + "\"")
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error reading signature file"));
        }
    }

    /**
     * Download signature form with user profile details (generates an HTML form for printing/signing)
     */
    @GetMapping("/download-form/{accountType}/{accountId}")
    public ResponseEntity<?> downloadSignatureForm(
            @PathVariable String accountType,
            @PathVariable Long accountId) {

        String name = "", accountNumber = "", customerId = "", phone = "", aadhar = "", pan = "", address = "";
        String accountLabel = "";
        String extraInfo = "";

        switch (accountType) {
            case "savings":
                Account savings = accountRepository.findById(accountId).orElse(null);
                if (savings == null) return ResponseEntity.status(404).body(Map.of("error", "Account not found"));
                name = savings.getName() != null ? savings.getName() : "";
                accountNumber = savings.getAccountNumber() != null ? savings.getAccountNumber() : "";
                customerId = savings.getCustomerId() != null ? savings.getCustomerId() : "";
                phone = savings.getPhone() != null ? savings.getPhone() : "";
                aadhar = savings.getAadharNumber() != null ? savings.getAadharNumber() : "";
                pan = savings.getPan() != null ? savings.getPan() : "";
                address = savings.getAddress() != null ? savings.getAddress() : "";
                accountLabel = "Savings Account";
                extraInfo = "Occupation: " + (savings.getOccupation() != null ? savings.getOccupation() : "N/A");
                break;
            case "salary":
                SalaryAccount salary = salaryAccountRepository.findById(accountId).orElse(null);
                if (salary == null) return ResponseEntity.status(404).body(Map.of("error", "Account not found"));
                name = salary.getEmployeeName() != null ? salary.getEmployeeName() : "";
                accountNumber = salary.getAccountNumber() != null ? salary.getAccountNumber() : "";
                customerId = salary.getCustomerId() != null ? salary.getCustomerId() : "";
                phone = salary.getMobileNumber() != null ? salary.getMobileNumber() : "";
                aadhar = salary.getAadharNumber() != null ? salary.getAadharNumber() : "";
                pan = salary.getPanNumber() != null ? salary.getPanNumber() : "";
                address = salary.getAddress() != null ? salary.getAddress() : "";
                accountLabel = "Salary Account";
                extraInfo = "Company: " + (salary.getCompanyName() != null ? salary.getCompanyName() : "N/A") +
                        " | Designation: " + (salary.getDesignation() != null ? salary.getDesignation() : "N/A");
                break;
            case "business":
                CurrentAccount business = currentAccountRepository.findById(accountId).orElse(null);
                if (business == null) return ResponseEntity.status(404).body(Map.of("error", "Account not found"));
                name = business.getOwnerName() != null ? business.getOwnerName() : "";
                accountNumber = business.getAccountNumber() != null ? business.getAccountNumber() : "";
                customerId = business.getCustomerId() != null ? business.getCustomerId() : "";
                phone = business.getMobile() != null ? business.getMobile() : "";
                aadhar = business.getAadharNumber() != null ? business.getAadharNumber() : "";
                pan = business.getPanNumber() != null ? business.getPanNumber() : "";
                address = business.getShopAddress() != null ? business.getShopAddress() : "";
                accountLabel = "Business/Current Account";
                extraInfo = "Business: " + (business.getBusinessName() != null ? business.getBusinessName() : "N/A") +
                        " | Type: " + (business.getBusinessType() != null ? business.getBusinessType() : "N/A");
                break;
            default:
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid account type"));
        }

        // Track form download for this account (used to verify account match on upload)
        String trackingKey = accountType + "_" + accountId;
        formDownloadTracker.put(trackingKey, new FormDownloadRecord(accountNumber, accountType, accountId));

        // Generate HTML signature form
        String htmlForm = "<!DOCTYPE html><html><head><meta charset='UTF-8'><title>Signature Form - " + accountNumber + "</title>"
                + "<style>"
                + "body { font-family: Arial, sans-serif; max-width: 800px; margin: 0 auto; padding: 40px; color: #333; }"
                + ".header { text-align: center; border-bottom: 3px solid #1a237e; padding-bottom: 20px; margin-bottom: 30px; }"
                + ".header h1 { color: #1a237e; margin: 0; font-size: 24px; }"
                + ".header h2 { color: #666; margin: 5px 0; font-size: 16px; font-weight: normal; }"
                + ".header p { color: #999; font-size: 12px; }"
                + ".info-table { width: 100%; border-collapse: collapse; margin: 20px 0; }"
                + ".info-table td { padding: 10px 15px; border: 1px solid #ddd; }"
                + ".info-table td:first-child { background: #f5f5f5; font-weight: bold; width: 200px; }"
                + ".signature-box { border: 2px dashed #999; height: 150px; margin: 30px 0; display: flex; align-items: center; justify-content: center; }"
                + ".signature-box p { color: #999; font-size: 14px; }"
                + ".footer { margin-top: 40px; display: flex; justify-content: space-between; }"
                + ".footer-box { text-align: center; width: 45%; }"
                + ".footer-box .line { border-top: 1px solid #333; margin-top: 60px; padding-top: 5px; }"
                + ".date-field { margin-top: 20px; }"
                + "@media print { body { padding: 20px; } }"
                + "</style></head><body>"
                + "<div class='header'>"
                + "<h1>NeoBank - Signature Verification Form</h1>"
                + "<h2>" + accountLabel + "</h2>"
                + "<p>Generated on: " + LocalDateTime.now().toString().replace("T", " ").substring(0, 19) + "</p>"
                + "</div>"
                + "<table class='info-table'>"
                + "<tr><td>Account Number</td><td>" + accountNumber + "</td></tr>"
                + "<tr><td>Customer ID</td><td>" + customerId + "</td></tr>"
                + "<tr><td>Account Holder Name</td><td>" + name + "</td></tr>"
                + "<tr><td>Phone Number</td><td>" + phone + "</td></tr>"
                + "<tr><td>Aadhaar Number</td><td>" + aadhar + "</td></tr>"
                + "<tr><td>PAN Number</td><td>" + pan + "</td></tr>"
                + "<tr><td>Address</td><td>" + address + "</td></tr>"
                + "<tr><td>Additional Info</td><td>" + extraInfo + "</td></tr>"
                + "</table>"
                + "<h3>Specimen Signature</h3>"
                + "<p style='font-size:12px;color:#666;'>Please sign within the box below. This signature will be used for verification of all banking transactions.</p>"
                + "<div class='signature-box'><p>Sign Here</p></div>"
                + "<div class='footer'>"
                + "<div class='footer-box'><div class='line'>Account Holder Signature</div></div>"
                + "<div class='footer-box'><div class='line'>Bank Officer Signature & Stamp</div></div>"
                + "</div>"
                + "<div class='date-field'><strong>Date:</strong> ___________________</div>"
                + "<p style='margin-top:30px;font-size:11px;color:#999;'>This is a system-generated signature form. Please print, sign, scan/photograph, and upload back to the system for verification.</p>"
                + "</body></html>";

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"signature_form_" + accountNumber + ".html\"")
                .body(htmlForm);
    }

    /**
     * Reject/remove signature (if re-upload is needed)
     */
    @DeleteMapping("/reject/{accountType}/{accountId}")
    public ResponseEntity<?> rejectSignature(
            @PathVariable String accountType,
            @PathVariable Long accountId) {

        switch (accountType) {
            case "savings":
                Account savings = accountRepository.findById(accountId).orElse(null);
                if (savings == null) return ResponseEntity.status(404).body(Map.of("error", "Account not found"));
                savings.setSignatureCopyPath(null);
                savings.setSignatureUploadedAt(null);
                savings.setSignatureVerified(false);
                savings.setSignatureVerifiedBy(null);
                savings.setSignatureVerifiedAt(null);
                accountRepository.save(savings);
                break;
            case "salary":
                SalaryAccount salary = salaryAccountRepository.findById(accountId).orElse(null);
                if (salary == null) return ResponseEntity.status(404).body(Map.of("error", "Account not found"));
                salary.setSignatureCopyPath(null);
                salary.setSignatureUploadedAt(null);
                salary.setSignatureVerified(false);
                salary.setSignatureVerifiedBy(null);
                salary.setSignatureVerifiedAt(null);
                salaryAccountRepository.save(salary);
                break;
            case "business":
                CurrentAccount business = currentAccountRepository.findById(accountId).orElse(null);
                if (business == null) return ResponseEntity.status(404).body(Map.of("error", "Account not found"));
                business.setSignatureCopyPath(null);
                business.setSignatureUploadedAt(null);
                business.setSignatureVerified(false);
                business.setSignatureVerifiedBy(null);
                business.setSignatureVerifiedAt(null);
                currentAccountRepository.save(business);
                break;
            default:
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid account type"));
        }

        return ResponseEntity.ok(Map.of("message", "Signature rejected and removed. Re-upload required."));
    }
}
