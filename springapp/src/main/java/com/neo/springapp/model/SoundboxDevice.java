package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "soundbox_devices")
public class SoundboxDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String deviceId;

    @Column(nullable = false)
    private String accountNumber;

    private String businessName;
    private String ownerName;

    private String status = "INACTIVE"; // ACTIVE, INACTIVE, MAINTENANCE

    private Boolean voiceEnabled = true;
    private String voiceLanguage = "en-IN"; // en-IN, hi-IN, te-IN
    private String volumeMode = "NORMAL"; // NORMAL, LOUD, SILENT

    private String linkedUpi;

    private Double monthlyCharge = 100.0;
    private Double deviceCharge = 499.0;
    private String chargeStatus = "PENDING"; // PENDING, PAID, OVERDUE

    private LocalDateTime lastActiveAt;
    private LocalDateTime activatedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
