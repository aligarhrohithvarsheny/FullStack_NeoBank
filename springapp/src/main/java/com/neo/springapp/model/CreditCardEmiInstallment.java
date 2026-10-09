package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "credit_card_emi_installments")
public class CreditCardEmiInstallment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long planId;
    private Integer installmentNumber;
    private LocalDate dueDate;
    private Double principalAmount;
    private Double interestAmount;
    private Double totalAmount;
    private Double paidPrincipal;
    private Double paidInterest;
    private Double paidAmount;
    private Long billId;
    private String status = "Pending";
    private LocalDateTime paidAt;
}
