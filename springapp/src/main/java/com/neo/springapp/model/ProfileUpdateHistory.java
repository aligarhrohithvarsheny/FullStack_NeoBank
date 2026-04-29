package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "profile_update_history")
public class ProfileUpdateHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // User information
    private Long userId;
    private String accountNumber;
    private String userName;
    private String userEmail;
    
    // Update details
    private String fieldUpdated; // ADDRESS, PHONE
    private String oldValue; // Value before update
    private String newValue; // Value after update
    
    // Request reference
    private Long requestId; // Reference to ProfileUpdateRequest
    
    // Approval details
    private String approvedBy; // Admin who approved
    private LocalDateTime updateDate; // When the update was completed
    
    // Additional information
    private String remarks;
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (updateDate == null) {
            updateDate = LocalDateTime.now();
        }
    }
}
