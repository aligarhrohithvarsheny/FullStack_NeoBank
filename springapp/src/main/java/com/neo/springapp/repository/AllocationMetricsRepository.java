package com.neo.springapp.repository;

import com.neo.springapp.model.AllocationRealTimeMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AllocationMetricsRepository extends JpaRepository<AllocationRealTimeMetrics, Long> {

    Optional<AllocationRealTimeMetrics> findByAllocationId(Long allocationId);

    List<AllocationRealTimeMetrics> findByMetricsDate(LocalDate metricsDate);

    @Query("SELECT arm FROM AllocationRealTimeMetrics arm WHERE arm.metricsDate BETWEEN :fromDate AND :toDate")
    List<AllocationRealTimeMetrics> findByMetricsDateBetween(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    @Query("SELECT COALESCE(SUM(arm.currentBalance), 0) FROM AllocationRealTimeMetrics arm")
    Double getTotalAvailableBalance();

    @Query("SELECT COALESCE(SUM(arm.totalUtilized), 0) FROM AllocationRealTimeMetrics arm")
    Double getTotalUtilizedFunds();

    @Query("SELECT COALESCE(AVG(arm.utilizationPercentage), 0) FROM AllocationRealTimeMetrics arm WHERE arm.metricsDate = :date")
    Double getAverageUtilizationPercentage(@Param("date") LocalDate date);
}
