package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "current_account_business_users")
public class CurrentAccountBusinessUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String userId;

    @Column(nullable = false)
    private String accountNumber;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String mobile;

    @Column(nullable = false)
    private String role = "STAFF"; // OWNER, ACCOUNTANT, STAFF

    private String password;

    private String status = "ACTIVE"; // ACTIVE, INACTIVE, SUSPENDED

    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;
    private String createdBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (userId == null) {
            userId = "USR-" + System.currentTimeMillis();
        }
    }
}
