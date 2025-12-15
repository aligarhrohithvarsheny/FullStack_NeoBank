package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "loans")
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String type;
    private Double amount;
    private Integer tenure; // in months
    private Double interestRate;
    private String purpose; // Home Renovation, Vehicle Purchase, etc.
    private String status = "Pending"; // default status
    
    // User information
    private String userName;
    private String userEmail;
    private String accountNumber;
    private Double currentBalance;
    private String pan; // PAN card number for CIBIL check
    
    // For Education Loans: Child account number (if child has neobank account)
    private String childAccountNumber; // Account number of child/student (for education loans)
    
    // For Personal Loans: Uploaded application form
    private String personalLoanFormPath; // Path to uploaded Personal Loan application form
    
    // CIBIL and credit information
    private Integer cibilScore; // CIBIL score (300-900)
    private Double creditLimit; // Available credit limit based on CIBIL
    
    // Loan account information
    private String loanAccountNumber;
    private LocalDateTime applicationDate;
    private LocalDateTime approvalDate;
    private LocalDate emiStartDate; // EMI payment start date (1 month after approval)
    private String approvedBy; // Admin who approved the loan
    
    // Foreclosure information
    private LocalDateTime foreclosureDate;
    private Double foreclosureAmount; // Total amount paid for foreclosure
    private Double foreclosureCharges; // 4% foreclosure charges
    private Double foreclosureGst; // GST on foreclosure charges
    private Double principalPaid; // Principal amount already paid
    private Double interestPaid; // Interest amount already paid
    private Double remainingPrincipal; // Remaining principal to be paid
    private Double remainingInterest; // Remaining interest to be paid
    private String foreclosedBy; // Admin who processed foreclosure

    // Constructors
    public Loan() {
        this.applicationDate = LocalDateTime.now();
    }

    public Loan(String type, Double amount, Integer tenure, Double interestRate) {
        this.type = type;
        this.amount = amount;
        this.tenure = tenure;
        this.interestRate = interestRate;
        this.status = "Pending";
        this.applicationDate = LocalDateTime.now();
    }

    public Loan(String type, Double amount, Integer tenure, Double interestRate, 
                String userName, String userEmail, String accountNumber, Double currentBalance) {
        this.type = type;
        this.amount = amount;
        this.tenure = tenure;
        this.interestRate = interestRate;
        this.userName = userName;
        this.userEmail = userEmail;
        this.accountNumber = accountNumber;
        this.currentBalance = currentBalance;
        this.status = "Pending";
        this.applicationDate = LocalDateTime.now();
    }

    // Generate unique loan account number
    public void generateLoanAccountNumber() {
        if (this.loanAccountNumber == null) {
            this.loanAccountNumber = "LOAN" + System.currentTimeMillis() + (int)(Math.random() * 1000);
        }
    }

    // Getters and Setters
    
}
