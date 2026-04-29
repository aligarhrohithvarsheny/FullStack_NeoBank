package com.neo.springapp.service;

import com.neo.springapp.model.AdminAuditLog;
import com.neo.springapp.model.AdminAuditDocument;
import com.neo.springapp.repository.AdminAuditLogRepository;
import com.neo.springapp.repository.AdminAuditDocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Base64;

@Service
@SuppressWarnings("null")
public class AdminAuditService {
    
    @Autowired
    private AdminAuditLogRepository auditLogRepository;
    
    @Autowired
    private AdminAuditDocumentRepository documentRepository;
    
    private static final String UPLOAD_DIR = "uploads/admin-audit/";
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB
    // Allow PDF, Excel (XLS/XLSX), and common image types
    private static final List<String> ALLOWED_TYPES = Arrays.asList(
            "application/pdf",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "image/jpeg",
            "image/png",
            "image/gif"
    );
    
    /**
     * Create an audit log entry
     */
    @Transactional
    public AdminAuditLog createAuditLog(Long adminId, String adminName, String actionType, 
                                        String entityType, Long entityId, String entityName, 
                                        String reasonForChange, boolean requireDocument) {
        AdminAuditLog auditLog = new AdminAuditLog();
        auditLog.setAdminId(adminId);
        auditLog.setAdminName(adminName);
        auditLog.setActionType(actionType);
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setEntityName(entityName);
        auditLog.setReasonForChange(reasonForChange);
        auditLog.setDocumentRequired(requireDocument);
        auditLog.setStatus("PENDING");
        
        return auditLogRepository.save(auditLog);
    }
    
    /**
     * Update audit log with changes
     */
    @Transactional
    public AdminAuditLog updateAuditLog(Long auditLogId, String oldValues, String newValues, String changes) {
        Optional<AdminAuditLog> optional = auditLogRepository.findById(auditLogId);
        if (optional.isPresent()) {
            AdminAuditLog auditLog = optional.get();
            auditLog.setOldValues(oldValues);
            auditLog.setNewValues(newValues);
            auditLog.setChanges(changes);
            return auditLogRepository.save(auditLog);
        }
        return null;
    }
    
