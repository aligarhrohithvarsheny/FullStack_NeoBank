package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "pg_merchants")
public class PgMerchant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String merchantId;

    @Column(nullable = false)
    private String businessName;

    @Column(nullable = false)
    private String businessEmail;

    private String businessPhone;
    private String businessType = "ONLINE";

    @Column(unique = true, nullable = false)
    private String apiKey;

    @Column(nullable = false)
    private String secretKey;

    private String webhookUrl;
    private String callbackUrl;
    private String accountNumber;
    private String settlementAccount;

    private Boolean isActive = true;
    private Boolean isVerified = false;

    private BigDecimal dailyLimit = new BigDecimal("500000.00");
    private BigDecimal monthlyVolume = BigDecimal.ZERO;
    private BigDecimal totalVolume = BigDecimal.ZERO;

    private Integer riskScore = 0;

    // Login & Admin access fields
    private Boolean loginEnabled = true;
    private Boolean adminApproved = false;
    private String approvedBy;
    private LocalDateTime approvedAt;
    private String linkedAccountNumber;
    private Boolean linkedAccountVerified = false;
    private String linkedAccountHolderName;
    private String linkedAccountType = "CURRENT";
    private String registrationStatus = "PENDING";
    private String rejectionReason;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (merchantId == null) {
            merchantId = "PGMER" + System.currentTimeMillis();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
