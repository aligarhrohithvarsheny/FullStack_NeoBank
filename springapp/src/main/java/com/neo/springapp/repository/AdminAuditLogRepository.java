package com.neo.springapp.repository;

import com.neo.springapp.model.AdminAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> {
    
    List<AdminAuditLog> findByAdminId(Long adminId);
    
    List<AdminAuditLog> findByEntityTypeAndEntityId(String entityType, Long entityId);
    
    List<AdminAuditLog> findByActionType(String actionType);
    
    List<AdminAuditLog> findByStatus(String status);
    
    @Query("SELECT a FROM AdminAuditLog a WHERE a.createdAt BETWEEN :startDate AND :endDate ORDER BY a.createdAt DESC")
    List<AdminAuditLog> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT a FROM AdminAuditLog a WHERE a.adminId = :adminId AND a.createdAt BETWEEN :startDate AND :endDate ORDER BY a.createdAt DESC")
    List<AdminAuditLog> findByAdminAndDateRange(@Param("adminId") Long adminId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT a FROM AdminAuditLog a WHERE a.entityType = :entityType AND a.entityId = :entityId ORDER BY a.createdAt DESC")
    List<AdminAuditLog> findEntityAuditHistory(@Param("entityType") String entityType, @Param("entityId") Long entityId);
    
    List<AdminAuditLog> findByDocumentUploaded(Boolean documentUploaded);
    
    @Query("SELECT COUNT(a) FROM AdminAuditLog a WHERE a.status = 'PENDING' AND a.documentRequired = true AND a.documentUploaded = false")
    long countPendingDocuments();
}
