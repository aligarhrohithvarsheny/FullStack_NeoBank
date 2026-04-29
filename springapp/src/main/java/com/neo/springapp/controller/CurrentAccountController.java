package com.neo.springapp.controller;

import com.neo.springapp.model.CurrentAccount;
import com.neo.springapp.model.BusinessTransaction;
import com.neo.springapp.model.CurrentAccountEditHistory;
import com.neo.springapp.model.CurrentAccountInvoice;
import com.neo.springapp.model.CurrentAccountBusinessLoan;
import com.neo.springapp.model.CurrentAccountBusinessUser;
import com.neo.springapp.model.LinkedAccount;
import com.neo.springapp.service.CurrentAccountService;
import com.neo.springapp.service.SessionHistoryService;
import com.neo.springapp.repository.NetBankingServiceControlRepository;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/current-accounts")
public class CurrentAccountController {

    private final CurrentAccountService currentAccountService;
    private final SessionHistoryService sessionHistoryService;
    private final NetBankingServiceControlRepository netBankingServiceControlRepository;

    public CurrentAccountController(CurrentAccountService currentAccountService, SessionHistoryService sessionHistoryService, NetBankingServiceControlRepository netBankingServiceControlRepository) {
        this.currentAccountService = currentAccountService;
        this.sessionHistoryService = sessionHistoryService;
        this.netBankingServiceControlRepository = netBankingServiceControlRepository;
    }

