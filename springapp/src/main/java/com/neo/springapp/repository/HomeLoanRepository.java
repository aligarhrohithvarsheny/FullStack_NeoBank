package com.neo.springapp.repository;

import com.neo.springapp.model.HomeLoan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HomeLoanRepository extends JpaRepository<HomeLoan, Long> {
    List<HomeLoan> findByAccountNumberOrderByApplicationDateDesc(String accountNumber);
    Optional<HomeLoan> findByApplicationId(String applicationId);
    Optional<HomeLoan> findByLoanAccountNumber(String loanAccountNumber);
    List<HomeLoan> findByStatus(String status);
    List<HomeLoan> findAllByOrderByApplicationDateDesc();
}
