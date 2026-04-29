package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "current_account_business_loans")
public class CurrentAccountBusinessLoan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String applicationId;

    @Column(nullable = false)
    private String accountNumber;

    @Column(nullable = false)
    private String loanType; // Working Capital, Business Expansion

    @Column(nullable = false)
    private String panNumber;

    @Column(nullable = false)
    private Double requestedAmount;

    private Double approvedAmount;
    private Double interestRate;

    @Column(nullable = false)
    private Integer tenureMonths;

    private Double monthlyEmi;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String purpose;

    @Column(nullable = false)
    private Double annualRevenue;

    @Column(nullable = false)
    private Integer yearsInBusiness;

    private Integer cibilScore;
    private String cibilStatus; // EXCELLENT, GOOD, FAIR, POOR, VERY_POOR

    private String businessName;
    private String ownerName;

    private String status = "PENDING"; // PENDING, APPROVED, REJECTED, DISBURSED

    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    private LocalDateTime appliedAt;
    private LocalDateTime processedAt;
    private String processedBy;
    private LocalDateTime disbursedAt;

    @PrePersist
    protected void onCreate() {
        appliedAt = LocalDateTime.now();
        if (applicationId == null) {
            applicationId = "BL-" + System.currentTimeMillis();
        }
    }
}
