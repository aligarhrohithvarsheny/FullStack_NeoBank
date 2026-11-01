package com.neo.springapp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "loan_agreements")
public class LoanAgreement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 24)
    private String loanType;

    @Column(nullable = false)
    private Long loanId;

    @Column(length = 128)
    private String loanAccountNumber;

    @Column(length = 64)
    private String productName;

    @Column(length = 128)
    private String customerName;

    @Column(length = 254)
    private String customerEmail;

    @Column(length = 64)
    private String accountNumber;

    @Column(length = 512)
    private String purpose;

    @Column(columnDefinition = "TEXT")
    private String productDetails;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    private Integer tenureMonths;

    @Column(nullable = false)
    private Double annualInterestRate;

    @Column(nullable = false)
    private Double stampDuty;

    @Column(nullable = false, length = 32)
    private String status = "PENDING_SIGNATURE";

    @Column(nullable = false, unique = true, length = 64)
    private String shareTokenHash;

    @Column(length = 36)
    private String captchaChallengeId;

    @Column(length = 64)
    private String captchaAnswerHash;

    private LocalDateTime captchaExpiresAt;

    private Integer captchaAttempts = 0;

    @Column(length = 120)
    private String signedName;

    private LocalDateTime signedAt;

    @Column(length = 64)
    private String signedDocumentSha256;

    @Column(length = 120)
    private String approvedBy;

    private LocalDateTime approvedAt;

    private LocalDateTime issuedAt;

    private LocalDateTime expiresAt;

    @Column(columnDefinition = "TEXT")
    private String approvalContext;

    @Lob
    @JsonIgnore
    private byte[] pdfBytes;

    @PrePersist
    void initializeDates() {
        if (issuedAt == null) issuedAt = LocalDateTime.now();
        if (expiresAt == null) expiresAt = issuedAt.plusDays(7);
        if (captchaAttempts == null) captchaAttempts = 0;
    }
}
