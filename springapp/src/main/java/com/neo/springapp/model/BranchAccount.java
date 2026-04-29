package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Single branch deposit account where all user charges, loan interest, KYC charges,
 * debit card charges, and bank transaction fees are credited.
 * Mapped from Admin profile (manager enters account details) or set from Manager Dashboard.
 */
@Data
@Entity
@Table(name = "branch_account")
public class BranchAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String accountNumber;

    private String accountName;

    private String ifscCode;

    private Long updatedByAdminId;
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
