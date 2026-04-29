package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "soundbox_transactions")
public class SoundboxTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String txnId;

    @Column(nullable = false)
    private String accountNumber;

    private String deviceId;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    private String txnType = "CREDIT"; // CREDIT, DEBIT

    @Column(nullable = false)
    private String paymentMethod = "UPI"; // UPI, QR, NFC

    private String payerName;
    private String payerUpi;

    private String status = "SUCCESS"; // SUCCESS, FAILED, PENDING

    private Boolean voicePlayed = false;
    private String voiceMessage;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (txnId == null) {
            txnId = "SBTXN" + System.currentTimeMillis();
        }
    }
}
