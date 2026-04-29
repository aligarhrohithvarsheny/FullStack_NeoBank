package com.neo.springapp.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.LocalDate;

/**
 * Entity for tracking employee check-in and check-out times
 */
@Entity
@Table(name = "employee_time_tracking", indexes = {
    @Index(name = "idx_admin_date", columnList = "admin_id,tracking_date"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_tracking_date", columnList = "tracking_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeTimeTracking {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "admin_id", nullable = false)
    private String adminId;
    
    @Column(name = "admin_name")
    private String adminName;
    
    @Column(name = "admin_email")
    private String adminEmail;
    
    @Column(name = "id_card_number")
    private String idCardNumber;
    
    @Column(name = "tracking_date", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate trackingDate;
    
    @Column(name = "check_in_time")
    private LocalDateTime checkInTime;
    
    @Column(name = "check_out_time")
    private LocalDateTime checkOutTime;
    
    @Column(name = "total_working_hours")
    private Double totalWorkingHours; // In decimal format (e.g., 8.5 hours)
    
    @Column(name = "overtime_hours")
    private Double overtimeHours;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TimeTrackingStatus status; // ACTIVE, CHECKED_OUT, ABSENT, ON_LEAVE
    
    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;
    
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
        if (status == null) {
            status = TimeTrackingStatus.ACTIVE;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public enum TimeTrackingStatus {
        ACTIVE, CHECKED_OUT, ABSENT, ON_LEAVE
    }
}
