package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "ecs_mandate_events")
public class EcsMandateEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long mandateDbId;
    private String mandateId;
    private String loanAccountNumber;
    private String savingsAccountNumber;

    // LINKED, DEBIT_SUCCESS, DEBIT_FAILED, PAUSED, RESUMED, DATE_CHANGED, LIMIT_CHANGED,
    // ACCOUNT_CHANGED, CANCEL_REQUESTED, CANCEL_REJECTED, CANCELLED
    private String eventType;
    private Integer emiNumber;
    private Double amount;
    private String message;
    private String actor;
    private LocalDateTime createdAt = LocalDateTime.now();
}
