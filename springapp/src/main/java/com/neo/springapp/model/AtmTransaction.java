package com.neo.springapp.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "atm_transactions")
public class AtmTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_ref", unique = true, nullable = false)
    private String transactionRef;

    @Column(name = "atm_id", nullable = false)
    private String atmId;

    @Column(name = "account_number", nullable = false)
    private String accountNumber;

    @Column(name = "card_number")
    private String cardNumber;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "user_email")
    private String userEmail;

    @Column(name = "transaction_type", nullable = false)
    private String transactionType;

    @Column(nullable = false)
    private Double amount;

    @Column(name = "balance_before")
    private Double balanceBefore;

    @Column(name = "balance_after")
    private Double balanceAfter;

    @Column(name = "atm_balance_before")
    private Double atmBalanceBefore;

    @Column(name = "atm_balance_after")
    private Double atmBalanceAfter;

    @Column(length = 30)
    private String status = "SUCCESS";

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "receipt_number")
    private String receiptNumber;

    @Column(name = "receipt_generated")
    private Boolean receiptGenerated = true;

    @Column(name = "notes_dispensed_500")
    private Integer notesDispensed500 = 0;

    @Column(name = "notes_dispensed_200")
    private Integer notesDispensed200 = 0;

    @Column(name = "notes_dispensed_100")
    private Integer notesDispensed100 = 0;

    @Column(name = "notes_dispensed_2000")
    private Integer notesDispensed2000 = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTransactionRef() { return transactionRef; }
    public void setTransactionRef(String transactionRef) { this.transactionRef = transactionRef; }

    public String getAtmId() { return atmId; }
    public void setAtmId(String atmId) { this.atmId = atmId; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public Double getBalanceBefore() { return balanceBefore; }
    public void setBalanceBefore(Double balanceBefore) { this.balanceBefore = balanceBefore; }

    public Double getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(Double balanceAfter) { this.balanceAfter = balanceAfter; }

    public Double getAtmBalanceBefore() { return atmBalanceBefore; }
    public void setAtmBalanceBefore(Double atmBalanceBefore) { this.atmBalanceBefore = atmBalanceBefore; }

    public Double getAtmBalanceAfter() { return atmBalanceAfter; }
    public void setAtmBalanceAfter(Double atmBalanceAfter) { this.atmBalanceAfter = atmBalanceAfter; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public String getReceiptNumber() { return receiptNumber; }
    public void setReceiptNumber(String receiptNumber) { this.receiptNumber = receiptNumber; }

    public Boolean getReceiptGenerated() { return receiptGenerated; }
    public void setReceiptGenerated(Boolean receiptGenerated) { this.receiptGenerated = receiptGenerated; }

    public Integer getNotesDispensed500() { return notesDispensed500; }
    public void setNotesDispensed500(Integer notesDispensed500) { this.notesDispensed500 = notesDispensed500; }

    public Integer getNotesDispensed200() { return notesDispensed200; }
    public void setNotesDispensed200(Integer notesDispensed200) { this.notesDispensed200 = notesDispensed200; }

    public Integer getNotesDispensed100() { return notesDispensed100; }
    public void setNotesDispensed100(Integer notesDispensed100) { this.notesDispensed100 = notesDispensed100; }

    public Integer getNotesDispensed2000() { return notesDispensed2000; }
    public void setNotesDispensed2000(Integer notesDispensed2000) { this.notesDispensed2000 = notesDispensed2000; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
