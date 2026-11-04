package com.neo.springapp.repository;

import com.neo.springapp.model.AllocationAccountOperationRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AllocationAccountOperationRequestRepository extends JpaRepository<AllocationAccountOperationRequest, Long> {
    List<AllocationAccountOperationRequest> findByAllocationRecordIdOrderByCreatedAtDesc(Long allocationRecordId);

    Optional<AllocationAccountOperationRequest> findByIdAndAllocationRecordId(Long id, Long allocationRecordId);
}
