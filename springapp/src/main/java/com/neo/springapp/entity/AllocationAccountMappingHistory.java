package com.neo.springapp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "allocation_account_mapping_history")
public class AllocationAccountMappingHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long allocationId;

    @Column(nullable = false, length = 30)
    private String action;

    @Column(nullable = false, length = 50)
    private String accountNumber;

    @Column(length = 50)
    private String replacementAccountNumber;

    @Column(length = 50)
    private String accountType;

    @Column(length = 1000)
    private String summary;

    @Column(nullable = false)
    private Long performedByAdminId;

    @Column(nullable = false, length = 100)
    private String performedByAdminName;

    @Column(nullable = false)
    private LocalDateTime performedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAllocationId() { return allocationId; }
    public void setAllocationId(Long allocationId) { this.allocationId = allocationId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public String getReplacementAccountNumber() { return replacementAccountNumber; }
    public void setReplacementAccountNumber(String replacementAccountNumber) { this.replacementAccountNumber = replacementAccountNumber; }
    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public Long getPerformedByAdminId() { return performedByAdminId; }
    public void setPerformedByAdminId(Long performedByAdminId) { this.performedByAdminId = performedByAdminId; }
    public String getPerformedByAdminName() { return performedByAdminName; }
    public void setPerformedByAdminName(String performedByAdminName) { this.performedByAdminName = performedByAdminName; }
    public LocalDateTime getPerformedAt() { return performedAt; }
    public void setPerformedAt(LocalDateTime performedAt) { this.performedAt = performedAt; }
}
