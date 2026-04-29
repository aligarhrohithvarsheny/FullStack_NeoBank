package com.neo.springapp.repository;

import com.neo.springapp.model.PgTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PgTransactionRepository extends JpaRepository<PgTransaction, Long> {
    Optional<PgTransaction> findByTransactionId(String transactionId);
    Optional<PgTransaction> findByOrderId(String orderId);
    List<PgTransaction> findByMerchantIdOrderByCreatedAtDesc(String merchantId);
    Page<PgTransaction> findByMerchantId(String merchantId, Pageable pageable);
    List<PgTransaction> findByStatus(String status);
    List<PgTransaction> findByFraudFlaggedTrue();
    Page<PgTransaction> findByMerchantIdAndStatus(String merchantId, String status, Pageable pageable);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM PgTransaction t WHERE t.merchantId = :merchantId AND t.status = 'SUCCESS'")
    BigDecimal getTotalVolumeByMerchantId(String merchantId);

    @Query("SELECT COUNT(t) FROM PgTransaction t WHERE t.merchantId = :merchantId AND t.status = 'SUCCESS'")
    long countSuccessfulByMerchantId(String merchantId);

    @Query("SELECT COUNT(t) FROM PgTransaction t WHERE t.merchantId = :merchantId AND t.status = 'FAILED'")
    long countFailedByMerchantId(String merchantId);

    @Query("SELECT COUNT(t) FROM PgTransaction t WHERE t.payerAccount = :payerAccount AND t.createdAt > :since")
    long countRecentByPayer(String payerAccount, LocalDateTime since);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM PgTransaction t WHERE t.payerAccount = :payerAccount AND t.createdAt > :since AND t.status = 'SUCCESS'")
    BigDecimal getDailyVolumeByPayer(String payerAccount, LocalDateTime since);

    @Query("SELECT COALESCE(SUM(t.fee), 0) FROM PgTransaction t WHERE t.merchantId = :merchantId AND t.status = 'SUCCESS'")
    BigDecimal getTotalFeesByMerchantId(String merchantId);
}
