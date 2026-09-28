package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Tracks real-time debit and credit transactions for allocated funds.
 * Records each transaction against an allocation with full audit trail.
 */
@Data
@Entity
@Table(name = "allocation_utilizations", indexes = {
    @Index(name = "idx_allocation_id", columnList = "allocation_id"),
    @Index(name = "idx_utilization_id", columnList = "utilization_id", unique = true),
    @Index(name = "idx_transaction_type", columnList = "transaction_type"),
    @Index(name = "idx_product_type", columnList = "product_type"),
    @Index(name = "idx_transaction_date", columnList = "transaction_date")
})
public class AllocationUtilization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "allocation_id", nullable = false)
    private Long allocationId; // FK to FundsAllocation

    @Column(name = "utilization_id", unique = true, nullable = false, length = 50)
    private String utilizationId; // e.g., AU-2025-00001

    @Column(name = "transaction_type", length = 20, nullable = false)
    private String transactionType; // DEBIT or CREDIT

    @Column(name = "amount", nullable = false)
    private Double amount;

    @Column(name = "remaining_balance", nullable = false)
    private Double remainingBalance; // Balance after this transaction

    @Column(name = "linked_transaction_id", length = 100)
    private String linkedTransactionId; // Reference to actual transaction/loan/etc

    @Column(name = "product_type", length = 50, nullable = false)
    private String productType; // GOLD_LOAN, DEPOSIT, WITHDRAWAL, LOAN, OVERDRAFT, SALARY_CREDIT

    @Column(name = "user_account_number", length = 50)
    private String userAccountNumber;

    @Column(name = "user_name", length = 255)
    private String userName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "performed_by_admin_id")
    private Long performedByAdminId; // Branch Manager or processor

    @Column(name = "performed_by_name", length = 255)
    private String performedByName;

    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "reference_number", length = 100)
    private String referenceNumber; // Cheque, UPI, NEFT reference

    @Column(name = "status", length = 30)
    private String status; // SUCCESS, PENDING, FAILED

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        if (utilizationId == null) {
            utilizationId = "AU-" + System.currentTimeMillis();
        }
        if (transactionDate == null) {
            transactionDate = now;
        }
        if (status == null) {
            status = "SUCCESS";
        }
    }
}
