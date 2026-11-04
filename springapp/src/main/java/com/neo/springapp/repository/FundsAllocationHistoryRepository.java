package com.neo.springapp.repository;

import com.neo.springapp.model.FundsAllocationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FundsAllocationHistoryRepository extends JpaRepository<FundsAllocationHistory, Long> {
    List<FundsAllocationHistory> findByAllocationRecordIdOrderByChangedAtDesc(Long allocationRecordId);
}
