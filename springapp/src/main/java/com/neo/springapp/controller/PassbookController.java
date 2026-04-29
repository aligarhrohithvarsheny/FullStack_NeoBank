package com.neo.springapp.controller;

import com.neo.springapp.model.Account;
import com.neo.springapp.model.CurrentAccount;
import com.neo.springapp.model.SalaryAccount;
import com.neo.springapp.model.Transaction;
import com.neo.springapp.service.AccountService;
import com.neo.springapp.service.PdfService;
import com.neo.springapp.service.TransactionService;
import com.neo.springapp.repository.CurrentAccountRepository;
import com.neo.springapp.repository.SalaryAccountRepository;
import com.neo.springapp.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;
import java.util.Optional;

@RestController
@RequestMapping("/api/passbook")
@CrossOrigin(origins = "*")
public class PassbookController {

    @Autowired
    private AccountService accountService;

    @Autowired
    private PdfService pdfService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private CurrentAccountRepository currentAccountRepository;

    @Autowired
    private SalaryAccountRepository salaryAccountRepository;

    @Autowired
    private com.neo.springapp.service.CurrentAccountService currentAccountService;

    @Autowired
    private com.neo.springapp.service.SalaryAccountService salaryAccountService;

    @Autowired
    private com.neo.springapp.repository.AccountRepository accountRepository;

