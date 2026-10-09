package com.neo.springapp.repository;

import com.neo.springapp.model.InsuranceCredential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InsuranceCredentialRepository extends JpaRepository<InsuranceCredential, Long> {
    Optional<InsuranceCredential> findByUserId(Long userId);
}
