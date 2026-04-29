package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "fasttag_transactions")
public class FasttagTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Global transaction ID unique across entire banking system
    private Long globalTransactionSequence;

    private Long fasttagId;
    private String fasttagNumber;

    private Double amount;
    private String type; // RECHARGE, REFUND, DEBIT
    private String initiatedBy; // USER or ADMIN
    private String initiatedById; // admin id or user id

    private Double previousBalance;
    private Double newBalance;

    private String accountDebited; // account number debited when admin recharges

    private LocalDateTime createdAt = LocalDateTime.now();
}
