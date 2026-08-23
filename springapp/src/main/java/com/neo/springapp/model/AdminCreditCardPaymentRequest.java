package com.neo.springapp.model;

import lombok.Data;

@Data
public class AdminCreditCardPaymentRequest {
    private Double amount;
    private String paymentMethod; // CASH, ACCOUNT, CHEQUE
    private String debitAccountNumber;
    private String chequeNumber;
    private String adminName;
}
