package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "joint_transfer_approvals", indexes = {
        @Index(name = "idx_joint_transfer_account", columnList = "account_number"),
        @Index(name = "idx_joint_transfer_status", columnList = "status")
})
public class JointTransferApproval {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "account_number", nullable = false, length = 32)
    private String accountNumber;
    @Column(name = "from_user_id", nullable = false)
    private Long fromUserId;
    @Column(name = "approver_user_id", nullable = false)
    private Long approverUserId;
    @Column(name = "to_account_number", nullable = false, length = 32)
    private String toAccountNumber;
    @Column(nullable = false)
    private Double amount;
    @Column(length = 500)
    private String note;
    @Column(nullable = false, length = 20)
    private String status = "PENDING";
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime decidedAt;
    private String transactionReference;
}
