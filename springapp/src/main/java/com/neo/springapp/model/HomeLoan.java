package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "home_loans")
public class HomeLoan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String applicationId;
    private String loanAccountNumber;

    // Applicant
    private String accountNumber;
    private String userName;
    private String userEmail;
    private String pan;

    // Requested / approved terms
    private Double amount;
    private Integer tenure; // months
    private Double interestRate; // % p.a.
    private String purpose;
    private String propertyAddress;
    private Double propertyValue;

    // Workflow: Submitted, Under Review, Documents Required, Documents Submitted,
    // Documents Verified, Approved, Rejected, Closed
    private String status = "Submitted";
    private LocalDateTime applicationDate;
    private LocalDateTime lastUpdated;
    private String reviewedBy;
    @Column(length = 1000)
    private String adminNotes;
    private String rejectionReason;

    // Documents (FD receipt, property/home loan model, signature)
    private String fdReceiptPath;
    private String modelDocPath;
    private String signaturePath;
    private String fdReceiptStatus;   // Pending, Verified, Reupload Required
    private String modelDocStatus;
    private String signatureStatus;
    @Column(length = 1000)
    private String fdReceiptRemark;
    @Column(length = 1000)
    private String modelDocRemark;
    @Column(length = 1000)
    private String signatureRemark;

    // Approval / charges
    private LocalDateTime approvalDate;
    private String approvedBy;
    private Double processingFee;
    private Double gstOnFee;
    private Double legalCharges;
    private Double totalCharges;
    private Double netDisbursed;

    // Repayment tracking
    private Double emi;
    private Integer remainingTenure;
    private Integer paidEmis = 0;
    private Double principalPaid = 0.0;
    private Double interestPaid = 0.0;
    private Double prepaidAmount = 0.0;
    private Double remainingPrincipal;
    private LocalDate nextEmiDate;
    private LocalDateTime closureDate;
    private Double closureAmount;

    private Double topupTotal = 0.0;
    private String nocNumber;
    private LocalDateTime nocDate;

    // ML analysis snapshot
    private Double approvalProbability;
    private String riskBand;

    public HomeLoan() {
        this.applicationDate = LocalDateTime.now();
        this.lastUpdated = LocalDateTime.now();
    }
}
