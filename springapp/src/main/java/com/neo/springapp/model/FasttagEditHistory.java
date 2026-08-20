package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "fasttag_edit_history")
public class FasttagEditHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fasttag_id", nullable = false)
    private Long fasttagId;

    @Column(name = "fasttag_number", length = 100)
    private String fasttagNumber;

    @Column(name = "changed_by", length = 255)
    private String changedBy;

    @Column(nullable = false)
    private LocalDateTime changedAt = LocalDateTime.now();

    @Column(name = "before_values", columnDefinition = "TEXT")
    private String beforeValues;

    @Column(name = "after_values", columnDefinition = "TEXT")
    private String afterValues;
}
