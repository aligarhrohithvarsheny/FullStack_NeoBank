package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "pg_settlement_ledger")
public class PgSettlementLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String ledgerId;

    @Column(nullable = false)
    private String merchantId;

    @Column(nullable = false)
    private String transactionId;

    @Column(nullable = false)
    private String orderId;

    @Column(nullable = false)
    private BigDecimal grossAmount;

    @Column(nullable = false)
    private BigDecimal feeAmount;

    @Column(nullable = false)
    private BigDecimal taxAmount;

    @Column(nullable = false)
    private BigDecimal netAmount;

    @Column(nullable = false)
    private String creditAccount;

    private String creditStatus = "PENDING";
    private LocalDateTime creditedAt;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private String referenceNote;
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (ledgerId == null) {
            ledgerId = "PGLED" + System.currentTimeMillis();
        }
    }
}
