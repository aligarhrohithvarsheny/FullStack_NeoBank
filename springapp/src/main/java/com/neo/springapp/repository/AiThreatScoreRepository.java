package com.neo.springapp.repository;

import com.neo.springapp.model.AiThreatScore;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AiThreatScoreRepository extends JpaRepository<AiThreatScore, Long> {

    Optional<AiThreatScore> findByEntityIdAndEntityType(String entityId, String entityType);

    Page<AiThreatScore> findByRiskLevelOrderByOverallRiskScoreDesc(AiThreatScore.RiskLevel riskLevel, Pageable pageable);

    @Query("SELECT t FROM AiThreatScore t WHERE t.overallRiskScore >= :threshold ORDER BY t.overallRiskScore DESC")
    List<AiThreatScore> findHighRiskEntities(@Param("threshold") Double threshold);

    List<AiThreatScore> findByIsWatchlistedTrueOrderByOverallRiskScoreDesc();

    @Query("SELECT COUNT(t) FROM AiThreatScore t WHERE t.riskLevel = :level")
    long countByRiskLevel(@Param("level") AiThreatScore.RiskLevel level);

    @Query("SELECT t FROM AiThreatScore t ORDER BY t.overallRiskScore DESC")
    Page<AiThreatScore> findTopThreats(Pageable pageable);
}
