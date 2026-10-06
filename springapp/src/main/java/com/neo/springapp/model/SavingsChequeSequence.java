package com.neo.springapp.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * SavingsChequeSequence - Tracks unique cheque number generation per business/current account
 */
@Entity
@Table(name = "savings_cheque_sequence", uniqueConstraints = {
    @UniqueConstraint(columnNames = "account_id")
})
public class SavingsChequeSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false, unique = true)
    private Long accountId;

    @Column(name = "next_sequence", nullable = false)
    private Long nextSequence;

    @Column(name = "last_generated", length = 50)
    private String lastGenerated;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public SavingsChequeSequence() {
    }

    public SavingsChequeSequence(Long accountId) {
        this.accountId = accountId;
        this.nextSequence = 1000L;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public Long getNextSequence() { return nextSequence; }
    public void setNextSequence(Long nextSequence) { this.nextSequence = nextSequence; }

    public String getLastGenerated() { return lastGenerated; }
    public void setLastGenerated(String lastGenerated) { this.lastGenerated = lastGenerated; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
