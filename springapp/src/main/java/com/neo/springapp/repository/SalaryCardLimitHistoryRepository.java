package com.neo.springapp.repository;

import com.neo.springapp.model.SalaryCardLimitHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SalaryCardLimitHistoryRepository extends JpaRepository<SalaryCardLimitHistory, Long> {
    List<SalaryCardLimitHistory> findBySalaryAccountIdOrderByCreatedAtDesc(Long salaryAccountId);
}
