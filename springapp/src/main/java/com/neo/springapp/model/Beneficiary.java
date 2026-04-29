package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "beneficiaries")
public class Beneficiary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String senderAccountNumber; // Account that added this beneficiary

    @Column(nullable = false)
    private String recipientAccountNumber;

    @Column(nullable = false)
    private String recipientName;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
    private String ifsc;

    private String nickname; // Optional nickname for the beneficiary

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean isVerified = false; // Whether account details have been verified

    // Constructors
    public Beneficiary() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Beneficiary(String senderAccountNumber, String recipientAccountNumber, 
                      String recipientName, String phone, String ifsc) {
        this();
        this.senderAccountNumber = senderAccountNumber;
        this.recipientAccountNumber = recipientAccountNumber;
        this.recipientName = recipientName;
        this.phone = phone;
        this.ifsc = ifsc;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

