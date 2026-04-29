package com.neo.springapp.repository;

import com.neo.springapp.model.SoundboxTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SoundboxTransactionRepository extends JpaRepository<SoundboxTransaction, Long> {

    List<SoundboxTransaction> findByAccountNumberOrderByCreatedAtDesc(String accountNumber);

    Page<SoundboxTransaction> findByAccountNumber(String accountNumber, Pageable pageable);

    List<SoundboxTransaction> findByDeviceId(String deviceId);

    List<SoundboxTransaction> findByStatus(String status);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM SoundboxTransaction t WHERE t.accountNumber = :accountNumber AND t.txnType = 'CREDIT' AND t.status = 'SUCCESS'")
    Double getTotalReceivedByAccount(@Param("accountNumber") String accountNumber);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM SoundboxTransaction t WHERE t.accountNumber = :accountNumber AND t.txnType = 'CREDIT' AND t.status = 'SUCCESS' AND t.createdAt >= :startOfDay")
    Double getTodayReceivedByAccount(@Param("accountNumber") String accountNumber, @Param("startOfDay") LocalDateTime startOfDay);

    @Query("SELECT COUNT(t) FROM SoundboxTransaction t WHERE t.accountNumber = :accountNumber AND t.createdAt >= :startOfDay")
    long countTodayTransactions(@Param("accountNumber") String accountNumber, @Param("startOfDay") LocalDateTime startOfDay);

    @Query("SELECT COUNT(t) FROM SoundboxTransaction t WHERE t.status = 'SUCCESS'")
    long countSuccessfulTransactions();

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM SoundboxTransaction t WHERE t.status = 'SUCCESS'")
    Double getTotalTransactionAmount();

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM SoundboxTransaction t WHERE t.status = 'SUCCESS' AND t.createdAt >= :startOfDay")
    Double getTodayTotalAmount(@Param("startOfDay") LocalDateTime startOfDay);
}
