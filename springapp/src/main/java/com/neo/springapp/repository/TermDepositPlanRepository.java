package com.neo.springapp.repository;

import com.neo.springapp.model.TermDepositPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface TermDepositPlanRepository extends JpaRepository<TermDepositPlan, Long> {
    List<TermDepositPlan> findByActiveTrueOrderByDaysAsc();

    @Query("SELECT p FROM TermDepositPlan p WHERE p.active = true "
            + "AND (UPPER(p.productType) = UPPER(:productType) "
            + "OR (:productType = 'FD' AND p.productType IS NULL)) ORDER BY p.schemeName ASC")
    List<TermDepositPlan> findActiveByProductType(@Param("productType") String productType);
}
