package com.neo.springapp.repository;

import com.neo.springapp.model.RecurringDeposit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecurringDepositRepository extends JpaRepository<RecurringDeposit, Long> {
    List<RecurringDeposit> findByAccountNumberOrderByCreatedAtDesc(String accountNumber);
    List<RecurringDeposit> findAllByOrderByCreatedAtDesc();
}
