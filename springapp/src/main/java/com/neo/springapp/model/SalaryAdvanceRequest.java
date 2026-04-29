package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "salary_advance_requests")
public class SalaryAdvanceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "salary_account_id", nullable = false)
    private Long salaryAccountId;

    @Column(name = "account_number", nullable = false)
    private String accountNumber;

    @Column(name = "employee_name")
    private String employeeName;

    @Column(name = "monthly_salary")
    private Double monthlySalary;

    @Column(name = "advance_amount", nullable = false)
    private Double advanceAmount;

    @Column(name = "advance_limit")
    private Double advanceLimit;

    private String reason;

    private String status = "Pending";

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    private Boolean repaid = false;

    @Column(name = "repaid_at")
    private LocalDateTime repaidAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
