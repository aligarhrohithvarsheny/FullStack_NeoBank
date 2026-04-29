package com.neo.springapp.repository;

import com.neo.springapp.model.Beneficiary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BeneficiaryRepository extends JpaRepository<Beneficiary, Long> {
    
    // Find all beneficiaries for a sender account
    List<Beneficiary> findBySenderAccountNumber(String senderAccountNumber);
    
    // Find beneficiary by sender and recipient account number
    Optional<Beneficiary> findBySenderAccountNumberAndRecipientAccountNumber(
        String senderAccountNumber, String recipientAccountNumber);
    
    // Check if beneficiary exists
    boolean existsBySenderAccountNumberAndRecipientAccountNumber(
        String senderAccountNumber, String recipientAccountNumber);
}

