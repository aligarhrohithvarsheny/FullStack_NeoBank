package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "card360_audit")
public class Card360Audit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String accountNumber;

    private String cardType;
    private Long cardId;

    @Column(nullable = false)
    private String action;

    @Column(columnDefinition = "TEXT")
    private String oldValue;

    @Column(columnDefinition = "TEXT")
    private String newValue;

    private String changedBy;
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
