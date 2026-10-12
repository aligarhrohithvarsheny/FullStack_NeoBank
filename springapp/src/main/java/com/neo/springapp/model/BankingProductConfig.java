package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "banking_product_configs", indexes = {
        @Index(name = "idx_banking_product_code_status", columnList = "product_code,status"),
        @Index(name = "idx_banking_product_effective_from", columnList = "effective_from")
})
public class BankingProductConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_code", nullable = false, length = 40)
    private String productCode;

    @Column(name = "product_name", nullable = false, length = 120)
    private String productName;

    @Column(name = "account_type", nullable = false, length = 20)
    private String accountType;

    @Column(nullable = false)
    private Double interestRate;

    @Column(nullable = false)
    private Double minimumBalance;

    @Column(nullable = false)
    private Double dailyTransactionLimit;

    @Column(nullable = false)
    private Double monthlyTransactionLimit;

    @Column(nullable = false)
    private Double serviceCharge;

    @Column(nullable = false, length = 2000)
    private String eligibility;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(nullable = false, length = 24)
    private String status = "PENDING_APPROVAL";

    @Column(nullable = false, length = 120)
    private String requestedBy;

    @Column(length = 120)
    private String approvedBy;

    @Column(length = 1000)
    private String rejectionReason;

    private LocalDateTime createdAt;
    private LocalDateTime approvedAt;

    @PrePersist
    public void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (status == null) status = "PENDING_APPROVAL";
    }
}