    // ==================== Account CRUD ====================

    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createAccount(@RequestBody CurrentAccount account) {
        Map<String, Object> response = new HashMap<>();
        try {
            // Validate unique fields
            if (account.getGstNumber() != null && !account.getGstNumber().isEmpty()) {
                if (!currentAccountService.isGstUnique(account.getGstNumber())) {
                    response.put("success", false);
                    response.put("error", "GST number is already registered.");
                    return ResponseEntity.badRequest().body(response);
                }
            }
            if (account.getPanNumber() != null && !account.getPanNumber().isEmpty()) {
                if (!currentAccountService.isPanUnique(account.getPanNumber())) {
                    response.put("success", false);
                    response.put("error", "PAN number is already registered.");
                    return ResponseEntity.badRequest().body(response);
                }
            }
            if (account.getMobile() != null && !account.getMobile().isEmpty()) {
                if (!currentAccountService.isMobileUnique(account.getMobile())) {
                    response.put("success", false);
                    response.put("error", "Mobile number is already registered.");
                    return ResponseEntity.badRequest().body(response);
                }
            }

            CurrentAccount saved = currentAccountService.createAccount(account);
            response.put("success", true);
            response.put("account", saved);
            response.put("message", "Current account created successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Account creation failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping
    public ResponseEntity<List<CurrentAccount>> getAllAccounts() {
        return ResponseEntity.ok(currentAccountService.getAllAccounts());
    }

    @GetMapping("/paginated")
    public ResponseEntity<Page<CurrentAccount>> getAllAccountsPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return ResponseEntity.ok(currentAccountService.getAllAccountsPaginated(page, size, sortBy, sortDir));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CurrentAccount> getAccountById(@PathVariable Long id) {
        return currentAccountService.getAccountById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/number/{accountNumber}")
    public ResponseEntity<CurrentAccount> getAccountByNumber(@PathVariable String accountNumber) {
        return currentAccountService.getAccountByNumber(accountNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<CurrentAccount> getAccountByCustomerId(@PathVariable String customerId) {
        return currentAccountService.getAccountByCustomerId(customerId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<CurrentAccount>> getAccountsByStatus(@PathVariable String status) {
        return ResponseEntity.ok(currentAccountService.getAccountsByStatus(status));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<CurrentAccount>> searchAccounts(
            @RequestParam String searchTerm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(currentAccountService.searchAccounts(searchTerm, page, size));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<Map<String, Object>> updateAccount(@PathVariable Long id, @RequestBody CurrentAccount details) {
        Map<String, Object> response = new HashMap<>();
        CurrentAccount updated = currentAccountService.updateAccount(id, details);
        if (updated != null) {
            response.put("success", true);
            response.put("account", updated);
            return ResponseEntity.ok(response);
        }
        response.put("success", false);
        response.put("error", "Account not found");
        return ResponseEntity.notFound().build();
    }

    // ==================== Account Operations ====================

    @PutMapping("/approve/{id}")
    public ResponseEntity<Map<String, Object>> approveAccount(@PathVariable Long id,
                                                               @RequestParam String approvedBy) {
        Map<String, Object> response = new HashMap<>();
        CurrentAccount approved = currentAccountService.approveAccount(id, approvedBy);
        if (approved != null) {
            response.put("success", true);
            response.put("account", approved);
            response.put("message", "Account approved successfully");
            return ResponseEntity.ok(response);
        }
        response.put("success", false);
        response.put("error", "Account not found or already approved");
        return ResponseEntity.badRequest().body(response);
    }

    @PutMapping("/reject/{id}")
    public ResponseEntity<Map<String, Object>> rejectAccount(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        CurrentAccount rejected = currentAccountService.rejectAccount(id);
        if (rejected != null) {
            response.put("success", true);
            response.put("account", rejected);
            response.put("message", "Account rejected");
            return ResponseEntity.ok(response);
        }
        response.put("success", false);
        response.put("error", "Account not found or cannot be rejected");
        return ResponseEntity.badRequest().body(response);
    }

    @PutMapping("/verify-kyc/{id}")
    public ResponseEntity<Map<String, Object>> verifyKyc(@PathVariable Long id,
                                                          @RequestParam String verifiedBy) {
        Map<String, Object> response = new HashMap<>();
        CurrentAccount verified = currentAccountService.verifyKyc(id, verifiedBy);
        if (verified != null) {
            response.put("success", true);
            response.put("account", verified);
            response.put("message", "KYC verified successfully");
            return ResponseEntity.ok(response);
        }
        response.put("success", false);
        response.put("error", "Account not found");
        return ResponseEntity.badRequest().body(response);
    }

    @PutMapping("/freeze/{id}")
    public ResponseEntity<Map<String, Object>> freezeAccount(@PathVariable Long id,
                                                              @RequestParam String reason,
                                                              @RequestParam String frozenBy) {
        Map<String, Object> response = new HashMap<>();
        CurrentAccount frozen = currentAccountService.freezeAccount(id, reason, frozenBy);
        if (frozen != null) {
            response.put("success", true);
            response.put("account", frozen);
            response.put("message", "Account frozen successfully");
            return ResponseEntity.ok(response);
        }
        response.put("success", false);
        response.put("error", "Account not found or already frozen");
        return ResponseEntity.badRequest().body(response);
    }

    @PutMapping("/unfreeze/{id}")
    public ResponseEntity<Map<String, Object>> unfreezeAccount(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        CurrentAccount unfrozen = currentAccountService.unfreezeAccount(id);
        if (unfrozen != null) {
            response.put("success", true);
            response.put("account", unfrozen);
            response.put("message", "Account unfrozen successfully");
            return ResponseEntity.ok(response);
        }
        response.put("success", false);
        response.put("error", "Account not found or not frozen");
        return ResponseEntity.badRequest().body(response);
    }

    @PutMapping("/approve-overdraft/{id}")
    public ResponseEntity<Map<String, Object>> approveOverdraft(@PathVariable Long id,
                                                                 @RequestParam Double overdraftLimit) {
        Map<String, Object> response = new HashMap<>();
        CurrentAccount account = currentAccountService.approveOverdraft(id, overdraftLimit);
        if (account != null) {
            response.put("success", true);
            response.put("account", account);
            response.put("message", "Overdraft approved with limit: ₹" + overdraftLimit);
            return ResponseEntity.ok(response);
        }
        response.put("success", false);
        response.put("error", "Account not found or not active");
        return ResponseEntity.badRequest().body(response);
    }

    // ==================== Statistics ====================

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        return ResponseEntity.ok(currentAccountService.getStatistics());
    }

    // ==================== Transactions ====================

    @PostMapping("/transactions/process")
    public ResponseEntity<Map<String, Object>> processTransaction(
            @RequestParam String accountNumber,
            @RequestParam String txnType,
            @RequestParam Double amount,
            @RequestParam(required = false) String description) {
        Map<String, Object> response = new HashMap<>();
        BusinessTransaction txn = currentAccountService.processTransaction(accountNumber, txnType, amount, description);
        if (txn != null) {
            response.put("success", true);
            response.put("transaction", txn);
            response.put("message", txnType + " of ₹" + amount + " completed successfully");
            return ResponseEntity.ok(response);
        }
        response.put("success", false);
        response.put("error", "Transaction failed. Check account status and balance.");
        return ResponseEntity.badRequest().body(response);
    }

    // ==================== Verify Recipient Account ====================
    @GetMapping("/verify-recipient/{accountNumber}")
    public ResponseEntity<Map<String, Object>> verifyRecipient(@PathVariable String accountNumber) {
        return ResponseEntity.ok(currentAccountService.verifyRecipientAccount(accountNumber));
    }

    @PostMapping("/transactions/transfer")
    public ResponseEntity<Map<String, Object>> processTransfer(@RequestBody Map<String, Object> transferRequest) {
        Map<String, Object> response = new HashMap<>();
        try {
            String fromAccount = (String) transferRequest.get("fromAccount");
            String toAccount = (String) transferRequest.get("toAccount");
            String recipientName = (String) transferRequest.get("recipientName");
            Double amount = Double.valueOf(transferRequest.get("amount").toString());
            String transferType = (String) transferRequest.get("transferType");
            String description = (String) transferRequest.get("description");

            BusinessTransaction txn = currentAccountService.processTransfer(
                    fromAccount, toAccount, recipientName, amount, transferType, description);
            if (txn != null) {
                response.put("success", true);
                response.put("transaction", txn);
                response.put("message", transferType + " transfer of ₹" + amount + " completed successfully");
                return ResponseEntity.ok(response);
            }
            response.put("success", false);
            response.put("error", "Transfer failed. Check account status & balance.");
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Transfer failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/transactions/{accountNumber}")
    public ResponseEntity<List<BusinessTransaction>> getTransactions(@PathVariable String accountNumber) {
        return ResponseEntity.ok(currentAccountService.getTransactions(accountNumber));
    }

    @GetMapping("/transactions/{accountNumber}/paginated")
    public ResponseEntity<Page<BusinessTransaction>> getTransactionsPaginated(
            @PathVariable String accountNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(currentAccountService.getTransactionsPaginated(accountNumber, page, size));
    }

    @GetMapping("/transactions/{accountNumber}/summary")
    public ResponseEntity<Map<String, Object>> getTransactionSummary(@PathVariable String accountNumber) {
        return ResponseEntity.ok(currentAccountService.getTransactionSummary(accountNumber));
    }

    @GetMapping("/transactions/{accountNumber}/recent")
    public ResponseEntity<List<BusinessTransaction>> getRecentTransactions(
            @PathVariable String accountNumber,
            @RequestParam(defaultValue = "5") int count) {
        return ResponseEntity.ok(currentAccountService.getRecentTransactions(accountNumber, count));
    }

    @GetMapping("/transactions/{accountNumber}/date-range")
    public ResponseEntity<List<BusinessTransaction>> getTransactionsByDateRange(
            @PathVariable String accountNumber,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime start = LocalDateTime.parse(startDate + " 00:00:00", formatter);
        LocalDateTime end = LocalDateTime.parse(endDate + " 23:59:59", formatter);
        return ResponseEntity.ok(currentAccountService.getTransactionsByDateRange(accountNumber, start, end));
    }

    @GetMapping("/transactions/search")
    public ResponseEntity<Page<BusinessTransaction>> searchTransactions(
            @RequestParam String searchTerm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(currentAccountService.searchTransactions(searchTerm, page, size));
    }

    // ==================== Edit with History & Document Upload ====================

    @PutMapping("/edit-with-history/{id}")
    public ResponseEntity<Map<String, Object>> editAccountWithHistory(
            @PathVariable Long id,
            @RequestPart("account") CurrentAccount details,
            @RequestPart(value = "document", required = false) MultipartFile document,
            @RequestParam String editedBy) {
        Map<String, Object> response = new HashMap<>();
        try {
            String documentPath = null;
            String documentName = null;

            if (document != null && !document.isEmpty()) {
                String originalFilename = document.getOriginalFilename();
                if (originalFilename != null) {
                    String extension = "";
                    int dotIdx = originalFilename.lastIndexOf('.');
                    if (dotIdx > 0) {
                        extension = originalFilename.substring(dotIdx);
                    }
                    // Only allow PDF files
                    if (!".pdf".equalsIgnoreCase(extension)) {
                        response.put("success", false);
                        response.put("error", "Only PDF documents are allowed");
                        return ResponseEntity.badRequest().body(response);
                    }
                }
                documentPath = saveEditDocument(document, id);
                documentName = document.getOriginalFilename();
            }

            CurrentAccount updated = currentAccountService.updateAccountWithHistory(id, details, editedBy, documentPath, documentName);
            if (updated != null) {
                response.put("success", true);
                response.put("account", updated);
                response.put("message", "Account updated successfully. Edit history saved.");
                return ResponseEntity.ok(response);
            }
            response.put("success", false);
            response.put("error", "Account not found");
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Update failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/edit-history/{accountId}")
    public ResponseEntity<List<CurrentAccountEditHistory>> getEditHistory(@PathVariable Long accountId) {
        return ResponseEntity.ok(currentAccountService.getEditHistory(accountId));
    }

    @GetMapping("/edit-history/account/{accountNumber}")
    public ResponseEntity<List<CurrentAccountEditHistory>> getEditHistoryByAccountNumber(@PathVariable String accountNumber) {
        return ResponseEntity.ok(currentAccountService.getEditHistoryByAccountNumber(accountNumber));
    }

    @GetMapping("/edit-document/{filename}")
    public ResponseEntity<byte[]> getEditDocument(@PathVariable String filename) {
        try {
            Path filePath = Paths.get("uploads/current-account-edits", filename);
            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }
            byte[] fileContent = Files.readAllBytes(filePath);
            return ResponseEntity.ok()
                    .header("Content-Type", "application/pdf")
                    .header("Content-Disposition", "inline; filename=\"" + filename + "\"")
                    .body(fileContent);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // ==================== Authentication ====================

    @PostMapping("/signup")
    public ResponseEntity<Map<String, Object>> signup(@RequestBody Map<String, String> body) {
        String customerId = body.get("customerId");
        String password = body.get("password");
        if (customerId == null || password == null || customerId.isEmpty() || password.isEmpty()) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "Customer ID and password are required");
            return ResponseEntity.badRequest().body(err);
        }
        if (password.length() < 6) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "Password must be at least 6 characters");
            return ResponseEntity.badRequest().body(err);
        }
        Map<String, Object> result = currentAccountService.signup(customerId, password);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> body,
            @RequestHeader(value = "X-Forwarded-For", required = false) String forwardedFor,
            @RequestHeader(value = "User-Agent", required = false) String userAgent) {
        String accountNumber = body.get("accountNumber");
        String password = body.get("password");
        if (accountNumber == null || password == null || accountNumber.isEmpty() || password.isEmpty()) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "Account number and password are required");
            return ResponseEntity.badRequest().body(err);
        }
        // Check if Current Account net banking is enabled
        try {
            var currentControl = netBankingServiceControlRepository.findByServiceType("CURRENT_ACCOUNT");
            if (currentControl.isPresent() && !currentControl.get().getEnabled()) {
                Map<String, Object> err = new HashMap<>();
                err.put("success", false);
                err.put("netBankingDisabled", true);
                err.put("message", "Net Banking for Current Account is currently disabled by the administrator. Please try again later.");
                return ResponseEntity.badRequest().body(err);
            }
        } catch (Exception e) {
            System.err.println("Error checking net banking status: " + e.getMessage());
        }
        Map<String, Object> result = currentAccountService.authenticate(accountNumber, password);
        // Check per-customer net banking status on successful login
        if (result.containsKey("success") && Boolean.TRUE.equals(result.get("success"))) {
            try {
                Object accountObj = result.get("account");
                CurrentAccount ca = null;
                if (accountObj instanceof CurrentAccount) {
                    ca = (CurrentAccount) accountObj;
                }
                if (ca != null && ca.getNetBankingEnabled() != null && !ca.getNetBankingEnabled()) {
                    Map<String, Object> err = new HashMap<>();
                    err.put("success", false);
                    err.put("netBankingDisabled", true);
                    err.put("message", "Net Banking for your account has been disabled by the administrator. Please contact the bank for assistance.");
                    return ResponseEntity.badRequest().body(err);
                }
            } catch (Exception e) {
                System.err.println("Error checking per-customer net banking status: " + e.getMessage());
            }
        }
        // Record session history on successful login
        if (result.containsKey("success") && Boolean.TRUE.equals(result.get("success"))) {
            try {
                String clientIp = forwardedFor != null ? forwardedFor.split(",")[0].trim() : "Unknown";
                String deviceInfo = userAgent != null ? userAgent : "Unknown";
                Object accountObj = result.get("account");
                Long accountId = null;
                String email = "";
                String ownerName = "Current Account User";
                if (accountObj instanceof CurrentAccount) {
                    CurrentAccount ca = (CurrentAccount) accountObj;
                    accountId = ca.getId();
                    email = ca.getEmail() != null ? ca.getEmail() : "";
                    ownerName = ca.getOwnerName() != null ? ca.getOwnerName() : ownerName;
                } else if (accountObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> accMap = (Map<String, Object>) accountObj;
                    accountId = accMap.get("id") != null ? Long.parseLong(accMap.get("id").toString()) : null;
                    email = accMap.get("email") != null ? accMap.get("email").toString() : "";
                    ownerName = accMap.get("ownerName") != null ? accMap.get("ownerName").toString() : ownerName;
                }
                sessionHistoryService.recordCurrentAccountLogin(
                    accountId != null ? accountId : 0L, email, accountNumber, ownerName,
                    "Unknown", clientIp, deviceInfo, "PASSWORD");
            } catch (Exception e) {
                System.err.println("Error recording current account session: " + e.getMessage());
            }
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/verify-customer/{customerId}")
    public ResponseEntity<Map<String, Object>> verifyCustomer(@PathVariable String customerId) {
        Map<String, Object> response = new HashMap<>();
        Optional<CurrentAccount> opt = currentAccountService.getAccountByCustomerId(customerId);
        if (opt.isPresent()) {
            CurrentAccount account = opt.get();
            response.put("exists", true);
            response.put("ownerName", account.getOwnerName());
            response.put("businessName", account.getBusinessName());
            response.put("accountNumber", account.getAccountNumber());
            response.put("passwordSet", account.getPasswordSet());
            response.put("status", account.getStatus());
        } else {
            response.put("exists", false);
        }
        return ResponseEntity.ok(response);
    }

    // ==================== User Dashboard Endpoints ====================

    @PutMapping("/change-password/{accountNumber}")
    public ResponseEntity<Map<String, Object>> changePassword(
            @PathVariable String accountNumber,
            @RequestBody Map<String, String> body) {
        String currentPassword = body.get("currentPassword");
        String newPassword = body.get("newPassword");
        if (currentPassword == null || newPassword == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "currentPassword and newPassword are required");
            return ResponseEntity.badRequest().body(err);
        }
        Map<String, Object> result = currentAccountService.changePassword(accountNumber, currentPassword, newPassword);
        if (Boolean.TRUE.equals(result.get("success"))) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.badRequest().body(result);
    }

    @GetMapping("/user/dashboard-stats/{accountNumber}")
    public ResponseEntity<Map<String, Object>> getUserDashboardStats(@PathVariable String accountNumber) {
        Map<String, Object> stats = currentAccountService.getUserDashboardStats(accountNumber);
        if (stats.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(stats);
    }

    @PutMapping("/user/update-profile/{accountNumber}")
    public ResponseEntity<Map<String, Object>> updateBusinessProfile(
            @PathVariable String accountNumber,
            @RequestBody Map<String, String> updates) {
        Map<String, Object> response = new HashMap<>();
        CurrentAccount updated = currentAccountService.updateBusinessProfile(accountNumber, updates);
        if (updated != null) {
            response.put("success", true);
            response.put("account", updated);
            response.put("message", "Profile updated successfully");
            return ResponseEntity.ok(response);
        }
        response.put("success", false);
        response.put("error", "Account not found");
        return ResponseEntity.badRequest().body(response);
    }

    // ==================== Beneficiary Endpoints ====================

    @PostMapping("/beneficiaries/add")
    public ResponseEntity<Map<String, Object>> addBeneficiary(
            @RequestBody com.neo.springapp.model.CurrentAccountBeneficiary beneficiary) {
        Map<String, Object> response = new HashMap<>();
        com.neo.springapp.model.CurrentAccountBeneficiary saved = currentAccountService.addBeneficiary(beneficiary);
        response.put("success", true);
        response.put("beneficiary", saved);
        response.put("message", "Beneficiary added successfully");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/beneficiaries/{accountNumber}")
    public ResponseEntity<List<com.neo.springapp.model.CurrentAccountBeneficiary>> getBeneficiaries(
            @PathVariable String accountNumber) {
        return ResponseEntity.ok(currentAccountService.getBeneficiaries(accountNumber));
    }

    @GetMapping("/beneficiaries/{accountNumber}/active")
    public ResponseEntity<List<com.neo.springapp.model.CurrentAccountBeneficiary>> getActiveBeneficiaries(
            @PathVariable String accountNumber) {
        return ResponseEntity.ok(currentAccountService.getActiveBeneficiaries(accountNumber));
    }

    @DeleteMapping("/beneficiaries/{id}")
    public ResponseEntity<Map<String, Object>> deleteBeneficiary(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        com.neo.springapp.model.CurrentAccountBeneficiary deleted = currentAccountService.deleteBeneficiary(id);
        if (deleted != null) {
            response.put("success", true);
            response.put("message", "Beneficiary removed successfully");
            return ResponseEntity.ok(response);
        }
        response.put("success", false);
        response.put("error", "Beneficiary not found");
        return ResponseEntity.badRequest().body(response);
    }

    // ==================== Cheque Request Endpoints ====================

    @PostMapping("/cheque-requests/create")
    public ResponseEntity<Map<String, Object>> requestChequeBook(
            @RequestBody com.neo.springapp.model.CurrentAccountChequeRequest request) {
        Map<String, Object> response = new HashMap<>();
        com.neo.springapp.model.CurrentAccountChequeRequest saved = currentAccountService.requestChequeBook(request);
        response.put("success", true);
        response.put("request", saved);
        response.put("message", "Cheque book request submitted successfully");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/cheque-requests/{accountNumber}")
    public ResponseEntity<List<com.neo.springapp.model.CurrentAccountChequeRequest>> getChequeRequests(
            @PathVariable String accountNumber) {
        return ResponseEntity.ok(currentAccountService.getChequeRequests(accountNumber));
    }

    @GetMapping("/cheque-requests/pending")
    public ResponseEntity<List<com.neo.springapp.model.CurrentAccountChequeRequest>> getPendingChequeRequests() {
        return ResponseEntity.ok(currentAccountService.getPendingChequeRequests());
    }

    @PutMapping("/cheque-requests/approve/{id}")
    public ResponseEntity<Map<String, Object>> approveChequeRequest(
            @PathVariable Long id,
            @RequestParam String approvedBy) {
        Map<String, Object> response = new HashMap<>();
        com.neo.springapp.model.CurrentAccountChequeRequest approved = currentAccountService.approveChequeRequest(id, approvedBy);
        if (approved != null) {
            response.put("success", true);
            response.put("request", approved);
            response.put("message", "Cheque book request approved");
            return ResponseEntity.ok(response);
        }
        response.put("success", false);
        response.put("error", "Request not found or already processed");
        return ResponseEntity.badRequest().body(response);
    }

    // ==================== Vendor Payment Endpoints ====================

    @PostMapping("/vendor-payments/process")
    public ResponseEntity<Map<String, Object>> processVendorPayment(
            @RequestBody com.neo.springapp.model.CurrentAccountVendorPayment payment) {
        Map<String, Object> response = new HashMap<>();
        com.neo.springapp.model.CurrentAccountVendorPayment result = currentAccountService.processVendorPayment(payment);
        if (result != null) {
            response.put("success", true);
            response.put("payment", result);
            response.put("message", "Vendor payment of ₹" + payment.getAmount() + " processed successfully");
            return ResponseEntity.ok(response);
        }
        response.put("success", false);
        response.put("error", "Payment failed. Check account status and balance.");
        return ResponseEntity.badRequest().body(response);
    }

    @GetMapping("/vendor-payments/{accountNumber}")
    public ResponseEntity<List<com.neo.springapp.model.CurrentAccountVendorPayment>> getVendorPayments(
            @PathVariable String accountNumber) {
        return ResponseEntity.ok(currentAccountService.getVendorPayments(accountNumber));
    }

    private String saveEditDocument(MultipartFile file, Long accountId) throws IOException {
        String uploadDir = "uploads/current-account-edits";
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String filename = "edit_acc" + accountId + "_" + System.currentTimeMillis() + ".pdf";
        Path filePath = Paths.get(uploadDir, filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        return "/api/current-accounts/edit-document/" + filename;
    }

    // ==================== Invoice Management ====================

    @PostMapping("/invoices/create")
    public ResponseEntity<Map<String, Object>> createInvoice(@RequestBody CurrentAccountInvoice invoice) {
        Map<String, Object> response = new HashMap<>();
        try {
            CurrentAccountInvoice saved = currentAccountService.createInvoice(invoice);
            response.put("success", true);
            response.put("invoice", saved);
            response.put("message", "Invoice " + saved.getInvoiceNumber() + " created successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Failed to create invoice: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/invoices/{accountNumber}")
    public ResponseEntity<List<CurrentAccountInvoice>> getInvoices(@PathVariable String accountNumber) {
        return ResponseEntity.ok(currentAccountService.getInvoices(accountNumber));
    }

    @GetMapping("/invoices/number/{invoiceNumber}")
    public ResponseEntity<CurrentAccountInvoice> getInvoiceByNumber(@PathVariable String invoiceNumber) {
        return currentAccountService.getInvoiceByNumber(invoiceNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/invoices/status/{id}")
    public ResponseEntity<Map<String, Object>> updateInvoiceStatus(
            @PathVariable Long id, @RequestParam String status) {
        Map<String, Object> response = new HashMap<>();
        CurrentAccountInvoice updated = currentAccountService.updateInvoiceStatus(id, status);
        if (updated != null) {
            response.put("success", true);
            response.put("invoice", updated);
            return ResponseEntity.ok(response);
        }
        response.put("success", false);
        response.put("error", "Invoice not found");
        return ResponseEntity.badRequest().body(response);
    }

    @PutMapping("/invoices/mark-paid/{id}")
    public ResponseEntity<Map<String, Object>> markInvoicePaid(
            @PathVariable Long id, @RequestParam Double paidAmount) {
        Map<String, Object> response = new HashMap<>();
        CurrentAccountInvoice updated = currentAccountService.markInvoicePaid(id, paidAmount);
        if (updated != null) {
            response.put("success", true);
            response.put("invoice", updated);
            return ResponseEntity.ok(response);
        }
        response.put("success", false);
        response.put("error", "Invoice not found");
        return ResponseEntity.badRequest().body(response);
    }

    @DeleteMapping("/invoices/{id}")
    public ResponseEntity<Map<String, Object>> deleteInvoice(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        currentAccountService.deleteInvoice(id);
        response.put("success", true);
        response.put("message", "Invoice deleted");
        return ResponseEntity.ok(response);
    }

    // ==================== Business Loan Application ====================

    @PostMapping("/business-loans/apply")
    public ResponseEntity<Map<String, Object>> applyForBusinessLoan(@RequestBody CurrentAccountBusinessLoan loan) {
        Map<String, Object> result = currentAccountService.applyForBusinessLoan(loan);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/business-loans/{accountNumber}")
    public ResponseEntity<List<CurrentAccountBusinessLoan>> getBusinessLoans(@PathVariable String accountNumber) {
        return ResponseEntity.ok(currentAccountService.getBusinessLoans(accountNumber));
    }

    @GetMapping("/business-loans/pending")
    public ResponseEntity<List<CurrentAccountBusinessLoan>> getPendingBusinessLoans() {
        return ResponseEntity.ok(currentAccountService.getPendingBusinessLoans());
    }

    @PutMapping("/business-loans/approve/{id}")
    public ResponseEntity<Map<String, Object>> approveBusinessLoan(
            @PathVariable Long id,
            @RequestParam String approvedBy,
            @RequestParam Double approvedAmount,
            @RequestParam Double interestRate) {
        Map<String, Object> response = new HashMap<>();
        CurrentAccountBusinessLoan loan = currentAccountService.approveBusinessLoan(id, approvedBy, approvedAmount, interestRate);
        if (loan != null) {
            response.put("success", true);
            response.put("loan", loan);
            return ResponseEntity.ok(response);
        }
        response.put("success", false);
        response.put("error", "Loan not found");
        return ResponseEntity.badRequest().body(response);
    }

    @PutMapping("/business-loans/reject/{id}")
    public ResponseEntity<Map<String, Object>> rejectBusinessLoan(
            @PathVariable Long id,
            @RequestParam String rejectedBy,
            @RequestParam String reason) {
        Map<String, Object> response = new HashMap<>();
        CurrentAccountBusinessLoan loan = currentAccountService.rejectBusinessLoan(id, rejectedBy, reason);
        if (loan != null) {
            response.put("success", true);
            response.put("loan", loan);
            return ResponseEntity.ok(response);
        }
        response.put("success", false);
        response.put("error", "Loan not found");
        return ResponseEntity.badRequest().body(response);
    }

    // ==================== Multi-User Access ====================

    @PostMapping("/business-users/add")
    public ResponseEntity<Map<String, Object>> addBusinessUser(@RequestBody CurrentAccountBusinessUser user) {
        Map<String, Object> result = currentAccountService.addBusinessUser(user);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/business-users/{accountNumber}")
    public ResponseEntity<List<CurrentAccountBusinessUser>> getBusinessUsers(@PathVariable String accountNumber) {
        return ResponseEntity.ok(currentAccountService.getBusinessUsers(accountNumber));
    }

    @PutMapping("/business-users/role/{id}")
    public ResponseEntity<Map<String, Object>> updateUserRole(
            @PathVariable Long id, @RequestParam String role) {
        Map<String, Object> response = new HashMap<>();
        CurrentAccountBusinessUser user = currentAccountService.updateUserRole(id, role);
        if (user != null) {
            response.put("success", true);
            response.put("user", user);
            return ResponseEntity.ok(response);
        }
        response.put("success", false);
        response.put("error", "User not found");
        return ResponseEntity.badRequest().body(response);
    }

    @PutMapping("/business-users/status/{id}")
    public ResponseEntity<Map<String, Object>> updateUserStatus(
            @PathVariable Long id, @RequestParam String status) {
        Map<String, Object> response = new HashMap<>();
        CurrentAccountBusinessUser user = currentAccountService.updateUserStatus(id, status);
        if (user != null) {
            response.put("success", true);
            response.put("user", user);
            return ResponseEntity.ok(response);
        }
        response.put("success", false);
        response.put("error", "User not found");
        return ResponseEntity.badRequest().body(response);
    }

    @DeleteMapping("/business-users/{id}")
    public ResponseEntity<Map<String, Object>> removeBusinessUser(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        currentAccountService.removeBusinessUser(id);
        response.put("success", true);
        response.put("message", "User removed successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/business-users/login")
    public ResponseEntity<Map<String, Object>> businessUserLogin(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String password = body.get("password");
        if (email == null || password == null || email.isEmpty() || password.isEmpty()) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "Email and password are required");
            return ResponseEntity.badRequest().body(err);
        }
        Map<String, Object> result = currentAccountService.authenticateBusinessUser(email, password);
        return ResponseEntity.ok(result);
    }

    // ==================== Linked Accounts ====================

    @GetMapping("/linked-accounts/verify-savings")
    public ResponseEntity<Map<String, Object>> verifySavingsAccount(
            @RequestParam String accountNumber,
            @RequestParam String customerId) {
        Map<String, Object> result = currentAccountService.verifySavingsAccount(accountNumber, customerId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/linked-accounts/link")
    public ResponseEntity<Map<String, Object>> linkSavingsAccount(@RequestBody Map<String, String> body) {
        String currentAccountNumber = body.get("currentAccountNumber");
        String savingsAccountNumber = body.get("savingsAccountNumber");
        String savingsCustomerId = body.get("savingsCustomerId");
        String linkedBy = body.get("linkedBy");

        if (currentAccountNumber == null || savingsAccountNumber == null || savingsCustomerId == null || linkedBy == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "All fields are required");
            return ResponseEntity.badRequest().body(err);
        }

        Map<String, Object> result = currentAccountService.linkSavingsAccount(
                currentAccountNumber, savingsAccountNumber, savingsCustomerId, linkedBy);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/linked-accounts/{currentAccountNumber}")
    public ResponseEntity<List<LinkedAccount>> getLinkedAccounts(@PathVariable String currentAccountNumber) {
        return ResponseEntity.ok(currentAccountService.getLinkedAccounts(currentAccountNumber));
    }

    @GetMapping("/linked-accounts/savings-details/{savingsAccountNumber}")
    public ResponseEntity<Map<String, Object>> getLinkedSavingsDetails(@PathVariable String savingsAccountNumber) {
        return ResponseEntity.ok(currentAccountService.getLinkedSavingsAccountDetails(savingsAccountNumber));
    }

    @PutMapping("/linked-accounts/unlink/{linkId}")
    public ResponseEntity<Map<String, Object>> unlinkSavingsAccount(@PathVariable Long linkId) {
        return ResponseEntity.ok(currentAccountService.unlinkSavingsAccount(linkId));
    }

    // ==================== Switch PIN ====================

    @PostMapping("/linked-accounts/create-pin/{linkId}")
    public ResponseEntity<Map<String, Object>> createSwitchPin(@PathVariable Long linkId, @RequestBody Map<String, String> body) {
        String pin = body.get("pin");
        return ResponseEntity.ok(currentAccountService.createSwitchPin(linkId, pin));
    }

    @PostMapping("/linked-accounts/verify-pin/{linkId}")
    public ResponseEntity<Map<String, Object>> verifySwitchPin(@PathVariable Long linkId, @RequestBody Map<String, String> body) {
        String pin = body.get("pin");
        return ResponseEntity.ok(currentAccountService.verifySwitchPin(linkId, pin));
    }

    // ==================== User-Side Linked Account Lookups ====================

    @GetMapping("/linked-accounts/by-savings/{savingsAccountNumber}")
    public ResponseEntity<List<LinkedAccount>> getLinkedAccountsBySavings(@PathVariable String savingsAccountNumber) {
        return ResponseEntity.ok(currentAccountService.getLinkedAccountsBySavings(savingsAccountNumber));
    }

    @GetMapping("/linked-accounts/current-details/{currentAccountNumber}")
    public ResponseEntity<Map<String, Object>> getLinkedCurrentAccountDetails(@PathVariable String currentAccountNumber) {
        return ResponseEntity.ok(currentAccountService.getLinkedCurrentAccountDetails(currentAccountNumber));
    }
}
