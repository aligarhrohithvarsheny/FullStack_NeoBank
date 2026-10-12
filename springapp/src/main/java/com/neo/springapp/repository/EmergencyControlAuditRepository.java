package com.neo.springapp.repository;

import com.neo.springapp.model.EmergencyControlAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmergencyControlAuditRepository extends JpaRepository<EmergencyControlAudit, Long> {
    List<EmergencyControlAudit> findTop50ByOrderByChangedAtDesc();
}
