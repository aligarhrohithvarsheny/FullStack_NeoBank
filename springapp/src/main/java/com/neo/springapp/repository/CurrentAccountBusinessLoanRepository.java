package com.neo.springapp.repository;

import com.neo.springapp.model.CurrentAccountBusinessLoan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CurrentAccountBusinessLoanRepository extends JpaRepository<CurrentAccountBusinessLoan, Long> {
    List<CurrentAccountBusinessLoan> findByAccountNumberOrderByAppliedAtDesc(String accountNumber);
    List<CurrentAccountBusinessLoan> findByStatus(String status);
    Optional<CurrentAccountBusinessLoan> findByApplicationId(String applicationId);
    long countByAccountNumberAndStatusIn(String accountNumber, List<String> statuses);
}
