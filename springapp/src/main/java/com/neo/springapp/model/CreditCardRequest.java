package com.neo.springapp.model;

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
