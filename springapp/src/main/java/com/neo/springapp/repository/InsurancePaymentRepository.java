package com.neo.springapp.repository;

import com.neo.springapp.model.InsuranceApplication;
import com.neo.springapp.model.InsurancePayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface InsurancePaymentRepository extends JpaRepository<InsurancePayment, Long> {

    List<InsurancePayment> findByUserId(Long userId);

    List<InsurancePayment> findByAccountNumber(String accountNumber);

    List<InsurancePayment> findByApplication(InsuranceApplication application);

    List<InsurancePayment> findByPaymentDateBetween(LocalDateTime from, LocalDateTime to);
}

