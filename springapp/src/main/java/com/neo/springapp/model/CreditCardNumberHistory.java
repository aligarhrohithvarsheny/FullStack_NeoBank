package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "credit_card_number_history")
public class CreditCardNumberHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false)
    private Long requestId;

    @Column(name = "old_masked_card_number", length = 32)
    private String oldMaskedCardNumber;

    @Column(name = "new_masked_card_number", length = 32)
    private String newMaskedCardNumber;

    @Column(nullable = false, length = 20)
    private String action;

    @Column(nullable = false)
    private LocalDateTime generatedAt;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(length = 120)
    private String generatedBy;
}
