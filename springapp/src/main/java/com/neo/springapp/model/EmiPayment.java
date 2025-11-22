package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "emi_payments")
public class EmiPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long loanId;

    @Column(nullable = false)
    private String loanAccountNumber;

    @Column(nullable = false)
    private String accountNumber; // User's savings account

    @Column(nullable = false)
    private Integer emiNumber; // EMI installment number (1, 2, 3, ...)

    @Column(nullable = false)
    private LocalDate dueDate; // EMI due date

    @Column(nullable = false)
    private Double principalAmount; // Principal portion of this EMI

    @Column(nullable = false)
    private Double interestAmount; // Interest portion of this EMI

    @Column(nullable = false)
    private Double totalAmount; // Total EMI amount (principal + interest)

    @Column(nullable = false)
    private Double remainingPrincipal; // Remaining principal after this payment

    @Column(nullable = false)
    private String status = "Pending"; // Pending, Paid, Overdue, Skipped

    private LocalDateTime paymentDate; // Actual payment date

    private Double balanceBeforePayment; // Account balance before payment

    private Double balanceAfterPayment; // Account balance after payment

    private String transactionId; // Reference to transaction record

    private String pdfPath; // Path to generated PDF receipt

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

