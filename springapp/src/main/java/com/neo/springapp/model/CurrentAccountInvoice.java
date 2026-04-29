package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "current_account_invoices")
public class CurrentAccountInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String invoiceNumber;

    @Column(nullable = false)
    private String accountNumber;

    @Column(nullable = false)
    private String clientName;

    private String clientEmail;
    private String clientPhone;

    @Column(columnDefinition = "TEXT")
    private String clientAddress;

    private String clientGst;

    @Column(nullable = false)
    private LocalDate invoiceDate;

    @Column(nullable = false)
    private LocalDate dueDate;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String itemsJson;

    private Double subtotal = 0.0;
    private Double taxRate = 18.0;
    private Double taxAmount = 0.0;
    private Double discount = 0.0;
    private Double totalAmount = 0.0;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(columnDefinition = "TEXT")
    private String terms;

    private String status = "DRAFT"; // DRAFT, SENT, PAID, OVERDUE, CANCELLED

    private Double paidAmount = 0.0;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (invoiceNumber == null) {
            invoiceNumber = "INV-" + System.currentTimeMillis();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
