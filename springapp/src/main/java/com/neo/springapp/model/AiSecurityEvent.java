package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * AI Security Event - Central entity for all AI-detected security events across all banking channels.
 * Tracks threats, anomalies, and suspicious activities with AI confidence scoring.
 */
@Entity
@Data
@Table(name = "ai_security_events", indexes = {
    @Index(name = "idx_ai_event_type", columnList = "eventType"),
    @Index(name = "idx_ai_event_channel", columnList = "channel"),
    @Index(name = "idx_ai_event_severity", columnList = "severity"),
    @Index(name = "idx_ai_event_status", columnList = "status"),
    @Index(name = "idx_ai_event_created", columnList = "createdAt"),
    @Index(name = "idx_ai_event_source", columnList = "sourceEntityId, sourceEntityType"),
    @Index(name = "idx_ai_event_risk", columnList = "riskScore")
})
public class AiSecurityEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public enum EventType {
        SUSPICIOUS_LOGIN,
        BRUTE_FORCE_ATTACK,
        IMPOSSIBLE_TRAVEL,
        ANOMALOUS_TRANSACTION,
        RAPID_FIRE_TRANSACTIONS,
        SESSION_HIJACK,
        NEW_DEVICE_DETECTED,
        MASS_DATA_ACCESS,
        DORMANT_ACCOUNT_ACTIVITY,
        PHISHING_ATTEMPT,
        OFF_HOURS_ACTIVITY,
        PRIVILEGE_ESCALATION,
        API_ABUSE,
        CREDENTIAL_STUFFING,
        ACCOUNT_TAKEOVER
    }

    public enum Channel {
        WEB,
        MOBILE,
        API,
        ATM,
        NET_BANKING,
        UPI,
        ADMIN_PORTAL,
        ALL
    }

    public enum Severity {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    public enum Status {
        DETECTED,
        ANALYZING,
        BLOCKED,
        ESCALATED,
        RESOLVED,
        FALSE_POSITIVE
    }

    public enum ActionTaken {
        NONE,
        ALERT_SENT,
        SESSION_TERMINATED,
        ACCOUNT_LOCKED,
        TRANSACTION_BLOCKED,
        MFA_TRIGGERED,
        IP_BLOCKED,
        RATE_LIMITED
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Channel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity severity = Severity.MEDIUM;

    private Double riskScore = 0.0;

    private String sourceEntityId;
    private String sourceEntityType;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String detailsJson;

    private String clientIp;
    private String location;
    private String deviceFingerprint;

    @Column(columnDefinition = "TEXT")
    private String userAgent;

    private String sessionId;
    private String aiModelVersion = "v1.0";
    private Double aiConfidence = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.DETECTED;

    @Enumerated(EnumType.STRING)
    private ActionTaken actionTaken = ActionTaken.NONE;

    private String resolvedBy;
    private LocalDateTime resolvedAt;

    @Column(columnDefinition = "TEXT")
    private String resolutionNotes;

    private LocalDateTime createdAt = LocalDateTime.now();
}
