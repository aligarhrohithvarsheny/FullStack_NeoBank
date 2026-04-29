package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "credit_card_transactions")
public class CreditCardTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Global transaction ID unique across entire banking system
    private Long globalTransactionSequence;

    private Long creditCardId;
    private String cardNumber;
    private String accountNumber;
    private String userName;
    
    private String transactionType; // Purchase, Payment, Refund, Interest, Fee
    private Double amount;
    private String merchant;
    private String description;
    private LocalDateTime transactionDate;
    
    private String status; // Completed, Pending, Failed
    private Double balanceAfter; // Balance after transaction
    
    public CreditCardTransaction() {
        this.transactionDate = LocalDateTime.now();
        this.status = "Completed";
    }
}
