package com.neo.springapp.service;

import com.neo.springapp.model.Account;
import com.neo.springapp.model.Transaction;
import com.neo.springapp.repository.AccountRepository;
import com.neo.springapp.repository.CardRepository;
import com.neo.springapp.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Automatic bank charges: monthly bank charges, loan charges, debit card charges (6 months), CIBIL at loan apply, KYC verification.
 * All charges are debited from user account, credited to the manager branch account (NeoBank A/C) in real time,
 * and recorded in user transactions and branch transactions.
 */
@Service
public class BankChargesService {

    public static final String NEOBANK_ACCOUNT_NUMBER = "NEOBANK000001";

    public static final double MONTHLY_BANK_CHARGE_RS = 496;
    public static final double LOAN_CHARGE_RS = 1180;
    public static final double DEBIT_CARD_CHARGE_6MONTHS_RS = 596;
    public static final double CIBIL_CHARGE_RS = 118;
    public static final double KYC_VERIFICATION_CHARGE_RS = 59;

    private static final String MERCHANT_BANK_CHARGES = "NeoBank - Monthly Bank Charges";
    private static final String MERCHANT_LOAN_CHARGE = "NeoBank - Loan Processing Charge";
    private static final String MERCHANT_DEBIT_CARD = "NeoBank - Debit Card Charges (6 months)";
    private static final String MERCHANT_CIBIL = "NeoBank - CIBIL Report Charge";
    private static final String MERCHANT_KYC = "NeoBank - KYC Verification Charge";

    @Autowired
    private AccountService accountService;
    @Autowired
    private TransactionService transactionService;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private CardRepository cardRepository;

    @Autowired(required = false)
    private BranchAccountService branchAccountService;

    /** Resolve deposit account (branch/manager account or default NeoBank). */
    private String getDepositAccountNumber() {
        if (branchAccountService != null) {
            return branchAccountService.getDepositAccountNumber();
        }
        return NEOBANK_ACCOUNT_NUMBER;
    }

    /**
     * Apply a charge: debit user account, credit branch/NeoBank account, create debit transaction for user and credit for bank.
     */
    @Transactional
    public boolean applyCharge(String accountNumber, Double amount, String merchant, String description, String userName) {
        if (accountNumber == null || amount == null || amount <= 0) return false;
        String depositAccount = getDepositAccountNumber();
        Account userAccount = accountService.getAccountByNumber(accountNumber);
        if (userAccount == null) return false;
        Account bankAccount = accountService.getAccountByNumber(depositAccount);
        if (bankAccount == null) {
            System.err.println("Branch/NeoBank account " + depositAccount + " not found. Charge not applied.");
            return false;
        }
        Double newBalance = accountService.debitBalance(accountNumber, amount);
        if (newBalance == null) {
            System.err.println("Insufficient balance or debit failed for " + accountNumber + " amount " + amount);
            return false;
        }
        accountService.creditBalance(depositAccount, amount);

        String name = userName != null && !userName.trim().isEmpty() ? userName : userAccount.getName();

        Transaction debitTxn = new Transaction();
        debitTxn.setMerchant(merchant);
        debitTxn.setAmount(amount);
        debitTxn.setType("Debit");
        debitTxn.setDescription(description);
        debitTxn.setAccountNumber(accountNumber);
        debitTxn.setUserName(name);
        debitTxn.setBalance(newBalance);
        debitTxn.setDate(LocalDateTime.now());
        debitTxn.setStatus("Completed");
        transactionService.saveTransaction(debitTxn);

        Double bankBalance = accountService.getBalanceByAccountNumber(depositAccount);
        Transaction creditTxn = new Transaction();
        creditTxn.setMerchant(merchant + " - " + accountNumber);
        creditTxn.setAmount(amount);
        creditTxn.setType("Credit");
        creditTxn.setDescription(description + " (from " + accountNumber + ")");
        creditTxn.setAccountNumber(depositAccount);
        creditTxn.setUserName("NeoBank");
        creditTxn.setSourceAccountNumber(accountNumber); // debited user account for branch transaction list
        creditTxn.setBalance(bankBalance != null ? bankBalance : amount);
        creditTxn.setDate(LocalDateTime.now());
        creditTxn.setStatus("Completed");
        transactionService.saveTransaction(creditTxn);

        return true;
    }

