package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "account_tracking")
public class AccountTracking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String trackingId;

    @Column(nullable = false)
    private String aadharNumber;

    @Column(nullable = false)
    private String mobileNumber;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String status = "PENDING"; // PENDING, ADMIN_SEEN, ADMIN_APPROVED, ADMIN_SENT

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime statusChangedAt;
    private String updatedBy; // Admin who updated the status

    // Constructors
    public AccountTracking() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.statusChangedAt = LocalDateTime.now();
    }

    public AccountTracking(String trackingId, String aadharNumber, String mobileNumber, User user) {
        this();
        this.trackingId = trackingId;
        this.aadharNumber = aadharNumber;
        this.mobileNumber = mobileNumber;
        this.user = user;
    }
}

