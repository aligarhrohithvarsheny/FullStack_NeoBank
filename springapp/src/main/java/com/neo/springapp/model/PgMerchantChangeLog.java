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
    @Column(length = 10000)
    private String changedFields;
    @Column(length = 20000)
    private String previousDetails;
    @Column(length = 20000)
    private String updatedDetails;

    @PrePersist
    protected void onCreate() {
        if (changedAt == null) changedAt = LocalDateTime.now();
    }
}
