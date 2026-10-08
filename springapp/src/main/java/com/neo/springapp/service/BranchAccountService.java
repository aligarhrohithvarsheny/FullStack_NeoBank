package com.neo.springapp.service;

import com.neo.springapp.model.Account;
import com.neo.springapp.model.BranchAccount;
import com.neo.springapp.model.BranchDailyAllocation;
import com.neo.springapp.model.Transaction;
import com.neo.springapp.repository.AccountRepository;
import com.neo.springapp.repository.BranchDailyAllocationRepository;
import com.neo.springapp.repository.BranchAccountRepository;
import com.neo.springapp.repository.CurrentAccountRepository;
import com.neo.springapp.repository.SalaryAccountRepository;
import com.neo.springapp.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages the NeoBank treasury account used for loan funding and collected fees/repayments.
 */
@Service
public class BranchAccountService {

    public static final String DEFAULT_NEOBANK_ACCOUNT = "NEOBANK000001";

    /** Allowed branch account name patterns (must match Neo Bank name to verify). */
    private static final Pattern NEOBANK_NAME_PATTERN = Pattern.compile("(?i).*neo\\s*bank.*");

    @Autowired
    private BranchAccountRepository branchAccountRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private AccountService accountService;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private BranchDailyAllocationRepository allocationRepository;
    @Autowired
    private CurrentAccountRepository currentAccountRepository;
    @Autowired
    private SalaryAccountRepository salaryAccountRepository;

    /** Resolve the single allowed NeoBank treasury account, ignoring legacy non-NeoBank mappings. */
    public String getDepositAccountNumber() {
        return DEFAULT_NEOBANK_ACCOUNT;
    }

    /** Return the verified NeoBank-owned account that can be linked as the treasury account. */
    public List<Map<String, Object>> getAvailableTreasuryAccounts() {
        Account account = accountRepository.findByAccountNumber(DEFAULT_NEOBANK_ACCOUNT);
        if (!isNeoBankOwned(account)) {
            throw new IllegalStateException("The NeoBank treasury account is not available or is not verified.");
        }
        Map<String, Object> option = new HashMap<>();
        option.put("accountNumber", account.getAccountNumber());
        option.put("accountName", account.getName());
        option.put("balance", account.getBalance() == null ? 0.0 : account.getBalance());
        option.put("status", account.getStatus());
        return List.of(option);
    }

    /** Get full branch account record (for display). */
    public BranchAccount getBranchAccount() {
        return branchAccountRepository.findAll().stream().findFirst().orElse(null);
    }

