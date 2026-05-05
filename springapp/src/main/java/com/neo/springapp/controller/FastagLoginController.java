package com.neo.springapp.controller;

import com.neo.springapp.model.FastagUser;
import com.neo.springapp.model.FastagLinkedAccount;
import com.neo.springapp.model.Fasttag;
import com.neo.springapp.model.Account;
import com.neo.springapp.service.FastagLoginService;
import com.neo.springapp.service.FasttagService;
import com.neo.springapp.service.AccountService;
import com.neo.springapp.service.OtpService;
import com.neo.springapp.service.EmailService;
import com.neo.springapp.repository.FastagLinkedAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/fastag")
public class FastagLoginController {

    private static final Pattern GMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@gmail\\.com$", Pattern.CASE_INSENSITIVE);

    @Autowired
    private FastagLoginService fastagLoginService;

    @Autowired
    private FasttagService fasttagService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private OtpService otpService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private FastagLinkedAccountRepository fastagLinkedAccountRepository;

    /**
     * POST /api/fastag/test-email
     * Manual SMTP verification endpoint (for Render log debugging).
     */
    @PostMapping("/test-email")
    public ResponseEntity<Map<String, Object>> testEmail(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        String email = request.get("email");

        if (email == null || email.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "Email is required.");
            return ResponseEntity.badRequest().body(response);
        }

        String targetEmail = email.trim().toLowerCase();
        String testOtp = "654321";
        System.out.println("🧪 Fastag test-email endpoint invoked for: " + targetEmail);
        boolean sent = emailService.sendOtpEmail(targetEmail, testOtp);
        response.put("success", sent);
        response.put("message", sent
                ? "Test email send attempted successfully. Check inbox/spam."
                : "Test email failed. Check Render logs for SMTP/auth error details.");
        response.put("email", targetEmail);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/fastag/send-otp
     * Send OTP to Gmail ID for FASTag login
     */
    @PostMapping("/send-otp")
    public ResponseEntity<Map<String, Object>> sendOtp(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();

        String gmailId = request.get("gmailId");
        System.out.println("OTP API HIT: /api/fastag/send-otp");
        System.out.println("Email: " + gmailId);
        if (gmailId == null || gmailId.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "Gmail ID is required.");
            return ResponseEntity.badRequest().body(response);
        }

