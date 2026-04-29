package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * AI Threat Score - Real-time multi-dimensional risk scoring for users and sessions.
 * Aggregates risk factors across login, transaction, behavioral, device, and network dimensions.
 */
@Entity
@Data
@Table(name = "ai_threat_scores", indexes = {
    @Index(name = "idx_threat_entity", columnList = "entityId, entityType"),
    @Index(name = "idx_threat_risk_level", columnList = "riskLevel"),
    @Index(name = "idx_threat_score", columnList = "overallRiskScore"),
    @Index(name = "idx_threat_watchlist", columnList = "isWatchlisted")
})
public class AiThreatScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String entityId;

    @Column(nullable = false)
    private String entityType; // USER, ADMIN, SESSION, IP

    private Double overallRiskScore = 0.0;
    private Double loginRiskScore = 0.0;
    private Double transactionRiskScore = 0.0;
    private Double behavioralRiskScore = 0.0;
    private Double deviceRiskScore = 0.0;
    private Double networkRiskScore = 0.0;

    public enum RiskLevel {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RiskLevel riskLevel = RiskLevel.LOW;

    @Column(columnDefinition = "TEXT")
    private String riskFactors;

    private String lastActivity;
    private Integer totalEvents = 0;
    private Integer falsePositives = 0;
    private Integer confirmedThreats = 0;

    private Boolean isWatchlisted = false;
    private String watchlistReason;

    private LocalDateTime lastEvaluatedAt = LocalDateTime.now();
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
}
