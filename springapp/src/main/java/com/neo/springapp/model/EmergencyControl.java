package com.neo.springapp.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "emergency_controls", uniqueConstraints = {
        @UniqueConstraint(name = "uk_emergency_control_scope", columnNames = {"scope_type", "scope_key"})
})
public class EmergencyControl {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scope_type", nullable = false, length = 24)
    private String scopeType;

    @Column(name = "scope_key", nullable = false, length = 100)
    private String scopeKey;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(length = 500)
    private String reason;

    @Column(name = "prior_state", length = 32)
    private String priorState;

    @Column(name = "changed_by", nullable = false, length = 150)
    private String changedBy;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void updateTimestamp() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getScopeType() { return scopeType; }
    public void setScopeType(String scopeType) { this.scopeType = scopeType; }
    public String getScopeKey() { return scopeKey; }
    public void setScopeKey(String scopeKey) { this.scopeKey = scopeKey; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getPriorState() { return priorState; }
    public void setPriorState(String priorState) { this.priorState = priorState; }
    public String getChangedBy() { return changedBy; }
    public void setChangedBy(String changedBy) { this.changedBy = changedBy; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
