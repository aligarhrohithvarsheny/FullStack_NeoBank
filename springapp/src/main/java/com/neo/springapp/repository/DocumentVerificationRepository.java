package com.neo.springapp.repository;

import com.neo.springapp.model.DocumentVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentVerificationRepository extends JpaRepository<DocumentVerification, Long> {

    List<DocumentVerification> findByAccountNumber(String accountNumber);

    List<DocumentVerification> findByStatus(String status);

    List<DocumentVerification> findByAccountNumberAndDocumentType(String accountNumber, String documentType);

    List<DocumentVerification> findByStatusOrderByCreatedAtDesc(String status);

    long countByStatus(String status);
}
