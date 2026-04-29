package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * AI Device Fingerprint - Tracks and identifies unique devices for anomaly detection.
 * Builds trust profiles based on device history and behavioral patterns.
 */
@Entity
@Data
@Table(name = "ai_device_fingerprints", indexes = {
    @Index(name = "idx_device_entity", columnList = "entityId, entityType"),
    @Index(name = "idx_device_hash", columnList = "deviceHash"),
    @Index(name = "idx_device_trusted", columnList = "isTrusted")
})
public class AiDeviceFingerprint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String entityId;

    @Column(nullable = false)
    private String entityType = "USER";

    @Column(nullable = false)
    private String deviceHash;

    private String deviceType;
    private String browser;
    private String os;
    private String screenResolution;
    private String timezone;
    private String language;
    private String ipAddress;
    private String geoLocation;

    private Boolean isTrusted = false;
    private Double trustScore = 50.0;
    private Integer loginCount = 0;

    private LocalDateTime lastSeenAt = LocalDateTime.now();
    private LocalDateTime firstSeenAt = LocalDateTime.now();
}
