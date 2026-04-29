package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "support_tickets")
public class SupportTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String ticketId;

    @Column(nullable = false)
    private String accountNumber;

    @Column(nullable = false)
    private String userName;

    private String userEmail;

    @Column(nullable = false)
    private String category; // TRANSACTION_FAILED, DEBIT_CARD_ISSUE, LOGIN_PROBLEM, LOAN_EMI, ACCOUNT_ISSUE, OTHER

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(nullable = false)
    private String priority; // LOW, MEDIUM, HIGH, CRITICAL

    @Column(nullable = false)
    private String status; // OPEN, IN_PROGRESS, RESOLVED, CLOSED, REOPENED

    @Column(length = 2000)
    private String adminResponse;

    private String assignedTo;

    private String transactionId; // If related to a transaction

    private LocalDateTime resolvedAt;
    private LocalDateTime closedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public SupportTicket() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.status = "OPEN";
        this.priority = "MEDIUM";
        this.ticketId = "TKT-" + System.currentTimeMillis();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
