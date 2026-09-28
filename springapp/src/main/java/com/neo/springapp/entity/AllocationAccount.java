package com.neo.springapp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;

/**
 * AllocationAccount Entity
 * Represents the CURRENT ACCOUNT linked to an allocation
 * All debit/credit operations MUST use this account only
 * Verified via Cheque mechanism
 */
@Entity
@Table(name = "allocation_account", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"accountNumber"}),
    @UniqueConstraint(columnNames = {"allocationId"})
})
public class AllocationAccount {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private Long allocationId;  // FK to funds_allocation
    
    @Column(nullable = false, length = 50)
    private String accountNumber;
    
    @Column(nullable = false, length = 50)
    private String ifscCode;
    
    @Column(nullable = false, length = 100)
    private String accountHolderName;
    
    @Column(length = 100)
    private String bankName;
    
    @Column(length = 50)
    private String accountType;  // CURRENT, SAVINGS
    
    @Column(length = 100)
    private String branchName;
    
    @Column(length = 100)
    private String city;
    
    @Column(length = 100)
    private String location;
    
    @Column(length = 50)
    private String state;
    
    @Column(precision = 19, scale = 2)
    private BigDecimal accountBalance;
    
    // Account Verification
    @Column(nullable = false, length = 20)
    private String verificationStatus;  // PENDING, VERIFIED, REJECTED
    
    @Column(length = 50)
    private String chequeNumber;  // Reference cheque for verification
    
    @Column(length = 100)
    private String chequeHolderName;
    
    @Column(length = 20)
    private String chequeStatus;  // PENDING, CLEARED, BOUNCED
    
    @Column(columnDefinition = "LONGTEXT")
    private String verificationDocuments;  // JSON: {cheque_image_url, bank_statement_url, etc}
    
    @Column(length = 500)
    private String verificationNotes;
    
    // Account Status
    @Column(nullable = false, length = 20)
    private String accountStatus;  // ACTIVE, BLOCKED, INACTIVE
    
    @Column(precision = 19, scale = 2)
    private BigDecimal totalAllocated;  // Total fund allocated
    
    @Column(precision = 19, scale = 2)
    private BigDecimal totalDebited;    // Total debited
    
    @Column(precision = 19, scale = 2)
    private BigDecimal totalCredited;   // Total credited
    
    @Column(precision = 19, scale = 2)
    private BigDecimal currentBalance;  // Current available balance
    
    // Charge Tracking
    @Column(precision = 19, scale = 2)
    private BigDecimal totalChargesCollected;  // Total charges debited from users
    
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
    
    // Audit
    @Column(nullable = false)
    private Long linkedByAdminId;
    
    @Column(nullable = false, length = 100)
    private String linkedByAdminName;
    
    @Column(nullable = false)
    private LocalDateTime linkedAt;
    
    @Column
    private LocalDateTime verifiedAt;
    
    @Column
    private Long verifiedByAdminId;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getAllocationId() { return allocationId; }
    public void setAllocationId(Long allocationId) { this.allocationId = allocationId; }
    
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    
    public String getIfscCode() { return ifscCode; }
    public void setIfscCode(String ifscCode) { this.ifscCode = ifscCode; }
    
    public String getAccountHolderName() { return accountHolderName; }
    public void setAccountHolderName(String accountHolderName) { this.accountHolderName = accountHolderName; }
    
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    
    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }
    
    public String getBranchName() { return branchName; }
    public void setBranchName(String branchName) { this.branchName = branchName; }
    
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    
    public BigDecimal getAccountBalance() { return accountBalance; }
    public void setAccountBalance(BigDecimal accountBalance) { this.accountBalance = accountBalance; }
    
    public String getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(String verificationStatus) { this.verificationStatus = verificationStatus; }
    
    public String getChequeNumber() { return chequeNumber; }
    public void setChequeNumber(String chequeNumber) { this.chequeNumber = chequeNumber; }
    
    public String getChequeHolderName() { return chequeHolderName; }
    public void setChequeHolderName(String chequeHolderName) { this.chequeHolderName = chequeHolderName; }
    
    public String getChequeStatus() { return chequeStatus; }
    public void setChequeStatus(String chequeStatus) { this.chequeStatus = chequeStatus; }
    
    public String getVerificationDocuments() { return verificationDocuments; }
    public void setVerificationDocuments(String verificationDocuments) { this.verificationDocuments = verificationDocuments; }
    
    public String getVerificationNotes() { return verificationNotes; }
    public void setVerificationNotes(String verificationNotes) { this.verificationNotes = verificationNotes; }
    
    public String getAccountStatus() { return accountStatus; }
    public void setAccountStatus(String accountStatus) { this.accountStatus = accountStatus; }
    
    public BigDecimal getTotalAllocated() { return totalAllocated; }
    public void setTotalAllocated(BigDecimal totalAllocated) { this.totalAllocated = totalAllocated; }
    
    public BigDecimal getTotalDebited() { return totalDebited; }
    public void setTotalDebited(BigDecimal totalDebited) { this.totalDebited = totalDebited; }
    
    public BigDecimal getTotalCredited() { return totalCredited; }
    public void setTotalCredited(BigDecimal totalCredited) { this.totalCredited = totalCredited; }
    
    public BigDecimal getCurrentBalance() { return currentBalance; }
    public void setCurrentBalance(BigDecimal currentBalance) { this.currentBalance = currentBalance; }
    
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
    
    public Long getLinkedByAdminId() { return linkedByAdminId; }
    public void setLinkedByAdminId(Long linkedByAdminId) { this.linkedByAdminId = linkedByAdminId; }
    
    public String getLinkedByAdminName() { return linkedByAdminName; }
    public void setLinkedByAdminName(String linkedByAdminName) { this.linkedByAdminName = linkedByAdminName; }
    
    public LocalDateTime getLinkedAt() { return linkedAt; }
    public void setLinkedAt(LocalDateTime linkedAt) { this.linkedAt = linkedAt; }
    
    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }
    
    public Long getVerifiedByAdminId() { return verifiedByAdminId; }
    public void setVerifiedByAdminId(Long verifiedByAdminId) { this.verifiedByAdminId = verifiedByAdminId; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
