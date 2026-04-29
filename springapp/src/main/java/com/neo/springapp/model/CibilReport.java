package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "cibil_reports")
@Data
public class CibilReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String panNumber;

    @Column(nullable = false)
    private String name;

    private Double salary;

    private Integer cibilScore;

    private Double approvalLimit;

    @Column(length = 30)
    private String status; // APPROVED, REJECTED, NOT_ELIGIBLE, PENDING

    private String remarks;

    // Cross-reference account info (auto-populated from PAN lookup)
    private String savingsAccountNumber;
    private String salaryAccountNumber;
    private String currentAccountNumber;

    private Double savingsBalance;
    private Double salaryBalance;
    private Double currentBalance;

    // ML analysis results
    private Double riskScore;
    private String riskCategory; // LOW, MEDIUM, HIGH, VERY_HIGH
    private Double debtToIncomeRatio;
    private Double recommendedLimit;
    private String eligibilityReason;

    // Upload tracking
    @Column(nullable = false)
    private String uploadedBy;

    private String uploadBatchId;
    private String uploadFileName;

    @Column(length = 20)
    private String uploadType; // EXCEL, PDF, IMAGE

    @Column(columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) this.status = "PENDING";
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
