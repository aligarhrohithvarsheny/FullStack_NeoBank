package com.neo.springapp.repository;

import com.neo.springapp.model.MerchantTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MerchantTransactionRepository extends JpaRepository<MerchantTransaction, Long> {
    List<MerchantTransaction> findByMerchantIdOrderByCreatedAtDesc(String merchantId);
    Page<MerchantTransaction> findByMerchantId(String merchantId, Pageable pageable);
    List<MerchantTransaction> findByDeviceIdOrderByCreatedAtDesc(String deviceId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM MerchantTransaction t WHERE t.merchantId = :merchantId")
    java.math.BigDecimal getTotalAmountByMerchantId(String merchantId);

    @Query("SELECT COUNT(t) FROM MerchantTransaction t WHERE t.merchantId = :merchantId")
    long countByMerchantId(String merchantId);
}
