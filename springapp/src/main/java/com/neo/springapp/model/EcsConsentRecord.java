package com.neo.springapp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "ecs_consent_records",
        uniqueConstraints = @UniqueConstraint(name = "uk_ecs_consent_mandate", columnNames = "mandate_db_id"))
public class EcsConsentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mandate_db_id", nullable = false)
    private Long mandateDbId;

    @Column(nullable = false, length = 80)
    private String mandateId;

    @Column(nullable = false, length = 40)
    private String productType;

    @Column(nullable = false, length = 80)
    private String productAccountNumber;

    @Column(nullable = false, length = 80)
    private String savingsAccountNumber;

    @Column(nullable = false, length = 128)
    private String customerName;

    @Column(nullable = false, length = 128)
    private String confirmedName;

    @Column(nullable = false)
    private Double monthlyAmount;

    @Column(nullable = false)
    private Double amountLimit;

    @Column(nullable = false)
    private Integer debitDay;

    @Column(nullable = false, length = 64)
    private String consentVersion;

    @Column(nullable = false, length = 1000)
    private String consentStatement;

    @Column(nullable = false)
    private LocalDateTime acceptedAt;

    @Column(length = 64)
    private String remoteAddress;
}
