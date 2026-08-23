package com.neo.springapp.repository;

import com.neo.springapp.model.SalaryAccountEditHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SalaryAccountEditHistoryRepository extends JpaRepository<SalaryAccountEditHistory, Long> {

    List<SalaryAccountEditHistory> findBySalaryAccountIdOrderByEditedAtDesc(Long salaryAccountId);
}