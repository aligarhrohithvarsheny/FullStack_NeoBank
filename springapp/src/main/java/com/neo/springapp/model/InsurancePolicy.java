package com.neo.springapp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "insurance_policies")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class InsurancePolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String policyNumber;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String type; // Health / Life / Vehicle / Gold Protection

    @Column(nullable = false)
    private Double coverageAmount;

    @Column(nullable = false)
    private Double premiumAmount;

    @Column(nullable = false)
    private String premiumType; // MONTHLY / YEARLY

    @Column(nullable = false)
    private Integer durationMonths;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String benefits;

    @Column(columnDefinition = "TEXT")
    private String eligibility;

    private String status = "ACTIVE"; // ACTIVE / INACTIVE

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public InsurancePolicy() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.policyNumber == null || this.policyNumber.isEmpty()) {
            this.policyNumber = "POL" + System.currentTimeMillis();
        }
    }
}

