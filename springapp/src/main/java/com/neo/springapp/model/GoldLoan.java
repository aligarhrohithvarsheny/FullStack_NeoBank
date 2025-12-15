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
    
    // Calculate loan amount based on verified gold value (75% of verified value)
    public void calculateLoanAmountFromVerified() {
        if (this.verifiedGoldValue != null) {
            this.loanAmount = this.verifiedGoldValue * 0.75; // 75% of verified gold value
        } else if (this.verifiedGoldGrams != null && this.goldRatePerGram != null) {
            // Calculate from verified grams if verified value not set
            this.verifiedGoldValue = this.verifiedGoldGrams * this.goldRatePerGram;
            this.loanAmount = this.verifiedGoldValue * 0.75; // 75% of verified gold value
        }
    }
    
    // Calculate EMI automatically based on loan amount, interest rate, and tenure
    public Double calculateEMI() {
        if (this.loanAmount == null || this.loanAmount <= 0 || 
            this.interestRate == null || this.tenure == null || this.tenure <= 0) {
            return 0.0;
        }
        
        Double principal = this.loanAmount;
        Double annualRate = this.interestRate;
        Integer tenureMonths = this.tenure;
        Double monthlyRate = annualRate / (12 * 100);
        
        if (monthlyRate > 0) {
            Double emi = (principal * monthlyRate * Math.pow(1 + monthlyRate, tenureMonths)) / 
                         (Math.pow(1 + monthlyRate, tenureMonths) - 1);
            return Math.round(emi * 100.0) / 100.0; // Round to 2 decimal places
        } else {
            return Math.round((principal / tenureMonths) * 100.0) / 100.0;
        }
    }
}

