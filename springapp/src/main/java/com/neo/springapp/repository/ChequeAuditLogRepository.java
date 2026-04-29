package com.neo.springapp.repository;

import com.neo.springapp.model.ChequeAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChequeAuditLogRepository extends JpaRepository<ChequeAuditLog, Long> {
    
    // Find by cheque request ID ordered by timestamp
    List<ChequeAuditLog> findByChequeRequestIdOrderByTimestampDesc(Long chequeRequestId);
    
    // Find by admin email
    List<ChequeAuditLog> findByAdminEmail(String adminEmail);
    
    // Find by action
    List<ChequeAuditLog> findByAction(String action);
}
