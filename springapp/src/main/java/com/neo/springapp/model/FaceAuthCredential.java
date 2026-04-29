package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Face Authentication Credential Model
 * Stores face descriptors (128-dim float arrays) for camera-based Face ID
 */
@Entity
@Data
@Table(name = "face_auth_credentials")
public class FaceAuthCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String adminEmail;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String faceDescriptor; // JSON array of 128 floats

    @Column(length = 255)
    private String deviceName;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime lastUsedAt;

    @Column(nullable = false)
    private Boolean active = true;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        lastUsedAt = LocalDateTime.now();
        if (active == null) {
            active = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        if (active == null) {
            active = true;
        }
    }
}
