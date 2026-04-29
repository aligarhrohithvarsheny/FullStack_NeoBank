package com.neo.springapp.repository;

import com.neo.springapp.model.EmiPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmiPaymentRepository extends JpaRepository<EmiPayment, Long> {
    
    // Find all EMIs for a loan
    List<EmiPayment> findByLoanIdOrderByEmiNumberAsc(Long loanId);
    
    // Find all EMIs for a loan account number
    List<EmiPayment> findByLoanAccountNumberOrderByEmiNumberAsc(String loanAccountNumber);
    
    // Find EMIs by account number (user's savings account)
    List<EmiPayment> findByAccountNumberOrderByDueDateDesc(String accountNumber);
    
    // Find EMIs by status
    List<EmiPayment> findByStatusOrderByDueDateAsc(String status);
    
    // Find pending EMIs for a loan
    List<EmiPayment> findByLoanIdAndStatusOrderByDueDateAsc(Long loanId, String status);
    
    // Find overdue EMIs (due date passed and status is Pending)
    @Query("SELECT e FROM EmiPayment e WHERE e.dueDate < :currentDate AND e.status = 'Pending'")
    List<EmiPayment> findOverdueEmis(@Param("currentDate") LocalDate currentDate);
    
    // Find upcoming EMIs (due date within next 7 days)
    @Query("SELECT e FROM EmiPayment e WHERE e.dueDate BETWEEN :today AND :nextWeek AND e.status = 'Pending'")
    List<EmiPayment> findUpcomingEmis(@Param("today") LocalDate today, @Param("nextWeek") LocalDate nextWeek);
    
    // Find specific EMI by loan ID and EMI number
    Optional<EmiPayment> findByLoanIdAndEmiNumber(Long loanId, Integer emiNumber);
    
    // Count paid EMIs for a loan
    @Query("SELECT COUNT(e) FROM EmiPayment e WHERE e.loanId = :loanId AND e.status = 'Paid'")
    Long countPaidEmisByLoanId(@Param("loanId") Long loanId);
    
    // Count total EMIs for a loan
    @Query("SELECT COUNT(e) FROM EmiPayment e WHERE e.loanId = :loanId")
    Long countTotalEmisByLoanId(@Param("loanId") Long loanId);
    
    // Find next due EMI for a loan
    @Query("SELECT e FROM EmiPayment e WHERE e.loanId = :loanId AND e.status = 'Pending' ORDER BY e.dueDate ASC")
    List<EmiPayment> findNextDueEmi(@Param("loanId") Long loanId);
}

