package com.neo.springapp.repository;

import com.neo.springapp.entity.AllocationAccountMappingHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AllocationAccountMappingHistoryRepository extends JpaRepository<AllocationAccountMappingHistory, Long> {
    List<AllocationAccountMappingHistory> findByAllocationIdOrderByPerformedAtDesc(Long allocationId);
}
