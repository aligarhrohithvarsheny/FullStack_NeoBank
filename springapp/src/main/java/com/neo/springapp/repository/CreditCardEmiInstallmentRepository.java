package com.neo.springapp.repository;

import com.neo.springapp.model.CreditCardEmiInstallment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CreditCardEmiInstallmentRepository extends JpaRepository<CreditCardEmiInstallment, Long> {
    List<CreditCardEmiInstallment> findByPlanIdOrderByInstallmentNumberAsc(Long planId);
    List<CreditCardEmiInstallment> findByBillIdOrderByDueDateAsc(Long billId);
}
