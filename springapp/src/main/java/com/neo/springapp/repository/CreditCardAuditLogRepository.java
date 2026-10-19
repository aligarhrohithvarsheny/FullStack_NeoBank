package com.neo.springapp.repository;

import com.neo.springapp.model.CreditCardAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CreditCardAuditLogRepository extends JpaRepository<CreditCardAuditLog, Long> {
    List<CreditCardAuditLog> findByCreditCardIdOrderByCreatedAtDesc(Long creditCardId);
}
