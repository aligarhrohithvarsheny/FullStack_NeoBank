package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * WebAuthn Credential Model for storing FIDO2/Fingerprint authentication credentials
 */
@Entity
@Data
@Table(name = "webauthn_credentials")
public class WebAuthnCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String adminEmail; // Email of the admin who registered this credential

    @Column(nullable = false, unique = true, length = 500)
    private String credentialId; // Base64 encoded credential ID

    @Column(nullable = false, length = 2000)
    private String publicKey; // Base64 encoded public key

    @Column(nullable = false)
    private Long counter; // Signature counter for replay attack prevention

    @Column(nullable = false)
    private String algorithm; // Algorithm used (e.g., "ES256", "RS256")

    @Column(length = 1000)
    private String deviceName; // User-friendly device name (e.g., "Windows Hello", "Touch ID")

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime lastUsedAt;

    @Column(nullable = false)
    private Boolean active = true; // Whether this credential is still active

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        lastUsedAt = LocalDateTime.now();
        if (active == null) {
            active = true;
        }
        if (counter == null) {
            counter = 0L;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        if (active == null) {
            active = true;
        }
    }
}

