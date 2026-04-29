package com.neo.springapp.repository;

import com.neo.springapp.model.FraudAlert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FraudAlertRepository extends JpaRepository<FraudAlert, Long> {

    List<FraudAlert> findByStatusOrderByCreatedAtDesc(FraudAlert.Status status);

    Page<FraudAlert> findByStatusOrderByCreatedAtDesc(FraudAlert.Status status, Pageable pageable);

    List<FraudAlert> findByAlertTypeOrderByCreatedAtDesc(FraudAlert.AlertType alertType);

    List<FraudAlert> findBySourceTypeOrderByCreatedAtDesc(FraudAlert.SourceType sourceType);

    @Query("SELECT f FROM FraudAlert f WHERE (:status IS NULL OR f.status = :status) AND (:alertType IS NULL OR f.alertType = :alertType) AND (:sourceType IS NULL OR f.sourceType = :sourceType) ORDER BY f.createdAt DESC")
    Page<FraudAlert> findWithFilters(@Param("status") FraudAlert.Status status,
                                     @Param("alertType") FraudAlert.AlertType alertType,
                                     @Param("sourceType") FraudAlert.SourceType sourceType,
                                     Pageable pageable);

    @Query("SELECT f FROM FraudAlert f WHERE f.sourceEntityId = :entityId ORDER BY f.createdAt DESC")
    List<FraudAlert> findBySourceEntityId(@Param("entityId") String entityId);

    @Query("SELECT COUNT(f) FROM FraudAlert f WHERE f.status = :status")
    long countByStatus(@Param("status") FraudAlert.Status status);

    @Query("SELECT COUNT(f) FROM FraudAlert f WHERE f.status = 'PENDING_REVIEW'")
    long countPendingReview();

    List<FraudAlert> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime start, LocalDateTime end);
}
