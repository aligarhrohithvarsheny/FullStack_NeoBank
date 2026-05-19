package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "card_action_history")
public class CardActionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "card_id", nullable = false)
    private Long cardId;

    @Column(name = "account_number")
    private String accountNumber;

    @Column(name = "action_type", nullable = false)
    private String actionType; // REPLACE, BLOCK, UNBLOCK, LIMIT_CHANGE

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    @Column(name = "changed_by")
    private String changedBy = "ADMIN";

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
