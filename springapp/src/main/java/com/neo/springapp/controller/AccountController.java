package com.neo.springapp.controller;

import com.neo.springapp.model.Account;
import com.neo.springapp.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    @Autowired
    private AccountService accountService;

    @Value("${spring.web.cors.allowed-origins:}")
    private String allowedOrigins;

    // Basic CRUD operations
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createAccount(@RequestBody Account account) {
        try {
            Map<String, Object> response = new HashMap<>();
            
            // Validate unique fields - Aadhar
            if (account.getAadharNumber() != null && !account.getAadharNumber().isEmpty()) {
                if (!accountService.isAadharUnique(account.getAadharNumber())) {
                    response.put("success", false);
                    response.put("error", "Aadhar number is already registered. Another account exists with this Aadhar number.");
                    response.put("errorType", "AADHAR_EXISTS");
                    return ResponseEntity.badRequest().body(response);
                }
            }
            
            // Validate unique fields - PAN
            if (account.getPan() != null && !account.getPan().isEmpty()) {
                if (!accountService.isPanUnique(account.getPan())) {
                    response.put("success", false);
                    response.put("error", "PAN number is already registered. Another account exists with this PAN number.");
                    response.put("errorType", "PAN_EXISTS");
                    return ResponseEntity.badRequest().body(response);
                }
            }
            
            // Validate unique fields - Phone
            if (account.getPhone() != null && !account.getPhone().isEmpty()) {
                if (!accountService.isPhoneUnique(account.getPhone())) {
                    response.put("success", false);
                    response.put("error", "Mobile number is already registered. Another account exists with this mobile number.");
                    response.put("errorType", "PHONE_EXISTS");
                    return ResponseEntity.badRequest().body(response);
                }
            }
            
            Account savedAccount = accountService.saveAccount(account);
            response.put("success", true);
            response.put("account", savedAccount);
            response.put("message", "Account created successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "Account creation failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Account> getAccountById(@PathVariable Long id) {
        Optional<Account> account = accountService.getAccountById(id);
        return account.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/pan/{pan}")
    public ResponseEntity<Account> getAccountByPan(@PathVariable String pan) {
        Account account = accountService.getAccountByPan(pan);
        return account != null ? ResponseEntity.ok(account) : ResponseEntity.notFound().build();
    }

    @GetMapping("/number/{accountNumber}")
    public ResponseEntity<Account> getAccountByNumber(@PathVariable String accountNumber) {
        Account account = accountService.getAccountByNumber(accountNumber);
        return account != null ? ResponseEntity.ok(account) : ResponseEntity.notFound().build();
    }

    @GetMapping("/aadhar/{aadharNumber}")
    public ResponseEntity<Account> getAccountByAadhar(@PathVariable String aadharNumber) {
        Account account = accountService.getAccountByAadhar(aadharNumber);
        return account != null ? ResponseEntity.ok(account) : ResponseEntity.notFound().build();
    }

    /**
     * Verify account by both Aadhar number and account number for security purposes
     * Used in education loan application to verify child's account
     */
    @GetMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyAccountByAadharAndNumber(
            @RequestParam String aadharNumber,
            @RequestParam String accountNumber) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Get account by account number
            Account account = accountService.getAccountByNumber(accountNumber);
            
            if (account == null) {
                response.put("success", false);
                response.put("message", "Account not found");
                response.put("error", "ACCOUNT_NOT_FOUND");
                return ResponseEntity.status(404).body(response);
            }
            
            // Verify Aadhar matches
            if (account.getAadharNumber() == null || account.getAadharNumber().isEmpty()) {
                response.put("success", false);
                response.put("message", "Aadhar number not found in account");
                response.put("error", "AADHAR_NOT_FOUND");
                return ResponseEntity.status(400).body(response);
            }
            
            if (!account.getAadharNumber().equals(aadharNumber)) {
                response.put("success", false);
                response.put("message", "Aadhar number does not match the account number");
                response.put("error", "AADHAR_MISMATCH");
                return ResponseEntity.status(400).body(response);
            }
            
            // Verification successful
            response.put("success", true);
            response.put("message", "Account verified successfully");
            response.put("account", account);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to verify account: " + e.getMessage());
            response.put("error", "VERIFICATION_ERROR");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<Account> updateAccount(@PathVariable Long id, @RequestBody Account accountDetails) {
        Account updatedAccount = accountService.updateAccount(id, accountDetails);
        return updatedAccount != null ? ResponseEntity.ok(updatedAccount) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAccount(@PathVariable Long id) {
        try {
            accountService.deleteAccount(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // Pagination and sorting
    @GetMapping("/all")
    public ResponseEntity<Page<Account>> getAllAccounts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Page<Account> accounts = accountService.getAllAccountsWithPagination(page, size, sortBy, sortDir);
        return ResponseEntity.ok(accounts);
    }

    // Status-based operations
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Account>> getAccountsByStatus(@PathVariable String status) {
        List<Account> accounts = accountService.getAccountsByStatus(status);
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/status/{status}/paginated")
    public ResponseEntity<Page<Account>> getAccountsByStatusWithPagination(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<Account> accounts = accountService.getAccountsByStatusWithPagination(status, page, size);
        return ResponseEntity.ok(accounts);
    }

    // Account type operations
    @GetMapping("/type/{accountType}")
    public ResponseEntity<List<Account>> getAccountsByType(@PathVariable String accountType) {
        List<Account> accounts = accountService.getAccountsByType(accountType);
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/type/{accountType}/paginated")
    public ResponseEntity<Page<Account>> getAccountsByTypeWithPagination(
            @PathVariable String accountType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<Account> accounts = accountService.getAccountsByTypeWithPagination(accountType, page, size);
        return ResponseEntity.ok(accounts);
    }

    // KYC verification operations
    @GetMapping("/kyc-verified")
    public ResponseEntity<List<Account>> getKycVerifiedAccounts() {
        List<Account> accounts = accountService.getKycVerifiedAccounts();
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/verified-matrix")
    public ResponseEntity<List<Account>> getVerifiedMatrixAccounts() {
        List<Account> accounts = accountService.getVerifiedMatrixAccounts();
        return ResponseEntity.ok(accounts);
    }

    // Search operations
    @GetMapping("/search")
    public ResponseEntity<Page<Account>> searchAccounts(
            @RequestParam String searchTerm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<Account> accounts = accountService.searchAccounts(searchTerm, page, size);
        return ResponseEntity.ok(accounts);
    }

    // Filter operations
    @GetMapping("/filter/income")
    public ResponseEntity<List<Account>> getAccountsByIncomeRange(
            @RequestParam Double minIncome,
            @RequestParam Double maxIncome) {
        List<Account> accounts = accountService.getAccountsByIncomeRange(minIncome, maxIncome);
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/filter/balance")
    public ResponseEntity<List<Account>> getAccountsByBalanceRange(
            @RequestParam Double minBalance,
            @RequestParam Double maxBalance) {
        List<Account> accounts = accountService.getAccountsByBalanceRange(minBalance, maxBalance);
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/filter/occupation/{occupation}")
    public ResponseEntity<List<Account>> getAccountsByOccupation(@PathVariable String occupation) {
        List<Account> accounts = accountService.getAccountsByOccupation(occupation);
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/filter/created-date")
    public ResponseEntity<List<Account>> getAccountsByCreatedDateRange(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        LocalDateTime start = LocalDateTime.parse(startDate);
        LocalDateTime end = LocalDateTime.parse(endDate);
        List<Account> accounts = accountService.getAccountsByCreatedDateRange(start, end);
        return ResponseEntity.ok(accounts);
    }

    // Balance operations
    @GetMapping("/balance/{accountNumber}")
    public ResponseEntity<Map<String, Object>> getBalance(@PathVariable String accountNumber) {
        System.out.println("=== GETTING BALANCE ===");
        System.out.println("Account Number: " + accountNumber);
        
        Double balance = accountService.getBalanceByAccountNumber(accountNumber);
        System.out.println("Balance from service: " + balance);
        
        Map<String, Object> response = new HashMap<>();
        if (balance != null) {
            response.put("balance", balance);
            response.put("accountNumber", accountNumber);
            System.out.println("✅ Balance found: " + balance);
            return ResponseEntity.ok(response);
        } else {
            response.put("error", "Account not found");
            System.out.println("❌ Account not found: " + accountNumber);
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/balance/update/{accountNumber}")
    public ResponseEntity<Account> updateBalance(
            @PathVariable String accountNumber,
            @RequestParam Double amount) {
        Account account = accountService.updateBalance(accountNumber, amount);
        return account != null ? ResponseEntity.ok(account) : ResponseEntity.notFound().build();
    }

    @PutMapping("/balance/debit/{accountNumber}")
    public ResponseEntity<Map<String, Object>> debitBalance(
            @PathVariable String accountNumber,
            @RequestParam Double amount) {
        Double newBalance = accountService.debitBalance(accountNumber, amount);
        Map<String, Object> response = new HashMap<>();
        if (newBalance != null) {
            response.put("accountNumber", accountNumber);
            response.put("balance", newBalance);
            response.put("message", "Balance debited successfully");
            return ResponseEntity.ok(response);
        } else {
            response.put("error", "Account not found or insufficient balance");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/balance/credit/{accountNumber}")
    public ResponseEntity<Map<String, Object>> creditBalance(
            @PathVariable String accountNumber,
            @RequestParam Double amount) {
        System.out.println("=== CREDITING BALANCE ===");
        System.out.println("Account Number: " + accountNumber);
        System.out.println("Amount to credit: " + amount);
        
        Double newBalance = accountService.creditBalance(accountNumber, amount);
        Map<String, Object> response = new HashMap<>();
        
        if (newBalance != null) {
            System.out.println("✅ Credit successful. New balance: " + newBalance);
            response.put("accountNumber", accountNumber);
            response.put("balance", newBalance);
            response.put("message", "Balance credited successfully");
            return ResponseEntity.ok(response);
        } else {
            System.out.println("❌ Account not found: " + accountNumber);
            response.put("error", "Account not found");
            return ResponseEntity.status(404).body(response);
        }
    }

    // Statistics endpoints
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getAccountStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalAccounts", accountService.getTotalAccountsCount());
        stats.put("activeAccounts", accountService.getAccountsCountByStatus("ACTIVE"));
        stats.put("inactiveAccounts", accountService.getAccountsCountByStatus("INACTIVE"));
        stats.put("closedAccounts", accountService.getAccountsCountByStatus("CLOSED"));
        stats.put("kycVerifiedAccounts", accountService.getKycVerifiedAccountsCount());
        stats.put("verifiedMatrixAccounts", accountService.getVerifiedMatrixAccountsCount());
        stats.put("averageBalance", accountService.getAverageBalanceByStatus("ACTIVE"));
        stats.put("totalBalance", accountService.getTotalBalanceByStatus("ACTIVE"));
        stats.put("averageIncome", accountService.getAverageIncomeByStatus("ACTIVE"));
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/recent")
    public ResponseEntity<List<Account>> getRecentAccounts(@RequestParam(defaultValue = "5") int limit) {
        List<Account> accounts = accountService.getRecentAccounts(limit);
        return ResponseEntity.ok(accounts);
    }

    // Validation endpoints
    @GetMapping("/validate/pan/{pan}")
    public ResponseEntity<Map<String, Boolean>> validatePan(@PathVariable String pan) {
        Map<String, Boolean> response = new HashMap<>();
        response.put("isUnique", accountService.isPanUnique(pan));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/validate/aadhar/{aadharNumber}")
    public ResponseEntity<Map<String, Boolean>> validateAadhar(@PathVariable String aadharNumber) {
        Map<String, Boolean> response = new HashMap<>();
        response.put("isUnique", accountService.isAadharUnique(aadharNumber));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/validate/phone/{phone}")
    public ResponseEntity<Map<String, Boolean>> validatePhone(@PathVariable String phone) {
        Map<String, Boolean> response = new HashMap<>();
        response.put("isUnique", accountService.isPhoneUnique(phone));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/validate/account-number/{accountNumber}")
    public ResponseEntity<Map<String, Boolean>> validateAccountNumber(@PathVariable String accountNumber) {
        Map<String, Boolean> response = new HashMap<>();
        response.put("isUnique", accountService.isAccountNumberUnique(accountNumber));
        return ResponseEntity.ok(response);
    }

    // Aadhaar verification endpoints
    @PostMapping("/aadhar/verify/{accountNumber}")
    public ResponseEntity<Map<String, Object>> initiateAadharVerification(@PathVariable String accountNumber) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Account account = accountService.getAccountByNumber(accountNumber);
            if (account == null) {
                response.put("success", false);
                response.put("message", "Account not found");
                return ResponseEntity.notFound().build();
            }
            
            if (account.getAadharNumber() == null || account.getAadharNumber().isEmpty()) {
                response.put("success", false);
                response.put("message", "Aadhaar number not found in profile");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Generate verification reference
            String verificationRef = "AADHAR_" + System.currentTimeMillis() + "_" + accountNumber;
            
            // Create verification URL (in production, this would be UIDAI's e-KYC API URL)
            // For demo purposes, we'll use a callback URL that simulates the verification
            String baseUrl = (allowedOrigins != null && !allowedOrigins.trim().isEmpty()) 
                ? allowedOrigins.split(",")[0].trim() 
                : "";
            String callbackUrl = baseUrl + "/website/profile?aadhar_callback=" + verificationRef;
            
            // In production, this would redirect to UIDAI's Aadhaar verification portal
            // For demo: https://uidai.gov.in/en/ (UIDAI official website)
            String aadharVerificationUrl = "https://uidai.gov.in/en/";
            
            // Update account with verification reference
            account.setAadharVerificationReference(verificationRef);
            account.setAadharVerificationStatus("PENDING");
            accountService.saveAccount(account);
            
            response.put("success", true);
            response.put("verificationReference", verificationRef);
            response.put("verificationUrl", aadharVerificationUrl);
            response.put("callbackUrl", callbackUrl);
            response.put("message", "Aadhaar verification initiated. Please complete verification on Aadhaar website.");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to initiate Aadhaar verification: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/aadhar/callback")
    public ResponseEntity<Map<String, Object>> aadharVerificationCallback(
            @RequestParam String verificationReference,
            @RequestParam String status,
            @RequestParam(required = false) String aadharNumber,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String dob) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Find account by verification reference
            Account account = accountService.getAccountByVerificationReference(verificationReference);
            if (account == null) {
                response.put("success", false);
                response.put("message", "Invalid verification reference");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Update verification status
            if ("VERIFIED".equals(status) || "SUCCESS".equals(status)) {
                account.setAadharVerified(true);
                account.setAadharVerificationStatus("VERIFIED");
                account.setAadharVerifiedDate(LocalDateTime.now());
                
                // Update account details if provided from Aadhaar
                if (name != null && !name.isEmpty()) {
                    account.setName(name);
                }
                if (dob != null && !dob.isEmpty()) {
                    account.setDob(dob);
                }
                
                account.setLastUpdated(LocalDateTime.now());
                accountService.saveAccount(account);
                
                response.put("success", true);
                response.put("message", "Aadhaar verified successfully");
                response.put("accountNumber", account.getAccountNumber());
            } else {
                account.setAadharVerified(false);
                account.setAadharVerificationStatus("FAILED");
                account.setLastUpdated(LocalDateTime.now());
                accountService.saveAccount(account);
                
                response.put("success", false);
                response.put("message", "Aadhaar verification failed");
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to process verification callback: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/aadhar/status/{accountNumber}")
    public ResponseEntity<Map<String, Object>> getAadharVerificationStatus(@PathVariable String accountNumber) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Account account = accountService.getAccountByNumber(accountNumber);
            if (account == null) {
                response.put("success", false);
                response.put("message", "Account not found");
                return ResponseEntity.notFound().build();
            }
            
            response.put("success", true);
            response.put("aadharVerified", account.isAadharVerified());
            response.put("verificationStatus", account.getAadharVerificationStatus());
            response.put("verifiedDate", account.getAadharVerifiedDate());
            response.put("verificationReference", account.getAadharVerificationReference());
            response.put("aadharNumber", account.getAadharNumber());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to get verification status: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
