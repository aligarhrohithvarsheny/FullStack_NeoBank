package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "pg_merchant_change_logs")
public class PgMerchantChangeLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String merchantId;
    private String merchantName;
    private String changedBy;
    private LocalDateTime changedAt;
    @Column(columnDefinition = "TEXT")
    private String changedFields;
    @Column(columnDefinition = "TEXT")
    private String previousDetails;
    @Column(columnDefinition = "TEXT")
    private String updatedDetails;

    @PrePersist
    protected void onCreate() {
        if (changedAt == null) changedAt = LocalDateTime.now();
    }
}
