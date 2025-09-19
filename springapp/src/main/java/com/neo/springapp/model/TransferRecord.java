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

    private String transferId; // Custom transfer ID
    private String senderAccountNumber;
    private String senderName;
    private String recipientAccountNumber;
    private String recipientName;
    private String phone;
    private String ifsc;
    private Double amount;
    private String status = "Pending"; // Pending / Completed / Failed

    @Enumerated(EnumType.STRING)
    private TransferType transferType;

    private LocalDateTime date;

    // Enum for NEFT/RTGS
    public enum TransferType {
        NEFT, RTGS
    }

    // Constructors
    public TransferRecord() {
        this.date = LocalDateTime.now();
        this.transferId = "TRF" + System.currentTimeMillis();
    }

    public TransferRecord(String senderAccountNumber, String senderName, String recipientAccountNumber, 
                         String recipientName, String phone, String ifsc, Double amount, TransferType transferType) {
        this();
        this.senderAccountNumber = senderAccountNumber;
        this.senderName = senderName;
        this.recipientAccountNumber = recipientAccountNumber;
        this.recipientName = recipientName;
        this.phone = phone;
        this.ifsc = ifsc;
        this.amount = amount;
        this.transferType = transferType;
    }
}