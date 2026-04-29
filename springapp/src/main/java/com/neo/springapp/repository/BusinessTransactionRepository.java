package com.neo.springapp.repository;

import com.neo.springapp.model.BusinessTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BusinessTransactionRepository extends JpaRepository<BusinessTransaction, Long> {

    List<BusinessTransaction> findByAccountNumber(String accountNumber);

    Page<BusinessTransaction> findByAccountNumberOrderByDateDesc(String accountNumber, Pageable pageable);

    List<BusinessTransaction> findByTxnType(String txnType);

    Page<BusinessTransaction> findByTxnTypeOrderByDateDesc(String txnType, Pageable pageable);

    @Query("SELECT b FROM BusinessTransaction b WHERE b.accountNumber = :accountNumber AND b.date BETWEEN :startDate AND :endDate ORDER BY b.date DESC")
    List<BusinessTransaction> findByAccountNumberAndDateRange(
            @Param("accountNumber") String accountNumber,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT SUM(b.amount) FROM BusinessTransaction b WHERE b.accountNumber = :accountNumber AND b.txnType = 'Credit' AND b.status = 'Completed'")
    Double getTotalCredits(@Param("accountNumber") String accountNumber);

    @Query("SELECT SUM(b.amount) FROM BusinessTransaction b WHERE b.accountNumber = :accountNumber AND b.txnType = 'Debit' AND b.status = 'Completed'")
    Double getTotalDebits(@Param("accountNumber") String accountNumber);

    @Query("SELECT COUNT(b) FROM BusinessTransaction b WHERE b.accountNumber = :accountNumber AND cast(b.date as LocalDate) = local date")
    Long getTodayTransactionCount(@Param("accountNumber") String accountNumber);

    @Query("SELECT b FROM BusinessTransaction b WHERE b.accountNumber = :accountNumber ORDER BY b.date DESC")
    List<BusinessTransaction> findRecentTransactions(@Param("accountNumber") String accountNumber, Pageable pageable);

    @Query("SELECT b FROM BusinessTransaction b WHERE " +
           "LOWER(b.txnId) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(b.accountNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(b.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(b.recipientName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<BusinessTransaction> searchTransactions(@Param("searchTerm") String searchTerm, Pageable pageable);
}
