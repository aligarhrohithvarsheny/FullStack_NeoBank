package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "salary_upi_transactions")
public class SalaryUpiTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Global transaction ID unique across entire banking system
    @Column(name = "global_transaction_sequence")
    private Long globalTransactionSequence;

    @Column(name = "salary_account_id", nullable = false)
    private Long salaryAccountId;

    @Column(name = "account_number", nullable = false)
    private String accountNumber;

    @Column(name = "upi_id")
    private String upiId;

    @Column(name = "recipient_upi")
    private String recipientUpi;

    @Column(name = "recipient_name")
    private String recipientName;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private Double amount;

    private String remark;

    private String status = "Success";

    @Column(name = "transaction_ref")
    private String transactionRef;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
