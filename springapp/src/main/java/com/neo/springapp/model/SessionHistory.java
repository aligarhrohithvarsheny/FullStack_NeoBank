package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "session_history")
public class SessionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userType; // "USER" or "ADMIN"

    @Column(nullable = false)
    private Long userId; // User ID or Admin ID

    @Column(length = 100)
    private String email;

    @Column(length = 100)
    private String accountNumber; // For users

    @Column(length = 100)
    private String username;

    @Column(nullable = false)
    private LocalDateTime loginTime;

    @Column
    private LocalDateTime logoutTime;

    @Column(length = 50)
    private String sessionDuration; // Format: HH:MM:SS

    @Column(length = 500)
    private String loginLocation;

    @Column(length = 50)
    private String ipAddress;

    @Column(columnDefinition = "TEXT")
    private String deviceInfo;

    @Column(length = 100)
    private String loginMethod; // PASSWORD, GRAPHICAL_PASSWORD, OTP, FINGERPRINT

    @Column(length = 50)
    private String browserName; // Chrome, Firefox, Safari, Edge, etc.

    @Column(length = 50)
    private String accountType; // SAVINGS, CURRENT, SALARY, ADMIN, MANAGER

    @Column(nullable = false)
    private String status = "ACTIVE"; // ACTIVE, LOGGED_OUT

    @Column
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (loginTime == null) {
            loginTime = LocalDateTime.now();
        }
    }
}

