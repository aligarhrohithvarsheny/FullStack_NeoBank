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
    @Column(length = 32)
    private String loanType = "HOME"; // HOME, CAR, FARMER, CROP

    // Requested / approved terms
    private Double amount;
    private Integer tenure; // months
    private Double interestRate; // % p.a.
    @Column(columnDefinition = "TEXT")
    private String purpose;
    @Column(columnDefinition = "TEXT")
    private String propertyAddress;
    private Double propertyValue;

    // Vehicle loan details
    @Column(columnDefinition = "TEXT")
    private String vehicleMakeModel;
    @Column(length = 32)
    private String vehicleCondition;
    private Integer vehicleYear;
    @Column(length = 64)
    private String vehicleRegistrationNumber;
    @Column(columnDefinition = "TEXT")
    private String vehicleDealer;
    private Double vehiclePrice;
    @Column(columnDefinition = "TEXT")
    private String vehicleImagePaths;
    @Column(columnDefinition = "TEXT")
    private String vehicleInvoicePath;
    @Column(length = 32)
    private String vehicleInvoiceStatus;
    @Column(columnDefinition = "TEXT")
    private String vehicleInvoiceRemark;
    @Column(length = 32)
    private String vehicleImagesStatus;
    @Column(columnDefinition = "TEXT")
    private String vehicleImagesRemark;

    // Agricultural and crop loan details
    @Column(columnDefinition = "TEXT")
    private String farmLocation;
    @Column(columnDefinition = "TEXT")
    private String cropName;
    @Column(length = 64)
    private String cropSeason;
    private Double landArea;
    @Column(length = 32)
    private String landAreaUnit;
    private Double cultivationCostEstimate;
    @Column(columnDefinition = "TEXT")
    private String landRecordPath;
    @Column(length = 32)
    private String landRecordStatus;
    @Column(columnDefinition = "TEXT")
    private String landRecordRemark;
    @Column(columnDefinition = "TEXT")
    private String agricultureEstimatePath;
    @Column(length = 32)
    private String agricultureEstimateStatus;
    @Column(columnDefinition = "TEXT")
    private String agricultureEstimateRemark;
    @Column(columnDefinition = "TEXT")
    private String cropEstimatePath;
    @Column(length = 32)
    private String cropEstimateStatus;
    @Column(columnDefinition = "TEXT")
    private String cropEstimateRemark;

    // Workflow: Submitted, Under Review, Documents Required, Documents Submitted,
    // Documents Verified, Approved, Rejected, Closed
    private String status = "Submitted";
    private LocalDateTime applicationDate;
    private LocalDateTime lastUpdated;
    private String reviewedBy;
    @Column(columnDefinition = "TEXT")
    private String adminNotes;
    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    // Documents (FD receipt, property/home loan model, signature)
    @Column(columnDefinition = "TEXT")
    private String fdReceiptPath;
    @Column(columnDefinition = "TEXT")
    private String modelDocPath;
    @Column(columnDefinition = "TEXT")
    private String signaturePath;
    private String fdReceiptStatus;   // Pending, Verified, Reupload Required
    private String modelDocStatus;
    private String signatureStatus;
    @Column(columnDefinition = "TEXT")
    private String fdReceiptRemark;
    @Column(columnDefinition = "TEXT")
    private String modelDocRemark;
    @Column(columnDefinition = "TEXT")
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

    @PostLoad
    private void restoreLoanTypeFromApplicationId() {
        if (loanType != null && !"HOME".equalsIgnoreCase(loanType)) return;
        if (applicationId == null) return;
        if (applicationId.startsWith("CL")) loanType = "CAR";
        else if (applicationId.startsWith("FL")) loanType = "FARMER";
        else if (applicationId.startsWith("CR")) loanType = "CROP";
    }
}
