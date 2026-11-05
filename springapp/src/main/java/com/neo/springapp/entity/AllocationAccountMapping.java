package com.neo.springapp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "allocation_account_mapping")
public class AllocationAccountMapping {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long allocationId;

    @Column(nullable = false, length = 50)
    private String accountNumber;

    @Column(length = 50)
    private String resolvedAccountNumber;

    @Column(nullable = false, length = 50)
    private String accountType;

    @Column(length = 150)
    private String holderName;

    @Column(length = 100)
    private String branchName;

    @Column(length = 100)
    private String bankName;

    @Column(nullable = false, length = 20)
    private String mappingStatus = "ACTIVE";

    @Column(length = 50)
    private String replacedByAccountNumber;

    @Column(length = 500)
    private String notes;

    @Column(nullable = false)
    private Long mappedByAdminId;

    @Column(nullable = false, length = 100)
    private String mappedByAdminName;

    @Column(nullable = false)
    private LocalDateTime mappedAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAllocationId() { return allocationId; }
    public void setAllocationId(Long allocationId) { this.allocationId = allocationId; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public String getResolvedAccountNumber() { return resolvedAccountNumber; }
    public void setResolvedAccountNumber(String resolvedAccountNumber) { this.resolvedAccountNumber = resolvedAccountNumber; }
    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }
    public String getHolderName() { return holderName; }
    public void setHolderName(String holderName) { this.holderName = holderName; }
    public String getBranchName() { return branchName; }
    public void setBranchName(String branchName) { this.branchName = branchName; }
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public String getMappingStatus() { return mappingStatus; }
    public void setMappingStatus(String mappingStatus) { this.mappingStatus = mappingStatus; }
    public String getReplacedByAccountNumber() { return replacedByAccountNumber; }
    public void setReplacedByAccountNumber(String replacedByAccountNumber) { this.replacedByAccountNumber = replacedByAccountNumber; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Long getMappedByAdminId() { return mappedByAdminId; }
    public void setMappedByAdminId(Long mappedByAdminId) { this.mappedByAdminId = mappedByAdminId; }
    public String getMappedByAdminName() { return mappedByAdminName; }
    public void setMappedByAdminName(String mappedByAdminName) { this.mappedByAdminName = mappedByAdminName; }
    public LocalDateTime getMappedAt() { return mappedAt; }
    public void setMappedAt(LocalDateTime mappedAt) { this.mappedAt = mappedAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
