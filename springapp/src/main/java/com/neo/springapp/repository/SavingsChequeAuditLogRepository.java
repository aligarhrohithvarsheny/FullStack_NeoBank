package com.neo.springapp.repository;

import com.neo.springapp.model.SavingsChequeAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SavingsChequeAuditLogRepository extends JpaRepository<SavingsChequeAuditLog, Long> {

    List<SavingsChequeAuditLog> findByChequeRequestIdOrderByTimestampDesc(Long chequeRequestId);

    List<SavingsChequeAuditLog> findByAdminEmail(String adminEmail);

    List<SavingsChequeAuditLog> findByAction(String action);
}
