package com.neo.springapp.repository;

import com.neo.springapp.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
    // JPQL Query to find transactions by account number with pagination
    @Query("SELECT t FROM Transaction t WHERE t.accountNumber = :accountNumber ORDER BY t.date DESC")
    Page<Transaction> findByAccountNumberOrderByDateDesc(@Param("accountNumber") String accountNumber, Pageable pageable);
    
    // JPQL Query to find transactions by user name with pagination
    @Query("SELECT t FROM Transaction t WHERE t.userName = :userName ORDER BY t.date DESC")
    Page<Transaction> findByUserNameOrderByDateDesc(@Param("userName") String userName, Pageable pageable);
    
    // JPQL Query to find transactions by type with pagination
    @Query("SELECT t FROM Transaction t WHERE t.type = :type ORDER BY t.date DESC")
    Page<Transaction> findByTypeOrderByDateDesc(@Param("type") String type, Pageable pageable);
    
    // JPQL Query to find transactions by status with pagination
    @Query("SELECT t FROM Transaction t WHERE t.status = :status ORDER BY t.date DESC")
    Page<Transaction> findByStatusOrderByDateDesc(@Param("status") String status, Pageable pageable);
    
    // JPQL Query to find transactions by date range with pagination
    @Query("SELECT t FROM Transaction t WHERE t.date BETWEEN :startDate AND :endDate ORDER BY t.date DESC")
    Page<Transaction> findByDateBetweenOrderByDateDesc(@Param("startDate") LocalDateTime startDate, 
                                                     @Param("endDate") LocalDateTime endDate, 
                                                     Pageable pageable);
    
    // JPQL Query to find transactions by merchant with pagination
    @Query("SELECT t FROM Transaction t WHERE LOWER(t.merchant) LIKE LOWER(CONCAT('%', :merchant, '%')) ORDER BY t.date DESC")
    Page<Transaction> findByMerchantContainingIgnoreCaseOrderByDateDesc(@Param("merchant") String merchant, Pageable pageable);
    
    // JPQL Query to find transactions by description with pagination
    @Query("SELECT t FROM Transaction t WHERE LOWER(t.description) LIKE LOWER(CONCAT('%', :description, '%')) ORDER BY t.date DESC")
    Page<Transaction> findByDescriptionContainingIgnoreCaseOrderByDateDesc(@Param("description") String description, Pageable pageable);
    
    // JPQL Query to search transactions by multiple fields
    @Query("SELECT t FROM Transaction t WHERE " +
           "LOWER(t.userName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(t.accountNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(t.merchant) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(t.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(t.transactionId) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Transaction> searchTransactions(@Param("searchTerm") String searchTerm, Pageable pageable);
    
    // JPQL Query to find transactions by account number and date range
    @Query("SELECT t FROM Transaction t WHERE t.accountNumber = :accountNumber AND t.date BETWEEN :startDate AND :endDate ORDER BY t.date DESC")
    Page<Transaction> findByAccountNumberAndDateBetweenOrderByDateDesc(@Param("accountNumber") String accountNumber,
                                                                      @Param("startDate") LocalDateTime startDate,
                                                                      @Param("endDate") LocalDateTime endDate,
                                                                      Pageable pageable);
    
    // JPQL Query to get transaction summary by account
    @Query("SELECT COUNT(t), SUM(t.amount) FROM Transaction t WHERE t.accountNumber = :accountNumber AND t.type = :type")
    Object[] getTransactionSummaryByAccountAndType(@Param("accountNumber") String accountNumber, @Param("type") String type);
    
    // JPQL Query to get recent transactions (mini statement)
    @Query("SELECT t FROM Transaction t WHERE t.accountNumber = :accountNumber ORDER BY t.date DESC")
    List<Transaction> findTop5ByAccountNumberOrderByDateDesc(@Param("accountNumber") String accountNumber, Pageable pageable);
    
    // JPQL Query to find all transactions with sorting options
    @Query("SELECT t FROM Transaction t ORDER BY " +
           "CASE WHEN :sortBy = 'date' THEN t.date END DESC, " +
           "CASE WHEN :sortBy = 'amount' THEN t.amount END DESC, " +
           "CASE WHEN :sortBy = 'merchant' THEN t.merchant END ASC")
    Page<Transaction> findAllWithSorting(@Param("sortBy") String sortBy, Pageable pageable);
}
