package com.neo.springapp.repository;

import com.neo.springapp.model.CreditCardRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CreditCardRequestRepository extends JpaRepository<CreditCardRequest, Long> {
    List<CreditCardRequest> findByStatus(String status);
}
