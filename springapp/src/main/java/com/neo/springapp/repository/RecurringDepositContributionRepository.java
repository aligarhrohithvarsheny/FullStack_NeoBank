package com.neo.springapp.repository;

import com.neo.springapp.model.RecurringDepositContribution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecurringDepositContributionRepository extends JpaRepository<RecurringDepositContribution, Long> {
    boolean existsByIdempotencyKey(String idempotencyKey);
    List<RecurringDepositContribution> findByRecurringDepositIdOrderByPostedDateDesc(Long recurringDepositId);
}
