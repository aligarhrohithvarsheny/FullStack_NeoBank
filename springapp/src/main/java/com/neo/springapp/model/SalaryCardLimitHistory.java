package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "salary_card_limit_history")
public class SalaryCardLimitHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "salary_account_id", nullable = false)
    private Long salaryAccountId;

    @Column(name = "account_number")
    private String accountNumber;

    @Column(name = "old_limit")
    private Double oldLimit;

    @Column(name = "new_limit")
    private Double newLimit;

    @Column(name = "change_type")
    private String changeType; // LIMIT_UPDATE, LIMIT_INCREASE_ENABLED, REPLACE_CARD

    @Column(name = "changed_by")
    private String changedBy = "USER";

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
