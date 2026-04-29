package com.neo.springapp.repository;

import com.neo.springapp.model.InsurancePolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InsurancePolicyRepository extends JpaRepository<InsurancePolicy, Long> {

    Optional<InsurancePolicy> findByPolicyNumber(String policyNumber);

    List<InsurancePolicy> findByStatus(String status);

    List<InsurancePolicy> findByTypeAndStatus(String type, String status);
}

