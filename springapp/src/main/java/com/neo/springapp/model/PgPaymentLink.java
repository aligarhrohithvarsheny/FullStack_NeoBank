package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "pg_payment_links")
public class PgPaymentLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String linkId;

    @Column(unique = true, nullable = false)
    private String linkToken;

    @Column(nullable = false)
    private String merchantId;

    private String merchantName;
    private String merchantUpiId;

    @Column(nullable = false)
    private String recipientUpiId;

    private String recipientName;

    @Column(nullable = false)
    private BigDecimal amount;

    private String description;
    private String status = "PENDING"; // PENDING, PAID, EXPIRED, CANCELLED

    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime paidAt;

    private String payerAccountNumber;
    private String payerName;
    private String txnRef;
    private String orderId; // linked PG order (if created from pay-with-upi flow)

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (linkId == null) linkId = "LNK" + System.currentTimeMillis();
        if (expiresAt == null) expiresAt = LocalDateTime.now().plusHours(24);
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
