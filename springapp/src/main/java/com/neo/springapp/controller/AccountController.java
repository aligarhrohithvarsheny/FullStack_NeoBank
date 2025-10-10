package com.neo.springapp.controller;

import com.neo.springapp.model.Account;
import com.neo.springapp.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
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
@CrossOrigin(origins = "http://localhost:4200")
public class AccountController {

    @Autowired
    private AccountService accountService;

    // Basic CRUD operations
    @PostMapping("/create")
    public ResponseEntity<Account> createAccount(@RequestBody Account account) {
        try {
            // Validate unique fields
            if (!accountService.isPanUnique(account.getPan())) {
                return ResponseEntity.badRequest().build();
            }
            if (!accountService.isAadharUnique(account.getAadharNumber())) {
                return ResponseEntity.badRequest().build();
            }
            
            Account savedAccount = accountService.saveAccount(account);
            return ResponseEntity.ok(savedAccount);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
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

    @GetMapping("/validate/account-number/{accountNumber}")
    public ResponseEntity<Map<String, Boolean>> validateAccountNumber(@PathVariable String accountNumber) {
        Map<String, Boolean> response = new HashMap<>();
        response.put("isUnique", accountService.isAccountNumberUnique(accountNumber));
        return ResponseEntity.ok(response);
    }
}
