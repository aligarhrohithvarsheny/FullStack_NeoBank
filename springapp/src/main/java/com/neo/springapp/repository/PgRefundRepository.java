package com.neo.springapp.repository;

import com.neo.springapp.model.PgRefund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface PgRefundRepository extends JpaRepository<PgRefund, Long> {
    Optional<PgRefund> findByRefundId(String refundId);
    List<PgRefund> findByTransactionId(String transactionId);
    List<PgRefund> findByMerchantIdOrderByCreatedAtDesc(String merchantId);
    List<PgRefund> findByOrderId(String orderId);
    List<PgRefund> findByStatus(String status);

    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM PgRefund r WHERE r.transactionId = :transactionId AND r.status = 'PROCESSED'")
    BigDecimal getTotalRefundedForTransaction(String transactionId);
}
