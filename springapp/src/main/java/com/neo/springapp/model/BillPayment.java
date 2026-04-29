package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "bill_payments")
public class BillPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Global transaction ID unique across entire banking system
    private Long globalTransactionSequence;

    // User Information
    private String accountNumber;
    private String userName;
    private String userEmail;

    // Bill Information
    private String billType; // Mobile Bill, WiFi Bill
    private String networkProvider; // Airtel, Jio, BSNL, etc.
    private String customerNumber; // Mobile number or account number
    private Double amount;
    private String status = "Completed"; // Completed, Failed, Pending

    // Credit Card Information
    private String cardNumber; // Masked card number for display
    private String cardLastFour; // Last 4 digits
    private Long creditCardId;

    // Payment Details
    private LocalDateTime paymentDate;
    private String transactionId;
    private String description;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (paymentDate == null) {
            paymentDate = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
