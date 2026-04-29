package com.neo.springapp.repository;

import com.neo.springapp.model.InsuranceApplication;
import com.neo.springapp.model.InsuranceClaim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InsuranceClaimRepository extends JpaRepository<InsuranceClaim, Long> {

    Optional<InsuranceClaim> findByClaimNumber(String claimNumber);

    List<InsuranceClaim> findByUserId(Long userId);

    List<InsuranceClaim> findByAccountNumber(String accountNumber);

    List<InsuranceClaim> findByApplication(InsuranceApplication application);

    List<InsuranceClaim> findByStatus(String status);

    @Query("select c from InsuranceClaim c join c.application a join a.policy p where p.policyNumber = :policyNumber")
    List<InsuranceClaim> findByPolicyNumber(@Param("policyNumber") String policyNumber);
}

