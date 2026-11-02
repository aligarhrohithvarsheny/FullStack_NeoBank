package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "term_deposit_plans")
public class TermDepositPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String schemeName;
    @Column(length = 12)
    private String productType = "FD";
    private Integer days;
    private Integer durationMonths;
    private Integer years;
    private Double minimumAmount;
    private Double maximumAmount;
    private Double interestRate;
    private String payoutDate;
    private Double interestAmount;
    private Boolean active = true;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
