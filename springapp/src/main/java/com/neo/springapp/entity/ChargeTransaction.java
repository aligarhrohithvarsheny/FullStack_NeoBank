package com.neo.springapp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;

/**
 * ChargeTransaction Entity
 * Tracks all charges (Interest, CIBIL, Soundbox, UPI, Payment Gateway, etc.)
 * that are DEBITED from users and CREDITED to the allocation account
 * Creates automatic entries in allocation_utilization table
 */
@Entity
@Table(name = "charge_transaction", indexes = {
    @Index(name = "idx_allocation_id", columnList = "allocationId"),
    @Index(name = "idx_charge_type", columnList = "chargeType"),
    @Index(name = "idx_user_account", columnList = "userAccountNumber"),
    @Index(name = "idx_charge_transaction_status", columnList = "status")
})
public class ChargeTransaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 50)
    private String chargeTransactionId;  // CT-2025-XXXXX
    
    @Column(nullable = false)
    private Long allocationId;  // FK to funds_allocation
    
    @Column(nullable = false)
    private Long allocationAccountId;  // FK to allocation_account (where credits go)
    
    @Column(nullable = false, length = 50)
    private String chargeType;  // INTEREST, CIBIL, SOUNDBOX, UPI, PAYMENT_GATEWAY, OTHER
    
    @Column(nullable = false, length = 100)
    private String chargeDescription;
    
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal chargeAmount;
    
    // User details from whom charge is collected
    @Column(nullable = false, length = 50)
    private String userAccountNumber;
    
    @Column(length = 100)
    private String userName;
    
    @Column(length = 50)
    private String userProductType;  // GOLD_LOAN, DEPOSITS, etc.
    
    @Column(length = 100)
    private String linkedTransactionId;  // TXN-2025-XXXXX (e.g., loan disbursement)
    
    @Column(length = 100)
    private String linkedLoanId;  // For loan-specific charges
    
    @Column(length = 100)
    private String linkedDepositId;  // For deposit-specific charges
    
    // Credit Details
    @Column(nullable = false, length = 20)
    private String creditStatus;  // PENDING_CREDIT, CREDITED, FAILED
    
    @Column
    private LocalDateTime creditedAt;
    
    @Column(length = 100)
    private String creditReferenceNumber;  // Auto-generated when credited
    
    // Collection Details
    @Column(nullable = false, length = 20)
    private String collectionStatus;  // COLLECTED, PENDING, REVERSED
    
    @Column
    private LocalDateTime collectionDate;
    
    @Column(columnDefinition = "LONGTEXT")
    private String collectionDetails;  // JSON: {mode, gateway, ref_id, etc.}
    
    // Tax/Withholding (if applicable)
    @Column(precision = 19, scale = 2)
    private BigDecimal taxAmount;  // TDS or other withholding
    
    @Column(precision = 19, scale = 2)
    private BigDecimal netCreditAmount;  // chargeAmount - taxAmount
    
    // Status and Audit
    @Column(nullable = false, length = 20)
    private String status;  // SUCCESS, PENDING, FAILED, REVERSED
    
    @Column(length = 500)
    private String failureReason;
    
    @Column
    private Long processedByAdminId;
    
    @Column(length = 100)
    private String processedByAdminName;
    
    @Column
    private LocalDateTime processedAt;
    
    // Reconciliation
    @Column(length = 50)
    private String reconciliationStatus;  // PENDING, MATCHED, UNMATCHED
    
    @Column
    private LocalDateTime reconciliationDate;
    
    @Column(length = 100)
    private String reconciliationNotes;
    
    // Timestamps
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    // Constructors
    public ChargeTransaction() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.status = "PENDING";
        this.creditStatus = "PENDING_CREDIT";
        this.collectionStatus = "PENDING";
        this.reconciliationStatus = "PENDING";
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getChargeTransactionId() { return chargeTransactionId; }
    public void setChargeTransactionId(String chargeTransactionId) { this.chargeTransactionId = chargeTransactionId; }
    
    public Long getAllocationId() { return allocationId; }
    public void setAllocationId(Long allocationId) { this.allocationId = allocationId; }
    
    public Long getAllocationAccountId() { return allocationAccountId; }
    public void setAllocationAccountId(Long allocationAccountId) { this.allocationAccountId = allocationAccountId; }
    
    public String getChargeType() { return chargeType; }
    public void setChargeType(String chargeType) { this.chargeType = chargeType; }
    
    public String getChargeDescription() { return chargeDescription; }
    public void setChargeDescription(String chargeDescription) { this.chargeDescription = chargeDescription; }
    
    public BigDecimal getChargeAmount() { return chargeAmount; }
    public void setChargeAmount(BigDecimal chargeAmount) { this.chargeAmount = chargeAmount; }
    
    public String getUserAccountNumber() { return userAccountNumber; }
    public void setUserAccountNumber(String userAccountNumber) { this.userAccountNumber = userAccountNumber; }
    
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    
    public String getUserProductType() { return userProductType; }
    public void setUserProductType(String userProductType) { this.userProductType = userProductType; }
    
    public String getLinkedTransactionId() { return linkedTransactionId; }
    public void setLinkedTransactionId(String linkedTransactionId) { this.linkedTransactionId = linkedTransactionId; }
    
    public String getLinkedLoanId() { return linkedLoanId; }
    public void setLinkedLoanId(String linkedLoanId) { this.linkedLoanId = linkedLoanId; }
    
    public String getLinkedDepositId() { return linkedDepositId; }
    public void setLinkedDepositId(String linkedDepositId) { this.linkedDepositId = linkedDepositId; }
    
    public String getCreditStatus() { return creditStatus; }
    public void setCreditStatus(String creditStatus) { this.creditStatus = creditStatus; }
    
    public LocalDateTime getCreditedAt() { return creditedAt; }
    public void setCreditedAt(LocalDateTime creditedAt) { this.creditedAt = creditedAt; }
    
    public String getCreditReferenceNumber() { return creditReferenceNumber; }
    public void setCreditReferenceNumber(String creditReferenceNumber) { this.creditReferenceNumber = creditReferenceNumber; }
    
    public String getCollectionStatus() { return collectionStatus; }
    public void setCollectionStatus(String collectionStatus) { this.collectionStatus = collectionStatus; }
    
    public LocalDateTime getCollectionDate() { return collectionDate; }
    public void setCollectionDate(LocalDateTime collectionDate) { this.collectionDate = collectionDate; }
    
    public String getCollectionDetails() { return collectionDetails; }
    public void setCollectionDetails(String collectionDetails) { this.collectionDetails = collectionDetails; }
    
    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }
    
    public BigDecimal getNetCreditAmount() { return netCreditAmount; }
    public void setNetCreditAmount(BigDecimal netCreditAmount) { this.netCreditAmount = netCreditAmount; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    
    public Long getProcessedByAdminId() { return processedByAdminId; }
    public void setProcessedByAdminId(Long processedByAdminId) { this.processedByAdminId = processedByAdminId; }
    
    public String getProcessedByAdminName() { return processedByAdminName; }
    public void setProcessedByAdminName(String processedByAdminName) { this.processedByAdminName = processedByAdminName; }
    
    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
    
    public String getReconciliationStatus() { return reconciliationStatus; }
    public void setReconciliationStatus(String reconciliationStatus) { this.reconciliationStatus = reconciliationStatus; }
    
    public LocalDateTime getReconciliationDate() { return reconciliationDate; }
    public void setReconciliationDate(LocalDateTime reconciliationDate) { this.reconciliationDate = reconciliationDate; }
    
    public String getReconciliationNotes() { return reconciliationNotes; }
    public void setReconciliationNotes(String reconciliationNotes) { this.reconciliationNotes = reconciliationNotes; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
