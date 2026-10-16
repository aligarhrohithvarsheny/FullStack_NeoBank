package com.neo.springapp.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "credit_card_audit_logs")
public class CreditCardAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long creditCardId;
    private String accountNumber;
    private String action;
    private String previousValue;
    private String newValue;
    private LocalDateTime createdAt = LocalDateTime.now();
}
