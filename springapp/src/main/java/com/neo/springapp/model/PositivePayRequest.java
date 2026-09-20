package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "positive_pay_requests", indexes = {
        @Index(name = "idx_pp_reference", columnList = "reference_number", unique = true),
        @Index(name = "idx_pp_account", columnList = "account_number"),
        @Index(name = "idx_pp_cheque", columnList = "cheque_number"),
        @Index(name = "idx_pp_status", columnList = "status"),
        @Index(name = "idx_pp_submitted", columnList = "submitted_at")
})
public class PositivePayRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "reference_number", nullable = false, unique = true, length = 40)
    private String referenceNumber;
    private Long userId;
    private Long accountId;
    @Column(nullable = false, length = 64) private String accountNumber;
    @Column(nullable = false, length = 20) private String accountType;
    @Column(nullable = false, length = 30) private String chequeSource;
    private Long chequeId;
    @Column(nullable = false, length = 64) private String chequeNumber;
    private LocalDate chequeDate;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal amount;
    @Column(nullable = false, length = 255) private String payeeName;
    private String payeeAccountNumber;
    private String payeeBankName;
    private String payeeIfsc;
    @Column(columnDefinition = "TEXT") private String remarks;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32)
    private PositivePayStatus status = PositivePayStatus.PENDING_ADMIN_APPROVAL;
    private LocalDateTime submittedAt;
    private LocalDateTime approvedAt;
    private String approvedBy;
    private LocalDateTime rejectedAt;
    private String rejectedBy;
    @Column(columnDefinition = "TEXT") private String rejectionReason;
    private LocalDateTime cancelledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String accountHolderName;
    private String customerEmail;
    private String customerPhone;
    private String chequeStatus;
    private String chequeBookNumber;

    @PrePersist
    void onCreate() { LocalDateTime now = LocalDateTime.now(); createdAt = now; updatedAt = now; if (submittedAt == null) submittedAt = now; }
    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }
}
