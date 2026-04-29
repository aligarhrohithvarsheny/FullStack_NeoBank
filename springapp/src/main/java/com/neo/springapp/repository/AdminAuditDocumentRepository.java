package com.neo.springapp.repository;

import com.neo.springapp.model.AdminAuditDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AdminAuditDocumentRepository extends JpaRepository<AdminAuditDocument, Long> {
    
    List<AdminAuditDocument> findByAuditLogId(Long auditLogId);
    
    List<AdminAuditDocument> findByUploadedBy(Long adminId);
    
    List<AdminAuditDocument> findByDocumentType(String documentType);
    
    List<AdminAuditDocument> findByStatus(String status);
    
    @Query("SELECT d FROM AdminAuditDocument d WHERE d.uploadedAt BETWEEN :startDate AND :endDate ORDER BY d.uploadedAt DESC")
    List<AdminAuditDocument> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT d FROM AdminAuditDocument d WHERE d.auditLogId IN (SELECT a.id FROM AdminAuditLog a WHERE a.entityType = :entityType AND a.entityId = :entityId) ORDER BY d.uploadedAt DESC")
    List<AdminAuditDocument> findDocumentsByEntity(@Param("entityType") String entityType, @Param("entityId") Long entityId);
    
    @Query("SELECT COUNT(d) FROM AdminAuditDocument d WHERE d.auditLogId = :auditLogId")
    long countDocumentsByAuditLog(@Param("auditLogId") Long auditLogId);
    
    List<AdminAuditDocument> findBySignatureVerified(Boolean signatureVerified);
}
