package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "card_replacement_requests")
public class CardReplacementRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userId;
    private String userName;
    private String userEmail;
    private String accountNumber;
    private String currentCardNumber;
    private String reason; // Lost, Damaged, Stolen, etc.
    private String status = "Pending"; // Pending, Approved, Rejected
    private LocalDateTime requestDate;
    private LocalDateTime processedDate;
    private String processedBy; // Admin who processed the request
    private String newCardNumber; // Generated when approved

    // Constructors
    public CardReplacementRequest() {
        this.requestDate = LocalDateTime.now();
    }

    public CardReplacementRequest(String userId, String userName, String userEmail, 
                                 String accountNumber, String currentCardNumber, String reason) {
        this();
        this.userId = userId;
        this.userName = userName;
        this.userEmail = userEmail;
        this.accountNumber = accountNumber;
        this.currentCardNumber = currentCardNumber;
        this.reason = reason;
    }
}
