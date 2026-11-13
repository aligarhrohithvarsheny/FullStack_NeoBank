package com.neo.springapp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "credit_card_requests")
public class CreditCardRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String accountNumber;
    private String userName;
    private String userEmail;
    private String pan;
    private Double annualIncome;
    @Column(name = "mobile_number", length = 10)
    private String mobileNumber;
    @Column(name = "date_of_birth", length = 10)
    private String dateOfBirth;
    @Column(name = "aadhaar_last_four", length = 4)
    private String aadhaarLastFour;
    @Column(length = 500)
    private String address;
    @Column(name = "employment_type", length = 30)
    private String employmentType;
    @Column(name = "employer_name")
    private String employerName;
    private String designation;
    @Column(name = "application_id", unique = true, length = 20)
    private String applicationId;
    @JsonIgnore
    @Column(name = "tracking_credential_hash", length = 64)
    private String trackingCredentialHash;
    @Column(name = "consent_given", nullable = false)
    private boolean consentGiven;
    @Column(name = "consented_at")
    private LocalDateTime consentedAt;
    @Column(name = "documents_delete_after")
    private LocalDateTime documentsDeleteAfter;
    @Column(name = "documents_available", nullable = false)
    private boolean documentsAvailable;
    private Double approvedLimit;
    private Integer predictedCibil; // predicted by ML stub
    private Double suggestedLimit;
    private Double panBasedLimit; // Limit based on PAN card
    private String cardType; // Standard, Silver, Gold, Platinum
    private String eligibility; // Approved, Rejected, Under Review
    private Double interestRate; // Interest rate based on CIBIL
    private Double annualFee; // Annual fee for the card
    private String status = "Pending"; // Pending/Approved/Rejected
    private String last4;
    private LocalDateTime requestDate;
    private LocalDateTime processedDate;
    private String processedBy;

    public CreditCardRequest() {
        this.requestDate = LocalDateTime.now();
    }
}
