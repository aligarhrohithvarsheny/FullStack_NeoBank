package com.neo.springapp.repository;

import com.neo.springapp.model.RecurringDeposit;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RecurringDepositRepository extends JpaRepository<RecurringDeposit, Long> {
    List<RecurringDeposit> findByAccountNumberOrderByCreatedAtDesc(String accountNumber);
    List<RecurringDeposit> findAllByOrderByCreatedAtDesc();
    List<RecurringDeposit> findByStatus(String status);
    Optional<RecurringDeposit> findByShareTokenHash(String shareTokenHash);
    Optional<RecurringDeposit> findByRdAccountNumber(String rdAccountNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select rd from RecurringDeposit rd where rd.id = :id")
    Optional<RecurringDeposit> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select rd from RecurringDeposit rd where rd.rdAccountNumber = :rdAccountNumber")
    Optional<RecurringDeposit> findByRdAccountNumberForUpdate(@Param("rdAccountNumber") String rdAccountNumber);
}
