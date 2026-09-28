package com.neo.springapp.service;

import com.neo.springapp.entity.ChargeTransaction;
import com.neo.springapp.entity.AllocationAccount;
import com.neo.springapp.model.Account;
import com.neo.springapp.model.BusinessTransaction;
import com.neo.springapp.model.CurrentAccount;
import com.neo.springapp.model.FundsAllocation;
import com.neo.springapp.model.SalaryAccount;
import com.neo.springapp.model.SalaryNormalTransaction;
import com.neo.springapp.model.Transaction;
import com.neo.springapp.repository.AccountRepository;
import com.neo.springapp.repository.BusinessTransactionRepository;
import com.neo.springapp.repository.ChargeTransactionRepository;
import com.neo.springapp.repository.CurrentAccountRepository;
import com.neo.springapp.repository.FundsAllocationRepository;
import com.neo.springapp.repository.AllocationAccountRepository;
import com.neo.springapp.repository.SalaryAccountRepository;
import com.neo.springapp.repository.SalaryNormalTransactionRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Charge Management Service
 * Handles automatic debit of charges from users and credit to allocation account
 * Charges: Interest, CIBIL, Soundbox, UPI, Payment Gateway
 */
@Service
@Transactional
public class ChargeManagementService {

    private static final Set<String> SUPPORTED_CHARGE_TYPES = Set.of(
        "INTEREST", "CIBIL", "SOUNDBOX", "UPI", "PAYMENT_GATEWAY", "OTHER"
    );
    
    @Autowired
    private ChargeTransactionRepository chargeRepository;
    
    @Autowired
    private FundsAllocationRepository allocationRepository;
    
    @Autowired
    private AllocationAccountRepository accountRepository;

    @Autowired
    private AccountService accountService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private AccountRepository savingsAccountRepository;

    @Autowired
    private CurrentAccountRepository currentAccountRepository;

    @Autowired
    private SalaryAccountRepository salaryAccountRepository;

    @Autowired
    private BusinessTransactionRepository businessTransactionRepository;

    @Autowired
    private SalaryNormalTransactionRepository salaryTransactionRepository;
    
    /**
     * Process a charge - DEBIT from user, CREDIT to allocation account
     */
    public ChargeTransaction processCharge(
            Long allocationId,
            String chargeType,
            String chargeDescription,
            BigDecimal chargeAmount,
            String userAccountNumber,
            String userName,
            String userProductType,
            String linkedTransactionId,
            String linkedLoanId,
            String linkedDepositId) {
        
        // Verify allocation exists and account is verified
        FundsAllocation allocation = allocationRepository.findById(allocationId)
            .orElseThrow(() -> new RuntimeException("Allocation not found"));
        
        if (!Boolean.TRUE.equals(allocation.getChargeManagementEnabled())) {
            throw new RuntimeException("Charge management not enabled for this allocation");
        }
        
        if (!"VERIFIED".equals(allocation.getAccountStatus())) {
            throw new RuntimeException("Allocation account not verified. Cannot process charges.");
        }

        if (chargeAmount == null || chargeAmount.signum() <= 0) {
            throw new IllegalArgumentException("Charge amount must be greater than zero.");
        }
        chargeType = chargeType == null ? "" : chargeType.trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_CHARGE_TYPES.contains(chargeType)) {
            throw new IllegalArgumentException("Unsupported charge type: " + chargeType);
        }
        if (userAccountNumber == null || userAccountNumber.isBlank()) {
            throw new IllegalArgumentException("User account number is required.");
        }
        if (allocation.getAllocationAccountId() == null) {
            throw new IllegalStateException("No linked allocation account is configured.");
        }
        
        // Get linked account
        AllocationAccount account = accountRepository.findById(allocation.getAllocationAccountId())
            .orElseThrow(() -> new RuntimeException("Linked account not found"));
        if (!"VERIFIED".equals(account.getVerificationStatus()) || !"ACTIVE".equals(account.getAccountStatus())) {
            throw new RuntimeException("Linked allocation account is not verified and active.");
        }

        debitUserAccount(userAccountNumber, userName, chargeType, chargeDescription, chargeAmount);
        