    /**
     * Upload and save a signed document
     */
    @Transactional
    public AdminAuditDocument uploadDocument(Long auditLogId, MultipartFile file, Long adminId, 
                                             String adminName, String description) throws IOException {
        
        // Validate file
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }
        
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum limit of 10MB");
        }
        
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException("Invalid file type. Allowed types: PDF, Excel (XLS/XLSX), JPEG, PNG, GIF");
        }
        
        // Create upload directory if not exists
        Files.createDirectories(Paths.get(UPLOAD_DIR));
        
        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String fileExtension = originalFilename != null ? originalFilename.substring(originalFilename.lastIndexOf(".")) : "";
        String uniqueFileName = UUID.randomUUID() + fileExtension;
        Path filePath = Paths.get(UPLOAD_DIR).resolve(uniqueFileName);
        
        // Save file
        Files.write(filePath, file.getBytes());
        
        // Calculate file hash
        String fileHash = calculateHash(file.getBytes());
        
        // Create document record
        AdminAuditDocument document = new AdminAuditDocument();
        document.setAuditLogId(auditLogId);
        document.setDocumentName(originalFilename);
        document.setDocumentType(getDocumentType(file.getContentType()));
        document.setFilePath(filePath.toString());
        document.setFileSize(file.getSize());
        document.setFileUrl("/api/audit/documents/" + uniqueFileName);
        document.setUploadedBy(adminId);
        document.setUploadedByName(adminName);
        document.setDocumentHash(fileHash);
        document.setDescription(description);
        document.setIsSigned(true);
        document.setStatus("UPLOADED");
        
        // Save to database
        AdminAuditDocument savedDocument = documentRepository.save(document);
        
        // Update audit log
        Optional<AdminAuditLog> optional = auditLogRepository.findById(auditLogId);
        if (optional.isPresent()) {
            AdminAuditLog auditLog = optional.get();
            auditLog.setDocumentUploaded(true);
            auditLog.setStatus("COMPLETED");
            auditLogRepository.save(auditLog);
        }
        
        return savedDocument;
    }
    
    /**
     * Upload document with base64 encoding
     */
    @Transactional
    public AdminAuditDocument uploadDocumentBase64(Long auditLogId, String base64Content, String fileName, 
                                                   String mimeType, Long adminId, String adminName, 
                                                   String description) throws IOException {
        
        byte[] decodedBytes = Base64.getDecoder().decode(base64Content);
        
        if (decodedBytes.length > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum limit of 10MB");
        }
        
        if (!ALLOWED_TYPES.contains(mimeType)) {
            throw new IllegalArgumentException("Invalid file type. Allowed types: PDF, Excel (XLS/XLSX), JPEG, PNG, GIF");
        }
        
        // Create upload directory if not exists
        Files.createDirectories(Paths.get(UPLOAD_DIR));
        
        // Generate unique filename
        String fileExtension = fileName.substring(fileName.lastIndexOf("."));
        String uniqueFileName = UUID.randomUUID() + fileExtension;
        Path filePath = Paths.get(UPLOAD_DIR).resolve(uniqueFileName);
        
        // Save file
        Files.write(filePath, decodedBytes);
        
        // Calculate file hash
        String fileHash = calculateHash(decodedBytes);
        
        // Create document record
        AdminAuditDocument document = new AdminAuditDocument();
        document.setAuditLogId(auditLogId);
        document.setDocumentName(fileName);
        document.setDocumentType(getDocumentType(mimeType));
        document.setFilePath(filePath.toString());
        document.setFileSize((long) decodedBytes.length);
        document.setFileUrl("/api/audit/documents/" + uniqueFileName);
        document.setFileBase64(base64Content);
        document.setUploadedBy(adminId);
        document.setUploadedByName(adminName);
        document.setDocumentHash(fileHash);
        document.setDescription(description);
        document.setIsSigned(true);
        document.setStatus("UPLOADED");
        
        // Save to database
        AdminAuditDocument savedDocument = documentRepository.save(document);
        
        // Update audit log
        Optional<AdminAuditLog> optional = auditLogRepository.findById(auditLogId);
        if (optional.isPresent()) {
            AdminAuditLog auditLog = optional.get();
            auditLog.setDocumentUploaded(true);
            auditLog.setStatus("COMPLETED");
            auditLogRepository.save(auditLog);
        }
        
        return savedDocument;
    }
    
    /**
     * Get audit history for an entity
     */
    public List<AdminAuditLog> getAuditHistory(String entityType, Long entityId) {
        return auditLogRepository.findEntityAuditHistory(entityType, entityId);
    }
    
    /**
     * Get all documents for a specific audit log
     */
    public List<AdminAuditDocument> getAuditDocuments(Long auditLogId) {
        return documentRepository.findByAuditLogId(auditLogId);
    }
    
    /**
     * Get all documents for an entity
     */
    public List<AdminAuditDocument> getEntityDocuments(String entityType, Long entityId) {
        return documentRepository.findDocumentsByEntity(entityType, entityId);
    }
    
    /**
     * Get audit logs by admin
     */
    public List<AdminAuditLog> getAdminAuditLogs(Long adminId) {
        return auditLogRepository.findByAdminId(adminId);
    }
    
    /**
     * Get pending document uploads
     */
    public List<AdminAuditLog> getPendingDocuments() {
        return auditLogRepository.findByDocumentUploaded(false);
    }
    
    /**
     * Get count of pending documents
     */
    public long getPendingDocumentCount() {
        return auditLogRepository.countPendingDocuments();
    }
    
    /**
     * Download document file
     */
    public byte[] downloadDocument(Long documentId) throws IOException {
        Optional<AdminAuditDocument> optional = documentRepository.findById(documentId);
        if (optional.isPresent()) {
            AdminAuditDocument document = optional.get();
            return Files.readAllBytes(Paths.get(document.getFilePath()));
        }
        throw new IllegalArgumentException("Document not found");
    }
    
    /**
     * Verify document integrity
     */
    public boolean verifyDocument(Long documentId) throws IOException {
        Optional<AdminAuditDocument> optional = documentRepository.findById(documentId);
        if (optional.isPresent()) {
            AdminAuditDocument document = optional.get();
            byte[] fileBytes = Files.readAllBytes(Paths.get(document.getFilePath()));
            String currentHash = calculateHash(fileBytes);
            
            if (currentHash.equals(document.getDocumentHash())) {
                document.setSignatureVerified(true);
                document.setStatus("VERIFIED");
                documentRepository.save(document);
                return true;
            }
        }
        return false;
    }
    
    /**
     * Get audit logs by date range
     */
    public List<AdminAuditLog> getAuditLogsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return auditLogRepository.findByDateRange(startDate, endDate);
    }
    
    /**
     * Get audit logs by admin and date range
     */
    public List<AdminAuditLog> getAuditLogsByAdminAndDateRange(Long adminId, LocalDateTime startDate, LocalDateTime endDate) {
        return auditLogRepository.findByAdminAndDateRange(adminId, startDate, endDate);
    }
    
    /**
     * Delete old audit logs (archiving strategy)
     */
    @Transactional
    public void archiveOldAuditLogs(LocalDateTime beforeDate) {
        List<AdminAuditLog> oldLogs = auditLogRepository.findByDateRange(LocalDateTime.MIN, beforeDate);
        for (AdminAuditLog log : oldLogs) {
            log.setStatus("ARCHIVED");
            auditLogRepository.save(log);
        }
    }
    
    // Helper methods
    
    private String getDocumentType(String mimeType) {
        if ("application/pdf".equals(mimeType)) {
            return "PDF";
        } else if ("image/jpeg".equals(mimeType)) {
            return "IMAGE_JPG";
        } else if ("image/png".equals(mimeType)) {
            return "IMAGE_PNG";
        } else if ("image/gif".equals(mimeType)) {
            return "IMAGE_GIF";
        }
        return "UNKNOWN";
    }
    
    private String calculateHash(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
