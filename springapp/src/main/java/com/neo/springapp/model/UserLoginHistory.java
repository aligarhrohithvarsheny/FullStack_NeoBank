package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "user_login_history")
public class UserLoginHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime loginDate;

    @Column(nullable = false)
    private LocalDateTime loginTime;

    @Column(length = 500)
    private String loginLocation; // IP address, city, country, etc.

    @Column(length = 50)
    private String ipAddress;

    @Column(length = 100)
    private String deviceInfo; // Browser, device type, etc.

    @Column(length = 100)
    private String loginMethod; // PASSWORD, GRAPHICAL_PASSWORD, OTP

    @Column(nullable = false)
    private String status = "SUCCESS"; // SUCCESS, FAILED

    // Constructors
    public UserLoginHistory() {
        this.loginDate = LocalDateTime.now();
        this.loginTime = LocalDateTime.now();
    }

    public UserLoginHistory(User user, String loginLocation, String ipAddress, String deviceInfo, String loginMethod) {
        this();
        this.user = user;
        this.loginLocation = loginLocation;
        this.ipAddress = ipAddress;
        this.deviceInfo = deviceInfo;
        this.loginMethod = loginMethod;
        this.status = "SUCCESS";
    }
}


