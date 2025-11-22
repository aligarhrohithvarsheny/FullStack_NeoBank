package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "kyc_requests")
public class KycRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String panNumber;
    private String name;
    private String status = "Pending"; // "Pending", "Approved", "Rejected"
    
    // User information
    private String userId;
    private String userName;
    private String userEmail;
    private String userAccountNumber;
    
    // Timestamps
    private LocalDateTime submittedDate;
    private LocalDateTime approvedDate;
    private String approvedBy; // Admin who approved the KYC
    
    // Document uploads
    @Lob
    @Column(name = "aadhar_document", columnDefinition = "LONGBLOB")
    private byte[] aadharDocument; // Aadhar document (JPEG or PDF)
    
    @Lob
    @Column(name = "pan_document", columnDefinition = "LONGBLOB")
    private byte[] panDocument; // PAN document (JPEG or PDF)
    
    private String aadharDocumentType; // "image/jpeg" or "application/pdf"
    private String panDocumentType; // "image/jpeg" or "application/pdf"
    
    private String aadharDocumentName; // Original filename
    private String panDocumentName; // Original filename

    // Constructors
    public KycRequest() {
        this.submittedDate = LocalDateTime.now();
    }

    public KycRequest(String panNumber, String name, String userId, String userName, 
                      String userEmail, String userAccountNumber) {
        this();
        this.panNumber = panNumber;
        this.name = name;
        this.userId = userId;
        this.userName = userName;
        this.userEmail = userEmail;
        this.userAccountNumber = userAccountNumber;
    }
}
