package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "soundbox_linked_accounts")
public class SoundboxLinkedAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "soundbox_account_number", nullable = false)
    private String soundboxAccountNumber;

    @Column(name = "linked_account_number", nullable = false)
    private String linkedAccountNumber;

    @Column(name = "linked_customer_id", nullable = false)
    private String linkedCustomerId;

    @Column(name = "linked_account_name", nullable = false)
    private String linkedAccountName;

    @Column(name = "account_type", nullable = false)
    private String accountType;

    private String status = "PENDING";
    private String processedBy;
    private String adminRemarks;
    private LocalDateTime requestedAt;
    private LocalDateTime processedAt;

    @PrePersist
    protected void onCreate() {
        if (requestedAt == null) requestedAt = LocalDateTime.now();
    }
}