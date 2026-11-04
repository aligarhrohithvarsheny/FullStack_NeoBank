package com.neo.springapp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "funds_allocation_history")
public class FundsAllocationHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "allocation_record_id", nullable = false)
    private Long allocationRecordId;

    @Column(name = "allocation_reference", nullable = false, length = 50)
    private String allocationReference;

    @Column(name = "event_type", nullable = false, length = 30)
    private String eventType;

    @Column(name = "performed_by_admin_id")
    private Long performedByAdminId;

    @Column(name = "performed_by_name", length = 255)
    private String performedByName;

    @Column(name = "previous_amount")
    private Double previousAmount;

    @Column(name = "new_amount")
    private Double newAmount;

    @Column(name = "previous_valid_till")
    private LocalDate previousValidTill;

    @Column(name = "new_valid_till")
    private LocalDate newValidTill;

    @Column(name = "previous_description", columnDefinition = "TEXT")
    private String previousDescription;

    @Column(name = "new_description", columnDefinition = "TEXT")
    private String newDescription;

    @Column(name = "previous_status", length = 50)
    private String previousStatus;

    @Column(name = "new_status", length = 50)
    private String newStatus;

    @Column(name = "summary", length = 500)
    private String summary;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @PrePersist
    protected void onCreate() {
        if (changedAt == null) changedAt = LocalDateTime.now();
    }
}
