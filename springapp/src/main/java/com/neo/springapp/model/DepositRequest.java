package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "deposit_requests")
public class DepositRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String requestId;
    private String accountNumber;
    private String userName;
    private Double amount;
    private String method; // Cash / UPI / Cheque / Online Transfer
    private String referenceNumber;
    private String note;
    private String status; // PENDING / APPROVED / REJECTED
    private String processedBy;
    private LocalDateTime processedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String rejectionReason;
    private Double resultingBalance;
    private String transactionId;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.requestId == null) {
            this.requestId = "DEP" + System.currentTimeMillis();
        }
        if (this.status == null) {
            this.status = "PENDING";
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

