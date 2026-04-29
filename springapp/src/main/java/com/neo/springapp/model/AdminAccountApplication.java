package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "admin_account_applications")
public class AdminAccountApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, name = "application_number")
    private String applicationNumber;

    @Column(nullable = false, name = "account_type")
    private String accountType = "Savings"; // Savings, Current, Salary

    @Column(nullable = false)
    private String status = "PENDING"; // PENDING, DOCUMENTS_UPLOADED, ADMIN_VERIFIED, MANAGER_APPROVED, MANAGER_REJECTED, ACTIVE, CLOSED

    // Personal / Owner Details
    @Column(nullable = false, name = "full_name")
    private String fullName;

    @Column(name = "date_of_birth")
    private String dateOfBirth;

    private Integer age;
    private String gender;
    private String occupation;
    private Double income;

    @Column(nullable = false)
    private String phone;
    private String email;

    @Column(columnDefinition = "TEXT")
    private String address;
    private String city;
    private String state;
    private String pincode;

    // Identity Documents
    @Column(nullable = false, name = "aadhar_number")
    private String aadharNumber;

    @Column(nullable = false, name = "pan_number")
    private String panNumber;

    // Business Details (for Current accounts)
    @Column(name = "business_name")
    private String businessName;

    @Column(name = "business_type")
    private String businessType;

    @Column(name = "business_registration_number")
    private String businessRegistrationNumber;

    @Column(name = "gst_number")
    private String gstNumber;

    @Column(columnDefinition = "TEXT", name = "shop_address")
    private String shopAddress;

    // Salary Details (for Salary accounts)
    @Column(name = "company_name")
    private String companyName;

    @Column(name = "company_id")
    private String companyId;

    private String designation;

    @Column(name = "monthly_salary")
    private Double monthlySalary;

    @Column(name = "salary_credit_date")
    private Integer salaryCreditDate;

    @Column(columnDefinition = "TEXT", name = "employer_address")
    private String employerAddress;

    @Column(name = "hr_contact_number")
    private String hrContactNumber;

    // Bank Details
    @Column(name = "branch_name")
    private String branchName = "NeoBank Main Branch";

    @Column(name = "ifsc_code")
    private String ifscCode = "EZYV000123";

    @Column(unique = true, name = "account_number")
    private String accountNumber;

    @Column(unique = true, name = "customer_id")
    private String customerId;

    // Verification
    @Column(name = "admin_verified")
    private Boolean adminVerified = false;

    @Column(name = "admin_verified_by")
    private String adminVerifiedBy;

    @Column(name = "admin_verified_date")
    private LocalDateTime adminVerifiedDate;

    @Column(columnDefinition = "TEXT", name = "admin_remarks")
    private String adminRemarks;

    // Manager Approval
    @Column(name = "manager_approved")
    private Boolean managerApproved = false;

    @Column(name = "manager_approved_by")
    private String managerApprovedBy;

    @Column(name = "manager_approved_date")
    private LocalDateTime managerApprovedDate;

    @Column(columnDefinition = "TEXT", name = "manager_remarks")
    private String managerRemarks;

    // Document Paths
    @Column(name = "application_pdf_path")
    private String applicationPdfPath;

    @Column(name = "signed_application_path")
    private String signedApplicationPath;

    @Column(name = "additional_documents_path")
    private String additionalDocumentsPath;

    // Signatures & Declaration
    @Column(name = "applicant_signature_path")
    private String applicantSignaturePath;

    @Column(name = "bank_officer_signature")
    private String bankOfficerSignature;

    @Column(name = "declaration_accepted")
    private Boolean declarationAccepted = false;

    @Column(name = "declaration_date")
    private LocalDateTime declarationDate;

    // Audit
    @Column(nullable = false, name = "created_by")
    private String createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public AdminAccountApplication() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
