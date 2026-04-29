package com.neo.springapp.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "admin_audit_documents")
public class AdminAuditDocument {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "audit_log_id")
    private Long auditLogId;
    
    @Column(name = "document_name")
    private String documentName;
    
    @Column(name = "document_type")
    private String documentType; // PDF, IMAGE_JPG, IMAGE_PNG, IMAGE_GIF
    
    @Column(name = "file_path")
    private String filePath;
    
    @Column(name = "file_size")
    private Long fileSize; // in bytes
    
    @Column(name = "file_url")
    private String fileUrl; // URL for downloading
    
    @Column(columnDefinition = "LONGTEXT")
    private String fileBase64; // Base64 encoded file for backup
    
    @Column(name = "uploaded_by")
    private Long uploadedBy; // Admin user ID
    
    @Column(name = "uploaded_by_name")
    private String uploadedByName;
    
    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;
    
    @Column(name = "document_hash")
    private String documentHash; // SHA-256 hash for integrity verification
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "is_signed")
    private Boolean isSigned = true; // Indicates if document is digitally signed
    
    @Column(name = "signature_verified")
    private Boolean signatureVerified = false;
    
    @Column(name = "status")
    private String status; // UPLOADED, VERIFIED, ARCHIVED
    
    // Constructors
    public AdminAuditDocument() {
        this.uploadedAt = LocalDateTime.now();
        this.status = "UPLOADED";
    }
    
    public AdminAuditDocument(Long auditLogId, String documentName, String documentType) {
        this();
        this.auditLogId = auditLogId;
        this.documentName = documentName;
        this.documentType = documentType;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getAuditLogId() {
        return auditLogId;
    }
    
    public void setAuditLogId(Long auditLogId) {
        this.auditLogId = auditLogId;
    }
    
    public String getDocumentName() {
        return documentName;
    }
    
    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }
    
    public String getDocumentType() {
        return documentType;
    }
    
    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    
    public Long getFileSize() {
        return fileSize;
    }
    
    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }
    
    public String getFileUrl() {
        return fileUrl;
    }
    
    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }
    
    public String getFileBase64() {
        return fileBase64;
    }
    
    public void setFileBase64(String fileBase64) {
        this.fileBase64 = fileBase64;
    }
    
    public Long getUploadedBy() {
        return uploadedBy;
    }
    
    public void setUploadedBy(Long uploadedBy) {
        this.uploadedBy = uploadedBy;
    }
    
    public String getUploadedByName() {
        return uploadedByName;
    }
    
    public void setUploadedByName(String uploadedByName) {
        this.uploadedByName = uploadedByName;
    }
    
    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }
    
    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
    
    public String getDocumentHash() {
        return documentHash;
    }
    
    public void setDocumentHash(String documentHash) {
        this.documentHash = documentHash;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Boolean getIsSigned() {
        return isSigned;
    }
    
    public void setIsSigned(Boolean isSigned) {
        this.isSigned = isSigned;
    }
    
    public Boolean getSignatureVerified() {
        return signatureVerified;
    }
    
    public void setSignatureVerified(Boolean signatureVerified) {
        this.signatureVerified = signatureVerified;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
}
