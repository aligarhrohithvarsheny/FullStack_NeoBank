package com.neo.springapp.repository;

import com.neo.springapp.model.GuestInsurancePayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GuestInsurancePaymentRepository extends JpaRepository<GuestInsurancePayment, Long> {
    List<GuestInsurancePayment> findByApplicationIdOrderByPaidAtDesc(Long applicationId);
}
