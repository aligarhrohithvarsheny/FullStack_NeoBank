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
        return DEFAULT_NEOBANK_ACCOUNT;
    }

    @Transactional
    public Map<String, Object> createBranchAccount(Long managerAdminId, Long hodId, Double openingBalance) {
        var manager = adminRepository.findById(managerAdminId).orElse(null);
        if (manager == null || !("MANAGER".equalsIgnoreCase(manager.getRole())
                || "ADMIN".equalsIgnoreCase(manager.getRole()))) {
            return failure("Select an existing manager profile.");
        }
        String city = manager.getAssignedCity() == null ? "" : manager.getAssignedCity().trim();
        if (city.isEmpty()) return failure("Assign the manager to a city before creating a branch account.");
        Account centralAccount = accountRepository.findByAccountNumber(DEFAULT_NEOBANK_ACCOUNT);
        if (!isNeoBankOwned(centralAccount)) return failure("Central NeoBank account is unavailable or unverified.");
        if (openingBalance != null && openingBalance != 0.0) {
            return failure("Branch setup does not create or fund another account. The existing central ledger is shared.");
        }
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Branch access is enabled for the existing central NeoBank account.");
        result.put("branchAccount", branchAccountDetails(centralAccount, manager));
        return result;
    }

    public List<Map<String, Object>> getBranchAccounts() {
        Account centralAccount = accountRepository.findByAccountNumber(DEFAULT_NEOBANK_ACCOUNT);
        if (!isNeoBankOwned(centralAccount)) return List.of();
        return adminRepository.findAll().stream()
                .filter(manager -> manager.getAssignedCity() != null && !manager.getAssignedCity().isBlank())
                .filter(manager -> "MANAGER".equalsIgnoreCase(manager.getRole())
                        || "ADMIN".equalsIgnoreCase(manager.getRole()))
                .map(manager -> branchAccountDetails(centralAccount, manager))
                .toList();
    }

    public Map<String, Object> assignCustomerToBranch(String accountNumber, String branchCity) {
        String city = branchCity == null ? "" : branchCity.trim();
        boolean assignedCity = adminRepository.findAll().stream()
                .anyMatch(admin -> city.equalsIgnoreCase(admin.getAssignedCity())
                        && ("MANAGER".equalsIgnoreCase(admin.getRole()) || "ADMIN".equalsIgnoreCase(admin.getRole())));
        if (!assignedCity) return failure("Assign a manager to this branch city before assigning customer accounts.");
        Account customer = accountRepository.findByAccountNumber(accountNumber);
        if (customer != null && customer.getName() != null
                && !NEOBANK_NAME_PATTERN.matcher(customer.getName()).matches()) {
            customer.setBranchCity(city);
            accountRepository.save(customer);
            return Map.of("success", true, "message", "Customer account assigned to " + city + ".");
        }
        var current = currentAccountRepository.findByAccountNumber(accountNumber);
        if (current.isPresent()) {
            current.get().setBranchCity(city);
            currentAccountRepository.save(current.get());
            return Map.of("success", true, "message", "Customer account assigned to " + city + ".");
        }
        var salary = salaryAccountRepository.findByAccountNumber(accountNumber);
        if (salary != null) {
            salary.setBranchCity(city);
            salaryAccountRepository.save(salary);
            return Map.of("success", true, "message", "Customer account assigned to " + city + ".");
        }
        return failure("Customer account was not found.");
    }

    public List<Map<String, Object>> searchCustomerAccounts(String query) {
        if (query == null || query.isBlank()) return List.of();
        var pageable = org.springframework.data.domain.PageRequest.of(0, 20);
        Map<String, Map<String, Object>> matches = new java.util.LinkedHashMap<>();
        accountRepository.searchAccounts(query.trim(), pageable).getContent().stream()
                .filter(account -> account.getName() == null || !NEOBANK_NAME_PATTERN.matcher(account.getName()).matches())
                .forEach(account -> matches.put(account.getAccountNumber(),
                        customerAccountMap(account.getAccountNumber(), account.getName(),
                                account.getAccountType(), account.getBranchCity())));
        currentAccountRepository.searchAccounts(query.trim(), pageable).getContent().forEach(account ->
                matches.putIfAbsent(account.getAccountNumber(),
                        customerAccountMap(account.getAccountNumber(), account.getBusinessName(),
                                "Current", account.getBranchCity() == null ? account.getCity() : account.getBranchCity())));
        salaryAccountRepository.search(query.trim()).stream().limit(20).forEach(account ->
                matches.putIfAbsent(account.getAccountNumber(),
                        customerAccountMap(account.getAccountNumber(), account.getEmployeeName(),
                                "Salary", account.getBranchCity())));
        return matches.values().stream().limit(20).toList();
    }

    private Map<String, Object> customerAccountMap(String accountNumber, String name, String accountType, String city) {
        Map<String, Object> row = new HashMap<>();
        row.put("name", name);
        row.put("accountNumber", accountNumber);
        row.put("accountType", accountType);
        row.put("branchCity", city);
        return row;
    }

    private Map<String, Object> failure(String message) {
        return Map.of("success", false, "message", message);
    }

    private Map<String, Object> branchAccountDetails(Account centralAccount, com.neo.springapp.model.Admin manager) {
        Map<String, Object> details = new HashMap<>();
        details.put("accountNumber", centralAccount.getAccountNumber());
        details.put("customerId", centralAccount.getCustomerId());
        details.put("accountName", centralAccount.getName());
        details.put("bankName", "NeoBank");
        details.put("ifscCode", null);
        details.put("branchCity", manager.getAssignedCity());
        details.put("managerAdminId", manager.getId());
        details.put("managerName", manager.getName());
        details.put("managerEmail", manager.getEmail());
        details.put("managerRole", manager.getRole());
        details.put("managerLoginUsername", manager.getManagerLoginUsername());
        details.put("managerLoginEnabled", manager.getManagerLoginUsername() != null
                && manager.getManagerLoginPasswordHash() != null
                && !manager.getManagerLoginPasswordHash().isBlank());
        details.put("managerLoginLocked", Boolean.TRUE.equals(manager.getManagerLoginLocked()));
        details.put("balance", centralAccount.getBalance() == null ? 0.0 : centralAccount.getBalance());
        details.put("sharedCentralAccount", true);
        return details;
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
        transaction.setCategory(transactionCategory(merchant, description));
        transaction.setChargeType(transactionChargeType(merchant, description));
        transaction.setBranchCity(resolveCustomerCity(sourceAccountNumber));
        transaction.setReferenceNumber(transaction.getTransactionId());
        transaction.setAccountNumber(accountNumber);
        transaction.setUserName("NeoBank");
        transaction.setSourceAccountNumber(credit ? sourceAccountNumber : accountNumber);
        transaction.setRecipientAccountNumber(credit ? accountNumber : sourceAccountNumber);
        Account counterparty = accountRepository.findByAccountNumber(credit ? accountNumber : sourceAccountNumber);
        if (counterparty != null) transaction.setRecipientName(counterparty.getName());
        transaction.setBalance(newBalance);
        transaction.setDate(LocalDateTime.now());
        transaction.setStatus("Completed");
        transactionRepository.save(transaction);
        return newBalance;
    }

    private String transactionCategory(String merchant, String description) {
        String text = ((merchant == null ? "" : merchant) + " " + (description == null ? "" : description))
                .toLowerCase(java.util.Locale.ROOT);
        if (text.contains("cibil")) return "CIBIL_CHARGE";
        if (text.contains("gold loan") && text.contains("interest")) return "GOLD_LOAN_INTEREST";
        if (text.contains("foreclosure")) return "FORECLOSURE";
        if (text.contains("processing") && text.contains("loan")) return "LOAN_PROCESSING_CHARGE";
        if (text.contains("loan") && text.contains("charge")) return "LOAN_CHARGE";
        if (text.contains("fastag")) return "FASTAG";
        if (text.contains("insurance")) return "INSURANCE";
        if (text.contains("interest")) return "LOAN_INTEREST";
        if (text.contains("charge") || text.contains("fee")) return "BANK_SERVICE_CHARGE";
        if (text.contains("loan") && (text.contains("sanction") || text.contains("disburs"))) return "LOAN_DISBURSEMENT";
        if (text.contains("loan") && (text.contains("emi") || text.contains("repay"))) return "LOAN_REPAYMENT";
        return "BANK_TRANSACTION";
    }

    private String transactionChargeType(String merchant, String description) {
        String category = transactionCategory(merchant, description);
        return category.endsWith("_CHARGE") || "FORECLOSURE".equals(category) || "GOLD_LOAN_INTEREST".equals(category)
                ? category : null;
    }

    private String resolveCustomerCity(String accountNumber) {
        Account source = accountRepository.findByAccountNumber(accountNumber);
        String city = source == null ? null : source.getBranchCity();
        if ((city == null || city.isBlank()) && currentAccountRepository != null) {
            var current = currentAccountRepository.findByAccountNumber(accountNumber);
            if (current != null && current.isPresent()) {
                city = current.get().getBranchCity();
                if (city == null || city.isBlank()) city = current.get().getCity();
            }
        }
        if ((city == null || city.isBlank()) && salaryAccountRepository != null) {
            var salary = salaryAccountRepository.findByAccountNumber(accountNumber);
            if (salary != null) city = salary.getBranchCity();
        }
        return city;
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
        String depositAccountNumber = getDepositAccountNumber();
        Double balance = accountService.getBalanceByAccountNumber(depositAccountNumber);
        Account account = accountRepository.findByAccountNumber(depositAccountNumber);
        summary.put("accountNumber", depositAccountNumber);
        summary.put("accountName", account != null && account.getName() != null ? account.getName() : "NeoBank Official");
        summary.put("bankName", "NeoBank");
        summary.put("customerId", account == null ? null : account.getCustomerId());
        summary.put("ifscCode", null);
        summary.put("balance", balance != null ? balance : 0.0);
        summary.put("isConfigured", isNeoBankOwned(account));
        addLedgerTotals(summary, transactionRepository.summarizeCentralLedger(depositAccountNumber, null), balance, true);
        return summary;
    }

    public Map<String, Object> getManagerBranchAccountSummary(Long managerAdminId) {
        var manager = adminRepository.findById(managerAdminId).orElse(null);
        Account account = accountRepository.findByAccountNumber(DEFAULT_NEOBANK_ACCOUNT);
        if (manager == null || manager.getAssignedCity() == null || manager.getAssignedCity().isBlank()
                || !isNeoBankOwned(account)) return Map.of("isConfigured", false, "balance", 0.0);
        Map<String, Object> summary = branchAccountDetails(account, manager);
        addLedgerTotals(summary, transactionRepository.summarizeCentralLedger(DEFAULT_NEOBANK_ACCOUNT,
                manager.getAssignedCity()), account.getBalance(), false);
        summary.put("isConfigured", true);
        summary.put("sharedCentralAccount", true);
        return summary;
    }

    public Map<String, Object> getManagerBranchAccountTransactions(Long managerAdminId, LocalDate fromDate,
                                                                    LocalDate toDate, String search,
                                                                    int page, int size, String category,
                                                                    String type, String status, Double minAmount,
                                                                    Double maxAmount) {
        var manager = adminRepository.findById(managerAdminId).orElse(null);
        if (manager == null || manager.getAssignedCity() == null || manager.getAssignedCity().isBlank()) {
            return Map.of("content", List.of(), "totalElements", 0L, "totalPages", 0);
        }
        return getBranchAccountTransactions(DEFAULT_NEOBANK_ACCOUNT, manager.getAssignedCity(), fromDate, toDate,
                search, page, size, category, type, status, minAmount, maxAmount);
    }

    public Transaction getCentralLedgerTransaction(String transactionId, String branchCity) {
        Transaction transaction = transactionRepository.findByTransactionId(transactionId).orElse(null);
        if (transaction == null || !DEFAULT_NEOBANK_ACCOUNT.equals(transaction.getAccountNumber())) return null;
        if (branchCity == null) return transaction;
        String transactionCity = transaction.getBranchCity();
        if (transactionCity == null || transactionCity.isBlank()) {
            transactionCity = resolveCustomerCity(transaction.getSourceAccountNumber());
        }
        return transactionCity != null && transactionCity.equalsIgnoreCase(branchCity) ? transaction : null;
    }

    public Transaction getManagerCentralLedgerTransaction(String transactionId, Long managerAdminId) {
        var manager = adminRepository.findById(managerAdminId).orElse(null);
        if (manager == null || manager.getAssignedCity() == null || manager.getAssignedCity().isBlank()) return null;
        return getCentralLedgerTransaction(transactionId, manager.getAssignedCity());
    }

    /**
     * Get treasury account movements with optional date filter and search.
     * Returns transaction details and the related customer account when available.
     */
    public Map<String, Object> getBranchAccountTransactions(String depositAccountNumberParam, String branchCity,
                                                             LocalDate fromDate, LocalDate toDate, String search,
                                                             int page, int size, String category, String type,
                                                             String status, Double minAmount, Double maxAmount) {
        Map<String, Object> result = new HashMap<>();
        String depositAccountNumber = (depositAccountNumberParam != null && !depositAccountNumberParam.trim().isEmpty())
            ? depositAccountNumberParam.trim() : getDepositAccountNumber();
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100)));
        LocalDateTime start = fromDate == null ? null : fromDate.atStartOfDay();
        LocalDateTime end = toDate == null ? null : toDate.atTime(LocalTime.MAX);
        Page<Transaction> txPage = transactionRepository.searchCentralLedger(depositAccountNumber,
                blankToNull(branchCity), start, end, minAmount, maxAmount, blankToNull(category),
                blankToNull(type), blankToNull(status), blankToNull(search), pageable);
        List<Map<String, Object>> content = new ArrayList<>();
        for (Transaction t : txPage.getContent()) {
            content.add(transactionMap(t));
        }
        result.put("content", content);
        result.put("totalElements", txPage.getTotalElements());
        result.put("totalPages", txPage.getTotalPages());
        result.put("number", txPage.getNumber());
        result.put("size", txPage.getSize());
        return result;
    }

    private Map<String, Object> transactionMap(Transaction transaction) {
        Map<String, Object> row = new HashMap<>();
        double amount = transaction.getAmount() == null ? 0.0 : transaction.getAmount();
        boolean credit = "Credit".equalsIgnoreCase(transaction.getType())
                || "Deposit".equalsIgnoreCase(transaction.getType())
                || "Loan Credit".equalsIgnoreCase(transaction.getType());
        row.put("id", transaction.getId());
        row.put("transactionId", transaction.getTransactionId());
        row.put("referenceNumber", transaction.getReferenceNumber() == null
                ? transaction.getTransactionId() : transaction.getReferenceNumber());
        row.put("date", transaction.getDate());
        row.put("amount", amount);
        row.put("debitAmount", credit ? 0.0 : amount);
        row.put("creditAmount", credit ? amount : 0.0);
        row.put("type", transaction.getType());
        row.put("category", transaction.getCategory());
        row.put("chargeType", transaction.getChargeType());
        row.put("status", displayTransactionStatus(transaction.getStatus()));
        row.put("description", transaction.getDescription());
        row.put("merchant", transaction.getMerchant());
        row.put("accountNumber", transaction.getAccountNumber());
        row.put("sourceAccountNumber", transaction.getSourceAccountNumber());
        row.put("destinationAccountNumber", transaction.getRecipientAccountNumber());
        row.put("recipientName", transaction.getRecipientName());
        row.put("branchCity", transaction.getBranchCity());
        row.put("balance", transaction.getBalance());
        return row;
    }

    private String displayTransactionStatus(String status) {
        return status == null || "completed".equalsIgnoreCase(status) || "success".equalsIgnoreCase(status)
                ? "Success" : status;
    }

    private void addLedgerTotals(Map<String, Object> summary, Object[] totals, Double balance,
                                 boolean includeOpeningBalance) {
        Object[] values = unwrapLedgerTotals(totals);
        double credits = ledgerTotal(values, 0);
        double debits = ledgerTotal(values, 1);
        double charges = ledgerTotal(values, 2);
        summary.put("totalCredits", round2(credits));
        summary.put("totalDebits", round2(debits));
        summary.put("totalCharges", round2(charges));
        summary.put("cibilCharges", round2(ledgerTotal(values, 4)));
        summary.put("loanProcessingCharges", round2(ledgerTotal(values, 5)));
        summary.put("goldLoanInterest", round2(ledgerTotal(values, 6)));
        summary.put("loanInterest", round2(ledgerTotal(values, 7)));
        summary.put("fasttagTransactions", round2(ledgerTotal(values, 8)));
        summary.put("insuranceTransactions", round2(ledgerTotal(values, 9)));
        summary.put("foreclosureCharges", round2(ledgerTotal(values, 10)));
        if (includeOpeningBalance) summary.put("openingBalance", round2((balance == null ? 0 : balance) - credits + debits));
        summary.put("lastTransactionDate", values != null && values.length > 3 ? values[3] : null);
    }

    private Object[] unwrapLedgerTotals(Object[] totals) {
        return totals != null && totals.length == 1 && totals[0] instanceof Object[] nested ? nested : totals;
    }

    private double ledgerTotal(Object[] totals, int index) {
        return totals != null && totals.length > index && totals[index] != null
                ? ((Number) totals[index]).doubleValue() : 0.0;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
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
        var manager = adminRepository.findById(managerAdminId).orElse(null);
        String branchCity = manager == null ? null : manager.getAssignedCity();
        LocalDate from = fromDate != null ? fromDate : LocalDate.now();
        LocalDate to = toDate != null ? toDate : from;
        if (to.isBefore(from)) throw new IllegalArgumentException("To date cannot be before from date");

        List<Transaction> transactions = transactionRepository.findByDateBetweenOrderByDateDesc(
                from.atStartOfDay(), to.atTime(LocalTime.MAX));
        if (branchCity == null || branchCity.isBlank()) transactions = List.of();
        Map<String, Double> totals = new HashMap<>();
        for (String key : List.of("deposits", "withdrawals", "transfers", "cashDeposits", "cashWithdrawals",
                "charges", "interestEarned", "processingCharges", "cibilCharges", "loanGiven", "credits", "debits")) {
            totals.put(key, 0.0);
        }
        List<Map<String, Object>> entries = new ArrayList<>();
        for (Transaction transaction : transactions) {
            String transactionCity = transaction.getBranchCity();
            if (transactionCity == null || transactionCity.isBlank()) {
                transactionCity = resolveCustomerCity(transaction.getSourceAccountNumber());
                if (transactionCity == null || transactionCity.isBlank()) {
                    transactionCity = resolveCustomerCity(transaction.getAccountNumber());
                }
            }
            if (transactionCity == null || !transactionCity.equalsIgnoreCase(branchCity)) continue;
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
            boolean internalTransfer = isInternalTransfer(transaction);

            if (debit) totals.merge("debits", amount, Double::sum);
            else totals.merge("credits", amount, Double::sum);
            if (loan && !internalTransfer) totals.merge("loanGiven", amount, Double::sum);
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
            entry.put("debitAmount", debit ? amount : 0.0);
            entry.put("creditAmount", debit ? 0.0 : amount);
            entry.put("type", transaction.getType());
            entry.put("category", transaction.getCategory());
            entry.put("description", transaction.getDescription());
            entry.put("status", displayTransactionStatus(transaction.getStatus()));
            entry.put("branchCity", transactionCity);
            entries.add(entry);
        }

        List<Map<String, Object>> accounts = new ArrayList<>();
        accountRepository.findAll().stream()
                .filter(account -> account.getBranchCity() != null && account.getBranchCity().equalsIgnoreCase(branchCity))
                .forEach(account -> accounts.add(accountBalanceMap(
                        account.getAccountNumber(), account.getName(), account.getAccountType(), account.getBalance())));
        currentAccountRepository.findAll().stream()
                .filter(account -> {
                    String city = account.getBranchCity() == null ? account.getCity() : account.getBranchCity();
                    return city != null && city.equalsIgnoreCase(branchCity);
                })
                .forEach(account -> accounts.add(accountBalanceMap(
                        account.getAccountNumber(), account.getBusinessName(), "Current", account.getBalance())));
        salaryAccountRepository.findAll().stream()
                .filter(account -> account.getBranchCity() != null && account.getBranchCity().equalsIgnoreCase(branchCity))
                .forEach(account -> accounts.add(accountBalanceMap(
                        account.getAccountNumber(), account.getEmployeeName(), "Salary", account.getBalance())));
        Account centralAccount = accountRepository.findByAccountNumber(DEFAULT_NEOBANK_ACCOUNT);
        if (centralAccount != null) accounts.add(accountBalanceMap(DEFAULT_NEOBANK_ACCOUNT, centralAccount.getName(),
                "NeoBank Central", centralAccount.getBalance()));

        double income = totals.get("charges") + totals.get("interestEarned");
        double expenses = totals.get("loanGiven");
        BranchDailyAllocation allocation = allocationRepository.findByAllocationDate(from).orElse(null);
        double allocated = allocation != null && allocation.getAllocatedAmount() != null
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
        result.put("branchCity", branchCity);
        result.put("branchBalance", centralAccount == null || centralAccount.getBalance() == null ? 0.0 : centralAccount.getBalance());
        result.put("accounts", accounts);
        result.put("entries", entries);
        return result;
    }

    private boolean isInternalTransfer(Transaction transaction) {
        String text = ((transaction.getDescription() == null ? "" : transaction.getDescription()) + " "
                + (transaction.getMerchant() == null ? "" : transaction.getMerchant())).toLowerCase();
        return text.contains("internal transfer") || (transaction.getRecipientAccountNumber() != null
                && transaction.getRecipientAccountNumber().equals(transaction.getAccountNumber()));
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
