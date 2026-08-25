package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "guardian_links", uniqueConstraints = @UniqueConstraint(name = "uk_guardian_child", columnNames = {"guardian_user_id", "child_user_id"}))
public class GuardianLink {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "guardian_user_id", nullable = false)
    private Long guardianUserId;
    @Column(name = "child_user_id", nullable = false)
    private Long childUserId;
    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
