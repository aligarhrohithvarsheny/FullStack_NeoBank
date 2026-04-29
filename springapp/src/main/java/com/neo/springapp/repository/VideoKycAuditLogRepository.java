package com.neo.springapp.repository;

import com.neo.springapp.model.VideoKycAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VideoKycAuditLogRepository extends JpaRepository<VideoKycAuditLog, Long> {

    List<VideoKycAuditLog> findBySessionIdOrderByCreatedAtDesc(Long sessionId);

    Page<VideoKycAuditLog> findBySessionId(Long sessionId, Pageable pageable);

    List<VideoKycAuditLog> findByAdminIdOrderByCreatedAtDesc(Long adminId);

    Page<VideoKycAuditLog> findAll(Pageable pageable);
}
