package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents funds allocated by HOD to a branch manager.
 * Tracks allocation amount, validity, product types, and status.
 */
@Data
@Entity
@Table(name = "funds_allocations", indexes = {
    @Index(name = "idx_funds_allocations_id", columnList = "allocation_id", unique = true),
    @Index(name = "idx_funds_allocations_manager", columnList = "manager_id"),
    @Index(name = "idx_funds_allocations_status", columnList = "status"),
    @Index(name = "idx_funds_allocations_date", columnList = "allocation_date")
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

    @Column(name = "allocation_account_id")
    private Long allocationAccountId;

    @Column(name = "linked_account_number", length = 50)
    private String linkedAccountNumber;

    @Column(name = "linked_ifsc_code", length = 20)
    private String linkedIfscCode;

    @Column(name = "linked_account_holder_name", length = 100)
    private String linkedAccountHolderName;

    @Column(name = "linked_bank_name", length = 100)
    private String linkedBankName;

    @Column(name = "linked_account_type", length = 30)
    private String linkedAccountType;

    @Column(name = "account_status", length = 30)
    private String accountStatus = "NOT_LINKED";

    @Column(name = "account_verification_status", length = 20)
    private String accountVerificationStatus = "PENDING";

    @Column(name = "account_verified_at")
    private LocalDateTime accountVerifiedAt;

    @Column(name = "linked_by_admin_id")
    private Long linkedByAdminId;

    @Column(name = "linked_by_admin_name", length = 100)
    private String linkedByAdminName;

    @Column(name = "linked_at")
    private LocalDateTime linkedAt;

    @Column(name = "cheque_verification_status", length = 20)
    private String chequeVerificationStatus = "PENDING";

    @Column(name = "linked_cheque_number", length = 50)
    private String linkedChequeNumber;

    @Column(name = "cheque_holder_name", length = 100)
    private String chequeHolderName;

    @Column(name = "cheque_date")
    private LocalDate chequeDate;

    @Column(name = "cheque_bank", length = 100)
    private String chequeBank;

    @Column(name = "cheque_image_url", columnDefinition = "LONGTEXT")
    private String chequeImageUrl;

    @Column(name = "cheque_verification_notes", length = 500)
    private String chequeVerificationNotes;

    @Column(name = "verified_by_admin_id")
    private Long verifiedByAdminId;

    @Column(name = "verified_by_admin_name", length = 100)
    private String verifiedByAdminName;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "charge_management_enabled")
    private Boolean chargeManagementEnabled = false;

    @Column(name = "total_charges_collected", precision = 19, scale = 2)
    private BigDecimal totalChargesCollected = BigDecimal.ZERO;

    @Column(name = "interest_charges", precision = 19, scale = 2)
    private BigDecimal interestCharges = BigDecimal.ZERO;

    @Column(name = "cibil_charges", precision = 19, scale = 2)
    private BigDecimal cibilCharges = BigDecimal.ZERO;

    @Column(name = "soundbox_charges", precision = 19, scale = 2)
    private BigDecimal soundboxCharges = BigDecimal.ZERO;

    @Column(name = "upi_charges", precision = 19, scale = 2)
    private BigDecimal upiCharges = BigDecimal.ZERO;

    @Column(name = "payment_gateway_charges", precision = 19, scale = 2)
    private BigDecimal paymentGatewayCharges = BigDecimal.ZERO;

    @Column(name = "other_charges", precision = 19, scale = 2)
    private BigDecimal otherCharges = BigDecimal.ZERO;

    @Column(name = "charge_transaction_count")
    private Integer chargeTransactionCount = 0;

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
