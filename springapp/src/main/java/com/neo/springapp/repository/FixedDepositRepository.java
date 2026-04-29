package com.neo.springapp.repository;

import com.neo.springapp.model.FixedDeposit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface FixedDepositRepository extends JpaRepository<FixedDeposit, Long> {
    
    List<FixedDeposit> findByAccountNumber(String accountNumber);
    
    List<FixedDeposit> findByStatus(String status);
    
    List<FixedDeposit> findByAccountNumberAndStatus(String accountNumber, String status);
    
    Optional<FixedDeposit> findByFdAccountNumber(String fdAccountNumber);
    
    List<FixedDeposit> findByMaturityDateBetween(java.time.LocalDate startDate, java.time.LocalDate endDate);
    
    List<FixedDeposit> findByStatusIn(List<String> statuses);
    
    List<FixedDeposit> findByIsMaturedFalseAndMaturityDateBefore(java.time.LocalDate date);
}

