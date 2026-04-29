package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * AI Security Rule - Configurable rules engine for the AI security system.
 * Admin can create, enable/disable, and tune security rules across all channels.
 */
@Entity
@Data
@Table(name = "ai_security_rules", indexes = {
    @Index(name = "idx_rule_category", columnList = "ruleCategory"),
    @Index(name = "idx_rule_channel", columnList = "channel"),
    @Index(name = "idx_rule_active", columnList = "isActive")
})
public class AiSecurityRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String ruleName;

    @Column(nullable = false)
    private String ruleCategory; // TRANSACTION, AUTHENTICATION, BEHAVIORAL, SESSION, DATA_ACCESS, ACCOUNT, CONTENT

    @Column(nullable = false)
    private String channel = "ALL"; // WEB, MOBILE, API, ATM, ALL

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String conditionJson;

    @Column(nullable = false)
    private String actionType = "ALERT"; // ALERT, BLOCK, MFA, RATE_LIMIT, NOTIFY

    @Column(nullable = false)
    private String severity = "MEDIUM";

    private Boolean isActive = true;
    private Integer priority = 5;
    private Long hitCount = 0L;
    private LocalDateTime lastTriggeredAt;

    private String createdBy;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
}
