package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Permanent record of email addresses a user has changed away from.
 *
 * When a user updates their login email, the previous value is archived here instead of
 * simply being overwritten. This stops a retired email from being picked up by a different
 * person when opening a new account, while still allowing the original owner to switch back
 * to it later (see UserService#isEmailUnique(String, Long)).
 */
@Entity
@Data
@Table(name = "user_email_history", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
public class UserEmailHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The user who previously owned this email address.
    @Column(nullable = false)
    private Long userId;

    // Normalized (trimmed + lowercased) retired email.
    @Column(nullable = false, unique = true)
    private String email;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
