package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "fasttags")
public class Fasttag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Allow storing string ids (guest) as well as numeric user ids
    private String userId;
    private String userName;

    private String vehicleDetails;
    private String vehicleNumber;
    private String aadharNumber;
    private String panNumber;
    private String dob;
    private String vehicleType;
    private Double amount;
    private String bank;

    // New purchase fields
    private String chassisNumber;
    private String engineNumber;
    private String make;
    private String model;
    private String fuelType;
    private String rcFrontPath;
    private String rcBackPath;
    private String mobileNumber;
    private String email;
    @Column(length = 1000)
    private String dispatchAddress;
    private String pinCode;
    private String city;
    private String state;
    private Double tagIssuanceFee;
    private Double tagSecurityDeposit;
    private Double tagUploadAmount;
    private Double tagTotalAmount;
    private String debitAccountNumber;
    private Boolean termsAccepted = false;

    private String status = "Applied"; // Applied, Approved, Rejected
    @Column(unique = true)
    private String fasttagNumber;
    
    // Generated barcode number (12-16 digits)
    private String barcodeNumber;

    // Issue date for the FASTag
    private java.time.LocalDateTime issueDate;

    // Path to generated sticker PDF/PNG
    private String stickerPath;

    private Double balance = 0.0;

    private LocalDateTime createdAt = LocalDateTime.now();

    // Assigned account for admin-initiated debits (one account per FASTag)
    private String assignedAccountId;
    private LocalDateTime assignedAt;
}
