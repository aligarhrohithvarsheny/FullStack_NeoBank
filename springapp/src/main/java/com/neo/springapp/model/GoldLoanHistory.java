package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "gold_loan_history")
public class GoldLoanHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long goldLoanId; // Reference to GoldLoan

    private String loanAccountNumber;

    private String action; // APPROVED, REJECTED, EDITED, RENEWED

    private String changedBy; // Admin who performed the action

    private LocalDateTime changeDate; // When the action happened

    @Column(length = 2000)
    private String details; // Human-readable summary of changes (old -> new values)

    private Double oldAmount; // Loan amount before change (if applicable)

    private Double newAmount; // Loan amount after change (if applicable)

    @Column(length = 1000)
    private String remarks; // Optional remarks

    public GoldLoanHistory() {
        this.changeDate = LocalDateTime.now();
    }
}
