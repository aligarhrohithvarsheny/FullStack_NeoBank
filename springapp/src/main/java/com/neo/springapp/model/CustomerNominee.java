package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "customer_nominees", indexes = {
        @Index(name = "idx_customer_nominee_account", columnList = "account_number")
})
public class CustomerNominee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", nullable = false, length = 40)
    private String accountNumber;

    @Column(nullable = false, length = 120)
    private String nomineeName;

    @Column(nullable = false, length = 60)
    private String relationship;

    @Column(nullable = false)
    private Double sharePercentage;

    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
