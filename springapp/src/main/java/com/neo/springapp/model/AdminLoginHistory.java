package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "admin_login_history")
public class AdminLoginHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "admin_id", nullable = false)
    private Admin admin;

    @Column(nullable = false)
    private LocalDateTime loginDate;

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
    private String loginMethod; // PASSWORD, FINGERPRINT

    @Column(nullable = false)
    private String status = "SUCCESS"; // SUCCESS, FAILED

    // Constructors
    public AdminLoginHistory() {
        this.loginDate = LocalDateTime.now();
        this.loginTime = LocalDateTime.now();
    }

    public AdminLoginHistory(Admin admin, String loginLocation, String ipAddress, String deviceInfo, String loginMethod) {
        this();
        this.admin = admin;
        this.loginLocation = loginLocation != null ? loginLocation : "Unknown";
        this.ipAddress = ipAddress != null ? ipAddress : "Unknown";
        this.deviceInfo = deviceInfo != null ? deviceInfo : "Unknown";
        this.loginMethod = loginMethod != null ? loginMethod : "PASSWORD";
        this.status = "SUCCESS";
    }
}

