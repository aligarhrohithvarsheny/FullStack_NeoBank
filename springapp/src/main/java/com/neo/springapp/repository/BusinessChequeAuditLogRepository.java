package com.neo.springapp.repository;

import com.neo.springapp.model.BusinessChequeAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BusinessChequeAuditLogRepository extends JpaRepository<BusinessChequeAuditLog, Long> {

    List<BusinessChequeAuditLog> findByChequeRequestIdOrderByTimestampDesc(Long chequeRequestId);

    List<BusinessChequeAuditLog> findByAdminEmail(String adminEmail);

    List<BusinessChequeAuditLog> findByAction(String action);
}
