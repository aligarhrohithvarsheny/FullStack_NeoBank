package com.neo.springapp.service;

import com.neo.springapp.model.Account;
import com.neo.springapp.model.AdminCashTransaction;
import com.neo.springapp.model.CurrentAccount;
import com.neo.springapp.model.SalaryAccount;
import com.neo.springapp.repository.AccountRepository;
import com.neo.springapp.repository.AdminCashTransactionRepository;
import com.neo.springapp.repository.CurrentAccountRepository;
import com.neo.springapp.repository.SalaryAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminCashTransactionService {

    @Autowired
    private AdminCashTransactionRepository repository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CurrentAccountRepository currentAccountRepository;

    @Autowired
    private SalaryAccountRepository salaryAccountRepository;

    // Account types whose balance is actually mutated by the quick-action deposit/withdraw form
    private static final List<String> BALANCE_BACKED_TYPES = List.of("regular", "salary", "current");

    public List<AdminCashTransaction> getAll(String search) {
        if (search != null && !search.isBlank()) {
            return repository.search(search.trim());
        }
        return repository.findAllByOrderByPerformedAtDesc();
    }

    public AdminCashTransaction record(String accountType, Long accountId, String accountNumber, String accountHolderName,
            String operationType, Double amount, String description, Double balanceBefore, Double balanceAfter,
            String performedBy) {
        AdminCashTransaction txn = new AdminCashTransaction();
        txn.setRefId("ADMTXN" + System.currentTimeMillis());
        txn.setAccountType(accountType);
        txn.setAccountId(accountId);
        txn.setAccountNumber(accountNumber);
        txn.setAccountHolderName(accountHolderName);
        txn.setOperationType(operationType != null ? operationType.toUpperCase() : "DEPOSIT");
        txn.setAmount(amount);
        txn.setDescription(description);
        txn.setBalanceBefore(balanceBefore);
        txn.setBalanceAfter(balanceAfter);
        txn.setStatus("COMPLETED");
        txn.setPerformedBy(performedBy != null && !performedBy.isBlank() ? performedBy : "Admin");
        txn.setPerformedAt(LocalDateTime.now());
        return repository.save(txn);
    }

    @Transactional
    public Map<String, Object> revert(Long id, String revertedBy, String reason) {
        Map<String, Object> result = new HashMap<>();
        AdminCashTransaction txn = repository.findById(id).orElseThrow(() -> new RuntimeException("Transaction not found"));

        if ("REVERTED".equals(txn.getStatus())) {
            throw new RuntimeException("This transaction has already been reverted");
        }
        if (txn.getPerformedAt() != null && LocalDateTime.now().isAfter(txn.getPerformedAt().plusHours(24))) {
            throw new RuntimeException("Transaction can only be reverted within 24 hours");
        }

        // Reverse the balance in real-time on the actual account, only for balance-backed account types.
        // (loan/goldloan/cheque quick-actions never mutate a real backend balance, so nothing to reverse there.)
        if (BALANCE_BACKED_TYPES.contains(txn.getAccountType())) {
            boolean wasDeposit = "DEPOSIT".equalsIgnoreCase(txn.getOperationType());
            double delta = wasDeposit ? -txn.getAmount() : txn.getAmount();
            adjustBalance(txn.getAccountType(), txn.getAccountNumber(), txn.getAccountId(), delta);
        }

        txn.setStatus("REVERTED");
        txn.setRevertedBy(revertedBy != null && !revertedBy.isBlank() ? revertedBy : "Admin");
        txn.setRevertedAt(LocalDateTime.now());
        txn.setRevertReason(reason);
        AdminCashTransaction saved = repository.save(txn);

        result.put("success", true);
        result.put("message", "Transaction reverted and balance updated in real-time.");
        result.put("transaction", saved);
        return result;
    }

    private void adjustBalance(String accountType, String accountNumber, Long accountId, double delta) {
        switch (accountType) {
            case "regular" -> {
                Account account = accountRepository.findByAccountNumber(accountNumber);
                if (account == null) throw new RuntimeException("Savings account not found: " + accountNumber);
                account.setBalance((account.getBalance() != null ? account.getBalance() : 0.0) + delta);
                account.setLastUpdated(LocalDateTime.now());
                accountRepository.save(account);
            }
            case "salary" -> {
                SalaryAccount account = accountId != null
                        ? salaryAccountRepository.findById(accountId).orElse(null)
                        : salaryAccountRepository.findByAccountNumber(accountNumber);
                if (account == null) throw new RuntimeException("Salary account not found: " + accountNumber);
                account.setBalance((account.getBalance() != null ? account.getBalance() : 0.0) + delta);
                account.setUpdatedAt(LocalDateTime.now());
                salaryAccountRepository.save(account);
            }
            case "current" -> {
                CurrentAccount account = currentAccountRepository.findByAccountNumber(accountNumber)
                        .orElseThrow(() -> new RuntimeException("Current account not found: " + accountNumber));
                account.setBalance((account.getBalance() != null ? account.getBalance() : 0.0) + delta);
                currentAccountRepository.save(account);
            }
            default -> throw new RuntimeException("Unsupported account type for revert: " + accountType);
        }
    }
}
