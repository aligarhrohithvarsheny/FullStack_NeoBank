package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "cheques")
public class Cheque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String chequeNumber; // Unique cheque number
    private String accountNumber; // Account number of the user
    private String accountHolderName; // Name on the cheque
    private String bankName = "NeoBank"; // Bank name
    private String branchName = "NeoBank Main Branch"; // Branch name
    private String ifscCode = "NEOB0001234"; // IFSC code
    private String micrCode; // MICR code
    private String accountType; // Savings/Current
    
    private Double amount; // Amount to be withdrawn when cheque is drawn
    private String status = "ACTIVE"; // ACTIVE, DRAWN, BOUNCED, CANCELLED
    
    // Request fields
    private String requestStatus = "NONE"; // NONE, PENDING, APPROVED, REJECTED
    private LocalDateTime requestDate; // Date when cheque draw was requested
    private String requestedBy; // User who requested the cheque draw
    private LocalDateTime approvedDate; // Date when request was approved
    private LocalDateTime rejectedDate; // Date when request was rejected
    private String approvedBy; // Admin who approved the request
    private String rejectedBy; // Admin who rejected the request
    private String rejectionReason; // Reason for rejection
    
    private LocalDateTime createdAt;
    private LocalDateTime usedDate;
    private LocalDateTime drawnDate; // Date when cheque was drawn
    private LocalDateTime bouncedDate; // Date when cheque was bounced
    private LocalDateTime cancelledDate;
    
    private String cancelledBy; // User who cancelled
    private String cancellationReason;
    private String drawnBy; // Admin who drew the cheque
    private String bouncedBy; // Admin who bounced the cheque
    private String bounceReason; // Reason for bouncing

    // Constructors
    public Cheque() {
        this.createdAt = LocalDateTime.now();
        this.chequeNumber = generateChequeNumber();
    }

    public Cheque(String accountNumber, String accountHolderName, String accountType) {
        this();
        this.accountNumber = accountNumber;
        this.accountHolderName = accountHolderName;
        this.accountType = accountType;
        this.micrCode = generateMicrCode();
    }

    public Cheque(String accountNumber, String accountHolderName, String accountType, Double amount) {
        this();
        this.accountNumber = accountNumber;
        this.accountHolderName = accountHolderName;
        this.accountType = accountType;
        this.amount = amount;
        this.micrCode = generateMicrCode();
    }

    // Generate unique cheque number
    private String generateChequeNumber() {
        return "CHQ" + System.currentTimeMillis() + String.valueOf((int)(Math.random() * 1000));
    }

    // Generate MICR code
    private String generateMicrCode() {
        return "123456789"; // Default MICR code format
    }

    // Check if cheque can be cancelled
    public boolean canBeCancelled() {
        return "ACTIVE".equals(this.status);
    }

    // Check if cheque can be drawn
    public boolean canBeDrawn() {
        return "ACTIVE".equals(this.status) && "APPROVED".equals(this.requestStatus);
    }

    // Check if cheque can be requested
    public boolean canBeRequested() {
        return "ACTIVE".equals(this.status) && ("NONE".equals(this.requestStatus) || "REJECTED".equals(this.requestStatus));
    }

    // Request cheque drawing
    public void requestDraw(String requestedBy) {
        if (canBeRequested()) {
            this.requestStatus = "PENDING";
            this.requestDate = LocalDateTime.now();
            this.requestedBy = requestedBy;
        }
    }

    // Approve cheque request
    public void approveRequest(String approvedBy) {
        if ("PENDING".equals(this.requestStatus)) {
            this.requestStatus = "APPROVED";
            this.approvedDate = LocalDateTime.now();
            this.approvedBy = approvedBy;
        }
    }

    // Reject cheque request
    public void rejectRequest(String rejectedBy, String reason) {
        if ("PENDING".equals(this.requestStatus)) {
            this.requestStatus = "REJECTED";
            this.rejectedDate = LocalDateTime.now();
            this.rejectedBy = rejectedBy;
            this.rejectionReason = reason;
        }
    }

    // Check if cheque can be bounced
    public boolean canBeBounced() {
        return "ACTIVE".equals(this.status);
    }

    // Cancel cheque
    public void cancel(String cancelledBy, String reason) {
        if (canBeCancelled()) {
            this.status = "CANCELLED";
            this.cancelledDate = LocalDateTime.now();
            this.cancelledBy = cancelledBy;
            this.cancellationReason = reason;
        }
    }

    // Draw cheque (withdraw amount)
    public void draw(String drawnBy) {
        if (canBeDrawn()) {
            this.status = "DRAWN";
            this.drawnDate = LocalDateTime.now();
            this.drawnBy = drawnBy;
            this.usedDate = LocalDateTime.now();
        }
    }

    // Bounce cheque
    public void bounce(String bouncedBy, String reason) {
        if (canBeBounced()) {
            this.status = "BOUNCED";
            this.bouncedDate = LocalDateTime.now();
            this.bouncedBy = bouncedBy;
            this.bounceReason = reason;
        }
    }
}

