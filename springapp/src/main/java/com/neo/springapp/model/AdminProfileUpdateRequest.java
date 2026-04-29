package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "admin_profile_update_requests")
public class AdminProfileUpdateRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Admin information
    private Long adminId;
    private String adminEmail;
    private String adminName;

    /**
     * JSON snapshots of the admin profile before and after the requested change.
     * These store all editable profile fields in one object so the manager
     * can see the full change set and optionally edit before approval.
     */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String oldProfileJson;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String newProfileJson;

    /**
     * Status values:
     * PENDING   - created by admin, waiting for manager review
     * APPROVED  - approved by manager, but not yet applied (internal use)
     * REJECTED  - rejected by manager
     * COMPLETED - changes applied to Admin entity
     */
    private String status = "PENDING";

    private LocalDateTime requestedAt;
    private LocalDateTime approvedAt;
    private String approvedBy;
    private String rejectionReason;
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        if (requestedAt == null) {
            requestedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        // no-op for now, but kept for symmetry / future auditing
    }
}

