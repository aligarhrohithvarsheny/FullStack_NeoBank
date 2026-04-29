package com.neo.springapp.repository;

import com.neo.springapp.model.ScheduledPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ScheduledPaymentRepository extends JpaRepository<ScheduledPayment, Long> {

    List<ScheduledPayment> findByAccountNumber(String accountNumber);

    List<ScheduledPayment> findByAccountNumberAndStatus(String accountNumber, String status);

    List<ScheduledPayment> findByStatusAndNextPaymentDateLessThanEqual(String status, LocalDate date);

    List<ScheduledPayment> findByStatus(String status);
}
