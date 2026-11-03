package com.neo.springapp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "savings_upi_transactions")
public class SavingsUpiTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_ref", unique = true, nullable = false)
    private String transactionRef;

    @Column(name = "sender_account")
    private String senderAccount;

    @Column(name = "sender_upi_id")
    private String senderUpiId;

    @Column(name = "sender_name")
    private String senderName;

    @Column(name = "receiver_upi_id", nullable = false)
    private String receiverUpiId;

    @Column(name = "receiver_name")
    private String receiverName;

    @Column(name = "receiver_account")
    private String receiverAccount;

    @Column(nullable = false)
    private BigDecimal amount;

    private String remark;

    @Column(length = 30)
    private String status = "SUCCESS";

    @Column(name = "payment_method", length = 30)
    private String paymentMethod = "UPI";

    @Column(name = "fraud_flagged")
    private Boolean fraudFlagged = false;

    @Column(name = "risk_score")
    private Integer riskScore = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "sender_debited")
    private Boolean senderDebited = false;

    @Column(name = "receiver_credited")
    private Boolean receiverCredited = false;

    @JsonIgnore
    @Column(name = "source_ip", length = 64)
    private String sourceIp;

    @JsonIgnore
    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "location_latitude")
    private Double locationLatitude;

    @Column(name = "location_longitude")
    private Double locationLongitude;

    @Column(name = "location_accuracy_meters")
    private Double locationAccuracyMeters;

    @Column(name = "sender_phone", length = 50)
    private String senderPhone;

    @Column(name = "sender_address", length = 1024)
    private String senderAddress;

    @Column(name = "receiver_phone", length = 50)
    private String receiverPhone;

    @Column(name = "receiver_address", length = 1024)
    private String receiverAddress;
}
