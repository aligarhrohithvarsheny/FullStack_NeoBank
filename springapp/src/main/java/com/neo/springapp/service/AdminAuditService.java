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
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "xls", "xlsx", "jpg", "jpeg", "png", "gif", "webp");
    // Allow PDF, Excel (XLS/XLSX), and common image types
    private static final List<String> ALLOWED_TYPES = Arrays.asList(
            "application/pdf",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp",
            "application/octet-stream"
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
        
        if (!isAllowedUpload(file.getContentType(), file.getOriginalFilename())) {
            throw new IllegalArgumentException("Invalid file type. Allowed types: PDF, Excel (XLS/XLSX), JPEG, PNG, GIF, WEBP");
        }
        
        byte[] fileBytes = file.getBytes();
        String resolvedMimeType = resolveMimeType(file.getContentType(), file.getOriginalFilename());
        
        // Create upload directory if not exists
        try {
            Files.createDirectories(Paths.get(UPLOAD_DIR));
        } catch (IOException diskDirError) {
            System.err.println("Admin audit upload directory unavailable: " + diskDirError.getMessage());
        }
        
        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String fileExtension = extension(originalFilename);
        String uniqueFileName = UUID.randomUUID() + (fileExtension.isEmpty() ? "" : "." + fileExtension);
        Path filePath = Paths.get(UPLOAD_DIR).resolve(uniqueFileName);
        
        // Save file to disk when available (DB copy is the source of truth on ephemeral hosts)
        try {
            Files.write(filePath, fileBytes);
        } catch (IOException diskError) {
            System.err.println("Admin audit upload stored in database only (disk unavailable): " + diskError.getMessage());
            filePath = Paths.get("db://" + uniqueFileName);
        }
        
        // Calculate file hash
        String fileHash = calculateHash(fileBytes);
        
        // Create document record
        AdminAuditDocument document = new AdminAuditDocument();
        document.setAuditLogId(auditLogId);
        document.setDocumentName(originalFilename);
        document.setDocumentType(getDocumentType(resolvedMimeType, originalFilename));
        document.setFilePath(filePath.toString());
        document.setFileSize(file.getSize());
        document.setFileUrl("/api/audit/documents/" + uniqueFileName);
        document.setFileBase64(Base64.getEncoder().encodeToString(fileBytes));
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
        
        if (!isAllowedUpload(mimeType, fileName)) {
            throw new IllegalArgumentException("Invalid file type. Allowed types: PDF, Excel (XLS/XLSX), JPEG, PNG, GIF, WEBP");
        }
        
        String resolvedMimeType = resolveMimeType(mimeType, fileName);
        
        // Create upload directory if not exists
        try {
            Files.createDirectories(Paths.get(UPLOAD_DIR));
        } catch (IOException diskDirError) {
            System.err.println("Admin audit upload directory unavailable: " + diskDirError.getMessage());
        }
        
        // Generate unique filename
        String fileExtension = extension(fileName);
        String uniqueFileName = UUID.randomUUID() + (fileExtension.isEmpty() ? "" : "." + fileExtension);
        Path filePath = Paths.get(UPLOAD_DIR).resolve(uniqueFileName);
        
        // Save file
        try {
            Files.write(filePath, decodedBytes);
        } catch (IOException diskError) {
            System.err.println("Admin audit base64 upload stored in database only (disk unavailable): " + diskError.getMessage());
            filePath = Paths.get("db://" + uniqueFileName);
        }
        
        // Calculate file hash
        String fileHash = calculateHash(decodedBytes);
        
        // Create document record
        AdminAuditDocument document = new AdminAuditDocument();
        document.setAuditLogId(auditLogId);
        document.setDocumentName(fileName);
        document.setDocumentType(getDocumentType(resolvedMimeType, fileName));
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

    public Optional<AdminAuditDocument> findDocument(Long documentId) {
        return documentRepository.findById(documentId);
    }
    
    /**
     * Download document file
     */
    public byte[] downloadDocument(Long documentId) throws IOException {
        Optional<AdminAuditDocument> optional = documentRepository.findById(documentId);
        if (optional.isEmpty()) {
            throw new IllegalArgumentException("Document not found");
        }
        AdminAuditDocument document = optional.get();
        byte[] fromDb = readDocumentBytes(document);
        if (fromDb != null && fromDb.length > 0) {
            return fromDb;
        }
        throw new IllegalArgumentException("Document file not found on server");
    }
    
    public String resolveDownloadContentType(AdminAuditDocument document) {
        String name = document.getDocumentName();
        String ext = extension(name);
        return switch (ext) {
            case "pdf" -> "application/pdf";
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            default -> "application/octet-stream";
        };
    }
    
    /**
     * Verify document integrity
     */
    public boolean verifyDocument(Long documentId) throws IOException {
        Optional<AdminAuditDocument> optional = documentRepository.findById(documentId);
        if (optional.isPresent()) {
            AdminAuditDocument document = optional.get();
            byte[] fileBytes = readDocumentBytes(document);
            if (fileBytes == null || fileBytes.length == 0) {
                return false;
            }
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
    
    private byte[] readDocumentBytes(AdminAuditDocument document) throws IOException {
        if (document.getFileBase64() != null && !document.getFileBase64().isBlank()) {
            return Base64.getDecoder().decode(document.getFileBase64());
        }
        String filePath = document.getFilePath();
        if (filePath == null || filePath.isBlank() || filePath.startsWith("db://")) {
            return null;
        }
        Path path = Paths.get(filePath).normalize();
        if (!path.isAbsolute()) {
            path = Paths.get(System.getProperty("user.dir")).resolve(path).normalize();
        }
        if (!Files.exists(path)) {
            return null;
        }
        return Files.readAllBytes(path);
    }

    private boolean isAllowedUpload(String contentType, String filename) {
        if (ALLOWED_EXTENSIONS.contains(extension(filename))) {
            return true;
        }
        return contentType != null && ALLOWED_TYPES.contains(contentType);
    }

    private String resolveMimeType(String contentType, String filename) {
        if (contentType != null && !contentType.isBlank() && !"application/octet-stream".equals(contentType)) {
            return contentType;
        }
        return switch (extension(filename)) {
            case "pdf" -> "application/pdf";
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            default -> "application/octet-stream";
        };
    }

    private static String extension(String filename) {
        if (filename == null || filename.isBlank()) {
            return "";
        }
        String lower = filename.toLowerCase();
        int dot = lower.lastIndexOf('.');
        return dot >= 0 ? lower.substring(dot + 1) : "";
    }
    
    private String getDocumentType(String mimeType, String filename) {
        if ("application/pdf".equals(mimeType) || "pdf".equals(extension(filename))) {
            return "PDF";
        } else if ("image/jpeg".equals(mimeType) || Set.of("jpg", "jpeg").contains(extension(filename))) {
            return "IMAGE_JPG";
        } else if ("image/png".equals(mimeType) || "png".equals(extension(filename))) {
            return "IMAGE_PNG";
        } else if ("image/gif".equals(mimeType) || "gif".equals(extension(filename))) {
            return "IMAGE_GIF";
        } else if ("image/webp".equals(mimeType) || "webp".equals(extension(filename))) {
            return "IMAGE_WEBP";
        } else if ("application/vnd.ms-excel".equals(mimeType) || "xls".equals(extension(filename))) {
            return "EXCEL_XLS";
        } else if ("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet".equals(mimeType)
                || "xlsx".equals(extension(filename))) {
            return "EXCEL_XLSX";
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
