package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "document_verifications")
public class DocumentVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String accountNumber;

    @Column(nullable = false)
    private String userName;

    private String userEmail;

    @Column(nullable = false)
    private String documentType; // AADHAAR_CARD, PAN_CARD, ADDRESS_PROOF

    @Column(nullable = false)
    private String documentNumber;

    private String documentFilePath;

    @Column(nullable = false)
    private String status; // PENDING, VERIFIED, REJECTED, EXPIRED

    private String verifiedBy;

    @Column(length = 1000)
    private String rejectionReason;

    @Column(length = 1000)
    private String remarks;

    private LocalDateTime verifiedAt;
    private LocalDateTime submittedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public DocumentVerification() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.submittedAt = LocalDateTime.now();
        this.status = "PENDING";
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
