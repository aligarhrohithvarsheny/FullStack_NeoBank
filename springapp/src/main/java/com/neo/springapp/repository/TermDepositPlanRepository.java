package com.neo.springapp.repository;

import com.neo.springapp.model.TermDepositPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TermDepositPlanRepository extends JpaRepository<TermDepositPlan, Long> {
    List<TermDepositPlan> findByActiveTrueOrderByDaysAsc();
}
