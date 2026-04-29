package com.neo.springapp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "insurance_applications")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class InsuranceApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String applicationNumber;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "policy_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private InsurancePolicy policy;

    @Column(nullable = false)
    private Long userId; // Reference to User.id

    @Column(nullable = false)
    private String accountNumber; // Convenience for lookups

    @Column(nullable = false)
    private String nomineeName;

    private String nomineeRelation;

    // Store file path or URL; actual storage handled elsewhere
    private String kycDocumentPath;

    @Column(nullable = false)
    private String premiumType; // MONTHLY / YEARLY

    // Additional underwriting / health information
    private Integer proposerAge;

    @Column(columnDefinition = "TEXT")
    private String healthConditions; // e.g. existing diseases, conditions

    @Column(columnDefinition = "TEXT")
    private String lifestyleHabits; // e.g. alcohol / smoking / exercise

    private Boolean hasExistingEmis; // whether customer has existing EMIs

    private Double fraudScore; // 0.0 (low) - 1.0 (high)

    @Column(nullable = false)
    private String paymentStatus = "NOT_PAID"; // NOT_PAID / COMPLETED

    private Boolean createdByAdmin = false;

    private Boolean autoDebitRequested = false;
    private Boolean autoDebitApproved = false;

    private Double premiumAmountCalculated; // premium after risk/age/disease adjustments

    @Column(nullable = false)
    private String status = "PENDING_APPROVAL"; // PENDING_APPROVAL / ACTIVE / REJECTED / EXPIRED

    @Column(columnDefinition = "TEXT")
    private String adminRemark;

    private LocalDateTime appliedAt;
    private LocalDateTime approvedAt;

    public InsuranceApplication() {
        this.appliedAt = LocalDateTime.now();
        if (this.applicationNumber == null || this.applicationNumber.isEmpty()) {
            this.applicationNumber = "APP" + System.currentTimeMillis();
        }
    }
}

