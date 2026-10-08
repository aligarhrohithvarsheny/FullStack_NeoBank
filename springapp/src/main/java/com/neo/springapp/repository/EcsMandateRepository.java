package com.neo.springapp.repository;

import com.neo.springapp.model.EcsMandate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EcsMandateRepository extends JpaRepository<EcsMandate, Long> {
    List<EcsMandate> findAllByOrderByCreatedAtDesc();
    List<EcsMandate> findBySavingsAccountNumberOrderByCreatedAtDesc(String savingsAccountNumber);
    List<EcsMandate> findByLoanAccountNumberOrderByCreatedAtDesc(String loanAccountNumber);
    List<EcsMandate> findByStatus(String status);
    Optional<EcsMandate> findByMandateId(String mandateId);
    boolean existsByLoanAccountNumberAndStatusIn(String loanAccountNumber, List<String> statuses);
}
