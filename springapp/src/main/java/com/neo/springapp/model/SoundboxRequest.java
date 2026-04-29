package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "soundbox_requests")
public class SoundboxRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String requestId;

    @Column(nullable = false)
    private String accountNumber;

    @Column(nullable = false)
    private String businessName;

    @Column(nullable = false)
    private String ownerName;

    private String deliveryAddress;
    private String city;
    private String state;
    private String pincode;
    private String mobile;

    private String status = "PENDING"; // PENDING, APPROVED, REJECTED, DELIVERED

    private String adminRemarks;
    private String assignedDeviceId;
    private Double monthlyCharge = 100.0;
    private Double deviceCharge = 499.0;

    private String processedBy;
    private LocalDateTime processedAt;
    private LocalDateTime requestedAt;

    @PrePersist
    protected void onCreate() {
        if (requestedAt == null) requestedAt = LocalDateTime.now();
        if (requestId == null) {
            requestId = "SBREQ" + System.currentTimeMillis();
        }
    }
}
