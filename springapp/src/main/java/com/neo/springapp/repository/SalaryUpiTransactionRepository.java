package com.neo.springapp.repository;

import com.neo.springapp.model.SalaryUpiTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SalaryUpiTransactionRepository extends JpaRepository<SalaryUpiTransaction, Long> {

    List<SalaryUpiTransaction> findBySalaryAccountIdOrderByCreatedAtDesc(Long salaryAccountId);

    List<SalaryUpiTransaction> findByAccountNumberOrderByCreatedAtDesc(String accountNumber);
}
