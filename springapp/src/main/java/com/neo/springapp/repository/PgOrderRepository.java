package com.neo.springapp.repository;

import com.neo.springapp.model.PgOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PgOrderRepository extends JpaRepository<PgOrder, Long> {
    Optional<PgOrder> findByOrderId(String orderId);
    List<PgOrder> findByMerchantIdOrderByCreatedAtDesc(String merchantId);
    Page<PgOrder> findByMerchantId(String merchantId, Pageable pageable);
    List<PgOrder> findByStatus(String status);
    Page<PgOrder> findByMerchantIdAndStatus(String merchantId, String status, Pageable pageable);

    @Query("SELECT COUNT(o) FROM PgOrder o WHERE o.merchantId = :merchantId AND o.status = :status")
    long countByMerchantIdAndStatus(String merchantId, String status);
}
