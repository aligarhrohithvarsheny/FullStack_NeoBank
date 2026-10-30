package com.neo.springapp.service;

import com.neo.springapp.model.Account;
import com.neo.springapp.model.BranchAccount;
import com.neo.springapp.model.BranchDailyAllocation;
import com.neo.springapp.model.Transaction;
import com.neo.springapp.repository.AccountRepository;
import com.neo.springapp.repository.AdminRepository;
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
import java.security.SecureRandom;
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
    private static final SecureRandom RANDOM = new SecureRandom();

    @Autowired
    private BranchAccountRepository branchAccountRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private AdminRepository adminRepository;
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

    public String getAccountNumberForCustomer(String customerAccountNumber) {
        BranchAccount branch = resolveBranchAccount(customerAccountNumber);
        return branch == null ? DEFAULT_NEOBANK_ACCOUNT : branch.getAccountNumber();
    }

    @Transactional
    public Map<String, Object> createBranchAccount(Long managerAdminId, Long hodId, Double openingBalance) {
        Map<String, Object> result = new HashMap<>();
        double openingAmount = openingBalance == null ? 0.0 : openingBalance;
        if (!Double.isFinite(openingAmount) || openingAmount < 0) {
            return failure("Opening balance must be a non-negative amount.");
        }
        var manager = adminRepository.findById(managerAdminId).orElse(null);
        if (manager == null || !"MANAGER".equalsIgnoreCase(manager.getRole())) {
            return failure("Select an existing manager profile.");
        }
        String city = manager.getAssignedCity() == null ? "" : manager.getAssignedCity().trim();
        if (city.isEmpty()) return failure("Assign the manager to a city before creating a branch account.");
        if (branchAccountRepository.findByManagerAdminId(managerAdminId) != null) {
            return failure("This manager already has a bank-owned branch account.");
        }
        if (branchAccountRepository.findByBranchCityIgnoreCase(city) != null) {
            return failure("A bank-owned account already exists for " + city + ".");
        }
        Account centralAccount = accountRepository.findByAccountNumber(DEFAULT_NEOBANK_ACCOUNT);
        if (openingAmount > 0 && (!isNeoBankOwned(centralAccount)
                || centralAccount.getBalance() == null || centralAccount.getBalance() < openingAmount)) {
            return failure("The central NeoBank treasury does not have enough verified funds for this opening balance.");
        }
        BranchAccount account = new BranchAccount();
        account.setAccountNumber(generateIdentifier("NB", 12, branchAccountRepository::existsByAccountNumber));
        account.setCustomerId(generateIdentifier("NBC", 9, branchAccountRepository::existsByCustomerId));
        account.setAccountName("NeoBank " + city + " Branch");
        account.setIfscCode("NEOB0" + String.format("%06d", RANDOM.nextInt(1_000_000)));
        account.setBranchCity(city);
        account.setManagerAdminId(manager.getId());
        account.setBalance(round2(openingAmount));
        account.setUpdatedByAdminId(hodId);
        BranchAccount saved = branchAccountRepository.save(account);
        if (openingAmount > 0) {
            double centralBalance = round2(centralAccount.getBalance() - openingAmount);
            centralAccount.setBalance(centralBalance);
            accountRepository.save(centralAccount);
            saveOpeningTransaction(DEFAULT_NEOBANK_ACCOUNT, openingAmount, "Debit",
                    "Opening funding for " + city + " branch account", centralBalance, saved.getAccountNumber());
            saveOpeningTransaction(saved.getAccountNumber(), openingAmount, "Credit",
                    "Opening balance transferred from central NeoBank treasury", round2(openingAmount), DEFAULT_NEOBANK_ACCOUNT);
        }
        result.put("success", true);
        result.put("message", "Bank-owned branch account created.");
        result.put("branchAccount", branchAccountDetails(saved, manager.getName(), manager.getEmail()));
        return result;
    }

    private void saveOpeningTransaction(String accountNumber, double amount, String type, String description,
                                       double balance, String relatedAccountNumber) {
        Transaction transaction = new Transaction();
        transaction.setMerchant("NeoBank Branch Funding");
        transaction.setAmount(round2(amount));
        transaction.setType(type);
        transaction.setDescription(description);
        transaction.setAccountNumber(accountNumber);
        transaction.setUserName("NeoBank");
        transaction.setSourceAccountNumber(type.equals("Credit") ? relatedAccountNumber : null);
        transaction.setRecipientAccountNumber(type.equals("Debit") ? relatedAccountNumber : null);
        transaction.setBalance(balance);
        transaction.setDate(LocalDateTime.now());
        transaction.setStatus("Completed");
        transactionRepository.save(transaction);
    }

    public List<Map<String, Object>> getBranchAccounts() {
        return branchAccountRepository.findAll().stream()
                .filter(account -> account.getBranchCity() != null && !account.getBranchCity().isBlank())
                .map(account -> {
                    var manager = account.getManagerAdminId() == null ? null
                            : adminRepository.findById(account.getManagerAdminId()).orElse(null);
                    return branchAccountDetails(account, manager == null ? null : manager.getName(),
                            manager == null ? null : manager.getEmail());
                })
                .toList();
    }

    public Map<String, Object> assignCustomerToBranch(String accountNumber, String branchCity) {
        Account customer = accountRepository.findByAccountNumber(accountNumber);
        if (customer == null || customer.getName() == null || NEOBANK_NAME_PATTERN.matcher(customer.getName()).matches()) {
            return failure("Customer account was not found.");
        }
        BranchAccount branch = branchAccountRepository.findByBranchCityIgnoreCase(branchCity == null ? "" : branchCity.trim());
        if (branch == null) return failure("Create a bank-owned account for the selected branch first.");
        customer.setBranchCity(branch.getBranchCity());
        accountRepository.save(customer);
        return Map.of("success", true, "message", "Customer account assigned to " + branch.getBranchCity() + ".");
    }

    public List<Map<String, Object>> searchCustomerAccounts(String query) {
        if (query == null || query.isBlank()) return List.of();
        return accountRepository.searchAccounts(query.trim(),
                        org.springframework.data.domain.PageRequest.of(0, 20))
                .getContent().stream()
                .filter(account -> account.getName() == null || !NEOBANK_NAME_PATTERN.matcher(account.getName()).matches())
                .map(account -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("name", account.getName());
                    row.put("accountNumber", account.getAccountNumber());
                    row.put("accountType", account.getAccountType());
                    row.put("branchCity", account.getBranchCity());
                    return row;
                }).toList();
    }

    private Map<String, Object> failure(String message) {
        return Map.of("success", false, "message", message);
    }

    private Map<String, Object> branchAccountDetails(BranchAccount account, String managerName, String managerEmail) {
        Map<String, Object> details = new HashMap<>();
        details.put("id", account.getId());
        details.put("accountNumber", account.getAccountNumber());
        details.put("customerId", account.getCustomerId());
        details.put("accountName", account.getAccountName());
        details.put("bankName", "NeoBank");
        details.put("ifscCode", account.getIfscCode());
        details.put("branchCity", account.getBranchCity());
        details.put("managerAdminId", account.getManagerAdminId());
        details.put("managerName", managerName);
        details.put("managerEmail", managerEmail);
        details.put("balance", account.getBalance() == null ? 0.0 : account.getBalance());
        details.put("updatedAt", account.getUpdatedAt());
        return details;
    }

    private String generateIdentifier(String prefix, int digits, java.util.function.Predicate<String> exists) {
        String candidate;
        do {
            StringBuilder number = new StringBuilder(prefix);
            for (int i = 0; i < digits; i++) number.append(RANDOM.nextInt(10));
            candidate = number.toString();
        } while (exists.test(candidate));
        return candidate;
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
        return branchAccountRepository.findAll().stream()
                .filter(account -> account.getBranchCity() == null || account.getBranchCity().isBlank())
                .findFirst().orElse(null);
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
        BranchAccount branch = resolveBranchAccount(sourceAccountNumber);
        if (branch != null) {
            return recordBranchMovement(branch, amount, credit, merchant, description, sourceAccountNumber);
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

    private BranchAccount resolveBranchAccount(String sourceAccountNumber) {
        if (sourceAccountNumber == null || sourceAccountNumber.isBlank()) return null;
        String city = resolveCustomerCity(sourceAccountNumber);
        return city == null || city.isBlank() ? null : branchAccountRepository.findByBranchCityIgnoreCase(city.trim());
    }

    private String resolveCustomerCity(String accountNumber) {
        Account source = accountRepository.findByAccountNumber(accountNumber);
        String city = source == null ? null : source.getBranchCity();
        if ((city == null || city.isBlank()) && currentAccountRepository != null) {
            var current = currentAccountRepository.findByAccountNumber(accountNumber);
            if (current != null && current.isPresent()) city = current.get().getCity();
        }
        if ((city == null || city.isBlank()) && salaryAccountRepository != null) {
            var salary = salaryAccountRepository.findByAccountNumber(accountNumber);
            if (salary != null) city = salary.getBranchCity();
        }
        return city;
    }

    private double recordBranchMovement(BranchAccount branch, double amount, boolean credit, String merchant,
                                        String description, String sourceAccountNumber) {
        double currentBalance = branch.getBalance() == null ? 0.0 : branch.getBalance();
        if (!credit && currentBalance < amount) {
            throw new IllegalStateException("Insufficient NeoBank branch funds. Available: " + currentBalance
                    + ", Required: " + amount);
        }
        double newBalance = round2(credit ? currentBalance + amount : currentBalance - amount);
        branch.setBalance(newBalance);
        branchAccountRepository.save(branch);

        Transaction transaction = new Transaction();
        transaction.setMerchant(merchant);
        transaction.setAmount(round2(amount));
        transaction.setType(credit ? "Credit" : "Debit");
        transaction.setDescription(description);
        transaction.setAccountNumber(branch.getAccountNumber());
        transaction.setUserName("NeoBank " + branch.getBranchCity());
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

    public Map<String, Object> getManagerBranchAccountSummary(Long managerAdminId) {
        BranchAccount branch = branchAccountRepository.findByManagerAdminId(managerAdminId);
        if (branch == null) return Map.of("isConfigured", false, "balance", 0.0);
        var manager = adminRepository.findById(managerAdminId).orElse(null);
        Map<String, Object> summary = branchAccountDetails(branch, manager == null ? null : manager.getName(),
                manager == null ? null : manager.getEmail());
        summary.put("isConfigured", true);
        return summary;
    }

    public Map<String, Object> getManagerBranchAccountTransactions(Long managerAdminId, LocalDate fromDate,
                                                                    LocalDate toDate, String search,
                                                                    int page, int size) {
        BranchAccount branch = branchAccountRepository.findByManagerAdminId(managerAdminId);
        if (branch == null) return Map.of("content", List.of(), "totalElements", 0L, "totalPages", 0);
        return getBranchAccountTransactions(branch.getAccountNumber(), fromDate, toDate, search, page, size);
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

    public Map<String, Object> getBranchOperations(Long managerAdminId, LocalDate fromDate, LocalDate toDate) {
        BranchAccount branch = branchAccountRepository.findByManagerAdminId(managerAdminId);
        LocalDate from = fromDate != null ? fromDate : LocalDate.now();
        LocalDate to = toDate != null ? toDate : from;
        if (to.isBefore(from)) throw new IllegalArgumentException("To date cannot be before from date");

        List<Transaction> transactions = transactionRepository.findByDateBetweenOrderByDateDesc(
                from.atStartOfDay(), to.atTime(LocalTime.MAX));
        if (branch == null) transactions = List.of();
        Map<String, Boolean> accountBranchCache = new HashMap<>();
        Map<String, Double> totals = new HashMap<>();
        for (String key : List.of("deposits", "withdrawals", "transfers", "cashDeposits", "cashWithdrawals",
                "charges", "interestEarned", "processingCharges", "cibilCharges", "loanGiven", "credits", "debits")) {
            totals.put(key, 0.0);
        }
        List<Map<String, Object>> entries = new ArrayList<>();
        for (Transaction transaction : transactions) {
            if (!transactionBelongsToBranch(transaction, branch, accountBranchCache)) continue;
            if (branch.getAccountNumber().equals(transaction.getAccountNumber())
                    || "NeoBank Branch Funding".equalsIgnoreCase(transaction.getMerchant())) continue;
            double amount = transaction.getAmount() == null ? 0.0 : Math.abs(transaction.getAmount());
            String type = transaction.getType() == null ? "" : transaction.getType().toLowerCase();
            String text = ((transaction.getDescription() == null ? "" : transaction.getDescription()) + " "
                    + (transaction.getMerchant() == null ? "" : transaction.getMerchant())).toLowerCase();
            boolean debit = type.contains("debit") || type.contains("withdraw") || text.contains("debit");
            boolean loan = type.contains("loan") || text.contains("loan disbursement") || text.contains("loan approved")
                    || (text.contains("loan") && (text.contains("disburs") || text.contains("sanction")));
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
        if (branch != null) {
            accountRepository.findAll().stream()
                    .filter(account -> belongsToBranch(account.getAccountNumber(), branch, accountBranchCache))
                    .forEach(account -> accounts.add(accountBalanceMap(
                            account.getAccountNumber(), account.getName(), account.getAccountType(), account.getBalance())));
            currentAccountRepository.findAll().stream()
                    .filter(account -> belongsToBranch(account.getAccountNumber(), branch, accountBranchCache))
                    .forEach(account -> accounts.add(accountBalanceMap(
                            account.getAccountNumber(), account.getBusinessName(), "Current", account.getBalance())));
            salaryAccountRepository.findAll().stream()
                    .filter(account -> belongsToBranch(account.getAccountNumber(), branch, accountBranchCache))
                    .forEach(account -> accounts.add(accountBalanceMap(
                            account.getAccountNumber(), account.getEmployeeName(), "Salary", account.getBalance())));
            accounts.add(accountBalanceMap(branch.getAccountNumber(), branch.getAccountName(),
                    "NeoBank Branch", branch.getBalance()));
        }

        double income = totals.get("charges") + totals.get("interestEarned");
        double expenses = totals.get("loanGiven");
        BranchDailyAllocation allocation = allocationRepository.findByAllocationDate(from).orElse(null);
        double allocated = branch != null && allocation != null && allocation.getAllocatedAmount() != null
                ? allocation.getAllocatedAmount() : 0.0;
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
        result.put("branchBalance", branch == null || branch.getBalance() == null ? 0.0 : branch.getBalance());
        result.put("accounts", accounts);
        result.put("entries", entries);
        return result;
    }

    private boolean transactionBelongsToBranch(Transaction transaction, BranchAccount branch,
                                               Map<String, Boolean> accountBranchCache) {
        if (branch == null) return false;
        return branch.getAccountNumber().equals(transaction.getAccountNumber())
                || belongsToBranch(transaction.getAccountNumber(), branch, accountBranchCache)
                || belongsToBranch(transaction.getSourceAccountNumber(), branch, accountBranchCache)
                || belongsToBranch(transaction.getRecipientAccountNumber(), branch, accountBranchCache);
    }

    private boolean belongsToBranch(String accountNumber, BranchAccount branch, Map<String, Boolean> cache) {
        if (accountNumber == null || accountNumber.isBlank() || branch == null) return false;
        if (branch.getAccountNumber().equals(accountNumber)) return true;
        return cache.computeIfAbsent(accountNumber, number -> {
            String city = resolveCustomerCity(number);
            return city != null && branch.getBranchCity() != null
                    && branch.getBranchCity().equalsIgnoreCase(city.trim());
        });
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
