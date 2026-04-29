package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "pg_transactions")
public class PgTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String transactionId;

    @Column(nullable = false)
    private String orderId;

    @Column(nullable = false)
    private String merchantId;

    private String payerAccount;
    private String payerName;

    @Column(nullable = false)
    private BigDecimal amount;

    private BigDecimal fee = BigDecimal.ZERO;
    private BigDecimal tax = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal netAmount;

    private String currency = "INR";

    @Column(nullable = false)
    private String paymentMethod;

    private String status = "INITIATED";
    private String signature;
    private Boolean signatureVerified = false;

    private String errorCode;
    private String errorDescription;

    private Integer riskScore = 0;
    private Boolean fraudFlagged = false;

    private String ipAddress;
    private String deviceInfo;

    private Boolean settled = false;
    private LocalDateTime settledAt;

    private String refundStatus;
    private BigDecimal refundedAmount = BigDecimal.ZERO;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (transactionId == null) {
            transactionId = "PGTXN" + System.currentTimeMillis();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
