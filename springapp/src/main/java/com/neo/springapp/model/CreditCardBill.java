package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "credit_card_bills")
public class CreditCardBill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long creditCardId;
    private String cardNumber;
    private String accountNumber;
    private String userName;
    
    private LocalDateTime billGenerationDate;
    private LocalDateTime dueDate;
    private LocalDateTime paidDate;
    
    private Double totalAmount; // Total bill amount
    private Double minimumDue; // Minimum due amount
    private Double paidAmount; // Amount paid
    private Double overdueAmount; // Overdue amount
    private Double fine; // Fine charged
    private Double penalty; // Penalty charged
    
    private String status; // Generated, Paid, Overdue, Partial
    private String billingPeriod; // e.g., "Jan 2024"
    
    public CreditCardBill() {
        this.billGenerationDate = LocalDateTime.now();
        this.status = "Generated";
    }
}
