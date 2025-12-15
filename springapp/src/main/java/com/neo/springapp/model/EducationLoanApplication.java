package com.neo.springapp.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "education_loan_applications")
public class EducationLoanApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Parent/Applicant Information
    private String applicantAccountNumber; // Account number of parent applying
    private String applicantName;
    private String applicantEmail;
    private String applicantPan;
    private String applicantAadhar;
    private String applicantMobile;

    // Child/Student Information (18+ to 26 years)
    private String childName;
    private LocalDate childDateOfBirth;
    private String childPlaceOfBirth;
    private Integer childAge; // Calculated from DOB, must be 18-26
    private String childAccountNumber; // Child's neobank account number (if child has account)
    
    // Education Details
    // 10th Standard
    private String tenthSchoolName;
    private String tenthBoard;
    private Integer tenthPassingYear;
    private Double tenthPercentage;
    private String tenthCertificatePath; // File path if uploaded
    
    // 12th Standard
    private String twelfthSchoolName;
    private String twelfthBoard;
    private Integer twelfthPassingYear;
    private Double twelfthPercentage;
    private String twelfthCertificatePath;
    
    // Undergraduate (UG) Details
    private String ugCollegeName;
    private String ugUniversity;
    private String ugCourse;
    private Integer ugAdmissionYear;
    private Integer ugExpectedGraduationYear;
    private Double ugCurrentCGPA;
    private String ugCertificatePath;
    
    // College/University Details for which loan is applied
    private String collegeType; // IIT, IIM, University, College
    private String collegeName;
    private String collegeUniversity;
    private String collegeState;
    private String collegeCity;
    private String collegeAddress;
    private String collegeCourse;
    private String collegeDegree; // UG, PG, PhD, etc.
    private Integer collegeAdmissionYear;
    private Integer collegeCourseDuration; // in years
    private Double collegeFeeAmount; // Total course fee
    
    // College Application Documents
    private String collegeApplicationPath; // Uploaded college application
    private String collegeAdmissionLetterPath; // If available
    private String collegeFeeStructurePath; // Fee structure document
    
    // College Account Details
    private String collegeAccountNumber;
    private String collegeAccountHolderName;
    private String collegeBankName;
    private String collegeBankBranch;
    private String collegeIFSCCode;
    
    // Loan Application Details
    private Long loanId; // Reference to main Loan table
    private String loanAccountNumber;
    private Double requestedLoanAmount;
    private String applicationStatus = "Pending"; // Pending, Under Review, Approved, Rejected
    private LocalDateTime applicationDate;
    private LocalDateTime lastUpdatedDate;
    
    // Admin Review
    private String reviewedBy; // Admin who reviewed
    private LocalDateTime reviewedDate;
    private String adminNotes;
    private String rejectionReason;

    public EducationLoanApplication() {
        this.applicationDate = LocalDateTime.now();
        this.lastUpdatedDate = LocalDateTime.now();
    }
    
    // Calculate age from DOB
    public Integer calculateAge() {
        if (this.childDateOfBirth == null) {
            return null;
        }
        LocalDate today = LocalDate.now();
        int age = today.getYear() - this.childDateOfBirth.getYear();
        if (today.getDayOfYear() < this.childDateOfBirth.getDayOfYear()) {
            age--;
        }
        return age;
    }
    
    // Validate age (18-26)
    public boolean isValidAge() {
        Integer age = calculateAge();
        return age != null && age >= 18 && age <= 26;
    }
}


