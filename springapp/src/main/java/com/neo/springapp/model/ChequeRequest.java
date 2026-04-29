package com.neo.springapp.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ChequeRequest Model - Salary Account Cheque Draw Requests
 * Represents a user's request to draw a cheque from their salary account
 */
@Entity
@Table(name = "cheque_requests", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"cheque_number", "salary_account_id"}),
    @UniqueConstraint(columnNames = {"serial_number", "salary_account_id"})
})
public class ChequeRequest {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "salary_account_id", nullable = false)
    private Long salaryAccountId;
    
    @Column(name = "cheque_number", nullable = false, length = 50)
    private String chequeNumber;
    
    @Column(name = "serial_number", nullable = false, length = 50)
    private String serialNumber;
    
    @Column(name = "request_date")
    private LocalDate requestDate;
    
    @Column(name = "cheque_date", nullable = false)
    private LocalDate chequeDate;
    
    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;
    
    @Column(name = "available_balance", precision = 12, scale = 2)
    private BigDecimal availableBalance;
    
    @Column(name = "payee_name", nullable = false, length = 255)
    private String payeeName;
    
    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;
    
    @Column(name = "status", nullable = false, length = 50)
    private String status; // PENDING, APPROVED, REJECTED, COMPLETED, CANCELLED
    
    @Column(name = "approved_by", length = 100)
    private String approvedBy;
    
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
    
    @Column(name = "rejected_by", length = 100)
    private String rejectedBy;
    
    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;
    
    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;
    
    @Column(name = "picked_up_at")
    private LocalDateTime pickedUpAt;
    
    @Column(name = "cleared_at")
    private LocalDateTime clearedAt;
    
    @Column(name = "cheque_downloaded")
    private Boolean chequeDownloaded = false;
    
    @Column(name = "cheque_downloaded_at")
    private LocalDateTime chequeDownloadedAt;
    
    @Column(name = "payee_account_number", length = 50)
    private String payeeAccountNumber;
    
    @Column(name = "payee_account_verified")
    private Boolean payeeAccountVerified = false;
    
    @Column(name = "payee_account_type", length = 30)
    private String payeeAccountType;
    
    @Column(name = "transaction_reference", length = 100)
    private String transactionReference;
    
    @Column(name = "debited_from_account", length = 50)
    private String debitedFromAccount;
    
    @Column(name = "credited_to_account", length = 50)
    private String creditedToAccount;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public ChequeRequest() {
    }
    
    public ChequeRequest(Long userId, Long salaryAccountId, String chequeNumber, String serialNumber,
                        LocalDate chequeDate, BigDecimal amount, String payeeName) {
        this.userId = userId;
        this.salaryAccountId = salaryAccountId;
        this.chequeNumber = chequeNumber;
        this.serialNumber = serialNumber;
        this.chequeDate = chequeDate;
        this.amount = amount;
        this.payeeName = payeeName;
        this.status = "PENDING";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public Long getSalaryAccountId() {
        return salaryAccountId;
    }
    
    public void setSalaryAccountId(Long salaryAccountId) {
        this.salaryAccountId = salaryAccountId;
    }
    
    public String getChequeNumber() {
        return chequeNumber;
    }
    
    public void setChequeNumber(String chequeNumber) {
        this.chequeNumber = chequeNumber;
    }
    
    public String getSerialNumber() {
        return serialNumber;
    }
    
    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }
    
    public LocalDate getRequestDate() {
        return requestDate;
    }
    
    public void setRequestDate(LocalDate requestDate) {
        this.requestDate = requestDate;
    }
    
    public LocalDate getChequeDate() {
        return chequeDate;
    }
    
    public void setChequeDate(LocalDate chequeDate) {
        this.chequeDate = chequeDate;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    public BigDecimal getAvailableBalance() {
        return availableBalance;
    }
    
    public void setAvailableBalance(BigDecimal availableBalance) {
        this.availableBalance = availableBalance;
    }
    
    public String getPayeeName() {
        return payeeName;
    }
    
    public void setPayeeName(String payeeName) {
        this.payeeName = payeeName;
    }
    
    public String getRemarks() {
        return remarks;
    }
    
    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getApprovedBy() {
        return approvedBy;
    }
    
    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }
    
    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }
    
    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }
    
    public String getRejectedBy() {
        return rejectedBy;
    }
    
    public void setRejectedBy(String rejectedBy) {
        this.rejectedBy = rejectedBy;
    }
    
    public String getRejectionReason() {
        return rejectionReason;
    }
    
    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
    
    public LocalDateTime getRejectedAt() {
        return rejectedAt;
    }
    
    public void setRejectedAt(LocalDateTime rejectedAt) {
        this.rejectedAt = rejectedAt;
    }
    
    public LocalDateTime getPickedUpAt() {
        return pickedUpAt;
    }
    
    public void setPickedUpAt(LocalDateTime pickedUpAt) {
        this.pickedUpAt = pickedUpAt;
    }
    
    public LocalDateTime getClearedAt() {
        return clearedAt;
    }
    
    public void setClearedAt(LocalDateTime clearedAt) {
        this.clearedAt = clearedAt;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public Boolean getChequeDownloaded() {
        return chequeDownloaded;
    }
    
    public void setChequeDownloaded(Boolean chequeDownloaded) {
        this.chequeDownloaded = chequeDownloaded;
    }
    
    public LocalDateTime getChequeDownloadedAt() {
        return chequeDownloadedAt;
    }
    
    public void setChequeDownloadedAt(LocalDateTime chequeDownloadedAt) {
        this.chequeDownloadedAt = chequeDownloadedAt;
    }
    
    public String getPayeeAccountNumber() {
        return payeeAccountNumber;
    }
    
    public void setPayeeAccountNumber(String payeeAccountNumber) {
        this.payeeAccountNumber = payeeAccountNumber;
    }
    
    public Boolean getPayeeAccountVerified() {
        return payeeAccountVerified;
    }
    
    public void setPayeeAccountVerified(Boolean payeeAccountVerified) {
        this.payeeAccountVerified = payeeAccountVerified;
    }
    
    public String getPayeeAccountType() {
        return payeeAccountType;
    }
    
    public void setPayeeAccountType(String payeeAccountType) {
        this.payeeAccountType = payeeAccountType;
    }
    
    public String getTransactionReference() {
        return transactionReference;
    }
    
    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }
    
    public String getDebitedFromAccount() {
        return debitedFromAccount;
    }
    
    public void setDebitedFromAccount(String debitedFromAccount) {
        this.debitedFromAccount = debitedFromAccount;
    }
    
    public String getCreditedToAccount() {
        return creditedToAccount;
    }
    
    public void setCreditedToAccount(String creditedToAccount) {
        this.creditedToAccount = creditedToAccount;
    }
}
