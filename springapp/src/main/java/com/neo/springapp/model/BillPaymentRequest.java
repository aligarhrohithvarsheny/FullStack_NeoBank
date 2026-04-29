package com.neo.springapp.model;

import lombok.Data;

@Data
public class BillPaymentRequest {
    private String accountNumber;
    private String userName;
    private String userEmail;
    private String billType; // Mobile Bill, WiFi Bill
    private String networkProvider; // Airtel, Jio, BSNL, etc.
    private String customerNumber; // Mobile number or account number
    private Double amount;
    private Long creditCardId;
    private String cardNumber; // Full card number for verification
    private String cvv;
    private String expiryDate; // Format: MM/YY
}
