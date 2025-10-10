package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String transactionId; // Custom transaction ID like TXN123456
    private String merchant;
    private Double amount;
    private String type; // Debit / Credit / Deposit / Withdraw / Transfer / Loan Credit
    private String description; // Detailed description of the transaction
    private Double balance;
    private LocalDateTime date;
    private String status = "Completed"; // Completed / Pending / Failed
    
    // User information
    private String userName;
    private String accountNumber;
    
    // Transfer specific fields
    private String recipientAccountNumber;
    private String recipientName;
    private String ifscCode;
    private String transferType; // NEFT / RTGS

    // Constructors
    public Transaction() {
        this.date = LocalDateTime.now();
        this.transactionId = "TXN" + System.currentTimeMillis();
    }

    public Transaction(String merchant, Double amount, String type, Double balance, String userName, String accountNumber) {
        this();
        this.merchant = merchant;
        this.amount = amount;
        this.type = type;
        this.balance = balance;
        this.userName = userName;
        this.accountNumber = accountNumber;
    }
}
