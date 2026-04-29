package com.neo.springapp.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "net_banking_service_audit")
public class NetBankingServiceAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_type", nullable = false, length = 50)
    private String serviceType;

    @Column(name = "old_status", nullable = false)
    private Boolean oldStatus;

    @Column(name = "new_status", nullable = false)
    private Boolean newStatus;

    @Column(name = "changed_by", nullable = false, length = 100)
    private String changedBy;

    @Column(name = "changed_at", updatable = false)
    private LocalDateTime changedAt;

    @Column(name = "remarks", length = 500)
    private String remarks;

    @Column(name = "account_number", length = 50)
    private String accountNumber;

    @Column(name = "customer_name", length = 100)
    private String customerName;

    @PrePersist
    protected void onCreate() {
        this.changedAt = LocalDateTime.now();
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getServiceType() { return serviceType; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }

    public Boolean getOldStatus() { return oldStatus; }
    public void setOldStatus(Boolean oldStatus) { this.oldStatus = oldStatus; }

    public Boolean getNewStatus() { return newStatus; }
    public void setNewStatus(Boolean newStatus) { this.newStatus = newStatus; }

    public String getChangedBy() { return changedBy; }
    public void setChangedBy(String changedBy) { this.changedBy = changedBy; }

    public LocalDateTime getChangedAt() { return changedAt; }
    public void setChangedAt(LocalDateTime changedAt) { this.changedAt = changedAt; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
}
