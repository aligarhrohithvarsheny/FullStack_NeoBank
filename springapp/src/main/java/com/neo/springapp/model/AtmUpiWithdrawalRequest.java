package com.neo.springapp.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "atm_upi_withdrawal_requests")
public class AtmUpiWithdrawalRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_token", unique = true, nullable = false, length = 36)
    private String requestToken;

    @Column(name = "atm_id", nullable = false)
    private String atmId;

    @Column(name = "account_number", nullable = false)
    private String accountNumber;

    @Column(name = "account_type", nullable = false)
    private String accountType;

    @Column(nullable = false)
    private Double amount;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "user_email")
    private String userEmail;

    @Column(nullable = false, length = 30)
    private String status = "PENDING";

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "atm_transaction_ref")
    private String atmTransactionRef;

    @Column(name = "upi_transaction_ref")
    private String upiTransactionRef;

    @Column(name = "pin_attempts", nullable = false)
    private Integer pinAttempts = 0;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getRequestToken() { return requestToken; }
    public void setRequestToken(String requestToken) { this.requestToken = requestToken; }
    public String getAtmId() { return atmId; }
    public void setAtmId(String atmId) { this.atmId = atmId; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public String getAtmTransactionRef() { return atmTransactionRef; }
    public void setAtmTransactionRef(String atmTransactionRef) { this.atmTransactionRef = atmTransactionRef; }
    public String getUpiTransactionRef() { return upiTransactionRef; }
    public void setUpiTransactionRef(String upiTransactionRef) { this.upiTransactionRef = upiTransactionRef; }
    public Integer getPinAttempts() { return pinAttempts; }
    public void setPinAttempts(Integer pinAttempts) { this.pinAttempts = pinAttempts; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}
