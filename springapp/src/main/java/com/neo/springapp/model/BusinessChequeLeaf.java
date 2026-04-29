package com.neo.springapp.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "business_cheque_leaves", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"leaf_number"})
})
public class BusinessChequeLeaf {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "current_account_id", nullable = false)
    private Long currentAccountId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "leaf_number", nullable = false, length = 20, unique = true)
    private String leafNumber;

    @Column(name = "status", nullable = false, length = 20)
    private String status; // AVAILABLE, USED, CANCELLED

    @Column(name = "used_cheque_request_id")
    private Long usedChequeRequestId;

    @Column(name = "allocated_at", nullable = false)
    private LocalDateTime allocatedAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    public BusinessChequeLeaf() {
    }

    public BusinessChequeLeaf(Long currentAccountId, Long userId, String leafNumber) {
        this.currentAccountId = currentAccountId;
        this.userId = userId;
        this.leafNumber = leafNumber;
        this.status = "AVAILABLE";
        this.allocatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCurrentAccountId() { return currentAccountId; }
    public void setCurrentAccountId(Long currentAccountId) { this.currentAccountId = currentAccountId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getLeafNumber() { return leafNumber; }
    public void setLeafNumber(String leafNumber) { this.leafNumber = leafNumber; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getUsedChequeRequestId() { return usedChequeRequestId; }
    public void setUsedChequeRequestId(Long usedChequeRequestId) { this.usedChequeRequestId = usedChequeRequestId; }

    public LocalDateTime getAllocatedAt() { return allocatedAt; }
    public void setAllocatedAt(LocalDateTime allocatedAt) { this.allocatedAt = allocatedAt; }

    public LocalDateTime getUsedAt() { return usedAt; }
    public void setUsedAt(LocalDateTime usedAt) { this.usedAt = usedAt; }
}
