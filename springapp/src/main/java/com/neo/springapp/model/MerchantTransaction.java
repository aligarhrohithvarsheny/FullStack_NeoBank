package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "merchant_transactions")
public class MerchantTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String transactionId;

    @Column(nullable = false)
    private String merchantId;

    private String deviceId;

    @Column(nullable = false)
    private BigDecimal amount;

    private String paymentMode;
    private String payerName;
    private String payerUpi;

    private String status = "SUCCESS";

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (transactionId == null) {
            transactionId = "MTXN" + System.currentTimeMillis();
        }
    }
}
