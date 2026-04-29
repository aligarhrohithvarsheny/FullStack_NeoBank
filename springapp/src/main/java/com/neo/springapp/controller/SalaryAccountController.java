package com.neo.springapp.controller;

import com.neo.springapp.model.SalaryAccount;
import com.neo.springapp.model.SalaryTransaction;
import com.neo.springapp.model.SalaryNormalTransaction;
import com.neo.springapp.model.SalaryLoginActivity;
import com.neo.springapp.model.SalaryUpiTransaction;
import com.neo.springapp.model.SalaryAdvanceRequest;
import com.neo.springapp.model.SalaryFraudAlert;
import com.neo.springapp.service.SalaryAccountService;
import com.neo.springapp.service.SessionHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/salary-accounts")
public class SalaryAccountController {

    @Autowired
    private SalaryAccountService salaryAccountService;

    @Autowired
    private SessionHistoryService sessionHistoryService;

    // ─── Create ──────────────────────────────────────────────

    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createAccount(@RequestBody SalaryAccount account) {
        Map<String, Object> response = new HashMap<>();
        try {
            SalaryAccount created = salaryAccountService.createAccount(account);
            response.put("success", true);
            response.put("message", "Salary account created successfully");
            response.put("account", created);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to create salary account: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ─── Read ────────────────────────────────────────────────

    @GetMapping("/all")
    public ResponseEntity<List<SalaryAccount>> getAll() {
        return ResponseEntity.ok(salaryAccountService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        Optional<SalaryAccount> opt = salaryAccountService.getById(id);
        if (opt.isPresent()) {
            response.put("success", true);
            response.put("account", opt.get());
            return ResponseEntity.ok(response);
        }
        response.put("success", false);
        response.put("message", "Salary account not found");
        return ResponseEntity.badRequest().body(response);
    }

    @GetMapping("/number/{accountNumber}")
    public ResponseEntity<SalaryAccount> getByAccountNumber(@PathVariable String accountNumber) {
        SalaryAccount acc = salaryAccountService.getByAccountNumber(accountNumber);
        return acc != null ? ResponseEntity.ok(acc) : ResponseEntity.notFound().build();
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<SalaryAccount>> getByStatus(@PathVariable String status) {
        return ResponseEntity.ok(salaryAccountService.getByStatus(status));
    }

    @GetMapping("/company/{companyName}")
    public ResponseEntity<List<SalaryAccount>> getByCompany(@PathVariable String companyName) {
        return ResponseEntity.ok(salaryAccountService.getByCompany(companyName));
    }

    @GetMapping("/search")
    public ResponseEntity<List<SalaryAccount>> search(@RequestParam String q) {
        return ResponseEntity.ok(salaryAccountService.search(q));
    }

    // ─── Update ──────────────────────────────────────────────

    @PutMapping("/update/{id}")
    public ResponseEntity<Map<String, Object>> updateAccount(@PathVariable Long id, @RequestBody SalaryAccount updates) {
        Map<String, Object> response = new HashMap<>();
        SalaryAccount updated = salaryAccountService.updateAccount(id, updates);
        if (updated != null) {
            response.put("success", true);
            response.put("message", "Salary account updated successfully");
            response.put("account", updated);
            return ResponseEntity.ok(response);
        }
        response.put("success", false);
        response.put("message", "Salary account not found");
        return ResponseEntity.badRequest().body(response);
    }

    @PutMapping("/freeze/{id}")
    public ResponseEntity<Map<String, Object>> freezeAccount(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        SalaryAccount frozen = salaryAccountService.freezeAccount(id);
        if (frozen != null) {
            response.put("success", true);
            response.put("message", "Account frozen successfully");
            response.put("account", frozen);
            return ResponseEntity.ok(response);
        }
        response.put("success", false);
        response.put("message", "Salary account not found");
        return ResponseEntity.badRequest().body(response);
    }

    @PutMapping("/unfreeze/{id}")
    public ResponseEntity<Map<String, Object>> unfreezeAccount(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        SalaryAccount unfrozen = salaryAccountService.unfreezeAccount(id);
        if (unfrozen != null) {
            response.put("success", true);
            response.put("message", "Account unfrozen successfully");
            response.put("account", unfrozen);
            return ResponseEntity.ok(response);
        }
        response.put("success", false);
        response.put("message", "Salary account not found");
        return ResponseEntity.badRequest().body(response);
    }

    // ─── Close Account ──────────────────────────────────────

    @PostMapping("/close/{id}")
    public ResponseEntity<Map<String, Object>> closeAccount(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String reason = body.getOrDefault("reason", "Closed by manager");
        String closedBy = body.getOrDefault("closedBy", "Manager");
        Map<String, Object> result = salaryAccountService.closeAccount(id, reason, closedBy);
        if (Boolean.TRUE.equals(result.get("success"))) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.badRequest().body(result);
    }

    @GetMapping("/close-precheck/{id}")
    public ResponseEntity<Map<String, Object>> closeAccountPreCheck(@PathVariable Long id) {
        return ResponseEntity.ok(salaryAccountService.closeAccountPreCheck(id));
    }

    // ─── Salary Credit ──────────────────────────────────────

    @PostMapping("/credit-salary/{id}")
    public ResponseEntity<Map<String, Object>> creditSalary(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        SalaryTransaction txn = salaryAccountService.creditSalary(id);
        if (txn != null) {
            response.put("success", true);
            response.put("message", "Salary credited successfully");
            response.put("transaction", txn);
            return ResponseEntity.ok(response);
        }
        response.put("success", false);
        response.put("message", "Unable to credit salary. Account may be frozen/closed or not found.");
        return ResponseEntity.badRequest().body(response);
    }

    // ─── Transactions ────────────────────────────────────────

    @GetMapping("/transactions/{id}")
    public ResponseEntity<List<SalaryTransaction>> getTransactions(@PathVariable Long id) {
        return ResponseEntity.ok(salaryAccountService.getTransactions(id));
    }

    @GetMapping("/transactions/account/{accountNumber}")
    public ResponseEntity<List<SalaryTransaction>> getTransactionsByAccount(@PathVariable String accountNumber) {
        return ResponseEntity.ok(salaryAccountService.getTransactionsByAccountNumber(accountNumber));
    }

    // ─── Statistics ──────────────────────────────────────────

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(salaryAccountService.getStats());
    }

    // ─── Employee Authentication ─────────────────────────────

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
        Map<String, Object> result = salaryAccountService.signup(customerId, password);
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
        Map<String, Object> result = salaryAccountService.authenticate(accountNumber, password);
        // Record session history on successful login
        if (result.containsKey("success") && Boolean.TRUE.equals(result.get("success"))) {
            try {
                String clientIp = forwardedFor != null ? forwardedFor.split(",")[0].trim() : "Unknown";
                String deviceInfo = userAgent != null ? userAgent : "Unknown";
                Object accountObj = result.get("account");
                Long accountId = null;
                String email = "";
                String employeeName = "Salary Account User";
                if (accountObj instanceof SalaryAccount) {
                    SalaryAccount sa = (SalaryAccount) accountObj;
                    accountId = sa.getId();
                    email = sa.getEmail() != null ? sa.getEmail() : "";
                    employeeName = sa.getEmployeeName() != null ? sa.getEmployeeName() : employeeName;
                } else if (accountObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> accMap = (Map<String, Object>) accountObj;
                    accountId = accMap.get("id") != null ? Long.parseLong(accMap.get("id").toString()) : null;
                    email = accMap.get("email") != null ? accMap.get("email").toString() : "";
                    employeeName = accMap.get("employeeName") != null ? accMap.get("employeeName").toString() : employeeName;
                }
                sessionHistoryService.recordSalaryAccountLogin(
                    accountId != null ? accountId : 0L, email, accountNumber, employeeName,
                    "Unknown", clientIp, deviceInfo, "PASSWORD");
            } catch (Exception e) {
                System.err.println("Error recording salary account session: " + e.getMessage());
            }
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/verify-customer/{customerId}")
    public ResponseEntity<Map<String, Object>> verifyCustomer(@PathVariable String customerId) {
        Map<String, Object> response = new HashMap<>();
        SalaryAccount account = salaryAccountService.getByCustomerId(customerId);
        if (account != null) {
            response.put("exists", true);
            response.put("employeeName", account.getEmployeeName());
            response.put("accountNumber", account.getAccountNumber());
            response.put("passwordSet", account.getPasswordSet());
            response.put("status", account.getStatus());
        } else {
            response.put("exists", false);
        }
        return ResponseEntity.ok(response);
    }

    // ─── Transfer Money ──────────────────────────────────────

    @PostMapping("/transfer/{id}")
    public ResponseEntity<Map<String, Object>> transferMoney(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String recipientAccount = (String) body.get("recipientAccount");
        String recipientIfsc = (String) body.get("recipientIfsc");
        Double amount = body.get("amount") != null ? ((Number) body.get("amount")).doubleValue() : null;
        String remark = (String) body.get("remark");
        String transactionPin = (String) body.get("transactionPin");

        if (recipientAccount == null || amount == null || transactionPin == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "Recipient account, amount, and transaction PIN are required");
            return ResponseEntity.badRequest().body(err);
        }
        if (amount <= 0) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "Amount must be greater than 0");
            return ResponseEntity.badRequest().body(err);
        }
        return ResponseEntity.ok(salaryAccountService.transferMoney(id, recipientAccount, recipientIfsc, amount, remark, transactionPin));
    }

    // ─── Withdraw Money ──────────────────────────────────────

    @PostMapping("/withdraw/{id}")
    public ResponseEntity<Map<String, Object>> withdrawMoney(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Double amount = body.get("amount") != null ? ((Number) body.get("amount")).doubleValue() : null;
        String transactionPin = (String) body.get("transactionPin");

        if (amount == null || transactionPin == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "Amount and transaction PIN are required");
            return ResponseEntity.badRequest().body(err);
        }
        if (amount <= 0) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "Amount must be greater than 0");
            return ResponseEntity.badRequest().body(err);
        }
        return ResponseEntity.ok(salaryAccountService.withdrawMoney(id, amount, transactionPin));
    }

    // ─── Normal Transactions ─────────────────────────────────

    @GetMapping("/normal-transactions/{id}")
    public ResponseEntity<List<SalaryNormalTransaction>> getNormalTransactions(@PathVariable Long id) {
        return ResponseEntity.ok(salaryAccountService.getNormalTransactions(id));
    }

    // ─── Bank Charges ────────────────────────────────────────

    @GetMapping("/bank-charges/{id}")
    public ResponseEntity<Map<String, Object>> getBankCharges(@PathVariable Long id) {
        return ResponseEntity.ok(salaryAccountService.getBankCharges(id));
    }

    // ─── Transaction PIN ─────────────────────────────────────

    @PostMapping("/set-pin/{id}")
    public ResponseEntity<Map<String, Object>> setTransactionPin(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String pin = body.get("pin");
        String password = body.get("password");
        if (pin == null || password == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "PIN and password are required");
            return ResponseEntity.badRequest().body(err);
        }
        return ResponseEntity.ok(salaryAccountService.setTransactionPin(id, pin, password));
    }

    @PostMapping("/change-pin/{id}")
    public ResponseEntity<Map<String, Object>> changeTransactionPin(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String oldPin = body.get("oldPin");
        String newPin = body.get("newPin");
        if (oldPin == null || newPin == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "Old PIN and new PIN are required");
            return ResponseEntity.badRequest().body(err);
        }
        return ResponseEntity.ok(salaryAccountService.changeTransactionPin(id, oldPin, newPin));
    }

    // ─── Login Activity ──────────────────────────────────────

    @GetMapping("/login-activity/{id}")
    public ResponseEntity<List<SalaryLoginActivity>> getLoginActivity(@PathVariable Long id) {
        return ResponseEntity.ok(salaryAccountService.getLoginActivity(id));
    }

    @PostMapping("/record-activity/{id}")
    public ResponseEntity<Map<String, Object>> recordActivity(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String activityType = body.get("activityType");
        String deviceInfo = body.get("deviceInfo");
        String browserName = body.get("browserName");
        String status = body.getOrDefault("status", "Success");
        if (activityType == null || activityType.isEmpty()) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "Activity type is required");
            return ResponseEntity.badRequest().body(err);
        }
        salaryAccountService.recordLoginActivity(id, null, activityType, browserName, deviceInfo, status);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Activity recorded");
        return ResponseEntity.ok(result);
    }

    // ─── Employee Dashboard Stats ────────────────────────────

    @GetMapping("/dashboard-stats/{id}")
    public ResponseEntity<Map<String, Object>> getDashboardStats(@PathVariable Long id) {
        return ResponseEntity.ok(salaryAccountService.getEmployeeDashboardStats(id));
    }

    // ─── Change Password ─────────────────────────────────────

    @PostMapping("/change-password/{id}")
    public ResponseEntity<Map<String, Object>> changePassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String currentPassword = body.get("currentPassword");
        String newPassword = body.get("newPassword");
        if (currentPassword == null || newPassword == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "Current and new password are required");
            return ResponseEntity.badRequest().body(err);
        }
        return ResponseEntity.ok(salaryAccountService.changePassword(id, currentPassword, newPassword));
    }

    // ─── Verify Recipient Account ────────────────────────────

    @GetMapping("/verify-recipient/{accountNumber}")
    public ResponseEntity<Map<String, Object>> verifyRecipient(@PathVariable String accountNumber) {
        return ResponseEntity.ok(salaryAccountService.verifyRecipientAccount(accountNumber));
    }

    // ─── Salary Slip Data ────────────────────────────────────

    @GetMapping("/salary-slip/{accountId}/{transactionId}")
    public ResponseEntity<Map<String, Object>> getSalarySlip(@PathVariable Long accountId, @PathVariable Long transactionId) {
        return ResponseEntity.ok(salaryAccountService.getSalarySlipData(accountId, transactionId));
    }

    // ─── Employee Profile Update ─────────────────────────────

    @PutMapping("/update-profile/{id}")
    public ResponseEntity<Map<String, Object>> updateProfile(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(salaryAccountService.updateProfile(id, body));
    }

    // ─── UPI Integration ─────────────────────────────────────

    @PostMapping("/upi/enable/{id}")
    public ResponseEntity<Map<String, Object>> enableUpi(@PathVariable Long id) {
        return ResponseEntity.ok(salaryAccountService.enableUpi(id));
    }

    @PostMapping("/upi/pay/{id}")
    public ResponseEntity<Map<String, Object>> sendUpiPayment(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String recipientUpi = (String) body.get("recipientUpi");
        Double amount = body.get("amount") != null ? ((Number) body.get("amount")).doubleValue() : null;
        String remark = (String) body.get("remark");
        String transactionPin = (String) body.get("transactionPin");
        if (recipientUpi == null || amount == null || transactionPin == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "Recipient UPI, amount, and PIN are required");
            return ResponseEntity.badRequest().body(err);
        }
        return ResponseEntity.ok(salaryAccountService.sendUpiPayment(id, recipientUpi, amount, remark, transactionPin));
    }

    @GetMapping("/upi/transactions/{id}")
    public ResponseEntity<List<SalaryUpiTransaction>> getUpiTransactions(@PathVariable Long id) {
        return ResponseEntity.ok(salaryAccountService.getUpiTransactions(id));
    }

    // ─── Loan Eligibility ────────────────────────────────────

    @GetMapping("/loan-eligibility/{id}")
    public ResponseEntity<Map<String, Object>> getLoanEligibility(@PathVariable Long id) {
        return ResponseEntity.ok(salaryAccountService.getLoanEligibility(id));
    }

    // ─── Auto Savings ────────────────────────────────────────

    @PostMapping("/auto-savings/toggle/{id}")
    public ResponseEntity<Map<String, Object>> toggleAutoSavings(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Boolean enable = (Boolean) body.get("enable");
        Double percentage = body.get("percentage") != null ? ((Number) body.get("percentage")).doubleValue() : null;
        if (enable == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false); err.put("message", "'enable' is required");
            return ResponseEntity.badRequest().body(err);
        }
        return ResponseEntity.ok(salaryAccountService.toggleAutoSavings(id, enable, percentage));
    }

    @GetMapping("/auto-savings/{id}")
    public ResponseEntity<Map<String, Object>> getAutoSavingsInfo(@PathVariable Long id) {
        return ResponseEntity.ok(salaryAccountService.getAutoSavingsInfo(id));
    }

    @PostMapping("/auto-savings/withdraw/{id}")
    public ResponseEntity<Map<String, Object>> withdrawSavings(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Double amount = body.get("amount") != null ? ((Number) body.get("amount")).doubleValue() : null;
        String transactionPin = (String) body.get("transactionPin");
        if (amount == null || transactionPin == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false); err.put("message", "Amount and PIN are required");
            return ResponseEntity.badRequest().body(err);
        }
        return ResponseEntity.ok(salaryAccountService.withdrawSavings(id, amount, transactionPin));
    }

    // ─── Salary Advance ──────────────────────────────────────

    @PostMapping("/advance/request/{id}")
    public ResponseEntity<Map<String, Object>> requestSalaryAdvance(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Double amount = body.get("amount") != null ? ((Number) body.get("amount")).doubleValue() : null;
        String reason = (String) body.get("reason");
        if (amount == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false); err.put("message", "Amount is required");
            return ResponseEntity.badRequest().body(err);
        }
        return ResponseEntity.ok(salaryAccountService.requestSalaryAdvance(id, amount, reason));
    }

    @GetMapping("/advance/history/{id}")
    public ResponseEntity<List<SalaryAdvanceRequest>> getAdvanceHistory(@PathVariable Long id) {
        return ResponseEntity.ok(salaryAccountService.getAdvanceRequests(id));
    }

    @GetMapping("/advance/info/{id}")
    public ResponseEntity<Map<String, Object>> getAdvanceInfo(@PathVariable Long id) {
        return ResponseEntity.ok(salaryAccountService.getAdvanceInfo(id));
    }

    // ─── Debit Card Management ───────────────────────────────

    @PutMapping("/debit-card/settings/{id}")
    public ResponseEntity<Map<String, Object>> updateDebitCardSettings(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(salaryAccountService.updateDebitCardSettings(id, body));
    }

    @GetMapping("/debit-card/{id}")
    public ResponseEntity<Map<String, Object>> getDebitCardInfo(@PathVariable Long id) {
        return ResponseEntity.ok(salaryAccountService.getDebitCardInfo(id));
    }

    @PostMapping("/debit-card/generate/{id}")
    public ResponseEntity<Map<String, Object>> generateCardDetails(@PathVariable Long id) {
        Map<String, Object> result = salaryAccountService.generateCardDetails(id);
        if (Boolean.TRUE.equals(result.get("success"))) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.badRequest().body(result);
    }

    // ─── AI Fraud Detection ──────────────────────────────────

    @GetMapping("/fraud/alerts/{id}")
    public ResponseEntity<List<SalaryFraudAlert>> getFraudAlerts(@PathVariable Long id) {
        return ResponseEntity.ok(salaryAccountService.getFraudAlerts(id));
    }

    @GetMapping("/fraud/summary/{id}")
    public ResponseEntity<Map<String, Object>> getFraudSummary(@PathVariable Long id) {
        return ResponseEntity.ok(salaryAccountService.getFraudSummary(id));
    }

    @PostMapping("/fraud/resolve/{alertId}")
    public ResponseEntity<Map<String, Object>> resolveFraudAlert(@PathVariable Long alertId) {
        return ResponseEntity.ok(salaryAccountService.resolveFraudAlert(alertId));
    }

    // ─── Employee ID Linking (One-Time) ──────────────────────

    @PutMapping("/link-employee/{id}")
    public ResponseEntity<Map<String, Object>> linkEmployeeId(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String employeeId = body.get("employeeId");
        if (employeeId == null || employeeId.isBlank()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Employee ID is required");
            return ResponseEntity.badRequest().body(response);
        }
        Map<String, Object> result = salaryAccountService.linkEmployeeId(id, employeeId.trim());
        if (Boolean.TRUE.equals(result.get("success"))) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.badRequest().body(result);
    }

    @GetMapping("/by-employee/{employeeId}")
    public ResponseEntity<Map<String, Object>> getByEmployeeId(@PathVariable String employeeId) {
        Map<String, Object> response = new HashMap<>();
        SalaryAccount account = salaryAccountService.getByEmployeeId(employeeId);
        if (account != null) {
            response.put("success", true);
            response.put("account", account);
            return ResponseEntity.ok(response);
        }
        response.put("success", false);
        response.put("message", "No salary account linked with employee ID: " + employeeId);
        return ResponseEntity.ok(response);
    }

    // ─── Blocked Account Management ──────────────────────────

    @GetMapping("/blocked")
    public ResponseEntity<List<SalaryAccount>> getBlockedAccounts() {
        return ResponseEntity.ok(salaryAccountService.getBlockedAccounts());
    }

    @PostMapping("/unblock/{id}")
    public ResponseEntity<Map<String, Object>> unblockAccount(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        SalaryAccount account = salaryAccountService.unblockAccount(id);
        if (account != null) {
            response.put("success", true);
            response.put("message", "Employee account unblocked successfully");
            response.put("account", account);
            return ResponseEntity.ok(response);
        }
        response.put("success", false);
        response.put("message", "Account not found");
        return ResponseEntity.badRequest().body(response);
    }

    @PostMapping("/reset-password/{id}")
    public ResponseEntity<Map<String, Object>> resetAccountPassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Map<String, Object> response = new HashMap<>();
        String newPassword = body.get("newPassword");
        if (newPassword == null || newPassword.trim().isEmpty() || newPassword.length() < 6) {
            response.put("success", false);
            response.put("message", "Password must be at least 6 characters");
            return ResponseEntity.badRequest().body(response);
        }
        SalaryAccount account = salaryAccountService.resetAccountPassword(id, newPassword);
        if (account != null) {
            response.put("success", true);
            response.put("message", "Password reset and account unblocked successfully");
            return ResponseEntity.ok(response);
        }
        response.put("success", false);
        response.put("message", "Account not found");
        return ResponseEntity.badRequest().body(response);
    }
}
