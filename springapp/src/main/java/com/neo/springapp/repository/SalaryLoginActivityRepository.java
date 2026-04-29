package com.neo.springapp.repository;

import com.neo.springapp.model.SalaryLoginActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SalaryLoginActivityRepository extends JpaRepository<SalaryLoginActivity, Long> {

    List<SalaryLoginActivity> findBySalaryAccountIdOrderByCreatedAtDesc(Long salaryAccountId);

    List<SalaryLoginActivity> findByAccountNumberOrderByCreatedAtDesc(String accountNumber);
}
