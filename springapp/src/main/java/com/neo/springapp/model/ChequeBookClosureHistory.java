package com.neo.springapp.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cheque_book_closure_history", indexes = {
        @Index(name = "idx_closure_account_number", columnList = "account_number"),
        @Index(name = "idx_closure_account_type", columnList = "account_type"),
        @Index(name = "idx_closure_closed_at", columnList = "closed_at")
})
public class ChequeBookClosureHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", nullable = false, length = 50)
    private String accountNumber;

    @Column(name = "account_type", nullable = false, length = 30)
    private String accountType;

    @Column(name = "cheque_book_number", length = 50)
    private String chequeBookNumber;

    @Column(name = "book_type", nullable = false, length = 30)
    private String bookType;

    @Column(name = "serial_from", length = 50)
    private String serialFrom;

    @Column(name = "serial_to", length = 50)
    private String serialTo;

    @Column(name = "closed_by", nullable = false, length = 100)
    private String closedBy;

    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "closed_count")
    private Integer closedCount;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "CLOSED";

    @Column(name = "closed_at", nullable = false)
    private LocalDateTime closedAt;

    public ChequeBookClosureHistory() {
        this.closedAt = LocalDateTime.now();
    }

    public ChequeBookClosureHistory(String accountNumber, String accountType, String chequeBookNumber,
                                   String bookType, String serialFrom, String serialTo,
                                   String closedBy, String reason, Integer closedCount) {
        this();
        this.accountNumber = accountNumber;
        this.accountType = accountType;
        this.chequeBookNumber = chequeBookNumber;
        this.bookType = bookType;
        this.serialFrom = serialFrom;
        this.serialTo = serialTo;
        this.closedBy = closedBy;
        this.reason = reason;
        this.closedCount = closedCount;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }

    public String getChequeBookNumber() { return chequeBookNumber; }
    public void setChequeBookNumber(String chequeBookNumber) { this.chequeBookNumber = chequeBookNumber; }

    public String getBookType() { return bookType; }
    public void setBookType(String bookType) { this.bookType = bookType; }

    public String getSerialFrom() { return serialFrom; }
    public void setSerialFrom(String serialFrom) { this.serialFrom = serialFrom; }

    public String getSerialTo() { return serialTo; }
    public void setSerialTo(String serialTo) { this.serialTo = serialTo; }

    public String getClosedBy() { return closedBy; }
    public void setClosedBy(String closedBy) { this.closedBy = closedBy; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Integer getClosedCount() { return closedCount; }
    public void setClosedCount(Integer closedCount) { this.closedCount = closedCount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }
}
