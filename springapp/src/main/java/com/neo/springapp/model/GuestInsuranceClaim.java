package com.neo.springapp.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "guest_insurance_claims")
public class GuestInsuranceClaim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String claimNumber;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "guest_application_id", nullable = false)
    private GuestInsuranceApplication application;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal claimAmount;

    @Column(nullable = false, length = 80)
    private String reason;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String details;

    @Column(nullable = false)
    private String status = "PENDING_REVIEW";

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime reviewedAt;

    @Column(columnDefinition = "TEXT")
    private String adminRemark;

    public Long getId() { return id; }
    public String getClaimNumber() { return claimNumber; }
    public void setClaimNumber(String claimNumber) { this.claimNumber = claimNumber; }
    public GuestInsuranceApplication getApplication() { return application; }
    public void setApplication(GuestInsuranceApplication application) { this.application = application; }
    public BigDecimal getClaimAmount() { return claimAmount; }
    public void setClaimAmount(BigDecimal claimAmount) { this.claimAmount = claimAmount; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    public String getAdminRemark() { return adminRemark; }
    public void setAdminRemark(String adminRemark) { this.adminRemark = adminRemark; }
}
