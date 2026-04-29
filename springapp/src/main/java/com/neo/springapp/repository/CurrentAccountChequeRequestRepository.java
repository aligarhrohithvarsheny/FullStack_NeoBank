package com.neo.springapp.repository;

import com.neo.springapp.model.CurrentAccountChequeRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CurrentAccountChequeRequestRepository extends JpaRepository<CurrentAccountChequeRequest, Long> {
    List<CurrentAccountChequeRequest> findByAccountNumberOrderByRequestedAtDesc(String accountNumber);
    List<CurrentAccountChequeRequest> findByStatus(String status);
}
