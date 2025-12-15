package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "admins")
public class Admin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;
    @JsonIgnore // Don't serialize password in JSON responses
    private String password;
    private String role;
    private String pan;
    
    // Additional Profile Fields
    private String employeeId; // Employee ID
    private String address; // Address
    private String aadharNumber; // Aadhar number
    private String mobileNumber; // Mobile number
    private String qualifications; // Admin qualifications (e.g., "MBA, B.Tech")
    
    // Profile completion status
    private Boolean profileComplete = false; // Whether admin has completed their profile
    
    // Account lock fields (similar to User model)
    private Integer failedLoginAttempts = 0; // Use Integer to handle null values for existing admins
    private Boolean accountLocked = false; // Use Boolean to handle null values
    private LocalDateTime lastFailedLoginTime;
    
    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime lastUpdated;

    // Lombok @Data annotation provides all getters and setters automatically
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        lastUpdated = LocalDateTime.now();
        // Initialize Boolean fields if null
        if (profileComplete == null) {
            profileComplete = false;
        }
        if (accountLocked == null) {
            accountLocked = false;
        }
        if (failedLoginAttempts == null) {
            failedLoginAttempts = 0;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
        // Ensure Boolean fields are never null
        if (profileComplete == null) {
            profileComplete = false;
        }
        if (accountLocked == null) {
            accountLocked = false;
        }
        if (failedLoginAttempts == null) {
            failedLoginAttempts = 0;
        }
    }
}

