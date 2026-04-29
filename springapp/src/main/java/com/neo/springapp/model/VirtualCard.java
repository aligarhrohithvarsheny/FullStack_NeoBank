package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "virtual_cards")
public class VirtualCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String accountNumber;

    @Column(nullable = false, unique = true)
    private String cardNumber;

    @Column(nullable = false)
    private String cardholderName;

    @Column(nullable = false)
    private String cvv;

    @Column(nullable = false)
    private LocalDate expiryDate;

    @Column(nullable = false)
    private String status; // ACTIVE, FROZEN, EXPIRED, CANCELLED

    private Double dailyLimit;

    private Double monthlyLimit;

    private Double totalSpent;

    private boolean onlinePaymentsEnabled;

    private boolean internationalPaymentsEnabled;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public VirtualCard() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.status = "ACTIVE";
        this.dailyLimit = 50000.0;
        this.monthlyLimit = 200000.0;
        this.totalSpent = 0.0;
        this.onlinePaymentsEnabled = true;
        this.internationalPaymentsEnabled = false;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
