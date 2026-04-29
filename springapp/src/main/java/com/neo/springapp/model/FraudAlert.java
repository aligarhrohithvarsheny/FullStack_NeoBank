package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * AI Fraud Detection - Central entity for all fraud alerts (login, transaction, biometric, phishing, KYC).
 * Manager dashboard reviews and acts on these alerts.
 */
@Entity
@Data
@Table(name = "fraud_alerts", indexes = {
    @Index(name = "idx_fraud_alert_status", columnList = "status"),
    @Index(name = "idx_fraud_alert_type", columnList = "alertType"),
    @Index(name = "idx_fraud_alert_created", columnList = "createdAt"),
    @Index(name = "idx_fraud_alert_source_entity", columnList = "sourceEntityId, sourceType")
})
public class FraudAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public enum AlertType {
        LOGIN_FRAUD,           // 3 failed password attempts (user/admin)
        TRANSACTION_ANOMALY,   // High-value, new recipient, new location
        BEHAVIORAL_BIOMETRIC,  // Typing/mouse deviation - possible account takeover
        PHISHING_SUSPECT,      // Suspicious message/email content
        KYC_DOCUMENT_FRAUD     // Document alteration, forgery, inconsistency
    }

    public enum SourceType {
        USER,
        ADMIN,
        MANAGER
    }

    public enum Status {
        PENDING_REVIEW,
        REVIEWED,
        DISMISSED,
        CONFIRMED_FRAUD
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertType alertType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SourceType sourceType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING_REVIEW;

    /** User/Admin email or account number involved */
    private String sourceEntityId;
    private String sourceEntityName;

    /** Human-readable title */
    private String title;
    /** Detailed description for manager */
    @Column(columnDefinition = "TEXT")
    private String description;
    /** JSON or key-value details (e.g. amount, recipient, IP) */
    @Column(columnDefinition = "TEXT")
    private String detailsJson;

    private String clientIp;
    private String location;
    private String deviceInfo;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime reviewedAt;
    private String reviewedBy;  // Manager email/id
    @Column(columnDefinition = "TEXT")
    private String reviewNotes;

    /** Severity: LOW, MEDIUM, HIGH, CRITICAL */
    private String severity = "MEDIUM";

    public FraudAlert() {}
}
