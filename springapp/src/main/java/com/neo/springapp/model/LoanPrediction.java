package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "loan_predictions")
public class LoanPrediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // User information
    private String accountNumber;
    private String userName;
    private String userEmail;
    private String pan; // PAN card number used for prediction

    // Loan details
    private String loanType; // Personal Loan, Education Loan, Home Loan, Car Loan
    private Double requestedAmount;
    private Integer tenure; // in months
    private Double interestRate;

    // CIBIL information
    private Integer cibilScore;
    private String scoreCategory; // Excellent, Good, Fair, Average, Poor
    private Double creditLimit;

    // ML Prediction results
    private String predictionResult; // Approved, Rejected, Pending Review
    private Double approvalProbability; // 0.0 to 1.0 (percentage)
    private String rejectionReason; // Reason if rejected
    private String mlModelVersion; // Version of ML model used

    // Additional factors considered
    private Double currentBalance;
    private Double monthlyIncome;
    private String occupation;
    private Integer age;

    // Timestamp
    private LocalDateTime predictionDate;
    private LocalDateTime createdAt;

    // Constructor
    public LoanPrediction() {
        this.createdAt = LocalDateTime.now();
        this.predictionDate = LocalDateTime.now();
    }
}

