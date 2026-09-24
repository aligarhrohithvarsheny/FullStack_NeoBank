package com.neo.springapp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

@Entity
@Data
@Table(name = "branch_cities", uniqueConstraints = @UniqueConstraint(columnNames = "city"))
public class BranchCity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String city;

    private Double turnover = 0D;
    private Double profit = 0D;
    private Integer operations = 0;
    private String status = "PLANNING";
}