package com.neo.springapp.repository;

import com.neo.springapp.model.PreloadedCustomerData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PreloadedCustomerDataRepository extends JpaRepository<PreloadedCustomerData, Long> {

    List<PreloadedCustomerData> findByAadharNumberAndUsedFalse(String aadharNumber);

    Optional<PreloadedCustomerData> findFirstByAadharNumberAndUsedFalseOrderByCreatedAtDesc(String aadharNumber);

    Optional<PreloadedCustomerData> findFirstByPanNumberAndUsedFalseOrderByCreatedAtDesc(String panNumber);

    List<PreloadedCustomerData> findByUploadBatchId(String uploadBatchId);

    List<PreloadedCustomerData> findByUploadedByOrderByCreatedAtDesc(String uploadedBy);

    @Query("SELECT p FROM PreloadedCustomerData p WHERE p.used = false ORDER BY p.createdAt DESC")
    List<PreloadedCustomerData> findAllUnused();

    @Query("SELECT p FROM PreloadedCustomerData p ORDER BY p.createdAt DESC")
    List<PreloadedCustomerData> findAllOrderByCreatedAtDesc();

    long countByUsedFalse();

    long countByUsedTrue();

    long countByUploadBatchId(String batchId);

    @Query("SELECT DISTINCT p.uploadBatchId FROM PreloadedCustomerData p ORDER BY p.uploadBatchId DESC")
    List<String> findDistinctBatchIds();

    boolean existsByAadharNumberAndUsedFalse(String aadharNumber);
}
