package com.neo.springapp.service;

import com.neo.springapp.model.SalaryAccount;
import com.neo.springapp.model.SalaryTransaction;
import com.neo.springapp.model.SalaryNormalTransaction;
import com.neo.springapp.model.SalaryLoginActivity;
import com.neo.springapp.model.SalaryUpiTransaction;
import com.neo.springapp.model.SalaryAdvanceRequest;
import com.neo.springapp.model.SalaryFraudAlert;
import com.neo.springapp.model.Account;
import com.neo.springapp.model.CurrentAccount;
import com.neo.springapp.repository.AccountRepository;
import com.neo.springapp.repository.CurrentAccountRepository;
import com.neo.springapp.repository.UserRepository;
import com.neo.springapp.repository.SalaryAccountRepository;
import com.neo.springapp.repository.SalaryTransactionRepository;
import com.neo.springapp.repository.SalaryNormalTransactionRepository;
import com.neo.springapp.repository.SalaryLoginActivityRepository;
import com.neo.springapp.repository.SalaryUpiTransactionRepository;
import com.neo.springapp.repository.SalaryAdvanceRequestRepository;
import com.neo.springapp.repository.ChequeRequestRepository;
import com.neo.springapp.repository.SalaryFraudAlertRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@SuppressWarnings("null")
public class SalaryAccountService {

    @Autowired
    private SalaryAccountRepository salaryAccountRepository;

    @Autowired
    private SalaryTransactionRepository salaryTransactionRepository;

    @Autowired
    private SalaryNormalTransactionRepository normalTransactionRepository;

    @Autowired
    private SalaryLoginActivityRepository loginActivityRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CurrentAccountRepository currentAccountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SalaryUpiTransactionRepository upiTransactionRepository;

    @Autowired
    private SalaryAdvanceRequestRepository advanceRequestRepository;

    @Autowired
    private SalaryFraudAlertRepository fraudAlertRepository;

