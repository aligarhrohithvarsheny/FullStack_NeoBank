package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "salary_transactions")
public class SalaryTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Global transaction ID unique across entire banking system
    @Column(name = "global_transaction_sequence")
    private Long globalTransactionSequence;

    @Column(name = "salary_account_id")
    private Long salaryAccountId;

    @Column(name = "account_number")
    private String accountNumber;

    @Column(name = "salary_amount")
    private Double salaryAmount;

    @Column(name = "credit_date")
    private LocalDateTime creditDate;

    @Column(name = "company_name")
    private String companyName;

    private String description;

    private String type = "Credit";

    @Column(name = "previous_balance")
    private Double previousBalance = 0.0;

    @Column(name = "new_balance")
    private Double newBalance = 0.0;

    private String status = "Success";

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public SalaryTransaction() {
        this.createdAt = LocalDateTime.now();
    }
}
