package com.neo.springapp.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "account_conversion_requests")
public class AccountConversionRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String accountNumber;

    @Column(nullable = false)
    private String accountHolderName;

    @Column(nullable = false)
    private String originalAccountType;

    @Column(nullable = false)
    private String targetAccountType;

    @Column(nullable = false)
    private String requestStatus = "PENDING";

    @Column(nullable = false)
    private String requestedBy = "Admin";

    private String approvedBy;
    private LocalDateTime approvedAt;
    private String reason;

    @Column(name = "application_number", unique = true)
    private String applicationNumber;

    @Column(columnDefinition = "LONGTEXT")
    private String applicationContent;

    @Column(name = "terms_path")
    private String termsAndConditionsPath;

    @Column(name = "signature_verified")
    private Boolean signatureVerified = false;

    @Column(name = "signature_verified_by")
    private String signatureVerifiedBy;

    @Column(name = "signature_verified_at")
    private LocalDateTime signatureVerifiedAt;

    @Column(name = "requested_at")
    private LocalDateTime requestedAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "audit_summary", columnDefinition = "LONGTEXT")
    private String auditSummary;

    @Column(name = "prior_account_type")
    private String priorAccountType;

    @Column(name = "reverted_to_original")
    private Boolean revertedToOriginal = false;

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getAccountHolderName() {
        return accountHolderName;
    }

    public void setAccountHolderName(String accountHolderName) {
        this.accountHolderName = accountHolderName;
    }

    public String getOriginalAccountType() {
        return originalAccountType;
    }

    public void setOriginalAccountType(String originalAccountType) {
        this.originalAccountType = originalAccountType;
    }

    public String getTargetAccountType() {
        return targetAccountType;
    }

    public void setTargetAccountType(String targetAccountType) {
        this.targetAccountType = targetAccountType;
    }

    public String getRequestStatus() {
        return requestStatus;
    }

    public void setRequestStatus(String requestStatus) {
        this.requestStatus = requestStatus;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(String requestedBy) {
        this.requestedBy = requestedBy;
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

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getApplicationNumber() {
        return applicationNumber;
    }

    public void setApplicationNumber(String applicationNumber) {
        this.applicationNumber = applicationNumber;
    }

    public String getApplicationContent() {
        return applicationContent;
    }

    public void setApplicationContent(String applicationContent) {
        this.applicationContent = applicationContent;
    }

    public String getTermsAndConditionsPath() {
        return termsAndConditionsPath;
    }

    public void setTermsAndConditionsPath(String termsAndConditionsPath) {
        this.termsAndConditionsPath = termsAndConditionsPath;
    }

    public Boolean getSignatureVerified() {
        return signatureVerified;
    }

    public void setSignatureVerified(Boolean signatureVerified) {
        this.signatureVerified = signatureVerified;
    }

    public String getSignatureVerifiedBy() {
        return signatureVerifiedBy;
    }

    public void setSignatureVerifiedBy(String signatureVerifiedBy) {
        this.signatureVerifiedBy = signatureVerifiedBy;
    }

    public LocalDateTime getSignatureVerifiedAt() {
        return signatureVerifiedAt;
    }

    public void setSignatureVerifiedAt(LocalDateTime signatureVerifiedAt) {
        this.signatureVerifiedAt = signatureVerifiedAt;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getAuditSummary() {
        return auditSummary;
    }

    public void setAuditSummary(String auditSummary) {
        this.auditSummary = auditSummary;
    }

    public String getPriorAccountType() {
        return priorAccountType;
    }

    public void setPriorAccountType(String priorAccountType) {
        this.priorAccountType = priorAccountType;
    }

    public Boolean getRevertedToOriginal() {
        return revertedToOriginal;
    }

    public void setRevertedToOriginal(Boolean revertedToOriginal) {
        this.revertedToOriginal = revertedToOriginal;
    }
}
