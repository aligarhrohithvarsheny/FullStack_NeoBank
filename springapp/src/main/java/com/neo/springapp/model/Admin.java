package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "admins")
public class Admin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;
    @JsonIgnore // Don't serialize password in JSON responses
    private String password;
    private String role;
    private String pan;
    
    // Additional Profile Fields
    private String employeeId; // Employee ID
    private String address; // Address
    private String aadharNumber; // Aadhar number
    private String mobileNumber; // Mobile number
    private String qualifications; // Admin qualifications (e.g., "MBA, B.Tech")
    
    // Profile completion status
    private Boolean profileComplete = false; // Whether admin has completed their profile
    
    // Account lock fields (similar to User model)
    private Integer failedLoginAttempts = 0; // Use Integer to handle null values for existing admins
    private Boolean accountLocked = false; // Use Boolean to handle null values
    private LocalDateTime lastFailedLoginTime;
    
    // Admin ID card fields
    private String idCardNumber;                // Unique NeoBank ID card number
    private LocalDateTime idCardGeneratedAt;    // Last generated time
    private Integer idCardGeneratedCount;       // How many times card was generated
    private LocalDateTime idCardLastUpdatedAt;  // Last time ID card meta was updated
    private String idCardLastUpdatedBy;         // Manager name/email who last updated
    private String idCardDesignation;           // Designation shown on ID card
    private String idCardDepartment;            // Department shown on ID card
    private LocalDateTime idCardValidTill;      // Validity end date for ID card

    // Admin profile photo + DOJ (used on printable PVC ID card)
    private String profilePhotoPath;            // Relative file path to stored profile photo
    private LocalDateTime dateOfJoining;        // Date of joining (defaults to createdAt date)

    // Manager signature for PVC card (captured during ID card generation)
    @Lob
    private String idCardManagerSignatureDataUrl; // data:image/... base64
    private LocalDateTime idCardManagerSignedAt;
    private String idCardManagerSignedBy;

    // Branch / NeoBank A/C where all user charges and interest are deposited (map from admin profile)
    private String branchAccountNumber;
    private String branchAccountName;
    private String branchAccountIfsc;

    // Salary payout account (admin employee NeoBank account number)
    private String salaryAccountNumber;

    // One-time salary account linking flag (once linked, cannot be changed)
    private Boolean salaryAccountLinked = false;
    
    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime lastUpdated;

    // Lombok @Data annotation provides all getters and setters automatically
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        lastUpdated = LocalDateTime.now();
        // Initialize Boolean fields if null
        if (profileComplete == null) {
            profileComplete = false;
        }
        if (accountLocked == null) {
            accountLocked = false;
        }
        if (failedLoginAttempts == null) {
            failedLoginAttempts = 0;
        }
        if (idCardGeneratedCount == null) {
            idCardGeneratedCount = 0;
        }
        if (dateOfJoining == null) {
            dateOfJoining = createdAt.toLocalDate().atStartOfDay();
        }
        if (salaryAccountLinked == null) {
            salaryAccountLinked = false;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
        // Ensure Boolean fields are never null
        if (profileComplete == null) {
            profileComplete = false;
        }
        if (accountLocked == null) {
            accountLocked = false;
        }
        if (failedLoginAttempts == null) {
            failedLoginAttempts = 0;
        }
        if (idCardGeneratedCount == null) {
            idCardGeneratedCount = 0;
        }
        if (dateOfJoining == null && createdAt != null) {
            dateOfJoining = createdAt.toLocalDate().atStartOfDay();
        }
        if (salaryAccountLinked == null) {
            salaryAccountLinked = false;
        }
    }
}

