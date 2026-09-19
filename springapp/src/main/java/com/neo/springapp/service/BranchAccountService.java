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
 * Manages the single branch deposit account where all charges and interest are credited.
 * Can be set from Manager Dashboard (Open Branch Account) or from Admin profile (map NeoBank A/C).
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

    /** Get the account number to use for depositing charges/interest. Falls back to NEOBANK000001 if not set. */
    public String getDepositAccountNumber() {
        BranchAccount b = getBranchAccount();
        if (b != null && b.getAccountNumber() != null && !b.getAccountNumber().trim().isEmpty()) {
            return b.getAccountNumber().trim();
        }
        return DEFAULT_NEOBANK_ACCOUNT;
    }

    /** Get full branch account record (for display). */
    public BranchAccount getBranchAccount() {
        return branchAccountRepository.findAll().stream().findFirst().orElse(null);
    }

    /**
     * Set or update the branch deposit account. Creates the Account in the system if it does not exist
     * so that credits can be applied. Called from Manager Dashboard or when admin saves profile with branch details.
     */
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

        // Verify branch account name matches Neo Bank name before add/update
        if (!accountName.isEmpty() && !NEOBANK_NAME_PATTERN.matcher(accountName).matches()) {
            result.put("success", false);
            result.put("message", "Branch account name must match Neo Bank name to verify and add (e.g. NeoBank, Neo Bank).");
            return result;
        }

        Account existingAccount = accountRepository.findByAccountNumber(accountNumber);
        if (existingAccount == null) {
            Account newAccount = new Account();
            newAccount.setAccountNumber(accountNumber);
            newAccount.setName(accountName.isEmpty() ? "Branch Account " + accountNumber : accountName);
            newAccount.setStatus("ACTIVE");
            newAccount.setAccountType("Current");
            newAccount.setBalance(0.0);
            String suffix = accountNumber.replaceAll("[^0-9A-Za-z]", "").toUpperCase();
            if (suffix.length() > 12) suffix = suffix.substring(0, 12);
            newAccount.setAadharNumber("BRANCHAADHAR" + suffix);
            newAccount.setPan("BRANCHPAN" + suffix);
            int hash = Math.abs((accountNumber + "branch").hashCode() % 100000000);
            newAccount.setPhone("98" + String.format("%08d", hash));
            newAccount.setDob("01-01-2000");
            newAccount.setAge(25);
            newAccount.setOccupation("Branch");
            newAccount.setAddress("NeoBank Branch");
            accountRepository.save(newAccount);
        }

        BranchAccount branch = getBranchAccount();
        if (branch == null) {
            branch = new BranchAccount();
        }
        branch.setAccountNumber(accountNumber);
        branch.setAccountName(accountName.isEmpty() ? null : accountName);
        branch.setIfscCode(ifscCode.isEmpty() ? null : ifscCode);
        branch.setUpdatedByAdminId(adminId);
        branch.setUpdatedAt(LocalDateTime.now());
        branchAccountRepository.save(branch);

        result.put("success", true);
        result.put("message", "Branch account set successfully. All charges and interest will be deposited here.");
        result.put("branchAccount", Map.of(
            "accountNumber", branch.getAccountNumber(),
            "accountName", branch.getAccountName(),
            "ifscCode", branch.getIfscCode()
        ));
        return result;
    }

    /** Get summary for dashboard: balance and deposit account info. */
    public Map<String, Object> getBranchAccountSummary() {
        Map<String, Object> summary = new HashMap<>();
        BranchAccount b = getBranchAccount();
        String depositAccountNumber = getDepositAccountNumber();
        Double balance = accountService.getBalanceByAccountNumber(depositAccountNumber);
        summary.put("accountNumber", depositAccountNumber);
        summary.put("accountName", b != null && b.getAccountName() != null ? b.getAccountName() : "NeoBank Branch");
        summary.put("ifscCode", b != null ? b.getIfscCode() : null);
        summary.put("balance", balance != null ? balance : 0.0);
        summary.put("isConfigured", b != null && b.getAccountNumber() != null);
        return summary;
    }

    /**
     * Get branch account transactions (credits to branch) with optional date filter and search.
     * Returns id, date, amount, type, description, debitedUserAccountNumber, debitedUserAccountName.
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
