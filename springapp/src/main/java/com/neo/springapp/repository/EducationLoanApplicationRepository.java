package com.neo.springapp.repository;

import com.neo.springapp.model.EducationLoanApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EducationLoanApplicationRepository extends JpaRepository<EducationLoanApplication, Long> {
    
    // Find by applicant account number
    List<EducationLoanApplication> findByApplicantAccountNumber(String accountNumber);
    
    // Find by loan ID
    Optional<EducationLoanApplication> findByLoanId(Long loanId);
    
    // Find by loan account number
    Optional<EducationLoanApplication> findByLoanAccountNumber(String loanAccountNumber);
    
    // Find by application status
    List<EducationLoanApplication> findByApplicationStatus(String status);
    
    // Find by child name
    List<EducationLoanApplication> findByChildNameContainingIgnoreCase(String childName);
    
    // Find by college name
    List<EducationLoanApplication> findByCollegeNameContainingIgnoreCase(String collegeName);
    
    // Find by state and city
    List<EducationLoanApplication> findByCollegeStateAndCollegeCity(String state, String city);
    
    // Find pending applications
    @Query("SELECT e FROM EducationLoanApplication e WHERE e.applicationStatus = 'Pending' ORDER BY e.applicationDate DESC")
    List<EducationLoanApplication> findPendingApplications();
    
    // Find by college type
    List<EducationLoanApplication> findByCollegeType(String collegeType);
}





