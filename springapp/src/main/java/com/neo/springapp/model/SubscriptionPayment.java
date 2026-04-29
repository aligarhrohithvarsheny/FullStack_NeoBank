package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "subscription_payments")
public class SubscriptionPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String employeeId;

    @Column(nullable = false)
    private String employeeName;

    private String employeeEmail;

    @Column(nullable = false)
    private String salaryAccountNumber;

    @Column(nullable = false)
    private String subscriptionName; // Netflix, Spotify, Gym, Magazine, etc.

    @Column(nullable = false)
    private String subscriptionCategory; // ENTERTAINMENT, HEALTH, EDUCATION, UTILITY, OTHER

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    private String frequency; // MONTHLY, QUARTERLY, YEARLY

    @Column(nullable = false)
    private LocalDate startDate;

    private LocalDate endDate;

    private LocalDate nextBillingDate;

    private LocalDate lastBillingDate;

    @Column(nullable = false)
    private String status; // ACTIVE, PAUSED, CANCELLED, EXPIRED

    private Integer billingCyclesCompleted;

    private Double totalAmountPaid;

    private boolean autoDebit;

    private String merchantId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public SubscriptionPayment() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.status = "ACTIVE";
        this.billingCyclesCompleted = 0;
        this.totalAmountPaid = 0.0;
        this.autoDebit = true;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
