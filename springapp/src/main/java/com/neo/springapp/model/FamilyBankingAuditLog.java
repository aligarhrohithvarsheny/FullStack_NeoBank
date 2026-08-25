package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "family_banking_audit_logs", indexes = {
        @Index(name = "idx_family_audit_actor", columnList = "actor_user_id,created_at")
})
public class FamilyBankingAuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "actor_user_id", nullable = false)
    private Long actorUserId;
    @Column(nullable = false, length = 80)
    private String action;
    @Column(length = 80)
    private String resourceType;
    @Column(length = 80)
    private String resourceId;
    @Column(length = 1000)
    private String details;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @PreUpdate
    @PreRemove
    private void preventMutation() {
        throw new IllegalStateException("Family Banking audit records are immutable");
    }
}