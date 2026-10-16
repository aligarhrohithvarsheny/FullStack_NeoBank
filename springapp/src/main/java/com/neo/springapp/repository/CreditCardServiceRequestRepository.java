package com.neo.springapp.repository;

import com.neo.springapp.model.CreditCardServiceRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CreditCardServiceRequestRepository extends JpaRepository<CreditCardServiceRequest, Long> {
    List<CreditCardServiceRequest> findByAccountNumberOrderByCreatedAtDesc(String accountNumber);
    Optional<CreditCardServiceRequest> findByRequestNumber(String requestNumber);
}
