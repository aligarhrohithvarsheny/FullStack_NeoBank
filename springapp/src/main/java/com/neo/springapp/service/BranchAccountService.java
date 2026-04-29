package com.neo.springapp.service;

import com.neo.springapp.model.Account;
import com.neo.springapp.model.BranchAccount;
import com.neo.springapp.model.Transaction;
import com.neo.springapp.repository.AccountRepository;
import com.neo.springapp.repository.BranchAccountRepository;
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
}