        // Create charge transaction record
        ChargeTransaction charge = new ChargeTransaction();
        charge.setChargeTransactionId("CT-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8));
        charge.setAllocationId(allocationId);
        charge.setAllocationAccountId(account.getId());
        charge.setChargeType(chargeType);  // INTEREST, CIBIL, SOUNDBOX, UPI, PAYMENT_GATEWAY
        charge.setChargeDescription(chargeDescription);
        charge.setChargeAmount(chargeAmount);
        charge.setUserAccountNumber(userAccountNumber);
        charge.setUserName(userName);
        charge.setUserProductType(userProductType);
        charge.setLinkedTransactionId(linkedTransactionId);
        charge.setLinkedLoanId(linkedLoanId);
        charge.setLinkedDepositId(linkedDepositId);
        
        // Set collection status
        charge.setCollectionStatus("COLLECTED");
        charge.setCollectionDate(LocalDateTime.now());
        charge.setStatus("SUCCESS");
        
        // Calculate net credit (after tax if applicable)
        charge.setTaxAmount(BigDecimal.ZERO);  // TODO: Calculate TDS if needed
        charge.setNetCreditAmount(chargeAmount);
        
        // Set credit status to CREDITED (auto-credit to allocation account)
        charge.setCreditStatus("CREDITED");
        charge.setCreditedAt(LocalDateTime.now());
        String creditRefNum = "CR-" + System.currentTimeMillis();
        charge.setCreditReferenceNumber(creditRefNum);
        
        charge.setReconciliationStatus("MATCHED");
        charge.setCreatedAt(LocalDateTime.now());
        charge.setUpdatedAt(LocalDateTime.now());
        
        // Save charge transaction
        ChargeTransaction savedCharge = chargeRepository.save(charge);
        
        // UPDATE ALLOCATION ACCOUNT BALANCE
        updateAccountBalanceWithCharge(account, chargeType, chargeAmount);
        
        // UPDATE ALLOCATION CHARGE TRACKING
        updateAllocationChargeTracking(allocation, chargeType, chargeAmount);
        
        return savedCharge;
    }
    
    /**
     * Update account balance and charge tracking
     */
    private void updateAccountBalanceWithCharge(AllocationAccount account, String chargeType, BigDecimal chargeAmount) {
        account.setCurrentBalance(
            (account.getCurrentBalance() != null ? account.getCurrentBalance() : BigDecimal.ZERO).add(chargeAmount)
        );
        account.setAccountBalance(
            (account.getAccountBalance() != null ? account.getAccountBalance() : BigDecimal.ZERO).add(chargeAmount)
        );
        account.setTotalCredited(
            (account.getTotalCredited() != null ? account.getTotalCredited() : BigDecimal.ZERO).add(chargeAmount)
        );
        // Update total charges collected
        account.setTotalChargesCollected(
            (account.getTotalChargesCollected() != null ? account.getTotalChargesCollected() : BigDecimal.ZERO)
            .add(chargeAmount)
        );
        
        // Update specific charge type
        switch(chargeType) {
            case "INTEREST":
                account.setInterestCharges(
                    (account.getInterestCharges() != null ? account.getInterestCharges() : BigDecimal.ZERO)
                    .add(chargeAmount)
                );
                break;
            case "CIBIL":
                account.setCibilCharges(
                    (account.getCibilCharges() != null ? account.getCibilCharges() : BigDecimal.ZERO)
                    .add(chargeAmount)
                );
                break;
            case "SOUNDBOX":
                account.setSoundboxCharges(
                    (account.getSoundboxCharges() != null ? account.getSoundboxCharges() : BigDecimal.ZERO)
                    .add(chargeAmount)
                );
                break;
            case "UPI":
                account.setUpiCharges(
                    (account.getUpiCharges() != null ? account.getUpiCharges() : BigDecimal.ZERO)
                    .add(chargeAmount)
                );
                break;
            case "PAYMENT_GATEWAY":
                account.setPaymentGatewayCharges(
                    (account.getPaymentGatewayCharges() != null ? account.getPaymentGatewayCharges() : BigDecimal.ZERO)
                    .add(chargeAmount)
                );
                break;
            case "OTHER":
                account.setOtherCharges(
                    (account.getOtherCharges() != null ? account.getOtherCharges() : BigDecimal.ZERO)
                    .add(chargeAmount)
                );
                break;
        }
        
        account.setUpdatedAt(LocalDateTime.now());
        accountRepository.save(account);
    }

