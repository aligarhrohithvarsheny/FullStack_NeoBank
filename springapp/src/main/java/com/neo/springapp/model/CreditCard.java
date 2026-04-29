package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "credit_cards")
public class CreditCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Card Information
    private String cardNumber;
    private String cvv;
    private String expiryDate;
    private String pin;
    private boolean pinSet = false;
    private String status = "Active"; // Active / Blocked / Closed
    
    // User Information
    private String accountNumber;
    private String userName;
    private String userEmail;
    
    // Credit Card Specific Fields
    private Double approvedLimit; // Total credit limit approved
    private Double currentBalance; // Current outstanding balance
    private Double availableLimit; // Available credit (approvedLimit - currentBalance)
    private Double usageLimit; // Usage percentage
    private Double userSetSpendingLimit; // User-set spending limit within approved limit
    
    // Dates
    private LocalDateTime appliedDate; // When user applied
    private LocalDateTime approvalDate; // When admin approved
    private LocalDateTime closureDate; // When card was closed
    
    // Billing Information
    private Double overdueAmount; // Overdue amount
    private LocalDateTime lastPaidDate; // Last payment date
    private LocalDateTime nextBillingDate; // Next billing cycle date
    private Double fine; // Fine amount
    private Double penalty; // Penalty amount
    
    // Card Management
    private LocalDateTime issueDate;
    private LocalDateTime expiryDateTime;
    private LocalDateTime lastUsed;
    private boolean blocked = false;
    private boolean deactivated = false;

    public CreditCard() {
        this.issueDate = LocalDateTime.now();
        this.expiryDateTime = LocalDateTime.now().plusYears(3);
        this.appliedDate = LocalDateTime.now();
        this.currentBalance = 0.0;
        this.overdueAmount = 0.0;
        this.fine = 0.0;
        this.penalty = 0.0;
        this.userSetSpendingLimit = null; // user may set later
    }

    // Calculate available limit
    public void calculateAvailableLimit() {
        if (approvedLimit != null && currentBalance != null) {
            this.availableLimit = approvedLimit - currentBalance;
            if (availableLimit < 0) {
                availableLimit = 0.0;
            }
        }
    }

    // Calculate usage percentage
    public void calculateUsageLimit() {
        if (approvedLimit != null && approvedLimit > 0 && currentBalance != null) {
            this.usageLimit = (currentBalance / approvedLimit) * 100;
        } else {
            this.usageLimit = 0.0;
        }
    }

    // Generate masked card number
    public String getMaskedCardNumber() {
        if (cardNumber != null && cardNumber.length() >= 4) {
            return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
        }
        return cardNumber;
    }
}
