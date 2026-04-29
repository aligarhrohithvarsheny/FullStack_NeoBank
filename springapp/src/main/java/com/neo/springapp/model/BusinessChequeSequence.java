package com.neo.springapp.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * BusinessChequeSequence - Tracks unique cheque number generation per business/current account
 */
@Entity
@Table(name = "business_cheque_sequence", uniqueConstraints = {
    @UniqueConstraint(columnNames = "current_account_id")
})
public class BusinessChequeSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "current_account_id", nullable = false, unique = true)
    private Long currentAccountId;

    @Column(name = "next_sequence", nullable = false)
    private Long nextSequence;

    @Column(name = "last_generated", length = 50)
    private String lastGenerated;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public BusinessChequeSequence() {
    }

    public BusinessChequeSequence(Long currentAccountId) {
        this.currentAccountId = currentAccountId;
        this.nextSequence = 1000L;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCurrentAccountId() { return currentAccountId; }
    public void setCurrentAccountId(Long currentAccountId) { this.currentAccountId = currentAccountId; }

    public Long getNextSequence() { return nextSequence; }
    public void setNextSequence(Long nextSequence) { this.nextSequence = nextSequence; }

    public String getLastGenerated() { return lastGenerated; }
    public void setLastGenerated(String lastGenerated) { this.lastGenerated = lastGenerated; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
