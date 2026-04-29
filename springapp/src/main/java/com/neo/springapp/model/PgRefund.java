package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "pg_refunds")
public class PgRefund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String refundId;

    @Column(nullable = false)
    private String transactionId;

    @Column(nullable = false)
    private String orderId;

    @Column(nullable = false)
    private String merchantId;

    @Column(nullable = false)
    private BigDecimal amount;

    private String reason;
    private String status = "INITIATED";
    private String refundType = "FULL";

    private LocalDateTime processedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (refundId == null) {
            refundId = "PGREF" + System.currentTimeMillis();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
