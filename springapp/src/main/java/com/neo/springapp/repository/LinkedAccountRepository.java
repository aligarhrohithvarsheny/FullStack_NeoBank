package com.neo.springapp.repository;

import com.neo.springapp.model.LinkedAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LinkedAccountRepository extends JpaRepository<LinkedAccount, Long> {

    List<LinkedAccount> findByCurrentAccountNumberAndStatus(String currentAccountNumber, String status);

    List<LinkedAccount> findBySavingsAccountNumberAndStatus(String savingsAccountNumber, String status);

    Optional<LinkedAccount> findByCurrentAccountNumberAndSavingsAccountNumber(String currentAccountNumber, String savingsAccountNumber);

    boolean existsByCurrentAccountNumberAndSavingsAccountNumberAndStatus(String currentAccountNumber, String savingsAccountNumber, String status);

    boolean existsByCurrentAccountNumberAndStatus(String currentAccountNumber, String status);

    boolean existsBySavingsAccountNumberAndStatus(String savingsAccountNumber, String status);
}