    private void debitUserAccount(String accountNumber, String userName, String chargeType,
                                  String description, BigDecimal amount) {
        double debitAmount = amount.doubleValue();
        Account savingsAccount = savingsAccountRepository.findByAccountNumber(accountNumber);
        if (savingsAccount != null) {
            Double balanceAfter = accountService.debitBalance(accountNumber, debitAmount);
            if (balanceAfter == null) throw new IllegalArgumentException("Insufficient balance for charge.");
            Transaction transaction = new Transaction();
            transaction.setAccountNumber(accountNumber);
            transaction.setUserName(userName != null ? userName : savingsAccount.getName());
            transaction.setAmount(debitAmount);
            transaction.setType("Debit");
            transaction.setMerchant("NeoBank " + chargeType + " Charge");
            transaction.setDescription(description);
            transaction.setBalance(balanceAfter);
            transaction.setDate(LocalDateTime.now());
            transaction.setStatus("Completed");
            transactionService.saveTransaction(transaction);
            return;
        }

        CurrentAccount currentAccount = currentAccountRepository.findByAccountNumber(accountNumber).orElse(null);
        if (currentAccount != null) {
            if (!"ACTIVE".equalsIgnoreCase(currentAccount.getStatus()) || Boolean.TRUE.equals(currentAccount.getAccountFrozen())) {
                throw new IllegalArgumentException("Cannot charge an inactive or frozen current account.");
            }
            double balance = currentAccount.getBalance() != null ? currentAccount.getBalance() : 0.0;
            if (balance < debitAmount) throw new IllegalArgumentException("Insufficient balance for charge.");
            currentAccount.setBalance(balance - debitAmount);
            currentAccountRepository.save(currentAccount);
            BusinessTransaction transaction = new BusinessTransaction();
            transaction.setAccountNumber(accountNumber);
            transaction.setTxnType("Debit");
            transaction.setAmount(debitAmount);
            transaction.setDescription(description);
            transaction.setBalance(currentAccount.getBalance());
            transaction.setStatus("Completed");
            businessTransactionRepository.save(transaction);
            return;
        }

        SalaryAccount salaryAccount = salaryAccountRepository.findByAccountNumber(accountNumber);
        if (salaryAccount != null) {
            if (!"ACTIVE".equalsIgnoreCase(salaryAccount.getStatus())) {
                throw new IllegalArgumentException("Cannot charge an inactive salary account.");
            }
            double balance = salaryAccount.getBalance() != null ? salaryAccount.getBalance() : 0.0;
            if (balance < debitAmount) throw new IllegalArgumentException("Insufficient balance for charge.");
            salaryAccount.setBalance(balance - debitAmount);
            salaryAccountRepository.save(salaryAccount);
            SalaryNormalTransaction transaction = new SalaryNormalTransaction();
            transaction.setSalaryAccountId(salaryAccount.getId());
            transaction.setAccountNumber(accountNumber);
            transaction.setType("Debit");
            transaction.setAmount(debitAmount);
            transaction.setRemark(description);
            transaction.setPreviousBalance(balance);
            transaction.setNewBalance(salaryAccount.getBalance());
            transaction.setStatus("Success");
            salaryTransactionRepository.save(transaction);
            return;
        }

        throw new IllegalArgumentException("User account was not found in savings, current, or salary accounts.");
    }
    
    /**
     * Update allocation charge tracking
     */
    private void updateAllocationChargeTracking(FundsAllocation allocation, String chargeType, BigDecimal chargeAmount) {
        // Update total charges collected
        allocation.setTotalChargesCollected(
            (allocation.getTotalChargesCollected() != null ? allocation.getTotalChargesCollected() : BigDecimal.ZERO)
            .add(chargeAmount)
        );
        
        // Update specific charge type
        switch(chargeType) {
            case "INTEREST":
                allocation.setInterestCharges(
                    (allocation.getInterestCharges() != null ? allocation.getInterestCharges() : BigDecimal.ZERO)
                    .add(chargeAmount)
                );
                break;
            case "CIBIL":
                allocation.setCibilCharges(
                    (allocation.getCibilCharges() != null ? allocation.getCibilCharges() : BigDecimal.ZERO)
                    .add(chargeAmount)
                );
                break;
            case "SOUNDBOX":
                allocation.setSoundboxCharges(
                    (allocation.getSoundboxCharges() != null ? allocation.getSoundboxCharges() : BigDecimal.ZERO)
                    .add(chargeAmount)
                );
                break;
            case "UPI":
                allocation.setUpiCharges(
                    (allocation.getUpiCharges() != null ? allocation.getUpiCharges() : BigDecimal.ZERO)
                    .add(chargeAmount)
                );
                break;
            case "PAYMENT_GATEWAY":
                allocation.setPaymentGatewayCharges(
                    (allocation.getPaymentGatewayCharges() != null ? allocation.getPaymentGatewayCharges() : BigDecimal.ZERO)
                    .add(chargeAmount)
                );
                break;
        }
        
        allocation.setChargeTransactionCount((allocation.getChargeTransactionCount() != null ? allocation.getChargeTransactionCount() : 0) + 1);
        allocation.setUpdatedAt(LocalDateTime.now());
        allocationRepository.save(allocation);
    }
    
