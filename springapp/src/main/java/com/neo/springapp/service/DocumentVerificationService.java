package com.neo.springapp.service;

import com.neo.springapp.model.DocumentVerification;
import com.neo.springapp.repository.DocumentVerificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@SuppressWarnings("null")
public class DocumentVerificationService {

    @Autowired
    private DocumentVerificationRepository documentVerificationRepository;

    public DocumentVerification submitDocument(DocumentVerification doc) {
        doc.setStatus("PENDING");
        doc.setSubmittedAt(LocalDateTime.now());
        return documentVerificationRepository.save(doc);
    }

    public List<DocumentVerification> getByAccountNumber(String accountNumber) {
        return documentVerificationRepository.findByAccountNumber(accountNumber);
    }

    public List<DocumentVerification> getPendingDocuments() {
        return documentVerificationRepository.findByStatusOrderByCreatedAtDesc("PENDING");
    }

    public List<DocumentVerification> getByStatus(String status) {
        return documentVerificationRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    public Optional<DocumentVerification> getById(Long id) {
        return documentVerificationRepository.findById(id);
    }

    public DocumentVerification verifyDocument(Long id, String verifiedBy, String remarks) {
        Optional<DocumentVerification> opt = documentVerificationRepository.findById(id);
        if (opt.isPresent()) {
            DocumentVerification doc = opt.get();
            doc.setStatus("VERIFIED");
            doc.setVerifiedBy(verifiedBy);
            doc.setVerifiedAt(LocalDateTime.now());
            doc.setRemarks(remarks);
            return documentVerificationRepository.save(doc);
        }
        return null;
    }

    public DocumentVerification rejectDocument(Long id, String verifiedBy, String rejectionReason) {
        Optional<DocumentVerification> opt = documentVerificationRepository.findById(id);
        if (opt.isPresent()) {
            DocumentVerification doc = opt.get();
            doc.setStatus("REJECTED");
            doc.setVerifiedBy(verifiedBy);
            doc.setVerifiedAt(LocalDateTime.now());
            doc.setRejectionReason(rejectionReason);
            return documentVerificationRepository.save(doc);
        }
        return null;
    }

    public long getPendingCount() {
        return documentVerificationRepository.countByStatus("PENDING");
    }

    public long getVerifiedCount() {
        return documentVerificationRepository.countByStatus("VERIFIED");
    }

    public long getRejectedCount() {
        return documentVerificationRepository.countByStatus("REJECTED");
    }

    public List<DocumentVerification> getAll() {
        return documentVerificationRepository.findAll();
    }
}
