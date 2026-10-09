package com.neo.springapp.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "guest_insurance_upi_payments")
public class GuestInsurancePayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guest_application_id", nullable = false)
    private GuestInsuranceApplication application;

    @Column(nullable = false, unique = true)
    private String transactionReference;

    @Column(nullable = false)
    private String payerAccountLastFour;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String status = "SUCCESS";

    @Column(nullable = false)
    private LocalDateTime paidAt = LocalDateTime.now();

    public Long getId() { return id; }
    public GuestInsuranceApplication getApplication() { return application; }
    public void setApplication(GuestInsuranceApplication application) { this.application = application; }
    public String getTransactionReference() { return transactionReference; }
    public void setTransactionReference(String transactionReference) { this.transactionReference = transactionReference; }
    public String getPayerAccountLastFour() { return payerAccountLastFour; }
    public void setPayerAccountLastFour(String payerAccountLastFour) { this.payerAccountLastFour = payerAccountLastFour; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
}
