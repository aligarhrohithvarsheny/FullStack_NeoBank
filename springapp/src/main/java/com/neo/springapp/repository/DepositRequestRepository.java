package com.neo.springapp.repository;

import com.neo.springapp.model.DepositRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepositRequestRepository extends JpaRepository<DepositRequest, Long> {
    List<DepositRequest> findByStatusOrderByCreatedAtDesc(String status);
    Optional<DepositRequest> findByRequestId(String requestId);
    List<DepositRequest> findAllByOrderByCreatedAtDesc();
    List<DepositRequest> findByAccountNumberOrderByCreatedAtDesc(String accountNumber);
}

