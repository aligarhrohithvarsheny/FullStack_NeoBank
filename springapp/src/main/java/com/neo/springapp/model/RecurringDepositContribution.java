package com.neo.springapp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "recurring_deposit_contributions")
public class RecurringDepositContribution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long recurringDepositId;

    @Column(nullable = false, unique = true, length = 40)
    private String idempotencyKey;

    @Column(nullable = false, length = 7)
    private String contributionMonth;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false, length = 64)
    private String sourceAccountNumber;

    @Column(length = 80)
    private String transactionId;

    @Column(nullable = false)
    private LocalDate postedDate;

    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
