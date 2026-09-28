package com.neo.springapp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;

/**
 * FundsAllocation Entity (ENHANCED)
 * Master record for fund allocations from HOD to branch managers
 * NOW INCLUDES: Account Linking, Charge Management, Cheque Verification
 * All debit/credit operations are linked to the allocation account
 */
@Entity(name = "AccountLinkedFundsAllocation")
@Table(name = "funds_allocation", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"allocationId"}),
    @UniqueConstraint(columnNames = {"allocationAccountId"})
}, indexes = {
    @Index(name = "idx_enhanced_allocation_id", columnList = "allocationId"),
    @Index(name = "idx_enhanced_manager_id", columnList = "managerId"),
    @Index(name = "idx_enhanced_status", columnList = "status"),
    @Index(name = "idx_enhanced_manager_status", columnList = "managerId, status"),
    @Index(name = "idx_enhanced_valid_till", columnList = "validTill"),
    @Index(name = "idx_enhanced_branch_name", columnList = "branchName"),
    @Index(name = "idx_enhanced_account_status", columnList = "accountStatus")
})
public class FundsAllocation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 50)
    private String allocationId;  // FA-2025-XXXXX
    
    @Column(nullable = false)
    private Long managerId;
    
    @Column(nullable = false, length = 100)
    private String managerName;
    
    @Column(length = 50)
    private String managerAccountNumber;
    
    @Column(nullable = false, length = 100)
    private String branchName;
    
    @Column(length = 100)
    private String city;
    
    @Column(length = 100)
    private String location;
    
    @Column(length = 50)
    private String state;
    
    // FUND ALLOCATION DETAILS
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal allocatedAmount;
    
    @Column(length = 50)
    private String allocationType;  // GENERAL, CAMPAIGN, SEASONAL
    
    @Column(length = 255)
    private String productTypes;  // GOLD_LOAN,DEPOSITS,WITHDRAWALS,LOANS,OVERDRAFT,SALARY_CREDITS
    
    @Column(nullable = false, length = 20)
    private String status;  // ACTIVE, PAUSED, COMPLETED, CANCELLED
    
    @Column(precision = 19, scale = 2)
    private BigDecimal currentBalance;
    
    @Column(precision = 19, scale = 2)
    private BigDecimal totalDebited;
    
    @Column(precision = 19, scale = 2)
    private BigDecimal totalCredited;
    
    @Column(precision = 19, scale = 2)
    private BigDecimal totalUtilized;
    
    // VALIDITY
    @Column
    private java.time.LocalDate allocationDate;
    
    @Column
    private java.time.LocalDate validFrom;
    
    @Column(nullable = false)
    private java.time.LocalDate validTill;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    // ==================== ACCOUNT LINKING ====================
    // This is the KEY: All allocations MUST have a linked account
    
    @Column(nullable = false)
    private Long allocationAccountId;  // FK to allocation_account
    
    @Column(nullable = false, length = 50)
    private String linkedAccountNumber;  // Denormalized for quick access
    
    @Column(nullable = false, length = 50)
    private String linkedIfscCode;
    
    @Column(nullable = false, length = 100)
    private String linkedAccountHolderName;
    
    @Column(length = 100)
    private String linkedBankName;
    
    @Column(length = 50)
    private String linkedAccountType;  // CURRENT, SAVINGS
    
    @Column(length = 20)
    private String accountVerificationStatus;  // PENDING, VERIFIED, REJECTED
    
    @Column
    private LocalDateTime accountVerifiedAt;
    
    // CHEQUE VERIFICATION DETAILS
    @Column(nullable = false, length = 20)
    private String chequeVerificationStatus;  // PENDING, CLEARED, BOUNCED
    
    @Column(length = 50)
    private String linkedChequeNumber;
    
    @Column(length = 100)
    private String chequeHolderName;
    
    @Column
    private java.time.LocalDate chequeDate;
    
    @Column(length = 50)
    private String chequeBank;
    
    @Column(columnDefinition = "LONGTEXT")
    private String chequeImageUrl;  // Base64 or S3 URL
    
    @Column(length = 500)
    private String chequeVerificationNotes;
    
    @Column
    private LocalDateTime chequeVerifiedAt;
    
    // ==================== CHARGE MANAGEMENT ====================
    // Track charges collected and credited to account
    
    @Column(nullable = false)
    private Boolean chargeManagementEnabled;  // Enable/disable charge collection
    
    @Column(precision = 19, scale = 2)
    private BigDecimal totalChargesCollected;  // All charges debited from users
    
    @Column(precision = 19, scale = 2)
    private BigDecimal interestCharges;
    
    @Column(precision = 19, scale = 2)
    private BigDecimal cibilCharges;
    
    @Column(precision = 19, scale = 2)
    private BigDecimal soundboxCharges;
    
    @Column(precision = 19, scale = 2)
    private BigDecimal upiCharges;
    
    @Column(precision = 19, scale = 2)
    private BigDecimal paymentGatewayCharges;
    
    @Column(precision = 19, scale = 2)
    private BigDecimal otherCharges;
    
    @Column
    private Integer chargeTransactionCount;
    
    // CHARGE POLICIES
    @Column(columnDefinition = "LONGTEXT")
    private String chargePolicy;  // JSON: {interest_rate, cibil_charges, soundbox_rate, etc.}
    
    // ==================== AUDIT & CONTROL ====================
    
    @Column
    private Long allocatedByAdminId;
    
    @Column(length = 100)
    private String allocatedByAdminName;
    
    @Column
    private Long linkedByAdminId;
    
    @Column(length = 100)
    private String linkedByAdminName;
    
    @Column
    private LocalDateTime linkedAt;
    
    @Column
    private Long verifiedByAdminId;
    
    @Column(length = 100)
    private String verifiedByAdminName;
    
    @Column
    private LocalDateTime verifiedAt;
    
    @Column
    private LocalDateTime cancelledAt;
    
    @Column(columnDefinition = "TEXT")
    private String cancelledReason;
    
    @Column
    private Long cancelledByAdminId;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    // Additional status for account linking
    @Column(nullable = false, length = 20)
    private String accountStatus;  // NOT_LINKED, LINKING_PENDING, VERIFIED, REJECTED, BLOCKED
    
    // Constructors
    public FundsAllocation() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.status = "ACTIVE";
        this.accountStatus = "NOT_LINKED";
        this.chequeVerificationStatus = "PENDING";
        this.accountVerificationStatus = "PENDING";
        this.chargeManagementEnabled = false;
        this.totalChargesCollected = BigDecimal.ZERO;
        this.totalDebited = BigDecimal.ZERO;
        this.totalCredited = BigDecimal.ZERO;
        this.totalUtilized = BigDecimal.ZERO;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getAllocationId() { return allocationId; }
    public void setAllocationId(String allocationId) { this.allocationId = allocationId; }
    
    public Long getManagerId() { return managerId; }
    public void setManagerId(Long managerId) { this.managerId = managerId; }
    
    public String getManagerName() { return managerName; }
    public void setManagerName(String managerName) { this.managerName = managerName; }
    
    public String getManagerAccountNumber() { return managerAccountNumber; }
    public void setManagerAccountNumber(String managerAccountNumber) { this.managerAccountNumber = managerAccountNumber; }
    
    public String getBranchName() { return branchName; }
    public void setBranchName(String branchName) { this.branchName = branchName; }
    
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    
    public BigDecimal getAllocatedAmount() { return allocatedAmount; }
    public void setAllocatedAmount(BigDecimal allocatedAmount) { this.allocatedAmount = allocatedAmount; }
    
    public String getAllocationType() { return allocationType; }
    public void setAllocationType(String allocationType) { this.allocationType = allocationType; }
    
    public String getProductTypes() { return productTypes; }
    public void setProductTypes(String productTypes) { this.productTypes = productTypes; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public BigDecimal getCurrentBalance() { return currentBalance; }
    public void setCurrentBalance(BigDecimal currentBalance) { this.currentBalance = currentBalance; }
    
    public BigDecimal getTotalDebited() { return totalDebited; }
    public void setTotalDebited(BigDecimal totalDebited) { this.totalDebited = totalDebited; }
    
    public BigDecimal getTotalCredited() { return totalCredited; }
    public void setTotalCredited(BigDecimal totalCredited) { this.totalCredited = totalCredited; }
    
    public BigDecimal getTotalUtilized() { return totalUtilized; }
    public void setTotalUtilized(BigDecimal totalUtilized) { this.totalUtilized = totalUtilized; }
    
    public java.time.LocalDate getAllocationDate() { return allocationDate; }
    public void setAllocationDate(java.time.LocalDate allocationDate) { this.allocationDate = allocationDate; }
    
    public java.time.LocalDate getValidFrom() { return validFrom; }
    public void setValidFrom(java.time.LocalDate validFrom) { this.validFrom = validFrom; }
    
    public java.time.LocalDate getValidTill() { return validTill; }
    public void setValidTill(java.time.LocalDate validTill) { this.validTill = validTill; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Long getAllocationAccountId() { return allocationAccountId; }
    public void setAllocationAccountId(Long allocationAccountId) { this.allocationAccountId = allocationAccountId; }
    
    public String getLinkedAccountNumber() { return linkedAccountNumber; }
    public void setLinkedAccountNumber(String linkedAccountNumber) { this.linkedAccountNumber = linkedAccountNumber; }
    
    public String getLinkedIfscCode() { return linkedIfscCode; }
    public void setLinkedIfscCode(String linkedIfscCode) { this.linkedIfscCode = linkedIfscCode; }
    
    public String getLinkedAccountHolderName() { return linkedAccountHolderName; }
    public void setLinkedAccountHolderName(String linkedAccountHolderName) { this.linkedAccountHolderName = linkedAccountHolderName; }
    
    public String getLinkedBankName() { return linkedBankName; }
    public void setLinkedBankName(String linkedBankName) { this.linkedBankName = linkedBankName; }
    
    public String getLinkedAccountType() { return linkedAccountType; }
    public void setLinkedAccountType(String linkedAccountType) { this.linkedAccountType = linkedAccountType; }
    
    public String getAccountVerificationStatus() { return accountVerificationStatus; }
    public void setAccountVerificationStatus(String accountVerificationStatus) { this.accountVerificationStatus = accountVerificationStatus; }
    
    public LocalDateTime getAccountVerifiedAt() { return accountVerifiedAt; }
    public void setAccountVerifiedAt(LocalDateTime accountVerifiedAt) { this.accountVerifiedAt = accountVerifiedAt; }
    
    public String getChequeVerificationStatus() { return chequeVerificationStatus; }
    public void setChequeVerificationStatus(String chequeVerificationStatus) { this.chequeVerificationStatus = chequeVerificationStatus; }
    
    public String getLinkedChequeNumber() { return linkedChequeNumber; }
    public void setLinkedChequeNumber(String linkedChequeNumber) { this.linkedChequeNumber = linkedChequeNumber; }
    
    public String getChequeHolderName() { return chequeHolderName; }
    public void setChequeHolderName(String chequeHolderName) { this.chequeHolderName = chequeHolderName; }
    
    public java.time.LocalDate getChequeDate() { return chequeDate; }
    public void setChequeDate(java.time.LocalDate chequeDate) { this.chequeDate = chequeDate; }
    
    public String getChequeBank() { return chequeBank; }
    public void setChequeBank(String chequeBank) { this.chequeBank = chequeBank; }
    
    public String getChequeImageUrl() { return chequeImageUrl; }
    public void setChequeImageUrl(String chequeImageUrl) { this.chequeImageUrl = chequeImageUrl; }
    
    public String getChequeVerificationNotes() { return chequeVerificationNotes; }
    public void setChequeVerificationNotes(String chequeVerificationNotes) { this.chequeVerificationNotes = chequeVerificationNotes; }
    
    public LocalDateTime getChequeVerifiedAt() { return chequeVerifiedAt; }
    public void setChequeVerifiedAt(LocalDateTime chequeVerifiedAt) { this.chequeVerifiedAt = chequeVerifiedAt; }
    
    public Boolean getChargeManagementEnabled() { return chargeManagementEnabled; }
    public void setChargeManagementEnabled(Boolean chargeManagementEnabled) { this.chargeManagementEnabled = chargeManagementEnabled; }
    
    public BigDecimal getTotalChargesCollected() { return totalChargesCollected; }
    public void setTotalChargesCollected(BigDecimal totalChargesCollected) { this.totalChargesCollected = totalChargesCollected; }
    
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
    
    public BigDecimal getOtherCharges() { return otherCharges; }
    public void setOtherCharges(BigDecimal otherCharges) { this.otherCharges = otherCharges; }
    
    public Integer getChargeTransactionCount() { return chargeTransactionCount; }
    public void setChargeTransactionCount(Integer chargeTransactionCount) { this.chargeTransactionCount = chargeTransactionCount; }
    
    public String getChargePolicy() { return chargePolicy; }
    public void setChargePolicy(String chargePolicy) { this.chargePolicy = chargePolicy; }
    
    public Long getAllocatedByAdminId() { return allocatedByAdminId; }
    public void setAllocatedByAdminId(Long allocatedByAdminId) { this.allocatedByAdminId = allocatedByAdminId; }
    
    public String getAllocatedByAdminName() { return allocatedByAdminName; }
    public void setAllocatedByAdminName(String allocatedByAdminName) { this.allocatedByAdminName = allocatedByAdminName; }
    
    public Long getLinkedByAdminId() { return linkedByAdminId; }
    public void setLinkedByAdminId(Long linkedByAdminId) { this.linkedByAdminId = linkedByAdminId; }
    
    public String getLinkedByAdminName() { return linkedByAdminName; }
    public void setLinkedByAdminName(String linkedByAdminName) { this.linkedByAdminName = linkedByAdminName; }
    
    public LocalDateTime getLinkedAt() { return linkedAt; }
    public void setLinkedAt(LocalDateTime linkedAt) { this.linkedAt = linkedAt; }
    
    public Long getVerifiedByAdminId() { return verifiedByAdminId; }
    public void setVerifiedByAdminId(Long verifiedByAdminId) { this.verifiedByAdminId = verifiedByAdminId; }
    
    public String getVerifiedByAdminName() { return verifiedByAdminName; }
    public void setVerifiedByAdminName(String verifiedByAdminName) { this.verifiedByAdminName = verifiedByAdminName; }
    
    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }
    
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
    
    public String getCancelledReason() { return cancelledReason; }
    public void setCancelledReason(String cancelledReason) { this.cancelledReason = cancelledReason; }
    
    public Long getCancelledByAdminId() { return cancelledByAdminId; }
    public void setCancelledByAdminId(Long cancelledByAdminId) { this.cancelledByAdminId = cancelledByAdminId; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public String getAccountStatus() { return accountStatus; }
    public void setAccountStatus(String accountStatus) { this.accountStatus = accountStatus; }
}
