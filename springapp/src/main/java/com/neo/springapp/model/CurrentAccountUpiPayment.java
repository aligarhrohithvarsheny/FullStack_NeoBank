package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "current_account_upi_payments")
public class CurrentAccountUpiPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String txnId;

    @Column(nullable = false)
    private String accountNumber;

    private String businessName;

    @Column(nullable = false)
    private String upiId;

    @Column(nullable = false)
    private Double amount;

    private String payerName;
    private String payerUpi;
    private String paymentMethod = "UPI";
    private String status = "SUCCESS";
    private String txnType = "CREDIT";
    private String note;
    private Boolean qrGenerated = false;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
        if (txnId == null) txnId = "UPI" + System.currentTimeMillis() + (int)(Math.random() * 1000);
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
