package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

// Admin-initiated fund transfer between any two bank accounts (savings/salary/current),
// with optional sender cheque verification and a 0.5% processing charge on transfer + revert.
@Entity
@Data
@Table(name = "admin_fund_transfers")
public class AdminFundTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String transferId; // e.g. AFT1234567890

    private String senderAccountNumber;
    private String senderName;
    private String senderAccountType;
    private String senderChequeNumber;

    private String receiverAccountNumber;
    private String receiverName;
    private String receiverAccountType;

    private Double amount;
    private Double transferCharge; // 0.5% of amount, deducted from sender
    private Double revertCharge;   // 0.5% of amount, deducted on revert (set only when reverted)

    private String description;
    private String status = "COMPLETED"; // COMPLETED, REVERTED

    private String performedBy;
    private LocalDateTime performedAt;

    private String editedBy;
    private LocalDateTime editedAt;

    private String revertedBy;
    private LocalDateTime revertedAt;
    private String revertReason;

    public AdminFundTransfer() {
        this.performedAt = LocalDateTime.now();
    }
}
