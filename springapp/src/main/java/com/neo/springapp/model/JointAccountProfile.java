package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "joint_account_profiles", uniqueConstraints = @UniqueConstraint(columnNames = "joint_account_number"))
public class JointAccountProfile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "joint_account_number", nullable = false, unique = true)
    private String jointAccountNumber;
    @Column(nullable = false)
    private Long primaryHolderUserId;
    @Column(nullable = false)
    private Long jointHolderUserId;
    @Column(nullable = false)
    private String operatingMode = "JOINTLY";
    @Column(nullable = false)
    private String status = "ACTIVE";
    private LocalDateTime createdAt = LocalDateTime.now();
}
