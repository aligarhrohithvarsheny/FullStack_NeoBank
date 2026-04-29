package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "linked_accounts")
public class LinkedAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "current_account_number", nullable = false)
    private String currentAccountNumber;

    @Column(name = "savings_account_number", nullable = false)
    private String savingsAccountNumber;

    @Column(name = "savings_customer_id", nullable = false)
    private String savingsCustomerId;

    @Column(name = "linked_by", nullable = false)
    private String linkedBy;

    @Column(name = "linked_at")
    private LocalDateTime linkedAt;

    @Column(name = "status")
    private String status = "ACTIVE";

    @Column(name = "switch_pin")
    private String switchPin;

    @Column(name = "pin_created")
    private Boolean pinCreated = false;

    @PrePersist
    protected void onCreate() {
        linkedAt = LocalDateTime.now();
    }
}
