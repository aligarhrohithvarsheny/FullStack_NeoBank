package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "credit_card_emi_plans")
public class CreditCardEmiPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long creditCardId;
    private String accountNumber;
    private String userName;
    private String cardNumber;
    private Integer tenureMonths;
    private Double principalAmount;
    private Double processingFeePercent;
    private Double processingFeeAmount;
    private Double annualInterestPercent;
    private Double monthlyEmi;
    private Double totalInterest;
    private Double totalRepayment;
    private Double outstandingPrincipal;
    private String status = "Active";
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
