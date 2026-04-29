package com.neo.springapp.repository;

import com.neo.springapp.model.SalaryTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SalaryTransactionRepository extends JpaRepository<SalaryTransaction, Long> {

    List<SalaryTransaction> findBySalaryAccountIdOrderByCreatedAtDesc(Long salaryAccountId);

    List<SalaryTransaction> findByAccountNumberOrderByCreatedAtDesc(String accountNumber);

    List<SalaryTransaction> findByCompanyNameOrderByCreatedAtDesc(String companyName);

    List<SalaryTransaction> findByStatus(String status);
}
