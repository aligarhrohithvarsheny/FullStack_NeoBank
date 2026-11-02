package com.neo.springapp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import com.fasterxml.jackson.annotation.JsonIgnore;
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

    @Column(unique = true, length = 32)
    private String rdAccountNumber;

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

    @Column(length = 128)
    private String payoutInstruction;

    @Column
    private Integer paidInstallments = 0;

    @Column
    private Double paidPrincipal = 0.0;

    @Column(nullable = false, length = 24)
    private String status = "DETAILS_ONLY";

    @JsonIgnore
    @Column(length = 64)
    private String shareTokenHash;

    private LocalDateTime shareExpiresAt;
    private LocalDateTime signedAt;

    @Column(length = 128)
    private String signedName;

    @JsonIgnore
    @Column(length = 64)
    private String signatureHash;

    @Column(length = 64)
    private String signatureIp;

    private LocalDateTime approvedAt;

    @Column(length = 128)
    private String approvedBy;

    @JsonIgnore
    @Column(length = 64)
    private String captchaChallengeId;

    @JsonIgnore
    @Column(length = 64)
    private String captchaAnswerHash;

    @JsonIgnore
    private LocalDateTime captchaExpiresAt;

    @JsonIgnore
    private Integer captchaAttempts = 0;

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

    @PostLoad
    void applyLegacyDefaults() {
        if (paidInstallments == null) paidInstallments = 0;
        if (paidPrincipal == null) paidPrincipal = 0.0;
    }
}
