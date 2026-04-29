package com.neo.springapp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "insurance_payments")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class InsurancePayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String paymentReference;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "application_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private InsuranceApplication application;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String accountNumber;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    private LocalDateTime paymentDate;

    @Column(nullable = false)
    private String status = "SUCCESS"; // SUCCESS / FAILED / PENDING

    private LocalDate premiumPeriodFrom;
    private LocalDate premiumPeriodTo;
    private LocalDate nextDueDate;

    private boolean autoDebitEnabled = false;

    public InsurancePayment() {
        this.paymentDate = LocalDateTime.now();
        if (this.paymentReference == null || this.paymentReference.isEmpty()) {
            this.paymentReference = "PAY" + System.currentTimeMillis();
        }
    }
}

