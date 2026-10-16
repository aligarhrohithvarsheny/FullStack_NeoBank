package com.neo.springapp.repository;

import com.neo.springapp.model.CreditCardServiceRequestHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CreditCardServiceRequestHistoryRepository extends JpaRepository<CreditCardServiceRequestHistory, Long> {
    List<CreditCardServiceRequestHistory> findByServiceRequestIdOrderByCreatedAtAsc(Long serviceRequestId);
}
