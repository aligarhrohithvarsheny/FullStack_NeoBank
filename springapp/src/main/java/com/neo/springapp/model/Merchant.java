package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "merchants")
public class Merchant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String merchantId;

    @Column(nullable = false)
    private String businessName;

    @Column(nullable = false)
    private String ownerName;

    @Column(nullable = false)
    private String mobile;

    private String email;

    @Column(nullable = false)
    private String businessType;

    private String gstNumber;

    @Column(nullable = false)
    private String shopAddress;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String state;

    @Column(nullable = false)
    private String pincode;

    @Column(nullable = false)
    private String bankName;

    @Column(nullable = false)
    private String accountNumber;

    @Column(nullable = false)
    private String ifscCode;

    @Column(nullable = false)
    private String accountHolderName;

    private String status = "PENDING";

    @Column(nullable = false)
    private String agentId;

    private String shopPhotoPath;
    private String ownerIdProofPath;
    private String bankProofPath;

    private String rejectionReason;

    private LocalDateTime activatedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
        if (merchantId == null) {
            merchantId = "MER" + System.currentTimeMillis();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
