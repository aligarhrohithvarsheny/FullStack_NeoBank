package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "agents")
public class Agent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String agentId;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(unique = true, nullable = false)
    private String mobile;

    @Column(nullable = false)
    private String password;

    private String role = "FIELD_AGENT";

    @Column(columnDefinition = "TEXT")
    private String bio;

    private String profilePhotoPath;
    private String idCardPath;

    private String otp;
    private LocalDateTime otpExpiry;

    private String status = "ACTIVE";

    private String region;

    private LocalDateTime frozenAt;
    private LocalDateTime deactivatedAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
        if (agentId == null) {
            agentId = "AGT" + System.currentTimeMillis();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
