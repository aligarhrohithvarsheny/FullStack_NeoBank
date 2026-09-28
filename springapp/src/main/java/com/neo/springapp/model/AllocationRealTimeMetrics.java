package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Real-time metrics for an allocation.
 * Updated after each debit/credit transaction to show current state of allocated funds.
 * Used for dashboard KPIs and monitoring.
 */
@Data
@Entity
@Table(name = "allocation_metrics", indexes = {
    @Index(name = "idx_allocation_id", columnList = "allocation_id", unique = true),
    @Index(name = "idx_metrics_date", columnList = "metrics_date")
})
public class AllocationRealTimeMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "allocation_id", nullable = false, unique = true)
    private Long allocationId; // FK to FundsAllocation

    @Column(name = "allocation_id_str", length = 50)
    private String allocationIdStr; // For reference: FA-2025-00001

    @Column(name = "total_allocated", nullable = false)
    private Double totalAllocated = 0.0; // Original allocated amount

    @Column(name = "total_debited", nullable = false)
    private Double totalDebited = 0.0; // Total amount debited (used)

    @Column(name = "total_credited", nullable = false)
    private Double totalCredited = 0.0; // Total amount credited back

    @Column(name = "current_balance", nullable = false)
    private Double currentBalance = 0.0; // allocated - debited + credited

    @Column(name = "utilization_percentage")
    private Double utilizationPercentage = 0.0; // (debited / allocated) * 100

    @Column(name = "debit_count")
    private Long debitCount = 0L; // Number of debit transactions

    @Column(name = "credit_count")
    private Long creditCount = 0L; // Number of credit transactions

    @Column(name = "total_transactions")
    private Long totalTransactions = 0L; // debitCount + creditCount

    @Column(name = "metrics_date")
    private LocalDate metricsDate;

    @Column(name = "last_transaction_date")
    private LocalDateTime lastTransactionDate;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    // Breakdown by product type (stored as JSON for flexibility)
    @Column(name = "product_breakdown", columnDefinition = "TEXT")
    private String productBreakdown; // JSON: {GOLD_LOAN: {debited: 50000, credited: 10000}, ...}

    @Column(name = "gold_loan_debited")
    private Double goldLoanDebited = 0.0;

    @Column(name = "deposits_debited")
    private Double depositsDebited = 0.0;

    @Column(name = "withdrawals_debited")
    private Double withdrawalsDebited = 0.0;

    @Column(name = "loans_debited")
    private Double loansDebited = 0.0;

    @Column(name = "overdraft_debited")
    private Double overdraftDebited = 0.0;

    @Column(name = "salary_credit_debited")
    private Double salaryCreditDebited = 0.0;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        lastUpdated = now;
        if (metricsDate == null) {
            metricsDate = LocalDate.now();
        }
        calculateBalance();
    }

    @PreUpdate
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
        calculateBalance();
    }

    private void calculateBalance() {
        if (totalAllocated != null) {
            this.currentBalance = totalAllocated - (totalDebited != null ? totalDebited : 0.0) + (totalCredited != null ? totalCredited : 0.0);
            this.utilizationPercentage = (totalDebited != null && totalAllocated > 0) 
                ? (totalDebited / totalAllocated) * 100.0 : 0.0;
        }
    }
}
