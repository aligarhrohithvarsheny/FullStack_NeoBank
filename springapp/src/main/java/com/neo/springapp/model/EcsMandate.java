package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "ecs_mandates")
public class EcsMandate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String mandateId; // ECS-xxxxxxxx

    private String loanAccountNumber;
    private String loanType; // Personal / Education / Gold ... etc.
    private String savingsAccountNumber; // Account debited every month
    private String customerName;
    private String customerId;
    private String dob;

    private Integer debitDay; // Day of month (1-31) the EMI is auto-debited
    private Double amountLimit; // Max amount allowed per debit
    private Double emiAmount;

    // ACTIVE, PAUSED, CANCELLED
    private String status = "ACTIVE";
    private String pausedBy; // ADMIN / USER

    private Boolean cancelRequested = false;
    private String cancelReason;
    private LocalDateTime cancelRequestedAt;
    private String cancelRejectionNote;

    private String createdBy;
    private String cancelledBy;
    private LocalDateTime cancelledAt;

    private LocalDate lastAttemptDate;
    private LocalDateTime lastDebitAt;
    private String lastDebitStatus; // SUCCESS / FAILED
    private String lastDebitMessage;
    private Integer successfulDebits = 0;
    private Integer failedDebits = 0;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
