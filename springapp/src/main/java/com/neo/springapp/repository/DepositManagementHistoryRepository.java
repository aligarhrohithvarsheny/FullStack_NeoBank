package com.neo.springapp.repository;

import com.neo.springapp.model.DepositManagementHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DepositManagementHistoryRepository extends JpaRepository<DepositManagementHistory, Long> {
    List<DepositManagementHistory> findByProductTypeAndProductIdOrderByOccurredAtDesc(
            String productType, Long productId);
}
