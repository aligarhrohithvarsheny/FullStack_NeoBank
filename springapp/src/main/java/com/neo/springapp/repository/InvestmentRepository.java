package com.neo.springapp.repository;

import com.neo.springapp.model.Investment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvestmentRepository extends JpaRepository<Investment, Long> {
    
    List<Investment> findByAccountNumber(String accountNumber);
    
    List<Investment> findByStatus(String status);
    
    List<Investment> findByAccountNumberAndStatus(String accountNumber, String status);

    Optional<Investment> findByTransactionId(String transactionId);
    
    List<Investment> findByInvestmentDateBetween(java.time.LocalDate startDate, java.time.LocalDate endDate);
    
    List<Investment> findByStatusIn(List<String> statuses);
}
