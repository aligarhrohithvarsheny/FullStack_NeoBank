package com.neo.springapp.repository;

import com.neo.springapp.model.WebAuthnCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WebAuthnCredentialRepository extends JpaRepository<WebAuthnCredential, Long> {
    
    Optional<WebAuthnCredential> findByCredentialId(String credentialId);
    
    List<WebAuthnCredential> findByAdminEmailAndActiveTrue(String adminEmail);
    
    Optional<WebAuthnCredential> findByAdminEmailAndCredentialId(String adminEmail, String credentialId);
    
    void deleteByAdminEmail(String adminEmail);
}

