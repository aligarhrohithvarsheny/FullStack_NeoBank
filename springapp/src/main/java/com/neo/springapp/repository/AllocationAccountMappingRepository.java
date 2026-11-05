package com.neo.springapp.repository;

import com.neo.springapp.entity.AllocationAccountMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AllocationAccountMappingRepository extends JpaRepository<AllocationAccountMapping, Long> {
    List<AllocationAccountMapping> findByAllocationIdOrderByUpdatedAtDesc(Long allocationId);
    List<AllocationAccountMapping> findByAllocationIdAndMappingStatusOrderByUpdatedAtDesc(Long allocationId, String mappingStatus);
    Optional<AllocationAccountMapping> findByAllocationIdAndAccountNumber(Long allocationId, String accountNumber);
    Optional<AllocationAccountMapping> findFirstByAllocationIdAndAccountNumberAndMappingStatus(Long allocationId, String accountNumber, String mappingStatus);
}
