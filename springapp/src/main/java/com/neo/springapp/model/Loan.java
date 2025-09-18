package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "loans")
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String type;
    private Double amount;
    private Integer tenure; // in months
    private Double interestRate;
    private String status = "Pending"; // default status

    // Constructors
    public Loan() {}

    public Loan(String type, Double amount, Integer tenure, Double interestRate) {
        this.type = type;
        this.amount = amount;
        this.tenure = tenure;
        this.interestRate = interestRate;
        this.status = "Pending";
    }

    // Getters and Setters
    
}