    @Autowired
    private ChequeRequestRepository chequeRequestRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);

    // ─── Account CRUD ──────────────────────────────────────────

    public SalaryAccount createAccount(SalaryAccount account) {
        if (account.getAccountNumber() == null || account.getAccountNumber().isEmpty()) {
            account.setAccountNumber(generateAccountNumber());
        }
        if (account.getCustomerId() == null || account.getCustomerId().isEmpty()) {
            account.setCustomerId(generateCustomerId());
        }
        if (account.getDebitCardNumber() == null || account.getDebitCardNumber().isEmpty()) {
            account.setDebitCardNumber(generateDebitCardNumber());
        }
        if (account.getDebitCardCvv() == null || account.getDebitCardCvv().isEmpty()) {
            account.setDebitCardCvv(generateCvv());
        }
        if (account.getDebitCardExpiry() == null || account.getDebitCardExpiry().isEmpty()) {
            account.setDebitCardExpiry(generateExpiryDate());
        }
        account.setCreatedAt(LocalDateTime.now());
        account.setUpdatedAt(LocalDateTime.now());
        return salaryAccountRepository.save(account);
    }

    public Optional<SalaryAccount> getById(Long id) {
        return salaryAccountRepository.findById(id);
    }

    public SalaryAccount getByAccountNumber(String accountNumber) {
        return salaryAccountRepository.findByAccountNumber(accountNumber);
    }

    public List<SalaryAccount> getAll() {
        return salaryAccountRepository.findAll();
    }

    public List<SalaryAccount> getByStatus(String status) {
        return salaryAccountRepository.findByStatus(status);
    }

    public List<SalaryAccount> getByCompany(String companyName) {
        return salaryAccountRepository.findByCompanyName(companyName);
    }

    public List<SalaryAccount> search(String query) {
        return salaryAccountRepository.search(query);
    }

    public SalaryAccount updateAccount(Long id, SalaryAccount updates) {
        Optional<SalaryAccount> opt = salaryAccountRepository.findById(id);
        if (opt.isEmpty()) return null;
        SalaryAccount existing = opt.get();

        if (updates.getEmployeeName() != null) existing.setEmployeeName(updates.getEmployeeName());
        if (updates.getDob() != null) existing.setDob(updates.getDob());
        if (updates.getMobileNumber() != null) existing.setMobileNumber(updates.getMobileNumber());
        if (updates.getEmail() != null) existing.setEmail(updates.getEmail());
        if (updates.getAadharNumber() != null) existing.setAadharNumber(updates.getAadharNumber());
        if (updates.getPanNumber() != null) existing.setPanNumber(updates.getPanNumber());
        if (updates.getCompanyName() != null) existing.setCompanyName(updates.getCompanyName());
        if (updates.getCompanyId() != null) existing.setCompanyId(updates.getCompanyId());
        if (updates.getEmployerAddress() != null) existing.setEmployerAddress(updates.getEmployerAddress());
        if (updates.getHrContactNumber() != null) existing.setHrContactNumber(updates.getHrContactNumber());
        if (updates.getMonthlySalary() != null) existing.setMonthlySalary(updates.getMonthlySalary());
        if (updates.getSalaryCreditDate() != null) existing.setSalaryCreditDate(updates.getSalaryCreditDate());
        if (updates.getDesignation() != null) existing.setDesignation(updates.getDesignation());
        if (updates.getBranchName() != null) existing.setBranchName(updates.getBranchName());
        if (updates.getIfscCode() != null) existing.setIfscCode(updates.getIfscCode());
        if (updates.getStatus() != null) existing.setStatus(updates.getStatus());

        existing.setUpdatedAt(LocalDateTime.now());
        return salaryAccountRepository.save(existing);
    }

    public SalaryAccount freezeAccount(Long id) {
        Optional<SalaryAccount> opt = salaryAccountRepository.findById(id);
        if (opt.isEmpty()) return null;
        SalaryAccount acc = opt.get();
        acc.setStatus("Frozen");
        acc.setUpdatedAt(LocalDateTime.now());
        return salaryAccountRepository.save(acc);
    }

    public SalaryAccount unfreezeAccount(Long id) {
        Optional<SalaryAccount> opt = salaryAccountRepository.findById(id);
        if (opt.isEmpty()) return null;
        SalaryAccount acc = opt.get();
        acc.setStatus("Active");
        acc.setUpdatedAt(LocalDateTime.now());
        return salaryAccountRepository.save(acc);
    }

    // ─── Close Account (with validations) ─────────────────────

    public Map<String, Object> closeAccount(Long id, String reason, String closedBy) {
        Map<String, Object> result = new HashMap<>();
        Optional<SalaryAccount> opt = salaryAccountRepository.findById(id);
        if (opt.isEmpty()) {
            result.put("success", false);
            result.put("message", "Salary account not found");
            return result;
        }
        SalaryAccount acc = opt.get();

        // Already closed
        if ("Closed".equals(acc.getStatus())) {
            result.put("success", false);
            result.put("message", "Account is already closed");
            return result;
        }

        List<String> blockers = new ArrayList<>();

        // 1. Check balance — must be zero
        double balance = acc.getBalance() != null ? acc.getBalance() : 0.0;
        double savingsBalance = acc.getSavingsBalance() != null ? acc.getSavingsBalance() : 0.0;
        if (balance > 0 || savingsBalance > 0) {
            blockers.add("Account has remaining balance of ₹" + String.format("%.2f", balance)
                    + (savingsBalance > 0 ? " (+ ₹" + String.format("%.2f", savingsBalance) + " in savings)" : "")
                    + ". Please withdraw or transfer all funds before closing.");
        }

        // 2. Check outstanding salary advances (loans)
        List<SalaryAdvanceRequest> activeAdvances = advanceRequestRepository.findActiveAdvances(id);
        if (activeAdvances != null && !activeAdvances.isEmpty()) {
            double totalOutstanding = activeAdvances.stream()
                    .mapToDouble(a -> a.getAdvanceAmount() != null ? a.getAdvanceAmount() : 0.0)
                    .sum();
            blockers.add("Account has " + activeAdvances.size() + " outstanding salary advance(s) totaling ₹"
                    + String.format("%.2f", totalOutstanding) + ". All advances must be repaid before closing.");
        }

        // 3. Check pending/approved cheques that haven't been completed
        long pendingCheques = chequeRequestRepository.countBySalaryAccountIdAndStatusIn(id,
                Arrays.asList("PENDING", "APPROVED"));
        if (pendingCheques > 0) {
            blockers.add("Account has " + pendingCheques + " pending/approved cheque(s). All cheques must be completed, cancelled, or rejected before closing.");
        }

        if (!blockers.isEmpty()) {
            result.put("success", false);
            result.put("message", "Cannot close account due to the following reasons:");
            result.put("blockers", blockers);
            return result;
        }

        // All checks passed — close the account
        acc.setStatus("Closed");
        acc.setClosedAt(LocalDateTime.now());
        acc.setClosedReason(reason != null && !reason.isEmpty() ? reason : "Closed by manager");
        acc.setClosedBy(closedBy != null ? closedBy : "Manager");
        acc.setNetBankingEnabled(false);
        acc.setDebitCardStatus("Blocked");
        acc.setUpiEnabled(false);
        acc.setUpdatedAt(LocalDateTime.now());
        salaryAccountRepository.save(acc);

        result.put("success", true);
        result.put("message", "Salary account closed successfully at " + acc.getClosedAt());
        result.put("account", acc);
        return result;
    }

    // ─── Close Account Pre-check (get blockers without closing) ──

    public Map<String, Object> closeAccountPreCheck(Long id) {
        Map<String, Object> result = new HashMap<>();
        Optional<SalaryAccount> opt = salaryAccountRepository.findById(id);
        if (opt.isEmpty()) {
            result.put("success", false);
            result.put("message", "Salary account not found");
            return result;
        }
        SalaryAccount acc = opt.get();

        if ("Closed".equals(acc.getStatus())) {
            result.put("success", false);
            result.put("message", "Account is already closed");
            result.put("closedAt", acc.getClosedAt());
            return result;
        }

        double balance = acc.getBalance() != null ? acc.getBalance() : 0.0;
        double savingsBalance = acc.getSavingsBalance() != null ? acc.getSavingsBalance() : 0.0;

        List<SalaryAdvanceRequest> activeAdvances = advanceRequestRepository.findActiveAdvances(id);
        double outstandingLoans = (activeAdvances != null) ? activeAdvances.stream()
                .mapToDouble(a -> a.getAdvanceAmount() != null ? a.getAdvanceAmount() : 0.0).sum() : 0.0;

        long pendingCheques = chequeRequestRepository.countBySalaryAccountIdAndStatusIn(id,
                Arrays.asList("PENDING", "APPROVED"));

        boolean canClose = (balance <= 0) && (savingsBalance <= 0) && (outstandingLoans <= 0) && (pendingCheques == 0);

        result.put("success", true);
        result.put("canClose", canClose);
        result.put("balance", balance);
        result.put("savingsBalance", savingsBalance);
        result.put("outstandingLoans", outstandingLoans);
        result.put("activeAdvanceCount", activeAdvances != null ? activeAdvances.size() : 0);
        result.put("pendingCheques", pendingCheques);
        result.put("accountStatus", acc.getStatus());
        return result;
    }

    // ─── Salary Credit (simulate monthly salary) ──────────────

    public SalaryTransaction creditSalary(Long accountId) {
        Optional<SalaryAccount> opt = salaryAccountRepository.findById(accountId);
        if (opt.isEmpty()) return null;
        SalaryAccount acc = opt.get();
        if (!"Active".equals(acc.getStatus())) return null;

        Double prevBalance = acc.getBalance() != null ? acc.getBalance() : 0.0;
        Double salary = acc.getMonthlySalary() != null ? acc.getMonthlySalary() : 0.0;
        Double newBalance = prevBalance + salary;

        acc.setBalance(newBalance);
        acc.setUpdatedAt(LocalDateTime.now());
        salaryAccountRepository.save(acc);

        SalaryTransaction txn = new SalaryTransaction();
        txn.setSalaryAccountId(accountId);
        txn.setAccountNumber(acc.getAccountNumber());
        txn.setSalaryAmount(salary);
        txn.setCreditDate(LocalDateTime.now());
        txn.setCompanyName(acc.getCompanyName());
        txn.setDescription("Salary Credit - " + acc.getCompanyName());
        txn.setType("Credit");
        txn.setPreviousBalance(prevBalance);
        txn.setNewBalance(newBalance);
        txn.setStatus("Success");
        txn.setCreatedAt(LocalDateTime.now());
        return salaryTransactionRepository.save(txn);
    }

    // ─── Transactions ─────────────────────────────────────────

    public List<SalaryTransaction> getTransactions(Long accountId) {
        return salaryTransactionRepository.findBySalaryAccountIdOrderByCreatedAtDesc(accountId);
    }

    public List<SalaryTransaction> getTransactionsByAccountNumber(String accountNumber) {
        return salaryTransactionRepository.findByAccountNumberOrderByCreatedAtDesc(accountNumber);
    }

    // ─── Statistics ───────────────────────────────────────────

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalAccounts", salaryAccountRepository.count());
        stats.put("activeAccounts", salaryAccountRepository.countByStatus("Active"));
        stats.put("frozenAccounts", salaryAccountRepository.countByStatus("Frozen"));
        stats.put("closedAccounts", salaryAccountRepository.countByStatus("Closed"));
        stats.put("totalMonthlySalary", salaryAccountRepository.totalActiveMonthlySalary());

        List<Object[]> byCompany = salaryAccountRepository.countByCompany();
        Map<String, Long> companyMap = new LinkedHashMap<>();
        for (Object[] row : byCompany) {
            companyMap.put(String.valueOf(row[0]), (Long) row[1]);
        }
        stats.put("companiesLinked", companyMap);
        stats.put("totalCompanies", companyMap.size());
        return stats;
    }

    // ─── Authentication ────────────────────────────────────────

    public SalaryAccount getByCustomerId(String customerId) {
        return salaryAccountRepository.findByCustomerId(customerId);
    }

    public SalaryAccount getByEmployeeId(String employeeId) {
        return salaryAccountRepository.findByEmployeeId(employeeId);
    }

    public Map<String, Object> linkEmployeeId(Long salaryAccountId, String employeeId) {
        Map<String, Object> result = new HashMap<>();
        Optional<SalaryAccount> opt = salaryAccountRepository.findById(salaryAccountId);
        if (opt.isEmpty()) {
            result.put("success", false);
            result.put("message", "Salary account not found");
            return result;
        }
        SalaryAccount account = opt.get();
        if (Boolean.TRUE.equals(account.getEmployeeIdLinked())) {
            result.put("success", false);
            result.put("message", "Employee ID already linked to this salary account. This is a one-time operation.");
            return result;
        }
        // Check if employeeId is already linked to another salary account
        SalaryAccount existing = salaryAccountRepository.findByEmployeeId(employeeId);
        if (existing != null) {
            result.put("success", false);
            result.put("message", "Employee ID '" + employeeId + "' is already linked to salary account " + existing.getAccountNumber());
            return result;
        }
        account.setEmployeeId(employeeId);
        account.setEmployeeIdLinked(true);
        salaryAccountRepository.save(account);
        result.put("success", true);
        result.put("message", "Employee ID linked successfully to salary account " + account.getAccountNumber());
        result.put("account", account);
        return result;
    }

    public Map<String, Object> signup(String customerId, String password) {
        Map<String, Object> result = new HashMap<>();
        SalaryAccount account = salaryAccountRepository.findByCustomerId(customerId);
        if (account == null) {
            result.put("success", false);
            result.put("message", "No salary account found with this Customer ID");
            return result;
        }
        if (!"Active".equals(account.getStatus())) {
            result.put("success", false);
            result.put("message", "Account is not active. Current status: " + account.getStatus());
            return result;
        }
        if (Boolean.TRUE.equals(account.getPasswordSet())) {
            result.put("success", false);
            result.put("message", "Password already set. Please login with your account number.");
            return result;
        }
        account.setPassword(passwordEncoder.encode(password));
        account.setPasswordSet(true);
        account.setUpdatedAt(LocalDateTime.now());
        salaryAccountRepository.save(account);

        result.put("success", true);
        result.put("message", "Password set successfully. You can now login.");
        result.put("accountNumber", account.getAccountNumber());
        result.put("employeeName", account.getEmployeeName());
        return result;
    }

    public Map<String, Object> authenticate(String accountNumber, String password) {
        Map<String, Object> result = new HashMap<>();
        SalaryAccount account = salaryAccountRepository.findByAccountNumber(accountNumber);
        if (account == null) {
            result.put("success", false);
            result.put("message", "No salary account found with this account number");
            return result;
        }
        if (!Boolean.TRUE.equals(account.getPasswordSet()) || account.getPassword() == null) {
            result.put("success", false);
            result.put("message", "Password not set. Please sign up first using your Customer ID.");
            return result;
        }
        if (!"Active".equals(account.getStatus())) {
            result.put("success", false);
            result.put("message", "Account is not active. Current status: " + account.getStatus());
            return result;
        }
        // Check if account is locked
        if (Boolean.TRUE.equals(account.getAccountLocked())) {
            result.put("success", false);
            result.put("accountLocked", true);
            result.put("message", "Account is locked due to multiple failed login attempts. Please contact manager to unlock.");
            result.put("lockReason", account.getLockReason() != null ? account.getLockReason() : "3 failed login attempts");
            return result;
        }
        if (!passwordEncoder.matches(password, account.getPassword())) {
            // Increment failed login attempts
            int attempts = (account.getFailedLoginAttempts() != null ? account.getFailedLoginAttempts() : 0) + 1;
            account.setFailedLoginAttempts(attempts);
            account.setLastFailedLoginTime(LocalDateTime.now());

            if (attempts >= 3) {
                account.setAccountLocked(true);
                account.setLockReason("Account locked after 3 failed password attempts");
                salaryAccountRepository.save(account);
                result.put("success", false);
                result.put("accountLocked", true);
                result.put("message", "Account locked due to 3 failed login attempts. Please contact manager to unlock.");
                return result;
            }

            salaryAccountRepository.save(account);
            result.put("success", false);
            result.put("failedAttempts", attempts);
            result.put("remainingAttempts", 3 - attempts);
            result.put("message", "Invalid password. " + (3 - attempts) + " attempt(s) remaining before account lock.");
            return result;
        }

        // Successful login - reset failed attempts
        if (account.getFailedLoginAttempts() != null && account.getFailedLoginAttempts() > 0) {
            account.setFailedLoginAttempts(0);
            account.setLastFailedLoginTime(null);
            salaryAccountRepository.save(account);
        }

        result.put("success", true);
        result.put("message", "Login successful");
        result.put("account", account);
        return result;
    }

    // ─── Transfer Money ─────────────────────────────────────

    public Map<String, Object> transferMoney(Long accountId, String recipientAccount, String recipientIfsc,
                                              Double amount, String remark, String transactionPin) {
        Map<String, Object> result = new HashMap<>();
        Optional<SalaryAccount> opt = salaryAccountRepository.findById(accountId);
        if (opt.isEmpty()) { result.put("success", false); result.put("message", "Account not found"); return result; }
        SalaryAccount acc = opt.get();
        if (!"Active".equals(acc.getStatus())) { result.put("success", false); result.put("message", "Account is not active"); return result; }

        // Verify transaction PIN
        if (!Boolean.TRUE.equals(acc.getTransactionPinSet()) || acc.getTransactionPin() == null) {
            result.put("success", false); result.put("message", "Transaction PIN not set. Please set it first."); return result;
        }
        if (!passwordEncoder.matches(transactionPin, acc.getTransactionPin())) {
            result.put("success", false); result.put("message", "Invalid transaction PIN"); return result;
        }

        // Fraud detection: check for rapid transactions in last 5 minutes
        List<SalaryNormalTransaction> recent = normalTransactionRepository.findRecentByAccount(accountId, LocalDateTime.now().minusMinutes(5));
        if (recent.size() >= 5) {
            result.put("success", false); result.put("message", "Too many transactions in short time. Please wait."); return result;
        }

        double charge = calculateTransferCharge(amount);
        double totalDebit = amount + charge;
        double prevBalance = acc.getBalance() != null ? acc.getBalance() : 0.0;

        if (prevBalance < totalDebit) { result.put("success", false); result.put("message", "Insufficient balance. Required: ₹" + totalDebit + " (includes ₹" + charge + " bank charge)"); return result; }

        double newBalance = prevBalance - totalDebit;
        acc.setBalance(newBalance);
        acc.setUpdatedAt(LocalDateTime.now());
        salaryAccountRepository.save(acc);

        SalaryNormalTransaction txn = new SalaryNormalTransaction();
        txn.setSalaryAccountId(accountId);
        txn.setAccountNumber(acc.getAccountNumber());
        txn.setType("Transfer");
        txn.setAmount(amount);
        txn.setCharge(charge);
        txn.setRecipientAccount(recipientAccount);
        txn.setRecipientIfsc(recipientIfsc);
        txn.setRemark(remark);
        txn.setPreviousBalance(prevBalance);
        txn.setNewBalance(newBalance);
        txn.setStatus("Success");
        normalTransactionRepository.save(txn);

        result.put("success", true);
        result.put("message", "Transfer of ₹" + amount + " successful. Bank charge: ₹" + charge);
        result.put("transaction", txn);
        result.put("newBalance", newBalance);
        return result;
    }

    // ─── Withdraw Money ──────────────────────────────────────

    public Map<String, Object> withdrawMoney(Long accountId, Double amount, String transactionPin) {
        Map<String, Object> result = new HashMap<>();
        Optional<SalaryAccount> opt = salaryAccountRepository.findById(accountId);
        if (opt.isEmpty()) { result.put("success", false); result.put("message", "Account not found"); return result; }
        SalaryAccount acc = opt.get();
        if (!"Active".equals(acc.getStatus())) { result.put("success", false); result.put("message", "Account is not active"); return result; }

        if (!Boolean.TRUE.equals(acc.getTransactionPinSet()) || acc.getTransactionPin() == null) {
            result.put("success", false); result.put("message", "Transaction PIN not set. Please set it first."); return result;
        }
        if (!passwordEncoder.matches(transactionPin, acc.getTransactionPin())) {
            result.put("success", false); result.put("message", "Invalid transaction PIN"); return result;
        }

        double charge = calculateWithdrawCharge(amount);
        double totalDebit = amount + charge;
        double prevBalance = acc.getBalance() != null ? acc.getBalance() : 0.0;

        if (prevBalance < totalDebit) { result.put("success", false); result.put("message", "Insufficient balance"); return result; }

        double newBalance = prevBalance - totalDebit;
        acc.setBalance(newBalance);
        acc.setUpdatedAt(LocalDateTime.now());
        salaryAccountRepository.save(acc);

        SalaryNormalTransaction txn = new SalaryNormalTransaction();
        txn.setSalaryAccountId(accountId);
        txn.setAccountNumber(acc.getAccountNumber());
        txn.setType("Withdrawal");
        txn.setAmount(amount);
        txn.setCharge(charge);
        txn.setRemark("ATM Withdrawal");
        txn.setPreviousBalance(prevBalance);
        txn.setNewBalance(newBalance);
        txn.setStatus("Success");
        normalTransactionRepository.save(txn);

        result.put("success", true);
        result.put("message", "Withdrawal of ₹" + amount + " successful. Bank charge: ₹" + charge);
        result.put("transaction", txn);
        result.put("newBalance", newBalance);
        return result;
    }

    // ─── Normal Transactions ─────────────────────────────────

    public List<SalaryNormalTransaction> getNormalTransactions(Long accountId) {
        return normalTransactionRepository.findBySalaryAccountIdOrderByCreatedAtDesc(accountId);
    }

    // ─── Bank Charges Summary ────────────────────────────────

    public Map<String, Object> getBankCharges(Long accountId) {
        Map<String, Object> result = new HashMap<>();
        Double totalCharges = normalTransactionRepository.totalChargesByAccount(accountId);
        Double totalTransferred = normalTransactionRepository.sumAmountByAccountAndType(accountId, "Transfer");
        Double totalWithdrawn = normalTransactionRepository.sumAmountByAccountAndType(accountId, "Withdrawal");
        result.put("totalCharges", totalCharges != null ? totalCharges : 0.0);
        result.put("totalTransferred", totalTransferred != null ? totalTransferred : 0.0);
        result.put("totalWithdrawn", totalWithdrawn != null ? totalWithdrawn : 0.0);
        List<SalaryNormalTransaction> all = normalTransactionRepository.findBySalaryAccountIdOrderByCreatedAtDesc(accountId);
        List<Map<String, Object>> chargeList = new ArrayList<>();
        for (SalaryNormalTransaction t : all) {
            if (t.getCharge() != null && t.getCharge() > 0) {
                Map<String, Object> item = new HashMap<>();
                item.put("type", t.getType());
                item.put("amount", t.getAmount());
                item.put("charge", t.getCharge());
                item.put("date", t.getCreatedAt());
                chargeList.add(item);
            }
        }
        result.put("chargeDetails", chargeList);
        return result;
    }

    // ─── Transaction PIN ─────────────────────────────────────

    public Map<String, Object> setTransactionPin(Long accountId, String pin, String password) {
        Map<String, Object> result = new HashMap<>();
        Optional<SalaryAccount> opt = salaryAccountRepository.findById(accountId);
        if (opt.isEmpty()) { result.put("success", false); result.put("message", "Account not found"); return result; }
        SalaryAccount acc = opt.get();

        if (!passwordEncoder.matches(password, acc.getPassword())) {
            result.put("success", false); result.put("message", "Invalid password"); return result;
        }
        if (pin == null || pin.length() != 4 || !pin.matches("\\d{4}")) {
            result.put("success", false); result.put("message", "PIN must be exactly 4 digits"); return result;
        }
        acc.setTransactionPin(passwordEncoder.encode(pin));
        acc.setTransactionPinSet(true);
        acc.setUpdatedAt(LocalDateTime.now());
        salaryAccountRepository.save(acc);
        result.put("success", true);
        result.put("message", "Transaction PIN set successfully");
        return result;
    }

    public Map<String, Object> changeTransactionPin(Long accountId, String oldPin, String newPin) {
        Map<String, Object> result = new HashMap<>();
        Optional<SalaryAccount> opt = salaryAccountRepository.findById(accountId);
        if (opt.isEmpty()) { result.put("success", false); result.put("message", "Account not found"); return result; }
        SalaryAccount acc = opt.get();

        if (!Boolean.TRUE.equals(acc.getTransactionPinSet())) {
            result.put("success", false); result.put("message", "Transaction PIN not set yet"); return result;
        }
        if (!passwordEncoder.matches(oldPin, acc.getTransactionPin())) {
            result.put("success", false); result.put("message", "Invalid current PIN"); return result;
        }
        if (newPin == null || newPin.length() != 4 || !newPin.matches("\\d{4}")) {
            result.put("success", false); result.put("message", "New PIN must be exactly 4 digits"); return result;
        }
        acc.setTransactionPin(passwordEncoder.encode(newPin));
        acc.setUpdatedAt(LocalDateTime.now());
        salaryAccountRepository.save(acc);
        result.put("success", true);
        result.put("message", "Transaction PIN changed successfully");
        return result;
    }

    // ─── Blocked Account Management ─────────────────────────

    public List<SalaryAccount> getBlockedAccounts() {
        return salaryAccountRepository.findByAccountLockedTrue();
    }

    public SalaryAccount unblockAccount(Long accountId) {
        Optional<SalaryAccount> opt = salaryAccountRepository.findById(accountId);
        if (opt.isEmpty()) return null;
        SalaryAccount account = opt.get();
        account.setAccountLocked(false);
        account.setFailedLoginAttempts(0);
        account.setLastFailedLoginTime(null);
        account.setLockReason(null);
        return salaryAccountRepository.save(account);
    }

    public SalaryAccount resetAccountPassword(Long accountId, String newPassword) {
        Optional<SalaryAccount> opt = salaryAccountRepository.findById(accountId);
        if (opt.isEmpty()) return null;
        SalaryAccount account = opt.get();
        account.setPassword(passwordEncoder.encode(newPassword));
        account.setAccountLocked(false);
        account.setFailedLoginAttempts(0);
        account.setLastFailedLoginTime(null);
        account.setLockReason(null);
        return salaryAccountRepository.save(account);
    }

    // ─── Login Activity Tracking ─────────────────────────────

    public void recordLoginActivity(Long accountId, String accountNumber, String activityType, String ipAddress, String deviceInfo, String status) {
        SalaryLoginActivity activity = new SalaryLoginActivity();
        activity.setSalaryAccountId(accountId);
        activity.setAccountNumber(accountNumber);
        activity.setActivityType(activityType);
        activity.setIpAddress(ipAddress);
        activity.setDeviceInfo(deviceInfo);
        activity.setStatus(status);
        loginActivityRepository.save(activity);
    }

    public List<SalaryLoginActivity> getLoginActivity(Long accountId) {
        return loginActivityRepository.findBySalaryAccountIdOrderByCreatedAtDesc(accountId);
    }

    // ─── Employee Dashboard Stats ────────────────────────────

    public Map<String, Object> getEmployeeDashboardStats(Long accountId) {
        Map<String, Object> stats = new HashMap<>();
        Optional<SalaryAccount> opt = salaryAccountRepository.findById(accountId);
        if (opt.isEmpty()) return stats;
        SalaryAccount acc = opt.get();

        stats.put("balance", acc.getBalance());
        stats.put("companyName", acc.getCompanyName());
        stats.put("salaryCreditDate", acc.getSalaryCreditDate());
        stats.put("monthlySalary", acc.getMonthlySalary());
        stats.put("transactionPinSet", acc.getTransactionPinSet());

        // Last salary credited
        List<SalaryTransaction> salaryTxns = salaryTransactionRepository.findBySalaryAccountIdOrderByCreatedAtDesc(accountId);
        if (!salaryTxns.isEmpty()) {
            SalaryTransaction last = salaryTxns.get(0);
            stats.put("lastSalaryCredited", last.getSalaryAmount());
            stats.put("lastSalaryDate", last.getCreditDate());
        }

        // Total salary received this year
        double totalSalaryThisYear = 0;
        int currentYear = LocalDateTime.now().getYear();
        for (SalaryTransaction t : salaryTxns) {
            if (t.getCreditDate() != null && t.getCreditDate().getYear() == currentYear) {
                totalSalaryThisYear += t.getSalaryAmount() != null ? t.getSalaryAmount() : 0;
            }
        }
        stats.put("totalSalaryThisYear", totalSalaryThisYear);
        stats.put("totalSalaryCredits", salaryTxns.size());

        // Total transfers and withdrawals
        Double totalTransferred = normalTransactionRepository.sumAmountByAccountAndType(accountId, "Transfer");
        Double totalWithdrawn = normalTransactionRepository.sumAmountByAccountAndType(accountId, "Withdrawal");
        stats.put("totalTransferred", totalTransferred != null ? totalTransferred : 0.0);
        stats.put("totalWithdrawn", totalWithdrawn != null ? totalWithdrawn : 0.0);

        return stats;
    }

    // ─── Change Password ─────────────────────────────────────

    public Map<String, Object> changePassword(Long accountId, String currentPassword, String newPassword) {
        Map<String, Object> result = new HashMap<>();
        Optional<SalaryAccount> opt = salaryAccountRepository.findById(accountId);
        if (opt.isEmpty()) { result.put("success", false); result.put("message", "Account not found"); return result; }
        SalaryAccount acc = opt.get();
        if (!passwordEncoder.matches(currentPassword, acc.getPassword())) {
            result.put("success", false); result.put("message", "Current password is incorrect"); return result;
        }
        if (newPassword.length() < 6) {
            result.put("success", false); result.put("message", "New password must be at least 6 characters"); return result;
        }
        acc.setPassword(passwordEncoder.encode(newPassword));
        acc.setUpdatedAt(LocalDateTime.now());
        salaryAccountRepository.save(acc);
        result.put("success", true);
        result.put("message", "Password changed successfully");
        return result;
    }

    // ─── Salary Slip Data ────────────────────────────────────

    public Map<String, Object> getSalarySlipData(Long accountId, Long transactionId) {
        Map<String, Object> result = new HashMap<>();
        Optional<SalaryAccount> accOpt = salaryAccountRepository.findById(accountId);
        if (accOpt.isEmpty()) { result.put("success", false); result.put("message", "Account not found"); return result; }
        SalaryAccount acc = accOpt.get();

        Optional<SalaryTransaction> txnOpt = salaryTransactionRepository.findById(transactionId);
        if (txnOpt.isEmpty()) { result.put("success", false); result.put("message", "Transaction not found"); return result; }
        SalaryTransaction txn = txnOpt.get();

        result.put("success", true);
        result.put("employeeName", acc.getEmployeeName());
        result.put("companyName", acc.getCompanyName());
        result.put("accountNumber", acc.getAccountNumber());
        result.put("designation", acc.getDesignation());
        result.put("salaryAmount", txn.getSalaryAmount());
        result.put("creditDate", txn.getCreditDate());
        result.put("bankName", "EzyBank Neo");
        result.put("branchName", acc.getBranchName());
        result.put("ifscCode", acc.getIfscCode());
        result.put("customerId", acc.getCustomerId());
        result.put("transactionId", txn.getId());
        return result;
    }

    // ─── Helpers ──────────────────────────────────────────────

    private String generateAccountNumber() {
        return "50" + String.format("%08d", new Random().nextInt(99999999));
    }

    private String generateCustomerId() {
        return "CUST" + String.format("%05d", new Random().nextInt(99999));
    }

    private String generateDebitCardNumber() {
        StringBuilder sb = new StringBuilder();
        Random r = new Random();
        for (int i = 0; i < 16; i++) sb.append(r.nextInt(10));
        return sb.toString();
    }

    private String generateCvv() {
        return String.format("%03d", new Random().nextInt(1000));
    }

    private String generateExpiryDate() {
        java.time.LocalDate now = java.time.LocalDate.now();
        java.time.LocalDate expiry = now.plusYears(5);
        return String.format("%02d/%d", expiry.getMonthValue(), expiry.getYear() % 100);
    }

    private double calculateTransferCharge(double amount) {
        if (amount <= 10000) return 0;
        if (amount <= 50000) return 5.0;
        if (amount <= 200000) return 15.0;
        return 25.0;
    }

    private double calculateWithdrawCharge(double amount) {
        if (amount <= 25000) return 0;
        return 10.0;
    }

    // ─── Verify Recipient Account ────────────────────────────

    public Map<String, Object> verifyRecipientAccount(String accountNumber) {
        Map<String, Object> result = new HashMap<>();

        // Check salary accounts first
        SalaryAccount sal = salaryAccountRepository.findByAccountNumber(accountNumber);
        if (sal != null) {
            result.put("found", true);
            result.put("name", sal.getEmployeeName());
            result.put("accountType", "Salary Account");
            result.put("bankName", "NEO BANK");
            result.put("ifscCode", sal.getIfscCode());
            return result;
        }

        // Check regular (savings) accounts
        Account acc = accountRepository.findByAccountNumber(accountNumber);
        if (acc != null) {
            result.put("found", true);
            result.put("name", acc.getName());
            result.put("accountType", "Savings Account");
            result.put("bankName", "NEO BANK");
            result.put("ifscCode", "NEOB0001234");
            return result;
        }

        // Check current accounts
        java.util.Optional<CurrentAccount> currentOpt = currentAccountRepository.findByAccountNumber(accountNumber);
        if (currentOpt.isPresent()) {
            CurrentAccount ca = currentOpt.get();
            result.put("found", true);
            result.put("name", ca.getOwnerName());
            result.put("accountType", "Current Account");
            result.put("bankName", "NEO BANK");
            result.put("ifscCode", ca.getIfscCode() != null ? ca.getIfscCode() : "NEOB0001234");
            return result;
        }

        result.put("found", false);
        result.put("message", "Account not found in NEO BANK. You can still transfer to external accounts.");
        return result;
    }

    // ═══════════════════════════════════════════════════════════
    //  NEW EMPLOYEE DASHBOARD FEATURE METHODS
    // ═══════════════════════════════════════════════════════════

    // ─── Update Profile ──────────────────────────────────────

    public Map<String, Object> updateProfile(Long accountId, Map<String, String> body) {
        Map<String, Object> result = new HashMap<>();
        Optional<SalaryAccount> opt = salaryAccountRepository.findById(accountId);
        if (opt.isEmpty()) { result.put("success", false); result.put("message", "Account not found"); return result; }
        SalaryAccount acc = opt.get();

        if (body.containsKey("address")) acc.setAddress(body.get("address"));
        if (body.containsKey("mobileNumber")) acc.setMobileNumber(body.get("mobileNumber"));
        if (body.containsKey("email")) acc.setEmail(body.get("email"));
        acc.setUpdatedAt(LocalDateTime.now());
        salaryAccountRepository.save(acc);

        result.put("success", true);
        result.put("message", "Profile updated successfully");
        return result;
    }

    // ─── Enable UPI ──────────────────────────────────────────

    public Map<String, Object> enableUpi(Long accountId) {
        Map<String, Object> result = new HashMap<>();
        Optional<SalaryAccount> opt = salaryAccountRepository.findById(accountId);
        if (opt.isEmpty()) { result.put("success", false); result.put("message", "Account not found"); return result; }
        SalaryAccount acc = opt.get();

        if (acc.getUpiEnabled() != null && acc.getUpiEnabled()) {
            result.put("success", false);
            result.put("message", "UPI is already enabled");
            return result;
        }

        String upiId = acc.getAccountNumber() + "@neobank";
        // Ensure UPI ID is not already taken by any other account type
        if (currentAccountRepository.findByUpiId(upiId).isPresent() ||
                userRepository.findByUpiId(upiId).isPresent() ||
                salaryAccountRepository.findByUpiId(upiId) != null) {
            result.put("success", false);
            result.put("message", "UPI ID " + upiId + " is already in use by another account");
            return result;
        }
        acc.setUpiId(upiId);
        acc.setUpiEnabled(true);
        acc.setUpdatedAt(LocalDateTime.now());
        salaryAccountRepository.save(acc);

        result.put("success", true);
        result.put("message", "UPI enabled successfully! Your UPI ID: " + upiId);
        result.put("upiId", upiId);
        return result;
    }

    // ─── Send UPI Payment ────────────────────────────────────

    public Map<String, Object> sendUpiPayment(Long accountId, String recipientUpi, Double amount, String remark, String transactionPin) {
        Map<String, Object> result = new HashMap<>();
        Optional<SalaryAccount> opt = salaryAccountRepository.findById(accountId);
        if (opt.isEmpty()) { result.put("success", false); result.put("message", "Account not found"); return result; }
        SalaryAccount acc = opt.get();

        if (acc.getUpiEnabled() == null || !acc.getUpiEnabled()) {
            result.put("success", false); result.put("message", "UPI is not enabled"); return result;
        }
        if (!passwordEncoder.matches(transactionPin, acc.getTransactionPin())) {
            result.put("success", false); result.put("message", "Invalid transaction PIN"); return result;
        }
        if (acc.getBalance() < amount) {
            result.put("success", false); result.put("message", "Insufficient balance"); return result;
        }

        // Deduct balance
        acc.setBalance(acc.getBalance() - amount);
        acc.setUpdatedAt(LocalDateTime.now());
        salaryAccountRepository.save(acc);

        // Create UPI transaction record
        SalaryUpiTransaction txn = new SalaryUpiTransaction();
        txn.setSalaryAccountId(accountId);
        txn.setAccountNumber(acc.getAccountNumber());
        txn.setUpiId(acc.getUpiId());
        txn.setRecipientUpi(recipientUpi);
        txn.setRecipientName(recipientUpi.split("@")[0]);
        txn.setType("DEBIT");
        txn.setAmount(amount);
        txn.setRemark(remark);
        txn.setStatus("SUCCESS");
        txn.setTransactionRef("UPI" + System.currentTimeMillis());
        txn.setCreatedAt(LocalDateTime.now());
        upiTransactionRepository.save(txn);

        // AI Fraud Detection — flag large transactions
        if (amount > 50000) {
            SalaryFraudAlert alert = new SalaryFraudAlert();
            alert.setSalaryAccountId(accountId);
            alert.setAccountNumber(acc.getAccountNumber());
            alert.setAlertType("LARGE_UPI_TRANSACTION");
            alert.setSeverity("MEDIUM");
            alert.setDescription("Large UPI payment of Rs." + amount + " to " + recipientUpi);
            alert.setAmount(amount);
            alert.setResolved(false);
            alert.setCreatedAt(LocalDateTime.now());
            fraudAlertRepository.save(alert);
        }

        result.put("success", true);
        result.put("message", "Payment of Rs." + amount + " sent to " + recipientUpi + " successfully");
        return result;
    }

    // ─── Get UPI Transactions ────────────────────────────────

    public List<SalaryUpiTransaction> getUpiTransactions(Long accountId) {
        return upiTransactionRepository.findBySalaryAccountIdOrderByCreatedAtDesc(accountId);
    }

    // ─── Loan Eligibility ────────────────────────────────────

    public Map<String, Object> getLoanEligibility(Long accountId) {
        Map<String, Object> result = new HashMap<>();
        Optional<SalaryAccount> opt = salaryAccountRepository.findById(accountId);
        if (opt.isEmpty()) { result.put("success", false); result.put("message", "Account not found"); return result; }
        SalaryAccount acc = opt.get();

        double salary = acc.getMonthlySalary() != null ? acc.getMonthlySalary() : 0;
        result.put("success", true);
        result.put("monthlySalary", salary);
        result.put("personalLoan", salary * 10);
        result.put("homeLoan", salary * 60);
        result.put("carLoan", salary * 15);
        result.put("educationLoan", salary * 25);
        result.put("generalLimit", salary * 20);
        return result;
    }

    // ─── Toggle Auto Savings ─────────────────────────────────

    public Map<String, Object> toggleAutoSavings(Long accountId, Boolean enable, Double percentage) {
        Map<String, Object> result = new HashMap<>();
        Optional<SalaryAccount> opt = salaryAccountRepository.findById(accountId);
        if (opt.isEmpty()) { result.put("success", false); result.put("message", "Account not found"); return result; }
        SalaryAccount acc = opt.get();

        acc.setAutoSavingsEnabled(enable);
        if (percentage != null && percentage > 0 && percentage <= 50) {
            acc.setAutoSavingsPercentage(percentage);
        }
        acc.setUpdatedAt(LocalDateTime.now());
        salaryAccountRepository.save(acc);

        result.put("success", true);
        result.put("message", enable ? "Auto savings enabled at " + acc.getAutoSavingsPercentage() + "%" : "Auto savings disabled");
        return result;
    }

    // ─── Get Auto Savings Info ───────────────────────────────

    public Map<String, Object> getAutoSavingsInfo(Long accountId) {
        Map<String, Object> result = new HashMap<>();
        Optional<SalaryAccount> opt = salaryAccountRepository.findById(accountId);
        if (opt.isEmpty()) { result.put("success", false); result.put("message", "Account not found"); return result; }
        SalaryAccount acc = opt.get();

        result.put("success", true);
        result.put("autoSavingsEnabled", acc.getAutoSavingsEnabled());
        result.put("autoSavingsPercentage", acc.getAutoSavingsPercentage());
        result.put("savingsBalance", acc.getSavingsBalance());
        result.put("monthlySavings", (acc.getMonthlySalary() != null ? acc.getMonthlySalary() : 0) * (acc.getAutoSavingsPercentage() != null ? acc.getAutoSavingsPercentage() : 10) / 100);
        return result;
    }

    // ─── Withdraw from Savings ───────────────────────────────

    public Map<String, Object> withdrawSavings(Long accountId, Double amount, String transactionPin) {
        Map<String, Object> result = new HashMap<>();
        Optional<SalaryAccount> opt = salaryAccountRepository.findById(accountId);
        if (opt.isEmpty()) { result.put("success", false); result.put("message", "Account not found"); return result; }
        SalaryAccount acc = opt.get();

        if (!passwordEncoder.matches(transactionPin, acc.getTransactionPin())) {
            result.put("success", false); result.put("message", "Invalid transaction PIN"); return result;
        }
        Double savBal = acc.getSavingsBalance() != null ? acc.getSavingsBalance() : 0;
        if (amount > savBal) {
            result.put("success", false); result.put("message", "Insufficient savings balance"); return result;
        }

        acc.setSavingsBalance(savBal - amount);
        acc.setBalance(acc.getBalance() + amount);
        acc.setUpdatedAt(LocalDateTime.now());
        salaryAccountRepository.save(acc);

        result.put("success", true);
        result.put("message", "Rs." + amount + " withdrawn from savings to main account");
        return result;
    }

    // ─── Request Salary Advance ──────────────────────────────

    public Map<String, Object> requestSalaryAdvance(Long accountId, Double amount, String reason) {
        Map<String, Object> result = new HashMap<>();
        Optional<SalaryAccount> opt = salaryAccountRepository.findById(accountId);
        if (opt.isEmpty()) { result.put("success", false); result.put("message", "Account not found"); return result; }
        SalaryAccount acc = opt.get();

        double salary = acc.getMonthlySalary() != null ? acc.getMonthlySalary() : 0;
        double advanceLimit = salary * 0.5;

        if (amount > advanceLimit) {
            result.put("success", false);
            result.put("message", "Amount exceeds advance limit of Rs." + advanceLimit);
            return result;
        }

        SalaryAdvanceRequest req = new SalaryAdvanceRequest();
        req.setSalaryAccountId(accountId);
        req.setAccountNumber(acc.getAccountNumber());
        req.setEmployeeName(acc.getEmployeeName());
        req.setMonthlySalary(salary);
        req.setAdvanceAmount(amount);
        req.setAdvanceLimit(advanceLimit);
        req.setReason(reason);
        req.setStatus("APPROVED");
        req.setApprovedAt(LocalDateTime.now());
        req.setRepaid(false);
        req.setCreatedAt(LocalDateTime.now());
        advanceRequestRepository.save(req);

        // Credit advance to account
        acc.setBalance(acc.getBalance() + amount);
        acc.setUpdatedAt(LocalDateTime.now());
        salaryAccountRepository.save(acc);

        result.put("success", true);
        result.put("message", "Salary advance of Rs." + amount + " approved and credited to your account");
        return result;
    }

    // ─── Get Advance Requests ────────────────────────────────

    public List<SalaryAdvanceRequest> getAdvanceRequests(Long accountId) {
        return advanceRequestRepository.findBySalaryAccountIdOrderByCreatedAtDesc(accountId);
    }

    // ─── Get Advance Info ────────────────────────────────────

    public Map<String, Object> getAdvanceInfo(Long accountId) {
        Map<String, Object> result = new HashMap<>();
        Optional<SalaryAccount> opt = salaryAccountRepository.findById(accountId);
        if (opt.isEmpty()) { result.put("success", false); result.put("message", "Account not found"); return result; }
        SalaryAccount acc = opt.get();

        double salary = acc.getMonthlySalary() != null ? acc.getMonthlySalary() : 0;
        List<SalaryAdvanceRequest> activeAdvances = advanceRequestRepository.findActiveAdvances(accountId);
        Long pendingCount = advanceRequestRepository.countPendingByAccount(accountId);

        result.put("success", true);
        result.put("monthlySalary", salary);
        result.put("advanceLimit", salary * 0.5);
        result.put("activeAdvances", activeAdvances.size());
        result.put("pendingCount", pendingCount);
        return result;
    }

    // ─── Update Debit Card Settings ──────────────────────────

    public Map<String, Object> updateDebitCardSettings(Long accountId, Map<String, Object> settings) {
        Map<String, Object> result = new HashMap<>();
        Optional<SalaryAccount> opt = salaryAccountRepository.findById(accountId);
        if (opt.isEmpty()) { result.put("success", false); result.put("message", "Account not found"); return result; }
        SalaryAccount acc = opt.get();

        if (settings.containsKey("internationalEnabled"))
            acc.setInternationalEnabled((Boolean) settings.get("internationalEnabled"));
        if (settings.containsKey("onlineEnabled"))
            acc.setOnlineEnabled((Boolean) settings.get("onlineEnabled"));
        if (settings.containsKey("contactlessEnabled"))
            acc.setContactlessEnabled((Boolean) settings.get("contactlessEnabled"));
        if (settings.containsKey("dailyLimit"))
            acc.setDailyLimit(((Number) settings.get("dailyLimit")).doubleValue());
        if (settings.containsKey("status"))
            acc.setDebitCardStatus((String) settings.get("status"));

        acc.setUpdatedAt(LocalDateTime.now());
        salaryAccountRepository.save(acc);

        result.put("success", true);
        result.put("message", "Card settings updated successfully");
        return result;
    }

    // ─── Get Debit Card Info ─────────────────────────────────

    public Map<String, Object> generateCardDetails(Long accountId) {
        Map<String, Object> result = new HashMap<>();
        Optional<SalaryAccount> opt = salaryAccountRepository.findById(accountId);
        if (opt.isEmpty()) { result.put("success", false); result.put("message", "Account not found"); return result; }
        SalaryAccount acc = opt.get();
        if (acc.getDebitCardCvv() != null && acc.getDebitCardExpiry() != null) {
            result.put("success", false);
            result.put("message", "Card details already generated. This is a one-time operation.");
            return result;
        }
        if (acc.getDebitCardNumber() == null || acc.getDebitCardNumber().isEmpty()) {
            acc.setDebitCardNumber(generateDebitCardNumber());
        }
        acc.setDebitCardCvv(generateCvv());
        acc.setDebitCardExpiry(generateExpiryDate());
        acc.setUpdatedAt(LocalDateTime.now());
        salaryAccountRepository.save(acc);
        result.put("success", true);
        result.put("message", "Card details generated permanently");
        result.put("cardNumber", acc.getDebitCardNumber());
        result.put("cvv", acc.getDebitCardCvv());
        result.put("expiryDate", acc.getDebitCardExpiry());
        return result;
    }

    public Map<String, Object> getDebitCardInfo(Long accountId) {
        Map<String, Object> result = new HashMap<>();
        Optional<SalaryAccount> opt = salaryAccountRepository.findById(accountId);
        if (opt.isEmpty()) { result.put("success", false); result.put("message", "Account not found"); return result; }
        SalaryAccount acc = opt.get();

        result.put("success", true);
        result.put("cardNumber", acc.getDebitCardNumber());
        result.put("cvv", acc.getDebitCardCvv());
        result.put("expiryDate", acc.getDebitCardExpiry());
        result.put("status", acc.getDebitCardStatus());
        result.put("dailyLimit", acc.getDailyLimit());
        result.put("internationalEnabled", acc.getInternationalEnabled());
        result.put("onlineEnabled", acc.getOnlineEnabled());
        result.put("contactlessEnabled", acc.getContactlessEnabled());
        return result;
    }

    // ─── Get Fraud Alerts ────────────────────────────────────

    public List<SalaryFraudAlert> getFraudAlerts(Long accountId) {
        return fraudAlertRepository.findBySalaryAccountIdOrderByCreatedAtDesc(accountId);
    }

    // ─── Get Fraud Summary ───────────────────────────────────

    public Map<String, Object> getFraudSummary(Long accountId) {
        Map<String, Object> result = new HashMap<>();
        List<SalaryFraudAlert> alerts = fraudAlertRepository.findBySalaryAccountIdOrderByCreatedAtDesc(accountId);
        Long unresolvedCount = fraudAlertRepository.countBySalaryAccountIdAndResolvedFalse(accountId);

        int riskScore = 0;
        if (unresolvedCount != null) {
            riskScore = (int) Math.min(100, unresolvedCount * 15);
        }

        String riskLevel = riskScore < 30 ? "LOW" : riskScore < 70 ? "MEDIUM" : "HIGH";

        result.put("success", true);
        result.put("totalAlerts", alerts.size());
        result.put("unresolvedAlerts", unresolvedCount != null ? unresolvedCount : 0);
        result.put("riskScore", riskScore);
        result.put("riskLevel", riskLevel);
        result.put("alerts", alerts);
        return result;
    }

    // ─── Resolve Fraud Alert ─────────────────────────────────

    public Map<String, Object> resolveFraudAlert(Long alertId) {
        Map<String, Object> result = new HashMap<>();
        Optional<SalaryFraudAlert> opt = fraudAlertRepository.findById(alertId);
        if (opt.isEmpty()) { result.put("success", false); result.put("message", "Alert not found"); return result; }
        SalaryFraudAlert alert = opt.get();

        alert.setResolved(true);
        alert.setResolvedAt(LocalDateTime.now());
        fraudAlertRepository.save(alert);

        result.put("success", true);
        result.put("message", "Fraud alert resolved successfully");
        return result;
    }
}
