package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "cards")
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String cardNumber;
    private String cardType; // Debit / Credit
    private String cvv;
    private String userName;
    private String expiryDate;
    private String pin;
    private boolean blocked = false;
    private boolean deactivated = false;
    private boolean pinSet = false;
    private String status = "Active"; // Active / Blocked / Deactivated
    
    // User information
    private String accountNumber;
    private String userEmail;
    
    // Card management
    private LocalDateTime issueDate;
    private LocalDateTime expiryDateTime;
    private LocalDateTime lastUsed;

    // Constructors
    public Card() {
        this.issueDate = LocalDateTime.now();
        this.expiryDateTime = LocalDateTime.now().plusYears(3); // 3 years validity
    }

    public Card(String cardNumber, String cardType, String userName, String accountNumber, String userEmail) {
        this();
        this.cardNumber = cardNumber;
        this.cardType = cardType;
        this.userName = userName;
        this.accountNumber = accountNumber;
        this.userEmail = userEmail;
        this.cvv = generateCVV();
    }

    // Generate random CVV
    private String generateCVV() {
        return String.format("%03d", (int)(Math.random() * 1000));
    }

    // Generate masked card number
    public String getMaskedCardNumber() {
        if (cardNumber != null && cardNumber.length() >= 4) {
            return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
        }
        return cardNumber;
    }
}
