package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/** Single-row admin-configurable settings for credit card operations (e.g. transfer-to-account fee). */
@Entity
@Data
@Table(name = "credit_card_settings")
public class CreditCardSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Percentage fee deducted from every credit-card-to-account transfer. Admin editable. Default 2%. */
    private Double transferFeePercent = 2.0;

    private String updatedBy;
    private LocalDateTime updatedAt;
}
