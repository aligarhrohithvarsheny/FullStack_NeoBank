package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "pg_orders")
public class PgOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String orderId;

    @Column(nullable = false)
    private String merchantId;

    private String customerEmail;
    private String customerPhone;
    private String customerName;

    @Column(nullable = false)
    private BigDecimal amount;

    private String currency = "INR";
    private String description;
    private String status = "CREATED";
    private String paymentMethod;
    private String receipt;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (orderId == null) {
            orderId = "ORD" + System.currentTimeMillis();
        }
        if (expiresAt == null) {
            expiresAt = LocalDateTime.now().plusMinutes(30);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
