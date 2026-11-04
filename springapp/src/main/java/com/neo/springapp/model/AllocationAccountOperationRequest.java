package com.neo.springapp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "allocation_account_operation_requests")
public class AllocationAccountOperationRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "allocation_record_id", nullable = false)
    private Long allocationRecordId;

    @Column(name = "allocation_reference", nullable = false, length = 50)
    private String allocationReference;

    @Column(name = "request_type", nullable = false, length = 20)
    private String requestType;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "destination_account_number", length = 50)
    private String destinationAccountNumber;

    @Column(name = "destination_name", length = 150)
    private String destinationName;

    @Column(name = "external_reference", length = 100)
    private String externalReference;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "reviewed_by_admin_id")
    private Long reviewedByAdminId;

    @Column(name = "reviewed_by_name", length = 255)
    private String reviewedByName;

    @Column(name = "review_note", length = 500)
    private String reviewNote;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (status == null) status = "PENDING";
    }
}
