package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "education_loan_subsidy_claims")
public class EducationLoanSubsidyClaim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Loan information
    private Long loanId;
    private String loanAccountNumber;
    private String loanType; // Should be "Education Loan"
    
    // User information
    private String userId;
    private String userName;
    private String userEmail;
    private String accountNumber;
    
    // Subsidy calculation
    private Double loanAmount; // Original loan amount
    private Double interestRate; // Annual interest rate
    private Integer loanTenure; // Total loan tenure in months
    private Double calculatedSubsidyAmount; // Auto-calculated: 3 years of interest
    private Double approvedSubsidyAmount; // Admin can edit this amount
    
    // Claim details
    private String status = "Pending"; // Pending, Approved, Rejected, Credited
    private LocalDateTime requestDate;
    private LocalDateTime processedDate;
    private String processedBy; // Admin who processed
    private String rejectionReason; // If rejected
    
    // Credit information
    private LocalDateTime creditedDate;
    private String creditedBy; // Admin who credited
    private String transactionId; // Transaction ID when credited
    
    // Additional notes
    @Column(length = 1000)
    private String adminNotes;
    
    @Column(length = 1000)
    private String userNotes; // User's reason/notes for claim
    
    // Child/Student information
    private String childAadharNumber; // Aadhar number of the child/student for whom the loan is taken

    public EducationLoanSubsidyClaim() {
        this.requestDate = LocalDateTime.now();
    }
}




