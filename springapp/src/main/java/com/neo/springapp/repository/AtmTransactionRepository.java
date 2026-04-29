package com.neo.springapp.repository;

import com.neo.springapp.model.AtmTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AtmTransactionRepository extends JpaRepository<AtmTransaction, Long> {

    Optional<AtmTransaction> findByTransactionRef(String transactionRef);

    Page<AtmTransaction> findByAtmIdOrderByCreatedAtDesc(String atmId, Pageable pageable);

    Page<AtmTransaction> findByAccountNumberOrderByCreatedAtDesc(String accountNumber, Pageable pageable);

    Page<AtmTransaction> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    Page<AtmTransaction> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<AtmTransaction> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime start, LocalDateTime end, Pageable pageable);

    List<AtmTransaction> findByAtmIdAndCreatedAtBetween(String atmId, LocalDateTime start, LocalDateTime end);

    List<AtmTransaction> findByAccountNumberAndCreatedAtBetween(String accountNumber, LocalDateTime start, LocalDateTime end);

    @Query("SELECT COUNT(t) FROM AtmTransaction t WHERE t.status = :status")
    long countByStatus(String status);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM AtmTransaction t WHERE t.transactionType = 'WITHDRAWAL' AND t.status = 'SUCCESS'")
    Double getTotalWithdrawals();

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM AtmTransaction t WHERE t.transactionType = 'WITHDRAWAL' AND t.status = 'SUCCESS' AND t.createdAt >= :since")
    Double getTotalWithdrawalsSince(LocalDateTime since);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM AtmTransaction t WHERE t.atmId = :atmId AND t.transactionType = 'WITHDRAWAL' AND t.status = 'SUCCESS' AND t.createdAt >= :since")
    Double getTotalWithdrawalsForAtmSince(String atmId, LocalDateTime since);

    @Query("SELECT COUNT(t) FROM AtmTransaction t WHERE t.createdAt >= :since")
    long countTransactionsSince(LocalDateTime since);

    @Query("SELECT t FROM AtmTransaction t WHERE t.receiptGenerated = true ORDER BY t.createdAt DESC")
    Page<AtmTransaction> findWithReceipts(Pageable pageable);

    List<AtmTransaction> findTop10ByAtmIdOrderByCreatedAtDesc(String atmId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM AtmTransaction t WHERE t.accountNumber = :accountNumber AND t.transactionType = 'WITHDRAWAL' AND t.status = 'SUCCESS' AND t.createdAt >= :since")
    Double getUserDailyWithdrawal(String accountNumber, LocalDateTime since);
}
