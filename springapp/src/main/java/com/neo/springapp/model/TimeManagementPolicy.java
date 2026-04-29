package com.neo.springapp.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.time.LocalDateTime;

/**
 * Entity for managing custom time policies per admin/employee
 */
@Entity
@Table(name = "time_management_policy", uniqueConstraints = {
    @UniqueConstraint(name = "uk_admin_policy", columnNames = {"admin_id", "policy_name"})
}, indexes = {
    @Index(name = "idx_admin_id", columnList = "admin_id"),
    @Index(name = "idx_is_active", columnList = "is_active")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeManagementPolicy {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "admin_id", nullable = false)
    private String adminId;
    
    @Column(name = "policy_name", nullable = false)
    private String policyName;
    
    @Column(name = "working_hours_per_day", nullable = false)
    private Integer workingHoursPerDay; // e.g., 8 hours
    
    @Column(name = "check_in_time", nullable = false)
    @JsonFormat(pattern = "HH:mm")
    private LocalTime checkInTime; // e.g., 09:00
    
    @Column(name = "check_out_time", nullable = false)
    @JsonFormat(pattern = "HH:mm")
    private LocalTime checkOutTime; // e.g., 17:00
    
    @Column(name = "grace_period_minutes")
    private Integer gracePeriodMinutes; // Grace period for late check-in
    
    @Column(name = "max_working_hours", nullable = false)
    private Integer maxWorkingHours; // Maximum working hours allowed per day
    
    @Column(name = "overtime_multiplier")
    private Double overtimeMultiplier; // Overtime calculation multiplier (e.g., 1.5x)
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isActive == null) {
            isActive = true;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
