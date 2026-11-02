package com.neo.springapp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "deposit_management_history")
public class DepositManagementHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 12)
    private String productType;

    @Column(nullable = false)
    private Long productId;

    @Column(length = 64)
    private String accountNumber;

    @Column(nullable = false, length = 32)
    private String action;

    @Column(nullable = false, length = 128)
    private String performedBy;

    @Column(length = 512)
    private String reason;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(nullable = false)
    private LocalDateTime occurredAt;

    @PrePersist
    void onCreate() {
        if (occurredAt == null) occurredAt = LocalDateTime.now();
    }
}
