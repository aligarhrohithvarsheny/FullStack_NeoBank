package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Data
@Table(name = "savings_demand_drafts")
public class DemandDraft {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String accountNumber;
    private String userName;
    private String userEmail;
    private String chequeNumber;
    private String payeeName;
    private String payeeAccountNumber;
    private BigDecimal amount;
    private LocalDate draftDate;
    private String reason;
    private BigDecimal availableBalance;
    private BigDecimal lockedAmount;
    private String status = "PENDING";
    private String ddNumber;
    private String approvedBy;
    private LocalDateTime approvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String editHistory;

    @PrePersist
    void created() { createdAt = LocalDateTime.now(); updatedAt = createdAt; }
    @PreUpdate
    void updated() { updatedAt = LocalDateTime.now(); }
}
