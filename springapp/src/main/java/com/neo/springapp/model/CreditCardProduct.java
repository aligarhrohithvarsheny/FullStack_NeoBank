package com.neo.springapp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "credit_card_products", uniqueConstraints = {
        @UniqueConstraint(name = "uk_credit_card_product_code", columnNames = "product_code")
})
public class CreditCardProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_code", nullable = false, length = 40)
    private String productCode;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false, length = 40)
    private String theme;

    @Column(columnDefinition = "TEXT")
    private String featuresText;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String eligibility;

    @Column(name = "terms_and_conditions", nullable = false, columnDefinition = "LONGTEXT")
    private String termsAndConditions;

    @Column(name = "image_data", columnDefinition = "LONGTEXT")
    private String imageData;

    @Column(name = "annual_fee", nullable = false)
    private Double annualFee = 0d;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "updated_by", length = 120)
    private String updatedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void updateTimestamp() {
        updatedAt = LocalDateTime.now();
    }
}
