package com.neo.springapp.repository;

import com.neo.springapp.model.AiSecurityEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AiSecurityEventRepository extends JpaRepository<AiSecurityEvent, Long> {

    Page<AiSecurityEvent> findByStatusOrderByCreatedAtDesc(AiSecurityEvent.Status status, Pageable pageable);

    Page<AiSecurityEvent> findByChannelOrderByCreatedAtDesc(AiSecurityEvent.Channel channel, Pageable pageable);

    Page<AiSecurityEvent> findBySeverityOrderByCreatedAtDesc(AiSecurityEvent.Severity severity, Pageable pageable);

    @Query("SELECT e FROM AiSecurityEvent e WHERE " +
           "(:eventType IS NULL OR e.eventType = :eventType) AND " +
           "(:channel IS NULL OR e.channel = :channel) AND " +
           "(:severity IS NULL OR e.severity = :severity) AND " +
           "(:status IS NULL OR e.status = :status) " +
           "ORDER BY e.createdAt DESC")
    Page<AiSecurityEvent> findWithFilters(
            @Param("eventType") AiSecurityEvent.EventType eventType,
            @Param("channel") AiSecurityEvent.Channel channel,
            @Param("severity") AiSecurityEvent.Severity severity,
            @Param("status") AiSecurityEvent.Status status,
            Pageable pageable);

    @Query("SELECT COUNT(e) FROM AiSecurityEvent e WHERE e.status = :status")
    long countByStatus(@Param("status") AiSecurityEvent.Status status);

    @Query("SELECT COUNT(e) FROM AiSecurityEvent e WHERE e.severity = :severity AND e.createdAt >= :since")
    long countBySeveritySince(@Param("severity") AiSecurityEvent.Severity severity, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(e) FROM AiSecurityEvent e WHERE e.createdAt >= :since")
    long countSince(@Param("since") LocalDateTime since);

    @Query("SELECT e.channel, COUNT(e) FROM AiSecurityEvent e WHERE e.createdAt >= :since GROUP BY e.channel")
    List<Object[]> countByChannelSince(@Param("since") LocalDateTime since);

    @Query("SELECT e.eventType, COUNT(e) FROM AiSecurityEvent e WHERE e.createdAt >= :since GROUP BY e.eventType ORDER BY COUNT(e) DESC")
    List<Object[]> countByEventTypeSince(@Param("since") LocalDateTime since);

    @Query("SELECT e.severity, COUNT(e) FROM AiSecurityEvent e WHERE e.createdAt >= :since GROUP BY e.severity")
    List<Object[]> countBySeverityGroupSince(@Param("since") LocalDateTime since);

    @Query("SELECT AVG(e.riskScore) FROM AiSecurityEvent e WHERE e.createdAt >= :since")
    Double avgRiskScoreSince(@Param("since") LocalDateTime since);

    List<AiSecurityEvent> findBySourceEntityIdOrderByCreatedAtDesc(String sourceEntityId);

    @Query("SELECT e FROM AiSecurityEvent e WHERE e.severity IN ('HIGH', 'CRITICAL') AND e.status = 'DETECTED' ORDER BY e.createdAt DESC")
    List<AiSecurityEvent> findActiveHighSeverityEvents();

    @Query("SELECT FUNCTION('DATE', e.createdAt), COUNT(e) FROM AiSecurityEvent e WHERE e.createdAt >= :since GROUP BY FUNCTION('DATE', e.createdAt) ORDER BY FUNCTION('DATE', e.createdAt)")
    List<Object[]> countDailyEventsSince(@Param("since") LocalDateTime since);
}
