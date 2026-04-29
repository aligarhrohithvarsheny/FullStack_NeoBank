package com.neo.springapp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "salary_accounts")
public class SalaryAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_name", nullable = false)
    private String employeeName;

    private String dob;

    @Column(name = "mobile_number")
    private String mobileNumber;

    private String email;

    @Column(name = "aadhar_number")
    private String aadharNumber;

    @Column(name = "pan_number")
    private String panNumber;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "company_id")
    private String companyId;

    @Column(name = "employer_address")
    private String employerAddress;

    @Column(name = "hr_contact_number")
    private String hrContactNumber;

    @Column(name = "monthly_salary")
    private Double monthlySalary = 0.0;

    @Column(name = "salary_credit_date")
    private Integer salaryCreditDate = 1;

    private String designation;

    @Column(name = "account_number", unique = true)
    private String accountNumber;

    @Column(name = "customer_id", unique = true)
    private String customerId;

    @Column(name = "debit_card_number")
    private String debitCardNumber;

    @Column(name = "debit_card_cvv")
    private String debitCardCvv;

    @Column(name = "debit_card_expiry")
    private String debitCardExpiry;

    @Column(name = "net_banking_enabled")
    private Boolean netBankingEnabled = true;

    @Column(name = "branch_name")
    private String branchName;

    @Column(name = "ifsc_code")
    private String ifscCode;

    private Double balance = 0.0;

    private String status = "Active";

    @JsonIgnore
    private String password;

    @Column(name = "password_set")
    private Boolean passwordSet = false;

    @JsonIgnore
    @Column(name = "transaction_pin")
    private String transactionPin;

    @Column(name = "transaction_pin_set")
    private Boolean transactionPinSet = false;

    private String address;

    @Column(name = "upi_id")
    private String upiId;

    @Column(name = "upi_enabled")
    private Boolean upiEnabled = false;

    @Column(name = "auto_savings_enabled")
    private Boolean autoSavingsEnabled = false;

    @Column(name = "auto_savings_percentage")
    private Double autoSavingsPercentage = 10.0;

    @Column(name = "savings_balance")
    private Double savingsBalance = 0.0;

    @Column(name = "debit_card_status")
    private String debitCardStatus = "Active";

    @Column(name = "daily_limit")
    private Double dailyLimit = 50000.0;

    @Column(name = "international_enabled")
    private Boolean internationalEnabled = false;

    @Column(name = "online_enabled")
    private Boolean onlineEnabled = true;

    @Column(name = "contactless_enabled")
    private Boolean contactlessEnabled = true;

    @Column(name = "employee_id", unique = true)
    private String employeeId;

    @Column(name = "employee_id_linked")
    private Boolean employeeIdLinked = false;

    @Column(name = "failed_login_attempts")
    private Integer failedLoginAttempts = 0;

    @Column(name = "account_locked")
    private Boolean accountLocked = false;

    @Column(name = "last_failed_login_time")
    private LocalDateTime lastFailedLoginTime;

    @Column(name = "lock_reason")
    private String lockReason;

    // Signature fields
    @Column(name = "signature_copy_path")
    private String signatureCopyPath;

    @Column(name = "signature_uploaded_at")
    private LocalDateTime signatureUploadedAt;

    @Column(name = "signature_verified")
    private Boolean signatureVerified = false;

    @Column(name = "signature_verified_by")
    private String signatureVerifiedBy;

    @Column(name = "signature_verified_at")
    private LocalDateTime signatureVerifiedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "closed_reason")
    private String closedReason;

    @Column(name = "closed_by")
    private String closedBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public SalaryAccount() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
