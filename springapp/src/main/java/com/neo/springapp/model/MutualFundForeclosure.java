package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "mutual_fund_foreclosures")
public class MutualFundForeclosure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Investment reference
    private Long investmentId;
    private String accountNumber;
    private String userName;
    
    // Investment details (snapshot at time of foreclosure request)
    private String fundName;
    private String fundCategory;
    private Double investmentAmount;
    private Double currentValue;
    private Double units;
    
    // Foreclosure details
    private Double foreclosureAmount; // Amount to be credited to user (currentValue - fine)
    private Double fine; // Fine amount (Rs 500-10000 based on investment amount)
    private String status = "PENDING"; // PENDING, APPROVED, REJECTED, COMPLETED
    
    // Request details
    private LocalDateTime requestDate;
    private String requestReason; // Optional reason from user
    private Boolean otpVerified = false; // True if user verified OTP sent to registered email
    
    // Approval details
    private LocalDateTime approvalDate;
    private String approvedBy; // Admin who approved/rejected
    private String rejectionReason; // Reason if rejected
    
    // Completion details
    private LocalDateTime completionDate;
    private String transactionId; // Reference to transaction
    private Double balanceBefore; // Account balance before credit
    private Double balanceAfter; // Account balance after credit
    
    // Additional information
    private String remarks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (requestDate == null) {
            requestDate = LocalDateTime.now();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
