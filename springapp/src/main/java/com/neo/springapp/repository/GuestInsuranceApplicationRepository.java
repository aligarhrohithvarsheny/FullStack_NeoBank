package com.neo.springapp.repository;

import com.neo.springapp.model.GuestInsuranceApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GuestInsuranceApplicationRepository extends JpaRepository<GuestInsuranceApplication, Long> {
    List<GuestInsuranceApplication> findByEmailOrderByCreatedAtDesc(String email);
    List<GuestInsuranceApplication> findByStatusOrderByCreatedAtAsc(String status);
    java.util.Optional<GuestInsuranceApplication> findByApplicationNumberAndEmailIgnoreCase(String applicationNumber, String email);
}
