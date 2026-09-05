package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "minor_account_applications", indexes = {
        @Index(name = "idx_minor_app_guardian", columnList = "guardian_user_id"),
        @Index(name = "idx_minor_app_status", columnList = "status")
})
public class MinorAccountApplication {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "guardian_user_id", nullable = false)
    private Long guardianUserId;
    @Column(nullable = false, length = 100)
    private String minorName;
    @Column(nullable = false)
    private LocalDate dateOfBirth;
    @Column(nullable = false, length = 20)
    private String status = "PENDING";
    @Column(nullable = false)
    private Double monthlyLimit = 5000.0;
    @Column(nullable = false)
    private Double dailyLimit = 2000.0;
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime reviewedAt;
    private String reviewedBy;
    private String rejectionReason;

    // Populated on approval so the admin/guardian can see the auto-generated minor account
    private String assignedAccountNumber;
    private String assignedCustomerId;
}
