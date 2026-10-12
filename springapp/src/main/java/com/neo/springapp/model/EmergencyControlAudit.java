package com.neo.springapp.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "emergency_control_audit")
public class EmergencyControlAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scope_type", nullable = false, length = 24)
    private String scopeType;

    @Column(name = "scope_key", nullable = false, length = 100)
    private String scopeKey;

    @Column(nullable = false, length = 32)
    private String action;

    @Column(name = "previous_state", nullable = false)
    private boolean previousState;

    @Column(name = "new_state", nullable = false)
    private boolean newState;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(name = "changed_by", nullable = false, length = 150)
    private String changedBy;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @PrePersist
    protected void setChangedAt() {
        changedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getScopeType() { return scopeType; }
    public void setScopeType(String scopeType) { this.scopeType = scopeType; }
    public String getScopeKey() { return scopeKey; }
    public void setScopeKey(String scopeKey) { this.scopeKey = scopeKey; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public boolean isPreviousState() { return previousState; }
    public void setPreviousState(boolean previousState) { this.previousState = previousState; }
    public boolean isNewState() { return newState; }
    public void setNewState(boolean newState) { this.newState = newState; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getChangedBy() { return changedBy; }
    public void setChangedBy(String changedBy) { this.changedBy = changedBy; }
    public LocalDateTime getChangedAt() { return changedAt; }
}
