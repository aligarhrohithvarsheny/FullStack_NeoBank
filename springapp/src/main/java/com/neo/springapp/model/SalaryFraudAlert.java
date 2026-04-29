package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "salary_fraud_alerts")
public class SalaryFraudAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "salary_account_id", nullable = false)
    private Long salaryAccountId;

    @Column(name = "account_number")
    private String accountNumber;

    @Column(name = "alert_type", nullable = false)
    private String alertType;

    private String severity = "Medium";

    private String description;

    private Double amount;

    private String location;

    private Boolean resolved = false;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
