package com.neo.springapp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "recurring_deposits")
public class RecurringDeposit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String accountNumber;

    @Column(length = 128)
    private String customerName;

    private Long planId;

    @Column(length = 128)
    private String schemeName;

    @Column(nullable = false)
    private Double monthlyInstallment;

    @Column(nullable = false)
    private Double annualInterestRate;

    @Column(nullable = false)
    private Integer tenureMonths;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate maturityDate;

    @Column(nullable = false)
    private Double estimatedPrincipal;

    @Column(nullable = false)
    private Double estimatedInterest;

    @Column(nullable = false)
    private Double estimatedMaturityAmount;

    @Column(nullable = false, length = 24)
    private String status = "DETAILS_ONLY";

    @Column(length = 128)
    private String createdBy;

    @Column(length = 128)
    private String updatedBy;

    @Column(length = 512)
    private String remarks;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
