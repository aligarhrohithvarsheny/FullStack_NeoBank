package com.neo.springapp.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "gold_rate_history")
public class GoldRateHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Double ratePerGram; // Gold rate per gram in INR

    @Column(nullable = false)
    private Double previousRate; // Previous rate before this change

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(nullable = false)
    private LocalDateTime changedAt; // When this rate was changed

    @Column(nullable = false)
    private String changedBy; // Who changed this rate (Admin username)

    @Column(length = 500)
    private String notes; // Optional notes about the change

    // Reference to the date for which this rate applies
    @Column(nullable = false)
    private java.time.LocalDate rateDate;

    // Constructors
    public GoldRateHistory() {
        this.changedAt = LocalDateTime.now();
    }

    public GoldRateHistory(Double ratePerGram, Double previousRate, String changedBy, java.time.LocalDate rateDate) {
        this.ratePerGram = ratePerGram;
        this.previousRate = previousRate;
        this.changedBy = changedBy;
        this.rateDate = rateDate;
        this.changedAt = LocalDateTime.now();
    }
}





