package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "fastag_linked_accounts")
public class FastagLinkedAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String gmailId;

    @Column(nullable = false)
    private String accountNumber;

    private String accountHolderName;

    private Boolean verified = false;

    private LocalDateTime linkedAt = LocalDateTime.now();

    private String status = "ACTIVE";
}
