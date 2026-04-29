package com.neo.springapp.repository;

import com.neo.springapp.model.DepositRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DepositRequestRepository extends JpaRepository<DepositRequest, Long> {
    List<DepositRequest> findByStatusOrderByCreatedAtDesc(String status);
    List<DepositRequest> findAllByOrderByCreatedAtDesc();
    List<DepositRequest> findByAccountNumberOrderByCreatedAtDesc(String accountNumber);
}

