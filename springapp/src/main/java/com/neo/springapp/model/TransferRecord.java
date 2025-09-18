package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "transfer_records")
public class TransferRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String accountNumber;
    private String name;
    private String phone;
    private String ifsc;
    private double amount;

    @Enumerated(EnumType.STRING)
    private TransferType transferType;

    private LocalDateTime date;

    // Enum for NEFT/RTGS
    public enum TransferType {
        NEFT, RTGS
    }

    // Lombok @Data annotation provides all getters and setters automatically
}