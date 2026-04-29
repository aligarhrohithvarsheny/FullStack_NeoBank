package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "scheduled_payments")
public class ScheduledPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String accountNumber;

    @Column(nullable = false)
    private String recipientAccountNumber;

    @Column(nullable = false)
    private String recipientName;

    private String recipientIfsc;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    private String paymentType; // RENT, LOAN_EMI, SUBSCRIPTION, UTILITY, OTHER

    private String description;

    @Column(nullable = false)
    private String frequency; // DAILY, WEEKLY, MONTHLY, QUARTERLY, YEARLY

    @Column(nullable = false)
    private LocalDate startDate;

    private LocalDate endDate;

    private LocalDate nextPaymentDate;

    private LocalDate lastPaymentDate;

    @Column(nullable = false)
    private String status; // ACTIVE, PAUSED, COMPLETED, CANCELLED, FAILED

    private Integer totalPayments;

    private Integer completedPayments;

    private Double totalAmountPaid;

    private String failureReason;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ScheduledPayment() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.status = "ACTIVE";
        this.completedPayments = 0;
        this.totalAmountPaid = 0.0;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
