package com.neo.springapp.repository;

import com.neo.springapp.model.PgPaymentSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PgPaymentSessionRepository extends JpaRepository<PgPaymentSession, Long> {
    Optional<PgPaymentSession> findBySessionId(String sessionId);
    Optional<PgPaymentSession> findByOrderId(String orderId);
    List<PgPaymentSession> findByMerchantIdOrderByCreatedAtDesc(String merchantId);
    List<PgPaymentSession> findByStatus(String status);
}
