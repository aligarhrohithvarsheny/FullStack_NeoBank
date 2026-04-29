package com.neo.springapp.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * ChequeAuditLog Model - Tracks all admin actions on cheque requests
 */
@Entity
@Table(name = "cheque_audit_log", indexes = {
    @Index(name = "idx_cheque_request_id", columnList = "cheque_request_id"),
    @Index(name = "idx_timestamp", columnList = "timestamp")
})
public class ChequeAuditLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "cheque_request_id", nullable = false)
    private Long chequeRequestId;
    
    @Column(name = "admin_email", nullable = false, length = 100)
    private String adminEmail;
    
    @Column(name = "action", nullable = false, length = 50)
    private String action; // APPROVE, REJECT, PICKUP, CLEAR
    
    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;
    
    @Column(name = "ip_address", length = 50)
    private String ipAddress;
    
    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;
    
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
    
    // Constructors
    public ChequeAuditLog() {
    }
    
    public ChequeAuditLog(Long chequeRequestId, String adminEmail, String action, String remarks) {
        this.chequeRequestId = chequeRequestId;
        this.adminEmail = adminEmail;
        this.action = action;
        this.remarks = remarks;
        this.timestamp = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getChequeRequestId() {
        return chequeRequestId;
    }
    
    public void setChequeRequestId(Long chequeRequestId) {
        this.chequeRequestId = chequeRequestId;
    }
    
    public String getAdminEmail() {
        return adminEmail;
    }
    
    public void setAdminEmail(String adminEmail) {
        this.adminEmail = adminEmail;
    }
    
    public String getAction() {
        return action;
    }
    
    public void setAction(String action) {
        this.action = action;
    }
    
    public String getRemarks() {
        return remarks;
    }
    
    public void setRemarks(String remarks) {
        this.remarks = remarks;
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
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
