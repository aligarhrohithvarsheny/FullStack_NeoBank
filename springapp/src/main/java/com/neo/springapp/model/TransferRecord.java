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

    // Global transaction ID unique across entire banking system
    private Long globalTransactionSequence;

    private String transferId; // Custom transfer ID
    private String senderAccountNumber;
    private String senderName;
    private String recipientAccountNumber;
    private String recipientName;
    private String phone;
    private String ifsc;
    private Double amount;
    private String status = "Pending"; // Pending / Completed / Failed / Cancelled

    @Enumerated(EnumType.STRING)
    private TransferType transferType;

    private LocalDateTime date;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime scheduledProcessingTime; // For NEFT - minimum 2 hours after creation
    private LocalDateTime processedAt; // Actual processing time
    private Boolean isCancellable = true;

    // Enum for NEFT/RTGS/IMPS
    public enum TransferType {
        NEFT, RTGS, IMPS
    }

    // Constructors
    public TransferRecord() {
        this.date = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
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

    // Method to check if transfer can be cancelled (within 3 minutes for NEFT/IMPS)
    public boolean canBeCancelled() {
        if (!isCancellable || !"Completed".equals(status)) {
            return false;
        }
        
        // Only NEFT and IMPS can be cancelled
        if (transferType != TransferType.NEFT && transferType != TransferType.IMPS) {
            return false;
        }
        
        // Check if within 3 minutes
        LocalDateTime threeMinutesAgo = LocalDateTime.now().minusMinutes(3);
        return createdAt.isAfter(threeMinutesAgo);
    }

    // Method to check if transfer is IMPS (immediate)
    public boolean isImmediateTransfer() {
        return transferType == TransferType.IMPS;
    }

    // Method to check if NEFT transfer can be processed (minimum 2 hours after creation)
    public boolean canProcessNeft() {
        if (transferType != TransferType.NEFT) {
            return false;
        }
        if (scheduledProcessingTime == null) {
            return false;
        }
        return LocalDateTime.now().isAfter(scheduledProcessingTime) || 
               LocalDateTime.now().isEqual(scheduledProcessingTime);
    }
}