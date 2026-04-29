package com.neo.springapp.repository;

import com.neo.springapp.model.SalaryAdvanceRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SalaryAdvanceRequestRepository extends JpaRepository<SalaryAdvanceRequest, Long> {

    List<SalaryAdvanceRequest> findBySalaryAccountIdOrderByCreatedAtDesc(Long salaryAccountId);

    @Query("SELECT sar FROM SalaryAdvanceRequest sar WHERE sar.salaryAccountId = :accountId AND sar.status = 'Approved' AND sar.repaid = false")
    List<SalaryAdvanceRequest> findActiveAdvances(@Param("accountId") Long accountId);

    @Query("SELECT COUNT(sar) FROM SalaryAdvanceRequest sar WHERE sar.salaryAccountId = :accountId AND sar.status = 'Pending'")
    Long countPendingByAccount(@Param("accountId") Long accountId);
}
