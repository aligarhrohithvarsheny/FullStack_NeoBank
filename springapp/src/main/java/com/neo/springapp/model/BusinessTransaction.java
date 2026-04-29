package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "business_transactions")
public class BusinessTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Global transaction ID unique across entire banking system
    @Column(name = "global_transaction_sequence")
    private Long globalTransactionSequence;

    @Column(unique = true, nullable = false)
    private String txnId;

    @Column(nullable = false)
    private String accountNumber;

    @Column(nullable = false)
    private String txnType; // Credit, Debit, Transfer, Vendor Payment, Bulk Payment

    @Column(nullable = false)
    private Double amount;

    private String description;
    private Double balance;
    private String status = "Completed"; // Completed, Pending, Failed

    // Transfer details
    private String recipientAccount;
    private String recipientName;
    private String transferType; // NEFT, RTGS

    // Charges
    private Double chargeAmount = 0.0;

    private LocalDateTime date;

    @PrePersist
    protected void onCreate() {
        if (date == null) {
            date = LocalDateTime.now();
        }
        if (txnId == null) {
            txnId = "BTXN" + System.currentTimeMillis() + (int) (Math.random() * 1000);
        }
    }
}