        gmailId = gmailId.trim();
        if (!GMAIL_PATTERN.matcher(gmailId).matches()) {
            response.put("success", false);
            response.put("message", "Please enter a valid Gmail address (e.g., user@gmail.com).");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            fastagLoginService.sendOtp(gmailId);
            response.put("success", true);
            response.put("message", "OTP sent successfully to " + gmailId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to send OTP. Please try again.");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * POST /api/fastag/verify-otp
     * Verify OTP and login/register FASTag user
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, Object>> verifyOtp(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();

        String gmailId = request.get("gmailId");
        String otp = request.get("otp");

        if (gmailId == null || gmailId.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "Gmail ID is required.");
            return ResponseEntity.badRequest().body(response);
        }

        if (otp == null || otp.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "OTP is required.");
            return ResponseEntity.badRequest().body(response);
        }

        if (otp.trim().length() != 6) {
            response.put("success", false);
            response.put("message", "OTP must be 6 digits.");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            FastagLoginService.VerifyResult result = fastagLoginService.verifyOtp(gmailId, otp);
            response.put("success", result.success);
            response.put("message", result.message);

            if (result.success && result.user != null) {
                Map<String, Object> userData = new HashMap<>();
                userData.put("id", result.user.getId());
                userData.put("gmailId", result.user.getGmailId());
                userData.put("isVerified", result.user.getIsVerified());
                userData.put("sessionToken", result.sessionToken);
                response.put("user", userData);

                // Fetch any existing FASTag details linked to this email
                List<Fasttag> fasttags = fasttagService.findByEmail(gmailId.trim().toLowerCase());
                response.put("fasttags", fasttags);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Verification failed. Please try again.");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * GET /api/fastag/user-details/{gmailId}
     * Get FASTag user details and linked FASTag accounts
     */
    @GetMapping("/user-details/{gmailId}")
    public ResponseEntity<Map<String, Object>> getUserDetails(@PathVariable String gmailId) {
        Map<String, Object> response = new HashMap<>();

        try {
            var optUser = fastagLoginService.getUserByGmail(gmailId);
            if (optUser.isEmpty()) {
                response.put("success", false);
                response.put("message", "User not found.");
                return ResponseEntity.ok(response);
            }

            FastagUser user = optUser.get();
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", user.getId());
            userData.put("gmailId", user.getGmailId());
            userData.put("isVerified", user.getIsVerified());
            userData.put("lastLoginAt", user.getLastLoginAt());
            response.put("user", userData);

            // Fetch FASTag details linked to this email
            List<Fasttag> fasttags = fasttagService.findByEmail(gmailId.trim().toLowerCase());
            response.put("fasttags", fasttags);
            response.put("success", true);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to fetch user details.");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * POST /api/fastag/apply
     * Apply for a new FASTag via FASTag login (no bank account needed)
     */
    @PostMapping("/apply")
    public ResponseEntity<Map<String, Object>> applyFasttag(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();

        String gmailId = request.get("gmailId");
        String vehicleNumber = request.get("vehicleNumber");
        String vehicleType = request.get("vehicleType");
        String userName = request.get("userName");

        if (gmailId == null || gmailId.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "Gmail ID is required.");
            return ResponseEntity.badRequest().body(response);
        }
        if (vehicleNumber == null || vehicleNumber.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "Vehicle number is required.");
            return ResponseEntity.badRequest().body(response);
        }
        if (vehicleType == null || vehicleType.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "Vehicle type is required.");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            Fasttag fasttag = new Fasttag();
            fasttag.setEmail(gmailId.trim().toLowerCase());
            fasttag.setVehicleNumber(vehicleNumber.trim().toUpperCase());
            fasttag.setVehicleType(vehicleType.trim());
            fasttag.setUserName(userName != null ? userName.trim() : gmailId.trim());
            fasttag.setUserId("FASTAG_" + gmailId.trim().toLowerCase());
            fasttag.setBank("NeoBank");
            fasttag.setVehicleDetails(vehicleType.trim() + " - " + vehicleNumber.trim().toUpperCase());
            // Optional fields
            if (request.get("chassisNumber") != null) fasttag.setChassisNumber(request.get("chassisNumber").trim());
            if (request.get("engineNumber") != null) fasttag.setEngineNumber(request.get("engineNumber").trim());
            if (request.get("mobileNumber") != null) fasttag.setMobileNumber(request.get("mobileNumber").trim());

            Fasttag saved = fasttagService.apply(fasttag);
            response.put("success", true);
            response.put("message", "FASTag application submitted successfully!");
            response.put("fasttag", saved);
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            response.put("success", false);
            response.put("message", ex.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to apply for FASTag.");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * POST /api/fastag/recharge
     * Recharge FASTag via FASTag login - debits from linked bank account
     */
    @PostMapping("/recharge")
    public ResponseEntity<Map<String, Object>> rechargeFasttag(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();

        String fasttagNumber = request.get("fasttagNumber") == null ? null : String.valueOf(request.get("fasttagNumber"));
        Double amount = request.get("amount") == null ? null : Double.valueOf(request.get("amount").toString());
        String gmailId = request.get("gmailId") == null ? null : String.valueOf(request.get("gmailId"));
        String accountNumber = request.get("accountNumber") == null ? null : String.valueOf(request.get("accountNumber"));

        if (fasttagNumber == null || fasttagNumber.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "FASTag number is required.");
            return ResponseEntity.badRequest().body(response);
        }
        if (amount == null || amount < 100 || amount > 50000) {
            response.put("success", false);
            response.put("message", "Recharge amount must be between ₹100 and ₹50,000.");
            return ResponseEntity.badRequest().body(response);
        }
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "Please link a bank account before recharging.");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            // Verify the account is linked to this user
            String normalizedGmail = gmailId != null ? gmailId.trim().toLowerCase() : "";
            var linkedOpt = fastagLinkedAccountRepository.findByGmailIdAndAccountNumberAndStatus(
                    normalizedGmail, accountNumber.trim(), "ACTIVE");
            if (linkedOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "This account is not linked to your FASTag. Please link it first.");
                return ResponseEntity.badRequest().body(response);
            }

            // Verify account has sufficient balance
            Account account = accountService.getAccountByNumber(accountNumber.trim());
            if (account == null) {
                response.put("success", false);
                response.put("message", "Linked bank account not found.");
                return ResponseEntity.badRequest().body(response);
            }
            if (account.getBalance() == null || account.getBalance() < amount) {
                response.put("success", false);
                response.put("message", "Insufficient balance in linked account. Available: ₹" + String.format("%.2f", account.getBalance()));
                return ResponseEntity.badRequest().body(response);
            }

            // Debit from bank account
            Double remaining = accountService.debitBalance(accountNumber.trim(), amount);
            if (remaining == null) {
                response.put("success", false);
                response.put("message", "Failed to debit from bank account. Insufficient funds.");
                return ResponseEntity.badRequest().body(response);
            }

            // Credit to FASTag
            Fasttag updated = fasttagService.rechargeByTag(fasttagNumber.trim(), amount, "FASTAG_USER", gmailId);
            if (updated == null) {
                // Rollback: credit back to bank account
                accountService.creditBalance(accountNumber.trim(), amount);
                response.put("success", false);
                response.put("message", "FASTag not found. Amount has been refunded to your account.");
                return ResponseEntity.badRequest().body(response);
            }

            response.put("success", true);
            response.put("message", "Recharge of ₹" + String.format("%.2f", amount) + " successful! Debited from account " + accountNumber.trim());
            response.put("fasttag", updated);
            response.put("accountBalance", remaining);
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            response.put("success", false);
            response.put("message", ex.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Recharge failed. Please try again.");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * POST /api/fastag/close/{id}
     * Close FASTag (only if balance is 0)
     */
    @PostMapping("/close/{id}")
    public ResponseEntity<Map<String, Object>> closeFasttag(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();

        try {
            Fasttag f = fasttagService.getById(id);
            if (f == null) {
                response.put("success", false);
                response.put("message", "FASTag not found.");
                return ResponseEntity.badRequest().body(response);
            }

            // Check balance is 0
            if (f.getBalance() != null && f.getBalance() > 0) {
                response.put("success", false);
                response.put("message", "Cannot close FASTag with balance ₹" + String.format("%.2f", f.getBalance()) + ". Balance must be ₹0.00 to close.");
                return ResponseEntity.badRequest().body(response);
            }

            Fasttag closed = fasttagService.closeFasttag(id);
            if (closed == null) {
                response.put("success", false);
                response.put("message", "Failed to close FASTag.");
                return ResponseEntity.badRequest().body(response);
            }
            response.put("success", true);
            response.put("message", "FASTag closed successfully.");
            response.put("fasttag", closed);
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            response.put("success", false);
            response.put("message", ex.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to close FASTag.");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * GET /api/fastag/transactions/{fasttagId}
     * Get transaction history for a FASTag
     */
    @GetMapping("/transactions/{fasttagId}")
    public ResponseEntity<Map<String, Object>> getTransactions(@PathVariable Long fasttagId) {
        Map<String, Object> response = new HashMap<>();
        try {
            var transactions = fasttagService.transactionsForTag(fasttagId);
            response.put("success", true);
            response.put("transactions", transactions);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to fetch transactions.");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * POST /api/fastag/link-account
     * Verify account number exists and send OTP to user's email to confirm linking
     */
    @PostMapping("/link-account")
    public ResponseEntity<Map<String, Object>> linkAccount(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();

        String gmailId = request.get("gmailId");
        String accountNumber = request.get("accountNumber");

        if (gmailId == null || gmailId.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "Gmail ID is required.");
            return ResponseEntity.badRequest().body(response);
        }
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "Account number is required.");
            return ResponseEntity.badRequest().body(response);
        }

        gmailId = gmailId.trim().toLowerCase();
        accountNumber = accountNumber.trim();

        try {
            // Check if account exists in the bank
            Account account = accountService.getAccountByNumber(accountNumber);
            if (account == null) {
                response.put("success", false);
                response.put("message", "Account number not found. Please enter a valid NeoBank account number.");
                return ResponseEntity.badRequest().body(response);
            }

            // Check account is active
            if (!"ACTIVE".equalsIgnoreCase(account.getStatus())) {
                response.put("success", false);
                response.put("message", "This account is not active. Only active accounts can be linked.");
                return ResponseEntity.badRequest().body(response);
            }

            // Check if already linked
            var existingLink = fastagLinkedAccountRepository.findByGmailIdAndAccountNumberAndStatus(gmailId, accountNumber, "ACTIVE");
            if (existingLink.isPresent()) {
                response.put("success", false);
                response.put("message", "This account is already linked to your FASTag.");
                return ResponseEntity.badRequest().body(response);
            }

            // Generate OTP and send to user's gmail
            String otp = otpService.generateOtp();
            otpService.storeOtp("FASTAG_LINK_" + gmailId, otp);

            // Send OTP email
            boolean sent = emailService.sendOtpEmail(gmailId, otp);
            if (!sent) {
                System.out.println("⚠️ Link account OTP email send reported failure. Check console for OTP.");
            }

            // Return account holder name (masked) for verification
            String holderName = account.getName() != null ? account.getName() : "Account Holder";
            response.put("success", true);
            response.put("message", "OTP sent to " + gmailId + ". Please verify to link your account.");
            response.put("accountHolderName", holderName);
            response.put("maskedBalance", "₹" + String.format("%,.2f", account.getBalance()));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to process account linking. Please try again.");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * POST /api/fastag/verify-link-account
     * Verify OTP and complete account linking
     */
    @PostMapping("/verify-link-account")
    public ResponseEntity<Map<String, Object>> verifyLinkAccount(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();

        String gmailId = request.get("gmailId");
        String accountNumber = request.get("accountNumber");
        String otp = request.get("otp");

        if (gmailId == null || accountNumber == null || otp == null) {
            response.put("success", false);
            response.put("message", "Gmail ID, account number, and OTP are required.");
            return ResponseEntity.badRequest().body(response);
        }

        gmailId = gmailId.trim().toLowerCase();
        accountNumber = accountNumber.trim();
        otp = otp.trim();

        try {
            // Verify OTP
            boolean otpValid = otpService.verifyOtp("FASTAG_LINK_" + gmailId, otp);
            if (!otpValid) {
                response.put("success", false);
                response.put("message", "Invalid or expired OTP. Please try again.");
                return ResponseEntity.badRequest().body(response);
            }

            // Verify account still exists
            Account account = accountService.getAccountByNumber(accountNumber);
            if (account == null) {
                response.put("success", false);
                response.put("message", "Account not found.");
                return ResponseEntity.badRequest().body(response);
            }

            // Create or update linked account record
            var existingLink = fastagLinkedAccountRepository.findByGmailIdAndAccountNumber(gmailId, accountNumber);
            FastagLinkedAccount linkedAccount;
            if (existingLink.isPresent()) {
                linkedAccount = existingLink.get();
                linkedAccount.setVerified(true);
                linkedAccount.setStatus("ACTIVE");
                linkedAccount.setLinkedAt(java.time.LocalDateTime.now());
            } else {
                linkedAccount = new FastagLinkedAccount();
                linkedAccount.setGmailId(gmailId);
                linkedAccount.setAccountNumber(accountNumber);
                linkedAccount.setVerified(true);
                linkedAccount.setStatus("ACTIVE");
            }
            linkedAccount.setAccountHolderName(account.getName());
            fastagLinkedAccountRepository.save(linkedAccount);

            response.put("success", true);
            response.put("message", "Account linked successfully! You can now recharge your FASTag from this account.");
            response.put("linkedAccount", Map.of(
                "accountNumber", accountNumber,
                "accountHolderName", account.getName() != null ? account.getName() : "N/A",
                "status", "ACTIVE"
            ));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to verify and link account. Please try again.");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * GET /api/fastag/linked-accounts/{gmailId}
     * Get all linked bank accounts for a FASTag user
     */
    @GetMapping("/linked-accounts/{gmailId}")
    public ResponseEntity<Map<String, Object>> getLinkedAccounts(@PathVariable String gmailId) {
        Map<String, Object> response = new HashMap<>();
        try {
            String normalized = gmailId.trim().toLowerCase();
            List<FastagLinkedAccount> accounts = fastagLinkedAccountRepository.findByGmailIdAndStatus(normalized, "ACTIVE");
            response.put("success", true);
            response.put("linkedAccounts", accounts);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to fetch linked accounts.");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * GET /api/fastag/sticker/{id}?gmailId=user@gmail.com
     * Download FASTag sticker for approved tag owned by this Gmail ID.
     */
    @GetMapping("/sticker/{id}")
    public ResponseEntity<?> downloadStickerForFastagUser(@PathVariable Long id, @RequestParam("gmailId") String gmailId) {
        if (gmailId == null || gmailId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "gmailId is required."));
        }
        Fasttag tag = fasttagService.getById(id);
        if (tag == null) {
            return ResponseEntity.notFound().build();
        }
        String normalizedGmail = gmailId.trim().toLowerCase();
        String tagEmail = tag.getEmail() == null ? "" : tag.getEmail().trim().toLowerCase();
        if (!tagEmail.equals(normalizedGmail)) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "You are not allowed to download this FASTag sticker."));
        }
        if (!"Approved".equalsIgnoreCase(tag.getStatus())) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "FASTag sticker is available only for approved FASTag."));
        }
        if (tag.getStickerPath() == null || tag.getStickerPath().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Sticker not available for this FASTag."));
        }

        try {
            File file = new File(tag.getStickerPath());
            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }
            String contentType = Files.probeContentType(file.toPath());
            if (contentType == null || contentType.isBlank()) {
                contentType = "application/octet-stream";
            }
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getName());
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(file.length())
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(new InputStreamResource(new FileInputStream(file)));
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "Failed to download FASTag sticker."));
        }
    }

    /**
     * POST /api/fastag/unlink-account
     * Unlink a bank account from FASTag
     */
    @PostMapping("/unlink-account")
    public ResponseEntity<Map<String, Object>> unlinkAccount(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        String gmailId = request.get("gmailId");
        String accountNumber = request.get("accountNumber");

        if (gmailId == null || accountNumber == null) {
            response.put("success", false);
            response.put("message", "Gmail ID and account number are required.");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            var link = fastagLinkedAccountRepository.findByGmailIdAndAccountNumberAndStatus(
                    gmailId.trim().toLowerCase(), accountNumber.trim(), "ACTIVE");
            if (link.isEmpty()) {
                response.put("success", false);
                response.put("message", "Linked account not found.");
                return ResponseEntity.badRequest().body(response);
            }
            FastagLinkedAccount account = link.get();
            account.setStatus("INACTIVE");
            fastagLinkedAccountRepository.save(account);
            response.put("success", true);
            response.put("message", "Account unlinked successfully.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to unlink account.");
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
