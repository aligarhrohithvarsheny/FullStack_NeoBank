package com.neo.springapp.repository;

import com.neo.springapp.model.Loan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {
    
    // Basic queries
    List<Loan> findByAccountNumber(String accountNumber);
    List<Loan> findByUserEmail(String userEmail);
    List<Loan> findByStatus(String status);
    Page<Loan> findByStatus(String status, Pageable pageable);
    
    // Loan type queries
    List<Loan> findByType(String type);
    List<Loan> findByTypeAndStatus(String type, String status);
    
    // Amount range queries
    @Query("SELECT l FROM Loan l WHERE l.amount BETWEEN :minAmount AND :maxAmount")
    List<Loan> findByAmountRange(@Param("minAmount") Double minAmount, @Param("maxAmount") Double maxAmount);
    
    // Date range queries
    @Query("SELECT l FROM Loan l WHERE l.applicationDate BETWEEN :startDate AND :endDate")
    List<Loan> findByApplicationDateRange(@Param("startDate") LocalDateTime startDate, 
                                          @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT l FROM Loan l WHERE l.approvalDate BETWEEN :startDate AND :endDate")
    List<Loan> findByApprovalDateRange(@Param("startDate") LocalDateTime startDate, 
                                       @Param("endDate") LocalDateTime endDate);
    
    // Search queries
    @Query("SELECT l FROM Loan l WHERE " +
           "LOWER(l.userName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(l.userEmail) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(l.accountNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(l.loanAccountNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(l.type) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Loan> searchLoans(@Param("searchTerm") String searchTerm, Pageable pageable);
    
    // Statistics queries
    @Query("SELECT COUNT(l) FROM Loan l WHERE l.status = :status")
    Long countByStatus(@Param("status") String status);
    
    @Query("SELECT SUM(l.amount) FROM Loan l WHERE l.status = 'Approved'")
    Double getTotalApprovedLoanAmount();
    
    @Query("SELECT AVG(l.amount) FROM Loan l WHERE l.status = 'Approved'")
    Double getAverageLoanAmount();
    
    @Query("SELECT l FROM Loan l ORDER BY l.applicationDate DESC")
    List<Loan> findRecentLoans(Pageable pageable);
    
    // Admin queries
    @Query("SELECT l FROM Loan l WHERE l.approvedBy = :adminName")
    List<Loan> findByApprovedBy(@Param("adminName") String adminName);
    
    // Pending loans for admin review
    @Query("SELECT l FROM Loan l WHERE l.status = 'Pending' ORDER BY l.applicationDate ASC")
    List<Loan> findPendingLoansForReview();
}
