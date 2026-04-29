package com.neo.springapp.repository;

import com.neo.springapp.model.InsuranceApplication;
import com.neo.springapp.model.InsurancePolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InsuranceApplicationRepository extends JpaRepository<InsuranceApplication, Long> {

    Optional<InsuranceApplication> findByApplicationNumber(String applicationNumber);

    List<InsuranceApplication> findByUserId(Long userId);

    List<InsuranceApplication> findByAccountNumber(String accountNumber);

    List<InsuranceApplication> findByStatus(String status);

    List<InsuranceApplication> findByPolicyAndStatus(InsurancePolicy policy, String status);

    @Query("select (count(a) > 0) from InsuranceApplication a " +
           "where a.accountNumber = :accountNumber " +
           "and a.policy.id = :policyId " +
           "and upper(a.status) not in ('REJECTED','EXPIRED')")
    boolean existsNonRejectedByAccountAndPolicy(@Param("accountNumber") String accountNumber,
                                               @Param("policyId") Long policyId);

    @Query("select (count(a) > 0) from InsuranceApplication a " +
           "where a.policy.id = :policyId " +
           "and upper(a.status) not in ('REJECTED','EXPIRED')")
    boolean existsNonRejectedByPolicy(@Param("policyId") Long policyId);

    @Query("select a from InsuranceApplication a join a.policy p where p.policyNumber = :policyNumber " +
           "and upper(a.status) not in ('REJECTED','EXPIRED')")
    List<InsuranceApplication> findActiveByPolicyNumber(@Param("policyNumber") String policyNumber);
}

