package com.neo.springapp.repository;

import com.neo.springapp.model.CreditCardEmiPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CreditCardEmiPlanRepository extends JpaRepository<CreditCardEmiPlan, Long> {
    List<CreditCardEmiPlan> findByCreditCardIdOrderByCreatedAtDesc(Long creditCardId);
    List<CreditCardEmiPlan> findAllByOrderByCreatedAtDesc();
}
