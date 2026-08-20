package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "fastag_login_history")
public class FastagLoginHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "gmail_id", nullable = false, length = 255)
    private String gmailId;

    @Column(nullable = false)
    private LocalDateTime loginTime = LocalDateTime.now();

    @Column(length = 50)
    private String status;

    @Column(length = 100)
    private String loginMethod;

    @Column(length = 1000)
    private String failureReason;

    @Column(length = 100)
    private String ipAddress;

    @Column(columnDefinition = "TEXT")
    private String deviceInfo;
}
