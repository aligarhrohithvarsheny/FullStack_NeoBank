package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "current_accounts")
public class CurrentAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String accountNumber;

    @Column(unique = true)
    private String customerId;

    // Business Details
    @Column(nullable = false)
    private String businessName;

    @Column(nullable = false)
    private String businessType; // Proprietor, Partnership, Pvt Ltd, Startup

    private String businessRegistrationNumber;
    private String gstNumber;

    // Owner Details
    @Column(nullable = false)
    private String ownerName;

    @Column(nullable = false)
    private String mobile;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String aadharNumber;

    @Column(nullable = false)
    private String panNumber;

    // Business Address
    private String shopAddress;
    private String city;
    private String state;
    private String pincode;

    // Bank Details
    private String branchName = "NeoBank Main Branch";
    private String ifscCode = "EZYV000123";

    // Account Details
    private Double balance = 0.0;
    private Double overdraftLimit = 0.0;
    private Boolean overdraftEnabled = false;
    private Double minimumBalance = 10000.0;

    // Status
    private String status = "PENDING"; // PENDING, APPROVED, ACTIVE, FROZEN, CLOSED

    // KYC
    private Boolean kycVerified = false;
    private LocalDateTime kycVerifiedDate;
    private String kycVerifiedBy;

    // KYC Document Paths
    private String gstCertificatePath;
    private String businessRegistrationCertificatePath;
    private String panCardPath;
    private String addressProofPath;

    // Signature fields
    private String signatureCopyPath;
    private LocalDateTime signatureUploadedAt;
    private Boolean signatureVerified = false;
    private String signatureVerifiedBy;
    private LocalDateTime signatureVerifiedAt;

    // Freeze
    private Boolean accountFrozen = false;
    private String frozenReason;
    private String frozenBy;
    private LocalDateTime frozenDate;

    // Authentication
    private String password;
    private Boolean passwordSet = false;

    // Net Banking control per customer
    private Boolean netBankingEnabled = true;
    private String netBankingToggledBy;
    private LocalDateTime netBankingToggledAt;

    // UPI
    private String upiId;
    private Boolean upiEnabled = true;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime approvedAt;
    private String approvedBy;
    private LocalDateTime lastUpdated;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        lastUpdated = LocalDateTime.now();
        if (accountNumber == null) {
            accountNumber = generateAccountNumber();
        }
        if (customerId == null) {
            customerId = generateCustomerId();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
    }

    private String generateAccountNumber() {
        return "60" + String.format("%08d", (long) (Math.random() * 100000000));
    }

    private String generateCustomerId() {
        return "CUST" + String.format("%05d", (int) (Math.random() * 100000));
    }
}
