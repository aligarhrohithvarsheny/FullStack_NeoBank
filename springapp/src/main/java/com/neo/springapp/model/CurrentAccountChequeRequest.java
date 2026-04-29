package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "current_account_cheque_requests")
public class CurrentAccountChequeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String requestId;

    @Column(nullable = false)
    private String accountNumber;

    private Integer leaves = 25;
    private String deliveryAddress;
    private String status = "PENDING";
    private LocalDateTime requestedAt;
    private LocalDateTime approvedAt;
    private String approvedBy;

    @PrePersist
    protected void onCreate() {
        requestedAt = LocalDateTime.now();
        if (requestId == null) {
            requestId = "CHQ" + String.format("%05d", (int) (Math.random() * 100000));
        }
    }
}
