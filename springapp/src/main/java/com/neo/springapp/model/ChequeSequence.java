package com.neo.springapp.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * ChequeSequence Model - Tracks unique cheque number generation per account
 */
@Entity
@Table(name = "cheque_sequence", uniqueConstraints = {
    @UniqueConstraint(columnNames = "salary_account_id")
})
public class ChequeSequence {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "salary_account_id", nullable = false, unique = true)
    private Long salaryAccountId;
    
    @Column(name = "next_sequence", nullable = false)
    private Long nextSequence;
    
    @Column(name = "last_generated", length = 50)
    private String lastGenerated;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public ChequeSequence() {
    }
    
    public ChequeSequence(Long salaryAccountId) {
        this.salaryAccountId = salaryAccountId;
        this.nextSequence = 1000L;
        this.updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getSalaryAccountId() {
        return salaryAccountId;
    }
    
    public void setSalaryAccountId(Long salaryAccountId) {
        this.salaryAccountId = salaryAccountId;
    }
    
    public Long getNextSequence() {
        return nextSequence;
    }
    
    public void setNextSequence(Long nextSequence) {
        this.nextSequence = nextSequence;
    }
    
    public String getLastGenerated() {
        return lastGenerated;
    }
    
    public void setLastGenerated(String lastGenerated) {
        this.lastGenerated = lastGenerated;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
