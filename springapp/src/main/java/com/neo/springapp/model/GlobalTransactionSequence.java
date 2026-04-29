package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Entity to manage globally incrementing transaction IDs
 * Ensures that every transaction in the system gets a unique, sequential ID
 */
@Entity
@Data
@Table(name = "global_transaction_sequence")
public class GlobalTransactionSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "current_sequence", nullable = false, unique = true)
    private Long currentSequence;

    @Column(name = "transaction_type")
    private String transactionType; // For reference only

    @Column(name = "last_transaction_id")
    private String lastTransactionId;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public GlobalTransactionSequence() {
        this.currentSequence = 1L;
        this.createdAt = LocalDateTime.now();
        this.lastUpdated = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.lastUpdated = LocalDateTime.now();
    }
}
