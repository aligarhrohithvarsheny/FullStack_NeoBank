package com.neo.springapp.repository;

import com.neo.springapp.model.CibilReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CibilReportRepository extends JpaRepository<CibilReport, Long> {

    List<CibilReport> findByPanNumber(String panNumber);

    Optional<CibilReport> findFirstByPanNumberOrderByCreatedAtDesc(String panNumber);

    List<CibilReport> findByUploadedByOrderByCreatedAtDesc(String uploadedBy);

    List<CibilReport> findByUploadBatchId(String uploadBatchId);

    List<CibilReport> findByStatus(String status);

    List<CibilReport> findByUploadedByAndStatusOrderByCreatedAtDesc(String uploadedBy, String status);

    long countByUploadedBy(String uploadedBy);

    long countByUploadedByAndStatus(String uploadedBy, String status);

    boolean existsByPanNumberAndUploadedBy(String panNumber, String uploadedBy);

    void deleteByUploadBatchId(String batchId);
}
