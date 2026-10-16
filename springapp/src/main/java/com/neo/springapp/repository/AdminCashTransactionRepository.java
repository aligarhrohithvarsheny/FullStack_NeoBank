package com.neo.springapp.repository;

import com.neo.springapp.model.AdminCashTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AdminCashTransactionRepository extends JpaRepository<AdminCashTransaction, Long> {

    List<AdminCashTransaction> findAllByOrderByPerformedAtDesc();

    List<AdminCashTransaction> findByAccountTypeAndAccountNumberOrderByPerformedAtDesc(
            String accountType, String accountNumber);

    @Query("SELECT UPPER(t.operationType), COALESCE(SUM(t.amount), 0) FROM AdminCashTransaction t " +
           "WHERE UPPER(t.status) = 'COMPLETED' GROUP BY UPPER(t.operationType)")
    List<Object[]> summarizeCompletedOperations();

    @Query("SELECT FUNCTION('DATE', t.performedAt), UPPER(t.operationType), COALESCE(SUM(t.amount), 0) " +
           "FROM AdminCashTransaction t WHERE t.performedAt >= :start AND t.performedAt < :end " +
           "AND UPPER(t.status) = 'COMPLETED' " +
           "GROUP BY FUNCTION('DATE', t.performedAt), UPPER(t.operationType) " +
           "ORDER BY FUNCTION('DATE', t.performedAt)")
    List<Object[]> summarizeDailyOperations(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT FUNCTION('YEAR', t.performedAt), FUNCTION('MONTH', t.performedAt), UPPER(t.operationType), COALESCE(SUM(t.amount), 0) " +
           "FROM AdminCashTransaction t WHERE t.performedAt >= :start AND t.performedAt < :end " +
           "AND UPPER(t.status) = 'COMPLETED' " +
           "GROUP BY FUNCTION('YEAR', t.performedAt), FUNCTION('MONTH', t.performedAt), UPPER(t.operationType) " +
           "ORDER BY FUNCTION('YEAR', t.performedAt), FUNCTION('MONTH', t.performedAt)")
    List<Object[]> summarizeMonthlyOperations(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT FUNCTION('YEAR', t.performedAt), UPPER(t.operationType), COALESCE(SUM(t.amount), 0) " +
           "FROM AdminCashTransaction t WHERE t.performedAt >= :start AND t.performedAt < :end " +
           "AND UPPER(t.status) = 'COMPLETED' " +
           "GROUP BY FUNCTION('YEAR', t.performedAt), UPPER(t.operationType) " +
           "ORDER BY FUNCTION('YEAR', t.performedAt)")
    List<Object[]> summarizeYearlyOperations(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT t FROM AdminCashTransaction t WHERE " +
           "LOWER(t.refId) LIKE LOWER(CONCAT('%', :term, '%')) OR " +
           "LOWER(t.accountNumber) LIKE LOWER(CONCAT('%', :term, '%')) OR " +
           "LOWER(t.accountHolderName) LIKE LOWER(CONCAT('%', :term, '%')) OR " +
           "LOWER(t.description) LIKE LOWER(CONCAT('%', :term, '%')) " +
           "ORDER BY t.performedAt DESC")
    List<AdminCashTransaction> search(@Param("term") String term);
}
