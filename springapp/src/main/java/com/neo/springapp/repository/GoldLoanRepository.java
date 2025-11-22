package com.neo.springapp.repository;

import com.neo.springapp.model.GoldLoan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GoldLoanRepository extends JpaRepository<GoldLoan, Long> {
    List<GoldLoan> findByAccountNumber(String accountNumber);
    Optional<GoldLoan> findByLoanAccountNumber(String loanAccountNumber);
    List<GoldLoan> findByStatus(String status);
}

