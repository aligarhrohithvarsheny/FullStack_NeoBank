package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "home_loan_events")
public class HomeLoanEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long homeLoanId;
    private String applicationId;
    private String accountNumber;

    // SUBMITTED, STATUS, EDITED, DOCUMENT, DISBURSED, EMI, INTEREST, PREPAYMENT, CLOSURE, RENEWED
    private String eventType;
    private String actor;
    private LocalDateTime eventDate = LocalDateTime.now();

    @Column(length = 2000)
    private String details;

    private Double debit;
    private Double credit;
    private Double principalComponent;
    private Double interestComponent;
    private Double balanceAfter; // outstanding principal after the event
}