    /**
     * Generate passbook PDF for any account type (Savings, Current, Salary).
     * Query param: accountType = savings | current | salary
     */
    @GetMapping("/generate/{accountNumber}")
    public ResponseEntity<?> generatePassbook(
            @PathVariable String accountNumber,
            @RequestParam(defaultValue = "savings") String accountType) {

        Map<String, Object> accountInfo = new HashMap<>();
        List<Transaction> transactions;

        switch (accountType.toLowerCase()) {
            case "current":
                Optional<CurrentAccount> caOpt = currentAccountRepository.findByAccountNumber(accountNumber);
                if (caOpt.isEmpty()) {
                    return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Current account not found: " + accountNumber));
                }
                CurrentAccount ca = caOpt.get();
                accountInfo.put("accountNumber", ca.getAccountNumber());
                accountInfo.put("customerId", ca.getCustomerId() != null ? ca.getCustomerId() : "N/A");
                accountInfo.put("name", ca.getOwnerName());
                accountInfo.put("address", buildCurrentAccountAddress(ca));
                accountInfo.put("phone", ca.getMobile());
                accountInfo.put("accountType", "Current");
                accountInfo.put("branchName", ca.getBranchName() != null ? ca.getBranchName() : "NeoBank Main Branch");
                accountInfo.put("ifscCode", ca.getIfscCode() != null ? ca.getIfscCode() : "NEOB0001234");
                accountInfo.put("pan", ca.getPanNumber());
                accountInfo.put("aadharNumber", ca.getAadharNumber());
                accountInfo.put("balance", ca.getBalance());
                accountInfo.put("modeOfOperation", "Single");
                break;

            case "salary":
                SalaryAccount sa = salaryAccountRepository.findByAccountNumber(accountNumber);
                if (sa == null) {
                    return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Salary account not found: " + accountNumber));
                }
                accountInfo.put("accountNumber", sa.getAccountNumber());
                accountInfo.put("customerId", sa.getCustomerId() != null ? sa.getCustomerId() : "N/A");
                accountInfo.put("name", sa.getEmployeeName());
                accountInfo.put("address", sa.getEmployerAddress() != null ? sa.getEmployerAddress() : "N/A");
                accountInfo.put("phone", sa.getMobileNumber());
                accountInfo.put("accountType", "Salary");
                accountInfo.put("branchName", sa.getBranchName() != null ? sa.getBranchName() : "NeoBank Main Branch");
                accountInfo.put("ifscCode", sa.getIfscCode() != null ? sa.getIfscCode() : "NEOB0001234");
                accountInfo.put("pan", sa.getPanNumber());
                accountInfo.put("aadharNumber", sa.getAadharNumber());
                accountInfo.put("balance", sa.getBalance());
                accountInfo.put("modeOfOperation", "Single");
                break;

            default: // savings
                Account acc = accountService.getAccountByNumber(accountNumber);
                if (acc == null) {
                    return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Savings account not found: " + accountNumber));
                }
                accountInfo.put("accountNumber", acc.getAccountNumber());
                accountInfo.put("customerId", acc.getCustomerId() != null ? acc.getCustomerId() : "N/A");
                accountInfo.put("name", acc.getName());
                accountInfo.put("address", acc.getAddress() != null ? acc.getAddress() : "N/A");
                accountInfo.put("phone", acc.getPhone());
                accountInfo.put("accountType", acc.getAccountType() != null ? acc.getAccountType() : "Savings");
                accountInfo.put("branchName", "NeoBank Main Branch");
                accountInfo.put("ifscCode", "NEOB0001234");
                accountInfo.put("pan", acc.getPan());
                accountInfo.put("aadharNumber", acc.getAadharNumber());
                accountInfo.put("balance", acc.getBalance());
                accountInfo.put("modeOfOperation", "Single");
                break;
        }

        // Fetch transactions (up to 500 most recent)
        Page<Transaction> txnPage = transactionService.getTransactionsByAccountNumber(accountNumber, 0, 500);
        transactions = txnPage.getContent();

        try {
            byte[] pdfBytes = pdfService.generatePassbook(accountInfo, transactions);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "Passbook_" + accountNumber + ".pdf");
            headers.setCacheControl("no-cache, no-store, must-revalidate");

            return ResponseEntity.ok().headers(headers).body(pdfBytes);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "Failed to generate passbook: " + e.getMessage()));
        }
    }

    /**
     * Get all accounts (savings + current + salary) for admin passbook management.
     */
    @GetMapping("/accounts/all")
    public ResponseEntity<?> getAllAccounts() {
        List<Map<String, Object>> result = new ArrayList<>();

        // Savings accounts
        List<Account> savingsAccounts = accountService.getAllAccounts();
        if (savingsAccounts != null) {
            for (Account a : savingsAccounts) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", a.getId());
                row.put("accountNumber", a.getAccountNumber());
                row.put("name", a.getName());
                row.put("phone", a.getPhone());
                row.put("accountType", a.getAccountType() != null ? a.getAccountType() : "Savings");
                row.put("balance", a.getBalance());
                row.put("status", a.getStatus());
                row.put("customerId", a.getCustomerId());
                row.put("type", "savings");
                result.add(row);
            }
        }

        // Current accounts
        List<CurrentAccount> currentAccounts = currentAccountRepository.findAll();
        if (currentAccounts != null) {
            for (CurrentAccount ca : currentAccounts) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", ca.getId());
                row.put("accountNumber", ca.getAccountNumber());
                row.put("name", ca.getOwnerName());
                row.put("phone", ca.getMobile());
                row.put("accountType", "Current");
                row.put("balance", ca.getBalance());
                row.put("status", ca.getStatus());
                row.put("customerId", ca.getCustomerId());
                row.put("type", "current");
                result.add(row);
            }
        }

        // Salary accounts
        List<SalaryAccount> salaryAccounts = salaryAccountRepository.findAll();
        if (salaryAccounts != null) {
            for (SalaryAccount sa : salaryAccounts) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", sa.getId());
                row.put("accountNumber", sa.getAccountNumber());
                row.put("name", sa.getEmployeeName());
                row.put("phone", sa.getMobileNumber());
                row.put("accountType", "Salary");
                row.put("balance", sa.getBalance());
                row.put("status", sa.getStatus());
                row.put("customerId", sa.getCustomerId());
                row.put("type", "salary");
                result.add(row);
            }
        }

        return ResponseEntity.ok(result);
    }

    private String buildCurrentAccountAddress(CurrentAccount ca) {
        StringBuilder addr = new StringBuilder();
        if (ca.getShopAddress() != null) addr.append(ca.getShopAddress());
        if (ca.getCity() != null) { if (addr.length() > 0) addr.append(", "); addr.append(ca.getCity()); }
        if (ca.getState() != null) { if (addr.length() > 0) addr.append(", "); addr.append(ca.getState()); }
        if (ca.getPincode() != null) { if (addr.length() > 0) addr.append(" - "); addr.append(ca.getPincode()); }
        return addr.length() > 0 ? addr.toString() : "N/A";
    }

    // ==================== Account Management Actions ====================

    /**
     * Freeze any account type by id
     */
    @PutMapping("/freeze/{id}")
    public ResponseEntity<Map<String, Object>> freezeAccount(
            @PathVariable Long id,
            @RequestParam String accountType,
            @RequestParam(defaultValue = "Frozen by admin") String reason,
            @RequestParam(defaultValue = "Admin") String frozenBy) {
        Map<String, Object> response = new HashMap<>();
        try {
            switch (accountType.toLowerCase()) {
                case "savings":
                    Optional<Account> accOpt = accountRepository.findById(id);
                    if (accOpt.isEmpty()) {
                        response.put("success", false);
                        response.put("message", "Savings account not found");
                        return ResponseEntity.badRequest().body(response);
                    }
                    Account acc = accOpt.get();
                    acc.setStatus("FROZEN");
                    accountRepository.save(acc);
                    response.put("success", true);
                    response.put("message", "Savings account frozen successfully");
                    return ResponseEntity.ok(response);

                case "current":
                    CurrentAccount frozen = currentAccountService.freezeAccount(id, reason, frozenBy);
                    if (frozen != null) {
                        response.put("success", true);
                        response.put("message", "Current account frozen successfully");
                        return ResponseEntity.ok(response);
                    }
                    response.put("success", false);
                    response.put("message", "Current account not found or already frozen");
                    return ResponseEntity.badRequest().body(response);

                case "salary":
                    SalaryAccount frozenSa = salaryAccountService.freezeAccount(id);
                    if (frozenSa != null) {
                        response.put("success", true);
                        response.put("message", "Salary account frozen successfully");
                        return ResponseEntity.ok(response);
                    }
                    response.put("success", false);
                    response.put("message", "Salary account not found");
                    return ResponseEntity.badRequest().body(response);

                default:
                    response.put("success", false);
                    response.put("message", "Invalid account type");
                    return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to freeze account: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Unfreeze any account type by id
     */
    @PutMapping("/unfreeze/{id}")
    public ResponseEntity<Map<String, Object>> unfreezeAccount(
            @PathVariable Long id,
            @RequestParam String accountType) {
        Map<String, Object> response = new HashMap<>();
        try {
            switch (accountType.toLowerCase()) {
                case "savings":
                    Optional<Account> accOpt = accountRepository.findById(id);
                    if (accOpt.isEmpty()) {
                        response.put("success", false);
                        response.put("message", "Savings account not found");
                        return ResponseEntity.badRequest().body(response);
                    }
                    Account acc = accOpt.get();
                    acc.setStatus("ACTIVE");
                    accountRepository.save(acc);
                    response.put("success", true);
                    response.put("message", "Savings account unfrozen successfully");
                    return ResponseEntity.ok(response);

                case "current":
                    CurrentAccount unfrozen = currentAccountService.unfreezeAccount(id);
                    if (unfrozen != null) {
                        response.put("success", true);
                        response.put("message", "Current account unfrozen successfully");
                        return ResponseEntity.ok(response);
                    }
                    response.put("success", false);
                    response.put("message", "Current account not found or not frozen");
                    return ResponseEntity.badRequest().body(response);

                case "salary":
                    SalaryAccount unfrozenSa = salaryAccountService.unfreezeAccount(id);
                    if (unfrozenSa != null) {
                        response.put("success", true);
                        response.put("message", "Salary account unfrozen successfully");
                        return ResponseEntity.ok(response);
                    }
                    response.put("success", false);
                    response.put("message", "Salary account not found");
                    return ResponseEntity.badRequest().body(response);

                default:
                    response.put("success", false);
                    response.put("message", "Invalid account type");
                    return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to unfreeze account: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Close any account type by id
     */
    @PutMapping("/close/{id}")
    public ResponseEntity<Map<String, Object>> closeAccount(
            @PathVariable Long id,
            @RequestParam String accountType,
            @RequestParam(defaultValue = "Closed by admin") String reason,
            @RequestParam(defaultValue = "Admin") String closedBy) {
        Map<String, Object> response = new HashMap<>();
        try {
            switch (accountType.toLowerCase()) {
                case "savings":
                    Optional<Account> accOpt = accountRepository.findById(id);
                    if (accOpt.isEmpty()) {
                        response.put("success", false);
                        response.put("message", "Savings account not found");
                        return ResponseEntity.badRequest().body(response);
                    }
                    Account acc = accOpt.get();
                    acc.setStatus("CLOSED");
                    accountRepository.save(acc);
                    response.put("success", true);
                    response.put("message", "Savings account closed successfully");
                    return ResponseEntity.ok(response);

                case "current":
                    Optional<CurrentAccount> caOpt = currentAccountRepository.findById(id);
                    if (caOpt.isEmpty()) {
                        response.put("success", false);
                        response.put("message", "Current account not found");
                        return ResponseEntity.badRequest().body(response);
                    }
                    CurrentAccount ca = caOpt.get();
                    ca.setStatus("CLOSED");
                    currentAccountRepository.save(ca);
                    response.put("success", true);
                    response.put("message", "Current account closed successfully");
                    return ResponseEntity.ok(response);

                case "salary":
                    Map<String, Object> result = salaryAccountService.closeAccount(id, reason, closedBy);
                    if (Boolean.TRUE.equals(result.get("success"))) {
                        return ResponseEntity.ok(result);
                    }
                    return ResponseEntity.badRequest().body(result);

                default:
                    response.put("success", false);
                    response.put("message", "Invalid account type");
                    return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to close account: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Delete any account type by id
     */
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Map<String, Object>> deleteAccount(
            @PathVariable Long id,
            @RequestParam String accountType) {
        Map<String, Object> response = new HashMap<>();
        try {
            switch (accountType.toLowerCase()) {
                case "savings":
                    accountService.deleteAccount(id);
                    response.put("success", true);
                    response.put("message", "Savings account deleted successfully");
                    return ResponseEntity.ok(response);

                case "current":
                    if (!currentAccountRepository.existsById(id)) {
                        response.put("success", false);
                        response.put("message", "Current account not found");
                        return ResponseEntity.badRequest().body(response);
                    }
                    currentAccountRepository.deleteById(id);
                    response.put("success", true);
                    response.put("message", "Current account deleted successfully");
                    return ResponseEntity.ok(response);

                case "salary":
                    if (!salaryAccountRepository.existsById(id)) {
                        response.put("success", false);
                        response.put("message", "Salary account not found");
                        return ResponseEntity.badRequest().body(response);
                    }
                    salaryAccountRepository.deleteById(id);
                    response.put("success", true);
                    response.put("message", "Salary account deleted successfully");
                    return ResponseEntity.ok(response);

                default:
                    response.put("success", false);
                    response.put("message", "Invalid account type");
                    return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to delete account: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
