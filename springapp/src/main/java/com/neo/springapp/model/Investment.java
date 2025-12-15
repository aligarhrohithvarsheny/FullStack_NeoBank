package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "investments")
public class Investment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // User information
    private String accountNumber;
    private String userName;
    private String userEmail;
    
    // Investment details
    private String investmentType = "Mutual Fund"; // Mutual Fund, SIP, etc.
    private String fundName; // Name of the mutual fund
    private String fundCategory; // Equity, Debt, Hybrid, etc.
    private String fundScheme; // Growth, Dividend, etc.
    
    // Investment amount
    private Double investmentAmount; // Initial investment amount
    private Double currentValue; // Current market value
    private Double units; // Number of units purchased
    
    // Investment dates
    private LocalDate investmentDate; // Date when investment was made
    private LocalDate maturityDate; // Maturity date (if applicable)
    
    // Status and approval
    private String status = "PENDING"; // PENDING, APPROVED, REJECTED, ACTIVE, MATURED, CLOSED
    private LocalDateTime applicationDate;
    private LocalDateTime approvalDate;
    private String approvedBy; // Admin who approved
    private String rejectionReason; // Reason if rejected
    
    // SIP details (if applicable)
    private Boolean isSIP = false; // Is this a SIP investment
    private Double sipAmount; // Monthly SIP amount
    private Integer sipDuration; // SIP duration in months
    private LocalDate sipStartDate;
    private LocalDate sipEndDate;
    
    // Returns and performance
    private Double returns; // Returns percentage
    private Double profitLoss; // Profit/Loss amount
    
    // Transaction details
    private String transactionId; // Reference to transaction
    private Double balanceBefore; // Account balance before investment
    private Double balanceAfter; // Account balance after investment
    
    // Additional information
    private String remarks; // Additional remarks
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (applicationDate == null) {
            applicationDate = LocalDateTime.now();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

