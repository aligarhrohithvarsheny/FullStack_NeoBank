package com.neo.springapp.repository;

import com.neo.springapp.model.CreditCardAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreditCardAuditLogRepository extends JpaRepository<CreditCardAuditLog, Long> {
}
