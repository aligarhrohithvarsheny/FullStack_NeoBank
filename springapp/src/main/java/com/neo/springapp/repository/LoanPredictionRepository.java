package com.neo.springapp.repository;

import com.neo.springapp.model.LoanPrediction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LoanPredictionRepository extends JpaRepository<LoanPrediction, Long> {
    
    // Find predictions by account number
    List<LoanPrediction> findByAccountNumber(String accountNumber);
    
    // Find predictions by PAN
    List<LoanPrediction> findByPan(String pan);
    
    // Find predictions by loan type
    List<LoanPrediction> findByLoanType(String loanType);
    
    // Find predictions by prediction result
    List<LoanPrediction> findByPredictionResult(String predictionResult);
    
    // Find predictions by account number and loan type
    List<LoanPrediction> findByAccountNumberAndLoanType(String accountNumber, String loanType);
    
    // Find recent predictions
    @Query("SELECT lp FROM LoanPrediction lp ORDER BY lp.predictionDate DESC")
    Page<LoanPrediction> findRecentPredictions(Pageable pageable);
    
    // Find predictions by date range
    @Query("SELECT lp FROM LoanPrediction lp WHERE lp.predictionDate BETWEEN :startDate AND :endDate ORDER BY lp.predictionDate DESC")
    List<LoanPrediction> findByPredictionDateRange(@Param("startDate") LocalDateTime startDate, 
                                                   @Param("endDate") LocalDateTime endDate);
    
    // Find all predictions ordered by date
    @Query("SELECT lp FROM LoanPrediction lp ORDER BY lp.predictionDate DESC")
    List<LoanPrediction> findAllOrderByPredictionDateDesc();
    
    // Count predictions by result
    Long countByPredictionResult(String predictionResult);
    
    // Count predictions by loan type
    Long countByLoanType(String loanType);
}

