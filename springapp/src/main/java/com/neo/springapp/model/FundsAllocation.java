package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents funds allocated by HOD to a branch manager.
 * Tracks allocation amount, validity, product types, and status.
 */
@Data
@Entity
@Table(name = "funds_allocations", indexes = {
    @Index(name = "idx_allocation_id", columnList = "allocation_id", unique = true),
    @Index(name = "idx_manager_id", columnList = "manager_id"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_allocation_date", columnList = "allocation_date")
})
public class FundsAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "allocation_id", unique = true, nullable = false, length = 50)
    private String allocationId; // e.g., FA-2025-00001

    @Column(name = "manager_id")
    private Long managerId; // Admin ID of branch manager

    @Column(name = "manager_name", length = 255)
    private String managerName;

    @Column(name = "branch_name", length = 255)
    private String branchName;

    @Column(name = "manager_account_number", length = 50)
    private String managerAccountNumber; // Manager's account to be credited

    @Column(name = "allocated_amount", nullable = false)
    private Double allocatedAmount = 0.0;

    @Column(name = "allocation_type", length = 50)
    private String allocationType; // GOLD_LOAN, DEPOSIT, WITHDRAWAL, LOAN, OVERDRAFT, SALARY_CREDIT, GENERAL

    @Column(name = "product_types", columnDefinition = "TEXT")
    private String productTypes; // Comma-separated: GOLD_LOAN,DEPOSITS,WITHDRAWALS,LOANS,OVERDRAFT,SALARY_CREDITS

    @Column(name = "allocation_date")
    private LocalDate allocationDate;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_till")
    private LocalDate validTill;

    @Column(name = "status", length = 50)
    private String status = "ACTIVE"; // ACTIVE, PAUSED, COMPLETED, CANCELLED

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "allocated_by_admin_id")
    private Long allocatedByAdminId; // HOD admin ID

    @Column(name = "allocated_by_name", length = 255)
    private String allocatedByName; // HOD name

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancelled_reason", columnDefinition = "TEXT")
    private String cancelledReason;

    @Column(name = "cancelled_by_admin_id")
    private Long cancelledByAdminId;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "total_utilized")
    private Double totalUtilized = 0.0;

    @Column(name = "total_debited")
    private Double totalDebited = 0.0;

    @Column(name = "total_credited")
    private Double totalCredited = 0.0;

    @Column(name = "current_balance")
    private Double currentBalance;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (allocationId == null) {
            allocationId = "FA-" + System.currentTimeMillis();
        }
        if (allocatedAmount != null) {
            currentBalance = allocatedAmount;
        }
        if (allocationType == null) {
            allocationType = "GENERAL";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
