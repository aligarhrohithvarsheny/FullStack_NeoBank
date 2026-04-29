package com.neo.springapp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "insurance_claims")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class InsuranceClaim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String claimNumber;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "application_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private InsuranceApplication application;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String accountNumber;

    @Column(nullable = false)
    private Double claimAmount;

    // Store combined documents path/URL or JSON list
    @Column(columnDefinition = "TEXT")
    private String documentsPath;

    @Column(nullable = false)
    private String status = "PENDING"; // PENDING / APPROVED / REJECTED / PAID

    @Column(columnDefinition = "TEXT")
    private String adminRemark;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime approvedAt;
    private LocalDateTime paidAt;

    private String payoutTransactionId; // Link to Transaction table if needed

    public InsuranceClaim() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.claimNumber == null || this.claimNumber.isEmpty()) {
            this.claimNumber = "CLM" + System.currentTimeMillis();
        }
    }
}

