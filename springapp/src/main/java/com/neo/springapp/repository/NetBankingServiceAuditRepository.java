package com.neo.springapp.repository;

import com.neo.springapp.model.NetBankingServiceAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NetBankingServiceAuditRepository extends JpaRepository<NetBankingServiceAudit, Long> {
    List<NetBankingServiceAudit> findByServiceTypeOrderByChangedAtDesc(String serviceType);
    List<NetBankingServiceAudit> findAllByOrderByChangedAtDesc();
}
