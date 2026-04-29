package com.neo.springapp.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "admin_audit_logs")
public class AdminAuditLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "admin_id")
    private Long adminId;
    
    @Column(name = "admin_name")
    private String adminName;
    
    @Column(name = "action_type")
    private String actionType; // EDIT, DELETE, APPROVE, REJECT, etc.
    
    @Column(name = "entity_type")
    private String entityType; // USER, ACCOUNT, KYC, CARD, etc.
    
    @Column(name = "entity_id")
    private Long entityId;
    
    @Column(name = "entity_name")
    private String entityName;
    
    @Column(columnDefinition = "LONGTEXT")
    private String changes; // JSON format of what changed
    
    @Column(columnDefinition = "LONGTEXT")
    private String oldValues; // JSON format of previous values
    
    @Column(columnDefinition = "LONGTEXT")
    private String newValues; // JSON format of new values
    
    @Column(name = "document_required")
    private Boolean documentRequired = false;
    
    @Column(name = "document_uploaded")
    private Boolean documentUploaded = false;
    
    @Column(name = "reason_for_change")
    private String reasonForChange;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "ip_address")
    private String ipAddress;
    
    @Column(name = "user_agent")
    private String userAgent;
    
    @Column(name = "status")
    private String status; // PENDING, COMPLETED, REJECTED
    
    // Constructors
    public AdminAuditLog() {
        this.createdAt = LocalDateTime.now();
        this.status = "PENDING";
    }
    
    public AdminAuditLog(Long adminId, String adminName, String actionType, String entityType, Long entityId) {
        this();
        this.adminId = adminId;
        this.adminName = adminName;
        this.actionType = actionType;
        this.entityType = entityType;
        this.entityId = entityId;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getAdminId() {
        return adminId;
    }
    
    public void setAdminId(Long adminId) {
        this.adminId = adminId;
    }
    
    public String getAdminName() {
        return adminName;
    }
    
    public void setAdminName(String adminName) {
        this.adminName = adminName;
    }
    
    public String getActionType() {
        return actionType;
    }
    
    public void setActionType(String actionType) {
        this.actionType = actionType;
    }
    
    public String getEntityType() {
        return entityType;
    }
    
    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }
    
    public Long getEntityId() {
        return entityId;
    }
    
    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }
    
    public String getEntityName() {
        return entityName;
    }
    
    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }
    
    public String getChanges() {
        return changes;
    }
    
    public void setChanges(String changes) {
        this.changes = changes;
    }
    
    public String getOldValues() {
        return oldValues;
    }
    
    public void setOldValues(String oldValues) {
        this.oldValues = oldValues;
    }
    
    public String getNewValues() {
        return newValues;
    }
    
    public void setNewValues(String newValues) {
        this.newValues = newValues;
    }
    
    public Boolean getDocumentRequired() {
        return documentRequired;
    }
    
    public void setDocumentRequired(Boolean documentRequired) {
        this.documentRequired = documentRequired;
    }
    
    public Boolean getDocumentUploaded() {
        return documentUploaded;
    }
    
    public void setDocumentUploaded(Boolean documentUploaded) {
        this.documentUploaded = documentUploaded;
    }
    
    public String getReasonForChange() {
        return reasonForChange;
    }
    
    public void setReasonForChange(String reasonForChange) {
        this.reasonForChange = reasonForChange;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    public String getUserAgent() {
        return userAgent;
    }
    
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
}
