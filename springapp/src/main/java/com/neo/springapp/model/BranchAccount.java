package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

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

    @Column(name = "customer_id", unique = true)
    private String customerId;

    @Column(name = "branch_city", unique = true)
    private String branchCity;

    @Column(name = "manager_admin_id", unique = true)
    private Long managerAdminId;

    private Double balance = 0.0;

    private Long updatedByAdminId;
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
