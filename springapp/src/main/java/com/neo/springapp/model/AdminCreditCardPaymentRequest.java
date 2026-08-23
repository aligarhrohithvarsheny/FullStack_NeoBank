package com.neo.springapp.model;

import lombok.Data;

@Data
public class AdminCreditCardPaymentRequest {
    private Double amount;
    private String paymentMethod; // CASH, ACCOUNT, CHEQUE
    private String debitAccountNumber;
    private String chequeNumber;
    private String chequeImageBase64;
    private String chequeImageName;
    private String chequeImageType;
    private String adminName;
}
