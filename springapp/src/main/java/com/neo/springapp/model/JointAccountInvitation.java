package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "joint_account_invitations", indexes = {
        @Index(name = "idx_joint_invitee", columnList = "invitee_user_id"),
        @Index(name = "idx_joint_account", columnList = "account_number")
})
public class JointAccountInvitation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "account_number", nullable = false, length = 32)
    private String accountNumber;
    @Column(name = "inviter_user_id", nullable = false)
    private Long inviterUserId;
    @Column(name = "invitee_user_id", nullable = false)
    private Long inviteeUserId;
    private String relationship;
    private String operatingMode = "JOINTLY";
    private String jointAccountNumber;
    @Column(nullable = false, length = 20)
    private String status = "PENDING";
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime respondedAt;
}
