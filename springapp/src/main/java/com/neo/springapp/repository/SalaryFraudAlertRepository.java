package com.neo.springapp.repository;

import com.neo.springapp.model.SalaryFraudAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SalaryFraudAlertRepository extends JpaRepository<SalaryFraudAlert, Long> {

    List<SalaryFraudAlert> findBySalaryAccountIdOrderByCreatedAtDesc(Long salaryAccountId);

    List<SalaryFraudAlert> findBySalaryAccountIdAndResolvedFalseOrderByCreatedAtDesc(Long salaryAccountId);

    Long countBySalaryAccountIdAndResolvedFalse(Long salaryAccountId);
}