    /** Link the existing, verified NeoBank treasury account from the admin or manager dashboard. */
    @Transactional
    public Map<String, Object> setBranchAccount(String accountNumber, String accountName, String ifscCode, Long adminId) {
        Map<String, Object> result = new HashMap<>();
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "Account number is required.");
            return result;
        }
        accountNumber = accountNumber.trim();
        accountName = accountName != null ? accountName.trim() : "";
        ifscCode = ifscCode != null ? ifscCode.trim() : "";

        if (!DEFAULT_NEOBANK_ACCOUNT.equals(accountNumber)) {
            result.put("success", false);
            result.put("message", "Only the verified NeoBank treasury account can be linked.");
            return result;
        }

        Account existingAccount = accountRepository.findByAccountNumber(accountNumber);
        if (!isNeoBankOwned(existingAccount)) {
            result.put("success", false);
            result.put("message", "The selected account must already exist and be owned by NeoBank.");
            return result;
        }
        if (!accountName.isEmpty() && !accountName.equalsIgnoreCase(existingAccount.getName())) {
            result.put("success", false);
            result.put("message", "The account name must match the verified NeoBank account name.");
            return result;
        }

        BranchAccount branch = getBranchAccount();
        if (branch == null) {
            branch = new BranchAccount();
        }
        branch.setAccountNumber(accountNumber);
        branch.setAccountName(existingAccount.getName());
        branch.setIfscCode(ifscCode.isEmpty() ? null : ifscCode);
        branch.setUpdatedByAdminId(adminId);
        branch.setUpdatedAt(LocalDateTime.now());
        branchAccountRepository.save(branch);

        result.put("success", true);
        result.put("message", "NeoBank treasury account linked successfully.");
        Map<String, Object> branchDetails = new HashMap<>();
        branchDetails.put("accountNumber", branch.getAccountNumber());
        branchDetails.put("accountName", branch.getAccountName());
        branchDetails.put("ifscCode", branch.getIfscCode());
        result.put("branchAccount", branchDetails);
        return result;
    }

    /**
     * Apply a loan/collection movement to the NeoBank treasury account and create its ledger entry.
     * Debits require sufficient available funds; the caller's transaction rolls back on failure.
     */
    @Transactional
    public double recordTreasuryMovement(double amount, boolean credit, String merchant, String description,
                                         String sourceAccountNumber) {
        if (!Double.isFinite(amount) || amount <= 0) {
            throw new IllegalArgumentException("Treasury transaction amount must be greater than zero.");
        }
        String accountNumber = getDepositAccountNumber();
        Account account = accountRepository.findByAccountNumber(accountNumber);
        if (!isNeoBankOwned(account)) {
            throw new IllegalStateException("The linked NeoBank treasury account is unavailable or unverified.");
        }
        double currentBalance = account.getBalance() == null ? 0.0 : account.getBalance();
        if (!credit && currentBalance < amount) {
            throw new IllegalStateException("Insufficient NeoBank treasury funds. Available: " + currentBalance + ", Required: " + amount);
        }
        double newBalance = round2(credit ? currentBalance + amount : currentBalance - amount);
        account.setBalance(newBalance);
        accountRepository.save(account);

        Transaction transaction = new Transaction();
        transaction.setMerchant(merchant);
        transaction.setAmount(round2(amount));
        transaction.setType(credit ? "Credit" : "Debit");
        transaction.setDescription(description);
        transaction.setAccountNumber(accountNumber);
        transaction.setUserName("NeoBank");
        transaction.setSourceAccountNumber(sourceAccountNumber);
        transaction.setBalance(newBalance);
        transaction.setDate(LocalDateTime.now());
        transaction.setStatus("Completed");
        transactionRepository.save(transaction);
        return newBalance;
    }

    private boolean isNeoBankOwned(Account account) {
        return account != null
            && DEFAULT_NEOBANK_ACCOUNT.equals(account.getAccountNumber())
            && account.getName() != null
            && NEOBANK_NAME_PATTERN.matcher(account.getName().trim()).matches()
            && "ACTIVE".equalsIgnoreCase(account.getStatus());
    }

    /** Get summary for dashboard: balance and deposit account info. */
    public Map<String, Object> getBranchAccountSummary() {
        Map<String, Object> summary = new HashMap<>();
        BranchAccount b = getBranchAccount();
        String depositAccountNumber = getDepositAccountNumber();
        Double balance = accountService.getBalanceByAccountNumber(depositAccountNumber);
        Account account = accountRepository.findByAccountNumber(depositAccountNumber);
        summary.put("accountNumber", depositAccountNumber);
        summary.put("accountName", account != null && account.getName() != null ? account.getName()
                : (b != null && b.getAccountName() != null ? b.getAccountName() : "NeoBank Official"));
        summary.put("ifscCode", b != null ? b.getIfscCode() : null);
        summary.put("balance", balance != null ? balance : 0.0);
        summary.put("isConfigured", b != null && DEFAULT_NEOBANK_ACCOUNT.equals(b.getAccountNumber()));
        return summary;
    }

    /**
     * Get treasury account movements with optional date filter and search.
     * Returns transaction details and the related customer account when available.
     */
    public Map<String, Object> getBranchAccountTransactions(String depositAccountNumberParam, LocalDate fromDate, LocalDate toDate, String search, int page, int size) {
        Map<String, Object> result = new HashMap<>();
        String depositAccountNumber = (depositAccountNumberParam != null && !depositAccountNumberParam.trim().isEmpty())
            ? depositAccountNumberParam.trim() : getDepositAccountNumber();
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<Transaction> txPage;
        if (fromDate != null && toDate != null) {
            LocalDateTime start = fromDate.atStartOfDay();
            LocalDateTime end = toDate.atTime(LocalTime.MAX);
            txPage = transactionRepository.findByAccountNumberAndDateBetweenOrderByDateDesc(
                depositAccountNumber, start, end, pageable);
        } else {
            txPage = transactionRepository.findByAccountNumberOrderByDateDesc(depositAccountNumber, pageable);
        }
        List<Map<String, Object>> content = new ArrayList<>();
        for (Transaction t : txPage.getContent()) {
            String sourceAcc = t.getSourceAccountNumber();
            if (sourceAcc == null && t.getDescription() != null) {
                Matcher m = Pattern.compile("\\(from\\s+([A-Za-z0-9]+)\\)").matcher(t.getDescription());
                if (m.find()) sourceAcc = m.group(1);
            }
            String debitedName = null;
            if (sourceAcc != null) {
                Account acc = accountService.getAccountByNumber(sourceAcc);
                if (acc != null) debitedName = acc.getName();
            }
            if (search != null && !search.trim().isEmpty()) {
                String term = search.trim().toLowerCase();
                boolean match = (t.getTransactionId() != null && t.getTransactionId().toLowerCase().contains(term))
                    || (t.getDescription() != null && t.getDescription().toLowerCase().contains(term))
                    || (debitedName != null && debitedName.toLowerCase().contains(term))
                    || (sourceAcc != null && sourceAcc.toLowerCase().contains(term));
                if (!match) continue;
            }
            Map<String, Object> row = new HashMap<>();
            row.put("id", t.getId());
            row.put("transactionId", t.getTransactionId());
            row.put("date", t.getDate());
            row.put("amount", t.getAmount());
            row.put("type", t.getType());
            row.put("description", t.getDescription());
            row.put("merchant", t.getMerchant());
            row.put("debitedUserAccountNumber", sourceAcc);
            row.put("debitedUserAccountName", debitedName != null ? debitedName : (sourceAcc != null ? sourceAcc : "—"));
            content.add(row);
        }
        result.put("content", content);
        result.put("totalElements", txPage.getTotalElements());
        result.put("totalPages", txPage.getTotalPages());
        result.put("number", txPage.getNumber());
        result.put("size", txPage.getSize());
        return result;
    }

    @Transactional
    public Map<String, Object> saveDailyAllocation(LocalDate date, Double amount, String note, String allocatedBy) {
        if (date == null) throw new IllegalArgumentException("Allocation date is required");
        if (amount == null || amount < 0) throw new IllegalArgumentException("Allocation amount cannot be negative");
        BranchDailyAllocation allocation = allocationRepository.findByAllocationDate(date).orElseGet(BranchDailyAllocation::new);
        allocation.setAllocationDate(date);
        allocation.setAllocatedAmount(round2(amount));
        allocation.setNote(note);
        allocation.setAllocatedBy(allocatedBy == null || allocatedBy.isBlank() ? "Admin" : allocatedBy);
        return allocationMap(allocationRepository.save(allocation));
    }

    public Map<String, Object> getBranchOperations(LocalDate fromDate, LocalDate toDate) {
        LocalDate from = fromDate != null ? fromDate : LocalDate.now();
        LocalDate to = toDate != null ? toDate : from;
        if (to.isBefore(from)) throw new IllegalArgumentException("To date cannot be before from date");

        List<Transaction> transactions = transactionRepository.findByDateBetweenOrderByDateDesc(
                from.atStartOfDay(), to.atTime(LocalTime.MAX));
        Map<String, Double> totals = new HashMap<>();
        for (String key : List.of("deposits", "withdrawals", "transfers", "cashDeposits", "cashWithdrawals",
                "charges", "interestEarned", "processingCharges", "cibilCharges", "loanGiven", "credits", "debits")) {
            totals.put(key, 0.0);
        }
        List<Map<String, Object>> entries = new ArrayList<>();
        for (Transaction transaction : transactions) {
            double amount = transaction.getAmount() == null ? 0.0 : Math.abs(transaction.getAmount());
            String type = transaction.getType() == null ? "" : transaction.getType().toLowerCase();
            String text = ((transaction.getDescription() == null ? "" : transaction.getDescription()) + " "
                    + (transaction.getMerchant() == null ? "" : transaction.getMerchant())).toLowerCase();
            boolean debit = type.contains("debit") || type.contains("withdraw") || text.contains("debit");
            boolean loan = type.contains("loan") || text.contains("loan disbursement") || text.contains("loan approved");
            boolean interest = text.contains("interest");
            boolean cibil = text.contains("cibil");
            boolean processing = text.contains("processing") || text.contains("charge") || text.contains("fee");
            boolean transfer = type.contains("transfer") || text.contains("transfer");
            boolean cash = text.contains("cash");

            if (debit) totals.merge("debits", amount, Double::sum);
            else totals.merge("credits", amount, Double::sum);
            if (loan) totals.merge("loanGiven", amount, Double::sum);
            if (interest) totals.merge("interestEarned", amount, Double::sum);
            if (cibil) totals.merge("cibilCharges", amount, Double::sum);
            if (processing) totals.merge("processingCharges", amount, Double::sum);
            if (processing || cibil) totals.merge("charges", amount, Double::sum);
            if (transfer) totals.merge("transfers", amount, Double::sum);
            if (cash && debit) totals.merge("cashWithdrawals", amount, Double::sum);
            if (cash && !debit) totals.merge("cashDeposits", amount, Double::sum);
            if (type.contains("deposit") || text.contains("deposit")) totals.merge("deposits", amount, Double::sum);
            if (type.contains("withdraw") || text.contains("withdraw")) totals.merge("withdrawals", amount, Double::sum);

            Map<String, Object> entry = new HashMap<>();
            entry.put("id", transaction.getId());
            entry.put("transactionId", transaction.getTransactionId());
            entry.put("date", transaction.getDate());
            entry.put("accountNumber", transaction.getAccountNumber());
            entry.put("userName", transaction.getUserName());
            entry.put("amount", amount);
            entry.put("type", transaction.getType());
            entry.put("description", transaction.getDescription());
            entry.put("status", transaction.getStatus());
            entries.add(entry);
        }

        List<Map<String, Object>> accounts = new ArrayList<>();
        accountRepository.findAll().forEach(account -> accounts.add(accountBalanceMap(
                account.getAccountNumber(), account.getName(), account.getAccountType(), account.getBalance())));
        currentAccountRepository.findAll().forEach(account -> accounts.add(accountBalanceMap(
                account.getAccountNumber(), account.getBusinessName(), "Current", account.getBalance())));
        salaryAccountRepository.findAll().forEach(account -> accounts.add(accountBalanceMap(
                account.getAccountNumber(), account.getEmployeeName(), "Salary", account.getBalance())));

        double income = totals.get("charges") + totals.get("interestEarned");
        double expenses = totals.get("loanGiven");
        BranchDailyAllocation allocation = allocationRepository.findByAllocationDate(from).orElse(null);
        double allocated = allocation != null && allocation.getAllocatedAmount() != null ? allocation.getAllocatedAmount() : 0.0;
        double utilized = totals.get("withdrawals") + totals.get("loanGiven");
        Map<String, Object> result = new HashMap<>();
        result.put("fromDate", from);
        result.put("toDate", to);
        result.put("totals", totals);
        result.put("income", round2(income));
        result.put("expenses", round2(expenses));
        result.put("profitLoss", round2(income - expenses));
        result.put("allocatedAmount", round2(allocated));
        result.put("utilizedAmount", round2(utilized));
        result.put("remainingAllocation", round2(allocated - utilized));
        result.put("branchBalance", getBranchAccountSummary().get("balance"));
        result.put("accounts", accounts);
        result.put("entries", entries);
        return result;
    }

    public List<Map<String, Object>> getDailyAllocations(LocalDate fromDate, LocalDate toDate) {
        LocalDate from = fromDate != null ? fromDate : LocalDate.now().minusDays(30);
        LocalDate to = toDate != null ? toDate : LocalDate.now();
        List<Map<String, Object>> result = new ArrayList<>();
        allocationRepository.findByAllocationDateBetweenOrderByAllocationDateDesc(from, to)
                .forEach(allocation -> result.add(allocationMap(allocation)));
        return result;
    }

    private Map<String, Object> allocationMap(BranchDailyAllocation allocation) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", allocation.getId());
        result.put("allocationDate", allocation.getAllocationDate());
        result.put("allocatedAmount", allocation.getAllocatedAmount());
        result.put("note", allocation.getNote());
        result.put("allocatedBy", allocation.getAllocatedBy());
        result.put("updatedAt", allocation.getUpdatedAt());
        return result;
    }

    private Map<String, Object> accountBalanceMap(String number, String name, String type, Double balance) {
        Map<String, Object> result = new HashMap<>();
        result.put("accountNumber", number);
        result.put("accountName", name);
        result.put("accountType", type);
        result.put("balance", balance == null ? 0.0 : balance);
        return result;
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
