package com.neo.springapp.repository;

import com.neo.springapp.model.EcsConsentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EcsConsentRecordRepository extends JpaRepository<EcsConsentRecord, Long> {
    Optional<EcsConsentRecord> findByMandateDbId(Long mandateDbId);
}
