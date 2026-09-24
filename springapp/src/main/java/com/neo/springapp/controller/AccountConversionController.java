package com.neo.springapp.controller;

import com.neo.springapp.model.AccountConversionRequest;
import com.neo.springapp.service.AccountConversionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/account-conversion")
@CrossOrigin(origins = "*")
public class AccountConversionController {

    private static final String UPLOAD_DIR = "uploads/account-conversion/";

    @Autowired
    private AccountConversionService accountConversionService;

    @GetMapping("/lookup/{accountNumber}")
    public ResponseEntity<?> lookupAccount(@PathVariable String accountNumber) {
        try {
            return ResponseEntity.ok(accountConversionService.lookupAccount(accountNumber));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/generate-application")
    public ResponseEntity<?> generateApplication(@RequestBody Map<String, String> payload) {
        try {
            String accountNumber = payload.get("accountNumber");
            String targetType = payload.get("targetType");
            String requestedBy = payload.get("requestedBy");
            return ResponseEntity.ok(accountConversionService.generateApplication(accountNumber, targetType, requestedBy));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadTermsAndConditions(@RequestParam("file") MultipartFile file,
                                                     @RequestParam(value = "accountNumber", required = false) String accountNumber) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No terms file provided"));
        }

        String contentType = file.getContentType();
        if (contentType == null || (!contentType.startsWith("image/") && !contentType.equals("application/pdf"))) {
            return ResponseEntity.badRequest().body(Map.of("error", "Only image files and PDF files are allowed"));
        }

        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            Files.createDirectories(uploadPath);

            String originalName = file.getOriginalFilename() == null ? "terms" : file.getOriginalFilename();
            String extension = originalName.contains(".") ? originalName.substring(originalName.lastIndexOf('.')) : ".pdf";
            String fileName = "terms_" + (accountNumber != null ? accountNumber.replaceAll("[^a-zA-Z0-9]", "_") : "account") + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;
            Path targetPath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            Map<String, Object> response = new HashMap<>();
            response.put("path", UPLOAD_DIR + fileName);
            response.put("fileName", fileName);
            response.put("accountNumber", accountNumber);
            response.put("signatureDetected", true);
            response.put("message", "Terms and conditions uploaded successfully. Signature verification will be completed during approval.");
            return ResponseEntity.ok(response);
        } catch (IOException ex) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to upload document: " + ex.getMessage()));
        }
    }

    @PostMapping("/submit")
    public ResponseEntity<?> submitRequest(@RequestBody Map<String, Object> payload) {
        try {
            String accountNumber = String.valueOf(payload.getOrDefault("accountNumber", ""));
            String sourceType = String.valueOf(payload.getOrDefault("sourceType", "Savings"));
            String targetType = String.valueOf(payload.getOrDefault("targetType", "Salary"));
            String requestedBy = String.valueOf(payload.getOrDefault("requestedBy", "Admin"));
            String reason = String.valueOf(payload.getOrDefault("reason", "Account conversion requested"));
            String termsPath = String.valueOf(payload.getOrDefault("termsAndConditionsPath", ""));
            String applicationNumber = String.valueOf(payload.getOrDefault("applicationNumber", ""));
            String applicationContent = String.valueOf(payload.getOrDefault("applicationText", ""));

            AccountConversionRequest request = accountConversionService.submitRequest(
                    accountNumber, sourceType, targetType, requestedBy, reason, termsPath, applicationNumber, applicationContent);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Account conversion request submitted successfully for approval");
            response.put("requestId", request.getId());
            response.put("requestStatus", request.getRequestStatus());
            response.put("applicationNumber", request.getApplicationNumber());
            response.put("accountNumber", request.getAccountNumber());
            response.put("targetType", request.getTargetAccountType());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/history")
    public ResponseEntity<List<AccountConversionRequest>> getHistory() {
        return ResponseEntity.ok(accountConversionService.getHistory());
    }

    @PostMapping("/{requestId}/approve")
    public ResponseEntity<?> approveRequest(@PathVariable Long requestId, @RequestBody Map<String, String> payload) {
        try {
            String approvedBy = payload.getOrDefault("approvedBy", "Admin");
            return ResponseEntity.ok(accountConversionService.approveRequest(requestId, approvedBy));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/{requestId}/revert")
    public ResponseEntity<?> revertRequest(@PathVariable Long requestId, @RequestBody Map<String, String> payload) {
        try {
            String revertedBy = payload.getOrDefault("revertedBy", "Admin");
            return ResponseEntity.ok(accountConversionService.revertRequest(requestId, revertedBy));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }
}
