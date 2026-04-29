package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "current_account_beneficiaries")
public class CurrentAccountBeneficiary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String accountNumber;

    @Column(nullable = false)
    private String beneficiaryName;

    @Column(nullable = false)
    private String beneficiaryAccount;

    @Column(nullable = false)
    private String beneficiaryIfsc;

    private String beneficiaryBank;
    private String nickName;
    private String status = "ACTIVE";
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
