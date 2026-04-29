package com.neo.springapp.repository;

import com.neo.springapp.model.CurrentAccountUpiPayment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CurrentAccountUpiPaymentRepository extends JpaRepository<CurrentAccountUpiPayment, Long> {

    Optional<CurrentAccountUpiPayment> findByTxnId(String txnId);

    List<CurrentAccountUpiPayment> findByTxnIdContainingIgnoreCaseOrderByCreatedAtDesc(String txnId);

    List<CurrentAccountUpiPayment> findByAccountNumberOrderByCreatedAtDesc(String accountNumber);

    Page<CurrentAccountUpiPayment> findByAccountNumberOrderByCreatedAtDesc(String accountNumber, Pageable pageable);

    List<CurrentAccountUpiPayment> findByStatus(String status);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM CurrentAccountUpiPayment p WHERE p.accountNumber = ?1 AND p.status = 'SUCCESS'")
    Double getTotalReceivedByAccount(String accountNumber);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM CurrentAccountUpiPayment p WHERE p.accountNumber = ?1 AND p.status = 'SUCCESS' AND p.createdAt >= ?2")
    Double getTodayReceivedByAccount(String accountNumber, LocalDateTime startOfDay);

    @Query("SELECT COUNT(p) FROM CurrentAccountUpiPayment p WHERE p.accountNumber = ?1 AND p.status = 'SUCCESS' AND p.createdAt >= ?2")
    Long getTodayTxnCountByAccount(String accountNumber, LocalDateTime startOfDay);

    @Query("SELECT COUNT(p) FROM CurrentAccountUpiPayment p WHERE p.status = 'SUCCESS'")
    Long getTotalSuccessfulPayments();

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM CurrentAccountUpiPayment p WHERE p.status = 'SUCCESS'")
    Double getTotalUpiVolume();

    @Query("SELECT COUNT(DISTINCT p.accountNumber) FROM CurrentAccountUpiPayment p WHERE p.status = 'SUCCESS'")
    Long getActiveUpiAccounts();

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM CurrentAccountUpiPayment p WHERE p.status = 'SUCCESS' AND p.createdAt >= ?1")
    Double getTodayTotalVolume(LocalDateTime startOfDay);

    List<CurrentAccountUpiPayment> findTop50ByOrderByCreatedAtDesc();
}
