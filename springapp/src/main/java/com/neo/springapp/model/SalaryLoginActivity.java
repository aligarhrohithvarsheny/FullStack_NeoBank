package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "salary_login_activity")
public class SalaryLoginActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "salary_account_id", nullable = false)
    private Long salaryAccountId;

    @Column(name = "account_number")
    private String accountNumber;

    @Column(name = "activity_type", nullable = false)
    private String activityType;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "device_info")
    private String deviceInfo;

    private String status = "Success";

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public SalaryLoginActivity() {
        this.createdAt = LocalDateTime.now();
    }
}