    /**
     * Get charge breakdown for allocation
     */
    public List<ChargeTransaction> getChargeHistory(Long allocationId) {
        return chargeRepository.findByAllocationId(allocationId);
    }
    
    /**
     * Get charges by type
     */
    public List<ChargeTransaction> getChargesByType(Long allocationId, String chargeType) {
        return chargeRepository.findByAllocationAndChargeType(allocationId, chargeType);
    }
    
    /**
     * Get pending credit transactions (for reconciliation)
     */
    public List<ChargeTransaction> getPendingCreditTransactions() {
        return chargeRepository.findPendingCreditTransactions();
    }
    
    /**
     * Get failed transactions
     */
    public List<ChargeTransaction> getFailedTransactions() {
        return chargeRepository.findFailedTransactions();
    }
    
    /**
     * Enable/disable charge management for allocation
     */
    public FundsAllocation setChargeManagement(Long allocationId, boolean enabled) {
        FundsAllocation allocation = allocationRepository.findById(allocationId)
            .orElseThrow(() -> new RuntimeException("Allocation not found"));
        
        allocation.setChargeManagementEnabled(enabled);
        allocation.setUpdatedAt(LocalDateTime.now());
        
        return allocationRepository.save(allocation);
    }
    
    /**
     * Get charge summary for dashboard
     */
    public ChargesSummary getChargeSummary(Long allocationId) {
        FundsAllocation allocation = allocationRepository.findById(allocationId)
            .orElseThrow(() -> new RuntimeException("Allocation not found"));
        
        ChargesSummary summary = new ChargesSummary();
        summary.setAllocationId(allocationId);
        summary.setTotalCharges(allocation.getTotalChargesCollected());
        summary.setInterestCharges(allocation.getInterestCharges());
        summary.setCibilCharges(allocation.getCibilCharges());
        summary.setSoundboxCharges(allocation.getSoundboxCharges());
        summary.setUpiCharges(allocation.getUpiCharges());
        summary.setPaymentGatewayCharges(allocation.getPaymentGatewayCharges());
        summary.setChargeTransactionCount(allocation.getChargeTransactionCount());
        
        return summary;
    }
    
    /**
     * Helper class for charge summary
     */
    public static class ChargesSummary {
        public Long allocationId;
        public BigDecimal totalCharges;
        public BigDecimal interestCharges;
        public BigDecimal cibilCharges;
        public BigDecimal soundboxCharges;
        public BigDecimal upiCharges;
        public BigDecimal paymentGatewayCharges;
        public Integer chargeTransactionCount;
        
        // Getters and Setters
        public Long getAllocationId() { return allocationId; }
        public void setAllocationId(Long allocationId) { this.allocationId = allocationId; }
        
        public BigDecimal getTotalCharges() { return totalCharges; }
        public void setTotalCharges(BigDecimal totalCharges) { this.totalCharges = totalCharges; }
        
        public BigDecimal getInterestCharges() { return interestCharges; }
        public void setInterestCharges(BigDecimal interestCharges) { this.interestCharges = interestCharges; }
        
        public BigDecimal getCibilCharges() { return cibilCharges; }
        public void setCibilCharges(BigDecimal cibilCharges) { this.cibilCharges = cibilCharges; }
        
        public BigDecimal getSoundboxCharges() { return soundboxCharges; }
        public void setSoundboxCharges(BigDecimal soundboxCharges) { this.soundboxCharges = soundboxCharges; }
        
        public BigDecimal getUpiCharges() { return upiCharges; }
        public void setUpiCharges(BigDecimal upiCharges) { this.upiCharges = upiCharges; }
        
        public BigDecimal getPaymentGatewayCharges() { return paymentGatewayCharges; }
        public void setPaymentGatewayCharges(BigDecimal paymentGatewayCharges) { this.paymentGatewayCharges = paymentGatewayCharges; }
        
        public Integer getChargeTransactionCount() { return chargeTransactionCount; }
        public void setChargeTransactionCount(Integer chargeTransactionCount) { this.chargeTransactionCount = chargeTransactionCount; }
    }
}
