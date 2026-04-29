package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "fastag_users")
public class FastagUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "gmail_id", nullable = false, unique = true)
    private String gmailId;

    private String otp;

    @Column(name = "otp_expiry")
    private LocalDateTime otpExpiry;

    @Column(name = "otp_attempts")
    private Integer otpAttempts = 0;

    @Column(name = "is_verified")
    private Boolean isVerified = false;

    @Column(name = "session_token", length = 500)
    private String sessionToken;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;
}
