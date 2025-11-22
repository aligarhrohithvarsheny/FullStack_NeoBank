package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "gold_loans")
public class GoldLoan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Gold loan specific fields
    @Column(nullable = false)
    private Double goldGrams; // Weight of gold in grams

    @Column(nullable = false)
    private Double goldRatePerGram; // Gold rate at the time of application

    @Column(nullable = false)
    private Double goldValue; // Total value of gold (grams * rate)

    @Column(nullable = false)
    private Double loanAmount; // 75% of gold value

    // Loan details
    private Integer tenure; // in months
    private Double interestRate;
    private String purpose = "Gold Loan";
    private String status = "Pending"; // Pending, Approved, Rejected, Foreclosed, Paid

    // User information
    private String userName;
    private String userEmail;
    private String accountNumber;
    private Double currentBalance;

    // Loan account information
    private String loanAccountNumber;
    private LocalDateTime applicationDate;
    private LocalDateTime approvalDate;
    private LocalDate emiStartDate;
    private String approvedBy;

    // Foreclosure information
    private LocalDateTime foreclosureDate;
    private Double foreclosureAmount;
    private Double foreclosureCharges;
    private Double foreclosureGst;
    private Double principalPaid;
    private Double interestPaid;
    private Double remainingPrincipal;
    private Double remainingInterest;
    private String foreclosedBy;

    // Gold details (filled by admin during acceptance)
    @Column(length = 2000)
    private String goldItems; // Description of gold items (e.g., "2 bangles, 1 chain, 3 rings")
    
    @Column(length = 1000)
    private String goldDescription; // Detailed description of gold items
    
    private String goldPurity; // Verified purity (e.g., "22K", "18K")
    
    private Double verifiedGoldGrams; // Verified weight in grams (may differ from application)
    
    private Double verifiedGoldValue; // Verified gold value based on actual verification
    
    private String verificationNotes; // Notes from admin during verification
    
    private String storageLocation; // Where gold is stored
    
    // Terms and conditions acceptance
    private Boolean termsAccepted = false; // Whether user has accepted terms after admin approval
    
    private LocalDateTime termsAcceptedDate; // When user accepted the terms
    
    private String termsAcceptedBy; // User who accepted (account number or email)

    // Constructors
    public GoldLoan() {
        this.applicationDate = LocalDateTime.now();
    }

    // Generate unique loan account number
    public void generateLoanAccountNumber() {
        if (this.loanAccountNumber == null) {
            this.loanAccountNumber = "GL" + System.currentTimeMillis() + (int)(Math.random() * 1000);
        }
    }

    // Calculate loan amount (75% of gold value)
    public void calculateLoanAmount() {
        if (this.goldGrams != null && this.goldRatePerGram != null) {
            this.goldValue = this.goldGrams * this.goldRatePerGram;
            this.loanAmount = this.goldValue * 0.75; // 75% of gold value
        }
    }
}

