package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "card360_closure_requests")
public class Card360ClosureRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long creditCardId;

    @Column(nullable = false)
    private String accountNumber;

    private String customerName;
    private String maskedCardNumber;

    @Column(length = 1000)
    private String reason;

    @Column(nullable = false)
    private String status = "Pending";

    @Column(nullable = false)
    private LocalDateTime requestedAt = LocalDateTime.now();

    private LocalDateTime reviewedAt;
    private String reviewedBy;

    @Column(length = 1000)
    private String reviewNote;
}
