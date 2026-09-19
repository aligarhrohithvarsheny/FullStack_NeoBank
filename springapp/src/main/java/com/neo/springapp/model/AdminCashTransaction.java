package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

// History record for admin "Quick Action" cash deposits/withdrawals across all account types
// (regular, salary, current, loan, goldloan, cheque) — see AdminCashTransactionService.
@Entity
@Data
@Table(name = "admin_cash_transactions")
public class AdminCashTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String refId; // e.g. ADMTXN1234567890

    private String accountType; // regular, salary, current, loan, goldloan, cheque
    private Long accountId; // internal id of the account entity, if applicable
    private String accountNumber;
    private String accountHolderName;

    private String operationType; // DEPOSIT, WITHDRAWAL
    private Double amount;
    private String description;

    private Double balanceBefore;
    private Double balanceAfter;

    private String status = "COMPLETED"; // COMPLETED, REVERTED

    private String performedBy;
    private LocalDateTime performedAt;

    private String revertedBy;
    private LocalDateTime revertedAt;
    private String revertReason;

    public AdminCashTransaction() {
        this.performedAt = LocalDateTime.now();
    }
}
