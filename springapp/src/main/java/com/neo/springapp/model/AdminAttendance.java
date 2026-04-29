package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
@Table(
    name = "admin_attendance",
    uniqueConstraints = @UniqueConstraint(columnNames = {"adminId", "attendanceDate"})
)
public class AdminAttendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long adminId;
    private String adminName;
    private String adminEmail;
    private String idCardNumber;

    private LocalDate attendanceDate;
    private Boolean present = true;

    private LocalDateTime verifiedAt;
    private String verifiedByManager;

    // Whether this day counts as a full paid day (DB column is counted_as_full_day, non-null)
    @Column(name = "counted_as_full_day")
    private Boolean countedAsFullDay = true;

    // Session duration in minutes for this admin day (DB column: session_duration_minutes, non-null)
    @Column(name = "session_duration_minutes")
    private Integer sessionDurationMinutes = 0;

    // Audit timestamps (map to created_at, last_updated in DB)
    private LocalDateTime createdAt;
    private LocalDateTime lastUpdated;

    // Legacy column used in table: login_time (non-null in DB)
    @Column(name = "login_time")
    private LocalDateTime loginTime;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (lastUpdated == null) lastUpdated = now;
        if (loginTime == null) loginTime = now;
        if (verifiedAt == null) verifiedAt = now;
        if (present == null) present = true;
        if (countedAsFullDay == null) countedAsFullDay = true;
        if (sessionDurationMinutes == null) sessionDurationMinutes = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
        if (present == null) present = true;
        if (countedAsFullDay == null) countedAsFullDay = true;
        if (sessionDurationMinutes == null) sessionDurationMinutes = 0;
    }
}

