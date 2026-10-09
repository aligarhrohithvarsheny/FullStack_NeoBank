package com.neo.springapp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "guest_insurance_applications")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class GuestInsuranceApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String applicationNumber;

    @Column(nullable = false)
    private String applicantName;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String phone;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "policy_id", nullable = false)
    private InsurancePolicy policy;

    @Column(nullable = false)
    private String status = "PENDING_APPROVAL";

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime reviewedAt;

    @Column(columnDefinition = "TEXT")
    private String adminRemark;

    @JsonIgnore
    private String passwordHash;

    @JsonIgnore
    private int failedAttempts;

    @JsonIgnore
    private LocalDateTime lockedUntil;

    private LocalDate nextPremiumDueDate;

    private LocalDate policyStartDate;

    private Double premiumAmountOverride;

    private String premiumTypeOverride;

    @Transient
    private List<Map<String, Object>> paymentReceipts;

    public Long getId() { return id; }
    public String getApplicationNumber() { return applicationNumber; }
    public void setApplicationNumber(String applicationNumber) { this.applicationNumber = applicationNumber; }
    public String getApplicantName() { return applicantName; }
    public void setApplicantName(String applicantName) { this.applicantName = applicantName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public InsurancePolicy getPolicy() { return policy; }
    public void setPolicy(InsurancePolicy policy) { this.policy = policy; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    public String getAdminRemark() { return adminRemark; }
    public void setAdminRemark(String adminRemark) { this.adminRemark = adminRemark; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public int getFailedAttempts() { return failedAttempts; }
    public void setFailedAttempts(int failedAttempts) { this.failedAttempts = failedAttempts; }
    public LocalDateTime getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(LocalDateTime lockedUntil) { this.lockedUntil = lockedUntil; }
    public LocalDate getNextPremiumDueDate() { return nextPremiumDueDate; }
    public void setNextPremiumDueDate(LocalDate nextPremiumDueDate) { this.nextPremiumDueDate = nextPremiumDueDate; }
    public LocalDate getPolicyStartDate() { return policyStartDate; }
    public void setPolicyStartDate(LocalDate policyStartDate) { this.policyStartDate = policyStartDate; }
    public Double getPremiumAmountOverride() { return premiumAmountOverride; }
    public void setPremiumAmountOverride(Double premiumAmountOverride) { this.premiumAmountOverride = premiumAmountOverride; }
    public String getPremiumTypeOverride() { return premiumTypeOverride; }
    public void setPremiumTypeOverride(String premiumTypeOverride) { this.premiumTypeOverride = premiumTypeOverride; }
    public List<Map<String, Object>> getPaymentReceipts() { return paymentReceipts; }
    public void setPaymentReceipts(List<Map<String, Object>> paymentReceipts) { this.paymentReceipts = paymentReceipts; }
}
