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

    private Long globalTransactionSequence; // Global unique transaction sequence across entire bank
    @Column(unique = true, length = 40)
    private String transactionId;
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

    // For branch/credit transactions: account from which amount was debited (source)
    private String sourceAccountNumber;

    @Column(length = 80)
    private String category;

    @Column(length = 120)
    private String chargeType;

    @Column(length = 80)
    private String loanAccountNumber;

    @Column(length = 80)
    private String loanType;

    @Column(length = 30)
    private String paymentMode;

    private Integer emiNumber;
    private Double principalAdjusted;
    private Double interestPaid;
    private Double chargesPaid;
    private Double gstPaid;
    private Double loanOutstandingAfter;

    @Column(length = 40)
    private String adjustmentType;

    @Column(name = "branch_city", length = 120)
    private String branchCity;

    @Column(length = 80)
    private String referenceNumber;

    // Constructors
    public Transaction() {
        this.date = LocalDateTime.now();
        this.transactionId = "TXN" + java.util.UUID.randomUUID().toString().replace("-", "").toUpperCase();
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
