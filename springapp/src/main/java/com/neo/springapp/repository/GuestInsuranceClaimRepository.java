package com.neo.springapp.repository;

import com.neo.springapp.model.GuestInsuranceClaim;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GuestInsuranceClaimRepository extends JpaRepository<GuestInsuranceClaim, Long> {
    List<GuestInsuranceClaim> findByStatusOrderByCreatedAtAsc(String status);
    List<GuestInsuranceClaim> findByApplicationIdOrderByCreatedAtDesc(Long applicationId);
}
