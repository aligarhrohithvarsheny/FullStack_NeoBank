package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "bank_messages")
public class BankMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String recipientAccountNumber;

    private String recipientEmail;

    @Column(nullable = false)
    private String messageType; // LOAN_APPROVAL, SALARY_CREDITED, ACCOUNT_UPDATE, TRANSACTION_ALERT, SYSTEM, PROMOTION

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(nullable = false)
    private String priority; // LOW, NORMAL, HIGH, URGENT

    @Column(nullable = false)
    private boolean isRead;

    private String sender; // SYSTEM, ADMIN, MANAGER

    private String actionUrl; // Optional URL for action buttons

    private LocalDateTime readAt;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;

    public BankMessage() {
        this.createdAt = LocalDateTime.now();
        this.isRead = false;
        this.priority = "NORMAL";
        this.sender = "SYSTEM";
    }
}
