package com.neo.springapp.repository;

import com.neo.springapp.model.SubscriptionPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SubscriptionPaymentRepository extends JpaRepository<SubscriptionPayment, Long> {

    List<SubscriptionPayment> findByEmployeeId(String employeeId);

    List<SubscriptionPayment> findBySalaryAccountNumber(String salaryAccountNumber);

    List<SubscriptionPayment> findByStatus(String status);

    List<SubscriptionPayment> findByStatusAndNextBillingDateLessThanEqual(String status, LocalDate date);

    List<SubscriptionPayment> findBySalaryAccountNumberAndStatus(String salaryAccountNumber, String status);
}
