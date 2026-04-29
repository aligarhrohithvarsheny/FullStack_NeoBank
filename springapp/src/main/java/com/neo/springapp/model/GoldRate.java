package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "gold_rates")
public class GoldRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private LocalDate date; // Date for which this rate is applicable

    @Column(nullable = false)
    private Double ratePerGram; // Gold rate per gram in INR

    private LocalDateTime lastUpdated; // When this rate was last updated
    private String updatedBy; // Who updated this rate (Admin username)

    // Constructors
    public GoldRate() {
        this.date = LocalDate.now();
        this.lastUpdated = LocalDateTime.now();
    }

    public GoldRate(LocalDate date, Double ratePerGram) {
        this.date = date;
        this.ratePerGram = ratePerGram;
        this.lastUpdated = LocalDateTime.now();
    }
}

