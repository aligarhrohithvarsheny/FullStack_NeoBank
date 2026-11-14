package com.neo.springapp.repository;

import com.neo.springapp.model.CreditCardNumberHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CreditCardNumberHistoryRepository extends JpaRepository<CreditCardNumberHistory, Long> {
    List<CreditCardNumberHistory> findByRequestIdOrderByGeneratedAtDesc(Long requestId);
}
