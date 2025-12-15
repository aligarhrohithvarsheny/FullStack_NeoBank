package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "fixed_deposits")
public class FixedDeposit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // User information
    private String accountNumber;
    private String userName;
    private String userEmail;
    
    // FD details
    private String fdAccountNumber; // Unique FD account number
    private Double principalAmount; // FD principal amount
    private Double interestRate; // Annual interest rate
    private Integer tenure; // Tenure in months
    private LocalDate startDate; // FD start date
    private LocalDate maturityDate; // FD maturity date
    
    // Interest calculation
    private String interestPayout; // Monthly, Quarterly, At Maturity
    private Double maturityAmount; // Total amount at maturity
    private Double interestAmount; // Total interest earned
    
    // Status and approval
    private String status = "PENDING"; // PENDING, APPROVED, REJECTED, ACTIVE, MATURED, CLOSED, PREMATURE_CLOSED
    private LocalDateTime applicationDate;
    private LocalDateTime approvalDate;
    private String approvedBy; // Admin who approved
    private String rejectionReason; // Reason if rejected
    
    // Premature closure
    private Boolean isPrematureClosure = false;
    private LocalDate closureDate; // Premature closure date
    private Double prematureClosureAmount; // Amount on premature closure
    private Double prematureClosurePenalty; // Penalty for premature closure
    private String closedBy; // Admin who processed closure
    
    // Maturity handling
    private Boolean isMatured = false;
    private LocalDateTime maturityProcessedDate;
    private String maturityProcessedBy;
    
    // Transaction details
    private String transactionId; // Reference to transaction
    private Double balanceBefore; // Account balance before FD creation
    private Double balanceAfter; // Account balance after FD creation
    
    // Additional information
    private String remarks; // Additional remarks
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Monthly interest credit tracking
    private Integer years; // FD tenure in years (for interest rate calculation)
    private LocalDate lastInterestCreditDate; // Last date when interest was credited
    private Integer monthsInterestCredited = 0; // Number of months interest has been credited
    private Double totalInterestCredited = 0.0; // Total interest credited so far
    
    // Withdrawal with cheque deposit
    private Boolean withdrawalRequested = false; // If user requested withdrawal
    private LocalDateTime withdrawalRequestDate; // Date when withdrawal was requested
    private String withdrawalRequestedBy; // User who requested withdrawal
    private String withdrawalChequeNumber; // Cheque number for withdrawal
    private Double withdrawalAmount; // Amount to be withdrawn
    private String withdrawalStatus = "NONE"; // NONE, PENDING, APPROVED, REJECTED, PROCESSED
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (applicationDate == null) {
            applicationDate = LocalDateTime.now();
        }
        // Generate FD account number
        if (fdAccountNumber == null) {
            this.fdAccountNumber = "FD" + System.currentTimeMillis() + (int)(Math.random() * 1000);
        }
        // Calculate years from tenure (months)
        if (tenure != null && years == null) {
            this.years = (int) Math.ceil(tenure / 12.0);
        }
        // Set interest rate based on years if not provided
        if (interestRate == null && years != null) {
            this.interestRate = calculateInterestRateByYears(years);
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        // Update years if tenure changed
        if (tenure != null) {
            this.years = (int) Math.ceil(tenure / 12.0);
        }
        // Update interest rate based on years if not explicitly set
        if (years != null && (interestRate == null || interestRate == 0.0)) {
            this.interestRate = calculateInterestRateByYears(years);
        }
    }
    
    /**
     * Calculate interest rate based on years (up to 8% for 5 years)
     * Formula: Base rate increases with years, max 8% at 5 years
     */
    private Double calculateInterestRateByYears(Integer years) {
        if (years == null || years <= 0) {
            return 4.0; // Default minimum rate
        }
        if (years >= 5) {
            return 8.0; // Maximum rate at 5 years
        }
        // Linear progression: 4% at 1 year, 8% at 5 years
        // Rate = 4 + (years - 1) * (8 - 4) / (5 - 1)
        // Rate = 4 + (years - 1) * 1
        double rate = 4.0 + (years - 1) * 1.0;
        return Math.min(rate, 8.0); // Cap at 8%
    }
}

