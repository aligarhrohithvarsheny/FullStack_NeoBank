package com.neo.springapp.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * BusinessChequeBankRange - Validates cheque serial numbers against issued cheque books for business accounts
 */
@Entity
@Table(name = "business_cheque_book_ranges", indexes = {
    @Index(name = "idx_biz_current_account_id", columnList = "current_account_id"),
    @Index(name = "idx_biz_cheque_book_number", columnList = "cheque_book_number")
})
public class BusinessChequeBankRange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "current_account_id", nullable = false)
    private Long currentAccountId;

    @Column(name = "cheque_book_number", nullable = false, length = 50)
    private String chequeBookNumber;

    @Column(name = "serial_from", nullable = false, length = 50)
    private String serialFrom;

    @Column(name = "serial_to", nullable = false, length = 50)
    private String serialTo;

    @Column(name = "issued_date")
    private LocalDate issuedDate;

    @Column(name = "status", nullable = false, length = 50)
    private String status; // ACTIVE, EXHAUSTED, CANCELLED

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public BusinessChequeBankRange() {
    }

    public BusinessChequeBankRange(Long currentAccountId, String chequeBookNumber, String serialFrom, String serialTo) {
        this.currentAccountId = currentAccountId;
        this.chequeBookNumber = chequeBookNumber;
        this.serialFrom = serialFrom;
        this.serialTo = serialTo;
        this.status = "ACTIVE";
        this.issuedDate = LocalDate.now();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCurrentAccountId() { return currentAccountId; }
    public void setCurrentAccountId(Long currentAccountId) { this.currentAccountId = currentAccountId; }

    public String getChequeBookNumber() { return chequeBookNumber; }
    public void setChequeBookNumber(String chequeBookNumber) { this.chequeBookNumber = chequeBookNumber; }

    public String getSerialFrom() { return serialFrom; }
    public void setSerialFrom(String serialFrom) { this.serialFrom = serialFrom; }

    public String getSerialTo() { return serialTo; }
    public void setSerialTo(String serialTo) { this.serialTo = serialTo; }

    public LocalDate getIssuedDate() { return issuedDate; }
    public void setIssuedDate(LocalDate issuedDate) { this.issuedDate = issuedDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
