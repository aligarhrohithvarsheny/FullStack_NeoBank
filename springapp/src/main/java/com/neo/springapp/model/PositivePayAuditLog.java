package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "positive_pay_audit_logs", indexes = {
        @Index(name = "idx_pp_audit_reference", columnList = "reference_number"),
        @Index(name = "idx_pp_audit_timestamp", columnList = "timestamp")
})
public class PositivePayAuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "reference_number", nullable = false) private String referenceNumber;
    @Column(nullable = false) private String action;
    private String performedBy;
    private String performedByRole;
    private String ipAddress;
    @Column(columnDefinition = "TEXT") private String remarks;
    @Column(name = "timestamp", nullable = false) private LocalDateTime timestamp;
    @PrePersist void onCreate() { if (timestamp == null) timestamp = LocalDateTime.now(); }
}
