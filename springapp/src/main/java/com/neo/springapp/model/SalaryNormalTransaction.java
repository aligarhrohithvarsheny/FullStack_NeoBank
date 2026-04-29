package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "salary_normal_transactions")
public class SalaryNormalTransaction {

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

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private Double amount;

    private Double charge = 0.0;

    @Column(name = "recipient_account")
    private String recipientAccount;

    @Column(name = "recipient_ifsc")
    private String recipientIfsc;

    private String remark;

    @Column(name = "previous_balance")
    private Double previousBalance = 0.0;

    @Column(name = "new_balance")
    private Double newBalance = 0.0;

    private String status = "Success";

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public SalaryNormalTransaction() {
        this.createdAt = LocalDateTime.now();
    }
}
