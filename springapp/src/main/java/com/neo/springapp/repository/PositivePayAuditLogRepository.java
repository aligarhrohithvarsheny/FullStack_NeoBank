package com.neo.springapp.repository;

import com.neo.springapp.model.PositivePayAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PositivePayAuditLogRepository extends JpaRepository<PositivePayAuditLog, Long> {
    List<PositivePayAuditLog> findByReferenceNumberOrderByTimestampDesc(String referenceNumber);
}
