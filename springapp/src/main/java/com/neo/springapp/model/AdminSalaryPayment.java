package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
@Table(
    name = "admin_salary_payments",
    uniqueConstraints = @UniqueConstraint(columnNames = {"adminId", "salaryDate"})
)
public class AdminSalaryPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long adminId;
    private String adminName;
    private String adminEmail;

    private LocalDate salaryDate;
    private Double amount;

    private String managerBranchAccountNumber;
    private String adminSalaryAccountNumber;

    private LocalDateTime paidAt;
    private String paidByManager;

    private Long managerDebitTransactionDbId;
    private Long adminCreditTransactionDbId;

    private String status = "PAID"; // PAID / FAILED
    private String failureReason;

    @PrePersist
    protected void onCreate() {
        if (paidAt == null) paidAt = LocalDateTime.now();
        if (status == null) status = "PAID";
    }
}

