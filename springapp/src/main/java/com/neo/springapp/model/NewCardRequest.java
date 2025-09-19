package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "new_card_requests")
public class NewCardRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userId;
    private String userName;
    private String userEmail;
    private String accountNumber;
    private String cardType; // Visa Debit, Mastercard, etc.
    private String reason; // Additional card, Upgrade, etc.
    private String status = "Pending"; // Pending, Approved, Rejected
    private LocalDateTime requestDate;
    private LocalDateTime processedDate;
    private String processedBy; // Admin who processed the request
    private String newCardNumber; // Generated when approved

    // Constructors
    public NewCardRequest() {
        this.requestDate = LocalDateTime.now();
    }

    public NewCardRequest(String userId, String userName, String userEmail, 
                         String accountNumber, String cardType, String reason) {
        this();
        this.userId = userId;
        this.userName = userName;
        this.userEmail = userEmail;
        this.accountNumber = accountNumber;
        this.cardType = cardType;
        this.reason = reason;
    }
}
