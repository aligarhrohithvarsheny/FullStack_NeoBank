package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "family_banking_notifications", indexes = {
        @Index(name = "idx_family_notification_recipient", columnList = "recipient_user_id,read_at")
})
public class FamilyBankingNotification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "recipient_user_id", nullable = false)
    private Long recipientUserId;
    @Column(nullable = false, length = 40)
    private String type;
    @Column(nullable = false, length = 120)
    private String title;
    @Column(nullable = false, length = 1000)
    private String message;
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime readAt;
}