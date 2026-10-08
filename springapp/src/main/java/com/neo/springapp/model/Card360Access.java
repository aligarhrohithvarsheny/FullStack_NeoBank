package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "card360_access")
public class Card360Access {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String accountNumber;

    @Column(nullable = false, length = 100)
    private String passcodeHash;

    @Column(nullable = false)
    private boolean enabled = false;

    @Column(nullable = false)
    private int failedAttempts = 0;

    private LocalDateTime lockedUntil;
    private String updatedBy;
    private LocalDateTime updatedAt;
}
