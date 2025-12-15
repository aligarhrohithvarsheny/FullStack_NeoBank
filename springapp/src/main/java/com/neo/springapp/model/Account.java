package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String dob;
    private int age;
    private String occupation;
    private String accountType = "Savings"; // Savings, Current, etc.
    private LocalDateTime createdAt;
    private LocalDateTime lastUpdated;
    private String status = "ACTIVE"; // ACTIVE, INACTIVE, CLOSED

    @Column(unique = true, nullable = false)
    private String aadharNumber;

    @Column(unique = true, nullable = false)
    private String pan;

    @Column(unique = true)
    private String accountNumber;

    private Double balance = 0.0; // Account balance
    private Double income;
    
    @Column(unique = true, nullable = false)
    private String phone;
    
    private String address;

    private boolean verifiedMatrix = false;
    private boolean kycVerified = false;
    
    // Aadhaar verification fields
    private boolean aadharVerified = false;
    private LocalDateTime aadharVerifiedDate;
    private String aadharVerificationReference; // Reference ID from Aadhaar verification
    private String aadharVerificationStatus = "PENDING"; // PENDING, VERIFIED, FAILED, EXPIRED

    // Child account fields
    private boolean isChildAccount = false;
    @Column(name = "parent_account_id")
    private Long parentAccountId; // Reference to parent account
    private String studies; // Child's studies/education details
    private String form60; // Form 60 details for child account
    private String panChildForm; // PAN child form details

    // Constructors
    public Account() {
        this.createdAt = LocalDateTime.now();
        this.lastUpdated = LocalDateTime.now();
    }

    public Account(String name, String dob, int age, String occupation, String aadharNumber, 
                   String pan, String accountNumber, Double income, String phone, String address) {
        this();
        this.name = name;
        this.dob = dob;
        this.age = age;
        this.occupation = occupation;
        this.aadharNumber = aadharNumber;
        this.pan = pan;
        this.accountNumber = accountNumber;
        this.income = income;
        this.phone = phone;
        this.address = address;
    }
}
