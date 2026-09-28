package com.neo.springapp.service;

import com.neo.springapp.entity.ChargeTransaction;
import com.neo.springapp.entity.FundsAllocation;
import com.neo.springapp.entity.AllocationAccount;
import com.neo.springapp.repository.ChargeTransactionRepository;
import com.neo.springapp.repository.FundsAllocationRepository;
import com.neo.springapp.repository.AllocationAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Charge Management Service
 * Handles automatic debit of charges from users and credit to allocation account
 * Charges: Interest, CIBIL, Soundbox, UPI, Payment Gateway
 */
@Service
@Transactional
public class ChargeManagementService {
    
    @Autowired
    private ChargeTransactionRepository chargeRepository;
    
    @Autowired
    private FundsAllocationRepository allocationRepository;
    
    @Autowired
    private AllocationAccountRepository accountRepository;
    
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
        
        if (!allocation.getChargeManagementEnabled()) {
            throw new RuntimeException("Charge management not enabled for this allocation");
        }
        
        if (!allocation.getAccountStatus().equals("VERIFIED")) {
            throw new RuntimeException("Allocation account not verified. Cannot process charges.");
        }
        
        // Get linked account
        AllocationAccount account = accountRepository.findById(allocation.getAllocationAccountId())
            .orElseThrow(() -> new RuntimeException("Linked account not found"));
        
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
        
        charge.setReconciliationStatus("PENDING");
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
