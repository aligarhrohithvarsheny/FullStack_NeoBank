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
