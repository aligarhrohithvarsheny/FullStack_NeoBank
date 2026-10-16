package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@Table(name = "credit_card_service_requests")
public class CreditCardServiceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private String requestNumber = "CC-" + UUID.randomUUID();

    @Column(nullable = false)
    private Long creditCardId;

    private Long transactionId;

    @Column(nullable = false)
    private String accountNumber;

    @Column(nullable = false)
    private String requestType;

    private String category;

    @Column(length = 2000)
    private String description;

    private BigDecimal requestedLimit;

    @Column(nullable = false)
    private String status = "Submitted";

    @Column(length = 2000)
    private String resolution;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
}
