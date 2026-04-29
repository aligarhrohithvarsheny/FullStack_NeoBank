package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "preloaded_customer_data")
public class PreloadedCustomerData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "aadhar_number", nullable = false)
    private String aadharNumber;

    @Column(name = "pan_number")
    private String panNumber;

    @Column(name = "account_type")
    private String accountType = "Savings";

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "date_of_birth")
    private String dateOfBirth;

    private Integer age;
    private String gender;
    private String occupation;
    private Double income;

    private String phone;
    private String email;

    @Column(columnDefinition = "TEXT")
    private String address;

    private String city;
    private String state;
    private String pincode;

    // Current Account fields
    @Column(name = "business_name")
    private String businessName;

    @Column(name = "business_type")
    private String businessType;

    @Column(name = "business_registration_number")
    private String businessRegistrationNumber;

    @Column(name = "gst_number")
    private String gstNumber;

    @Column(name = "shop_address", columnDefinition = "TEXT")
    private String shopAddress;

    // Salary Account fields
    @Column(name = "company_name")
    private String companyName;

    @Column(name = "company_id")
    private String companyId;

    private String designation;

    @Column(name = "monthly_salary")
    private Double monthlySalary;

    @Column(name = "salary_credit_date")
    private Integer salaryCreditDate;

    @Column(name = "hr_contact_number")
    private String hrContactNumber;

    @Column(name = "employer_address", columnDefinition = "TEXT")
    private String employerAddress;

    // Bank Details
    @Column(name = "branch_name")
    private String branchName = "NeoBank Main Branch";

    @Column(name = "ifsc_code")
    private String ifscCode = "EZYV000123";

    // Upload tracking
    @Column(name = "uploaded_by")
    private String uploadedBy;

    @Column(name = "upload_batch_id")
    private String uploadBatchId;

    @Column(name = "upload_file_name")
    private String uploadFileName;

    private Boolean used = false;

    @Column(name = "used_by")
    private String usedBy;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
