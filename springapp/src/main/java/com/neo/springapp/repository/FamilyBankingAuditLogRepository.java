package com.neo.springapp.repository;

import com.neo.springapp.model.FamilyBankingAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FamilyBankingAuditLogRepository extends JpaRepository<FamilyBankingAuditLog, Long> { }