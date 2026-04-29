package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "profile_update_requests")
public class ProfileUpdateRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // User information
    private Long userId;
    private String accountNumber;
    private String userName;
    private String userEmail;
    
    // Update fields
    private String fieldToUpdate; // ADDRESS, PHONE
    private String oldValue; // Current value before update
    private String newValue; // Requested new value
    
    // OTP verification
    private String otp;
    private LocalDateTime otpSentAt;
    private Boolean otpVerified = false;
    
    // Request details
    private LocalDateTime requestDate;
    private String status = "PENDING"; // PENDING, OTP_VERIFIED, APPROVED, REJECTED, COMPLETED
    
    // Approval details
    private LocalDateTime approvalDate;
    private String approvedBy; // Admin who approved/rejected
    private String rejectionReason; // Reason if rejected
    
    // Completion details
    private LocalDateTime completionDate;
    
    // Additional information
    private String remarks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (requestDate == null) {
            requestDate = LocalDateTime.now();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
