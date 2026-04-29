package com.neo.springapp.controller;

import com.neo.springapp.model.AdminAuditLog;
import com.neo.springapp.model.AdminAuditDocument;
import com.neo.springapp.service.AdminAuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/audit")
@CrossOrigin(origins = "*")
@SuppressWarnings("null")
public class AdminAuditController {
    
    @Autowired
    private AdminAuditService auditService;
    
    /**
     * Create a new audit log entry
     */
    @PostMapping("/log/create")
    public ResponseEntity<AdminAuditLog> createAuditLog(
            @RequestParam Long adminId,
            @RequestParam String adminName,
            @RequestParam String actionType,
            @RequestParam String entityType,
            @RequestParam Long entityId,
            @RequestParam String entityName,
            @RequestParam(required = false) String reasonForChange,
            @RequestParam(defaultValue = "false") boolean requireDocument) {
        
        AdminAuditLog auditLog = auditService.createAuditLog(
                adminId, adminName, actionType, entityType, entityId, 
                entityName, reasonForChange, requireDocument
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(auditLog);
    }
    
    /**
     * Update audit log with changes
     */
    @PutMapping("/log/{auditLogId}")
    public ResponseEntity<AdminAuditLog> updateAuditLog(
            @PathVariable Long auditLogId,
            @RequestBody Map<String, String> changes) {
        
        AdminAuditLog auditLog = auditService.updateAuditLog(
                auditLogId,
                changes.get("oldValues"),
                changes.get("newValues"),
                changes.get("changes")
        );
        
        if (auditLog != null) {
            return ResponseEntity.ok(auditLog);
        }
        return ResponseEntity.notFound().build();
    }
    
    /**
     * Upload signed document for audit log
     */
    @PostMapping("/documents/upload/{auditLogId}")
    public ResponseEntity<AdminAuditDocument> uploadDocument(
            @PathVariable Long auditLogId,
            @RequestParam("file") MultipartFile file,
            @RequestParam Long adminId,
            @RequestParam String adminName,
            @RequestParam(required = false) String description) {
        
        try {
            AdminAuditDocument document = auditService.uploadDocument(
                    auditLogId, file, adminId, adminName, description
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(document);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
    
    /**
     * Upload document using base64 encoding (for web signatures)
     */
    @PostMapping("/documents/upload-base64/{auditLogId}")
    public ResponseEntity<AdminAuditDocument> uploadDocumentBase64(
            @PathVariable Long auditLogId,
            @RequestBody Map<String, String> request) {
        
        try {
            AdminAuditDocument document = auditService.uploadDocumentBase64(
                    auditLogId,
                    request.get("base64Content"),
                    request.get("fileName"),
                    request.get("mimeType"),
                    Long.parseLong(request.get("adminId")),
                    request.get("adminName"),
                    request.get("description")
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(document);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
    
    /**
     * Get audit history for an entity
     */
    @GetMapping("/history/{entityType}/{entityId}")
    public ResponseEntity<List<AdminAuditLog>> getAuditHistory(
            @PathVariable String entityType,
            @PathVariable Long entityId) {
        
        List<AdminAuditLog> history = auditService.getAuditHistory(entityType, entityId);
        return ResponseEntity.ok(history);
    }
    
    /**
     * Get all documents for an audit log
     */
    @GetMapping("/documents/log/{auditLogId}")
    public ResponseEntity<List<AdminAuditDocument>> getAuditDocuments(
            @PathVariable Long auditLogId) {
        
        List<AdminAuditDocument> documents = auditService.getAuditDocuments(auditLogId);
        return ResponseEntity.ok(documents);
    }
    
    /**
     * Get all documents for an entity
     */
    @GetMapping("/documents/entity/{entityType}/{entityId}")
    public ResponseEntity<List<AdminAuditDocument>> getEntityDocuments(
            @PathVariable String entityType,
            @PathVariable Long entityId) {
        
        List<AdminAuditDocument> documents = auditService.getEntityDocuments(entityType, entityId);
        return ResponseEntity.ok(documents);
    }
    
    /**
     * Get audit logs by admin
     */
    @GetMapping("/logs/admin/{adminId}")
    public ResponseEntity<List<AdminAuditLog>> getAdminAuditLogs(
            @PathVariable Long adminId) {
        
        List<AdminAuditLog> logs = auditService.getAdminAuditLogs(adminId);
        return ResponseEntity.ok(logs);
    }
    
    /**
     * Get pending document uploads
     */
    @GetMapping("/pending-documents")
    public ResponseEntity<List<AdminAuditLog>> getPendingDocuments() {
        List<AdminAuditLog> pendingDocs = auditService.getPendingDocuments();
        return ResponseEntity.ok(pendingDocs);
    }
    
    /**
     * Get count of pending documents
     */
    @GetMapping("/pending-documents/count")
    public ResponseEntity<Long> getPendingDocumentCount() {
        long count = auditService.getPendingDocumentCount();
        return ResponseEntity.ok(count);
    }
    
    /**
     * Download document file
     */
    @GetMapping("/documents/{documentId}/download")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable Long documentId) {
        try {
            byte[] fileContent = auditService.downloadDocument(documentId);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=document.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(fileContent);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Verify document integrity
     */
    @PostMapping("/documents/{documentId}/verify")
    public ResponseEntity<Map<String, Boolean>> verifyDocument(@PathVariable Long documentId) {
        try {
            boolean isValid = auditService.verifyDocument(documentId);
            return ResponseEntity.ok(Collections.singletonMap("verified", isValid));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get audit logs by date range
     */
    @GetMapping("/logs/date-range")
    public ResponseEntity<List<AdminAuditLog>> getAuditLogsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        List<AdminAuditLog> logs = auditService.getAuditLogsByDateRange(startDate, endDate);
        return ResponseEntity.ok(logs);
    }
    
    /**
     * Get audit logs by admin and date range
     */
    @GetMapping("/logs/admin/{adminId}/date-range")
    public ResponseEntity<List<AdminAuditLog>> getAuditLogsByAdminAndDateRange(
            @PathVariable Long adminId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        List<AdminAuditLog> logs = auditService.getAuditLogsByAdminAndDateRange(adminId, startDate, endDate);
        return ResponseEntity.ok(logs);
    }
}
