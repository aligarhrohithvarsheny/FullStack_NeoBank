package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "current_account_vendor_payments")
public class CurrentAccountVendorPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String accountNumber;

    @Column(nullable = false)
    private String vendorName;

    @Column(nullable = false)
    private String vendorAccount;

    private String vendorIfsc;
    private Double amount;
    private String description;
    private String status = "PENDING";
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
