package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

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
    
    // Loan account information
    private String loanAccountNumber;
    private LocalDateTime applicationDate;
    private LocalDateTime approvalDate;
    private String approvedBy; // Admin who approved the loan

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
