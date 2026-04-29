package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "pg_payment_sessions")
public class PgPaymentSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String sessionId;

    @Column(nullable = false)
    private String orderId;

    @Column(nullable = false)
    private String merchantId;

    @Column(nullable = false)
    private BigDecimal amount;

    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String payerName;
    private Boolean payerVerified = false;
    private String payerAccount;
    private String paymentMethod = "UPI";
    private String upiId;

    @Column(columnDefinition = "TEXT")
    private String qrData;

    private String status = "PENDING";
    private BigDecimal nameMatchScore = BigDecimal.ZERO;
    private LocalDateTime expiresAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (sessionId == null) {
            sessionId = "PGSESS" + System.currentTimeMillis();
        }
        if (expiresAt == null) {
            expiresAt = LocalDateTime.now().plusMinutes(15);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
