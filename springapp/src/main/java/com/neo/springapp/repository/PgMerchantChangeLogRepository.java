package com.neo.springapp.repository;

import com.neo.springapp.model.PgMerchantChangeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PgMerchantChangeLogRepository extends JpaRepository<PgMerchantChangeLog, Long> {
    List<PgMerchantChangeLog> findByMerchantIdOrderByChangedAtDesc(String merchantId);
}