    /** CIBIL charge when user applies for a loan - Rs 118 every time. */
    public boolean applyCibilChargeAtLoanApply(String accountNumber, String userName) {
        return applyCharge(
            accountNumber,
            CIBIL_CHARGE_RS,
            MERCHANT_CIBIL,
            "CIBIL report charge at loan application - Rs " + (int) CIBIL_CHARGE_RS,
            userName
        );
    }

    /** Loan processing charge when loan is approved - Rs 1180 one-time per loan. */
    public boolean applyLoanChargeAtApproval(String accountNumber, String userName) {
        return applyCharge(
            accountNumber,
            LOAN_CHARGE_RS,
            MERCHANT_LOAN_CHARGE,
            "Loan processing charge (on approval) - Rs " + (int) LOAN_CHARGE_RS,
            userName
        );
    }

    /** KYC verification charge when KYC is approved - Rs 59. Credited to manager branch account in real time. */
    public boolean applyKycCharge(String accountNumber, String userName) {
        return applyCharge(
            accountNumber,
            KYC_VERIFICATION_CHARGE_RS,
            MERCHANT_KYC,
            "KYC verification charge (on approval) - Rs " + (int) KYC_VERIFICATION_CHARGE_RS,
            userName
        );
    }

    /** Monthly bank charges - Rs 496 per active account. Run on 1st of every month. */
    @Transactional
    public java.util.Map<String, Object> processMonthlyBankCharges() {
        List<Account> active = accountRepository.findByStatus("ACTIVE");
        int success = 0, failed = 0;
        for (Account acc : active) {
            String an = acc.getAccountNumber();
            if (an == null || getDepositAccountNumber().equals(an)) continue;
            boolean ok = applyCharge(an, MONTHLY_BANK_CHARGE_RS, MERCHANT_BANK_CHARGES,
                "Monthly bank charges - Rs " + (int) MONTHLY_BANK_CHARGE_RS, acc.getName());
            if (ok) success++; else failed++;
        }
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("success", true);
        result.put("successCount", success);
        result.put("failureCount", failed);
        result.put("message", "Monthly bank charges: " + success + " applied, " + failed + " failed.");
        return result;
    }

    /** Debit card charges Rs 596 every 6 months for accounts that have a debit card. */
    @Transactional
    public java.util.Map<String, Object> processDebitCardChargesEvery6Months() {
        List<Account> active = accountRepository.findByStatus("ACTIVE");
        int success = 0, failed = 0, skipped = 0;
        for (Account acc : active) {
            String an = acc.getAccountNumber();
            if (an == null || getDepositAccountNumber().equals(an)) continue;
            List<com.neo.springapp.model.Card> cards = cardRepository.findByAccountNumber(an);
            boolean hasDebit = cards.stream().anyMatch(c -> "Debit".equalsIgnoreCase(c.getCardType()));
            if (!hasDebit) { skipped++; continue; }
            List<Transaction> lastCharge = transactionRepository.findByAccountNumberAndDescriptionContainingOrderByDateDesc(
                an, "Debit Card Charges", PageRequest.of(0, 1));
            if (!lastCharge.isEmpty()) {
                long months = ChronoUnit.MONTHS.between(lastCharge.get(0).getDate(), LocalDateTime.now());
                if (months < 6) { skipped++; continue; }
            }
            boolean ok = applyCharge(an, DEBIT_CARD_CHARGE_6MONTHS_RS, MERCHANT_DEBIT_CARD,
                "Debit card charges (6 months) - Rs " + (int) DEBIT_CARD_CHARGE_6MONTHS_RS, acc.getName());
            if (ok) success++; else failed++;
        }
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("success", true);
        result.put("successCount", success);
        result.put("failureCount", failed);
        result.put("skippedCount", skipped);
        result.put("message", "Debit card charges: " + success + " applied, " + failed + " failed, " + skipped + " skipped.");
        return result;
    }
}
