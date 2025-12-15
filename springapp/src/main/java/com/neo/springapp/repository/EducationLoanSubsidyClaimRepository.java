package com.neo.springapp.repository;

import com.neo.springapp.model.EducationLoanSubsidyClaim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EducationLoanSubsidyClaimRepository extends JpaRepository<EducationLoanSubsidyClaim, Long> {
    
    // Find by user account number
    List<EducationLoanSubsidyClaim> findByAccountNumber(String accountNumber);
    
    // Find by loan ID (single claim - for backward compatibility)
    Optional<EducationLoanSubsidyClaim> findByLoanId(Long loanId);
    
    // Find all claims by loan ID
    List<EducationLoanSubsidyClaim> findAllByLoanId(Long loanId);
    
    // Find by loan account number
    List<EducationLoanSubsidyClaim> findByLoanAccountNumber(String loanAccountNumber);
    
    // Find by status
    List<EducationLoanSubsidyClaim> findByStatus(String status);
    
    // Find by user ID
    List<EducationLoanSubsidyClaim> findByUserId(String userId);
    
    // Find pending claims
    @Query("SELECT c FROM EducationLoanSubsidyClaim c WHERE c.status = 'Pending' ORDER BY c.requestDate DESC")
    List<EducationLoanSubsidyClaim> findPendingClaims();
    
    // Find approved but not credited claims
    @Query("SELECT c FROM EducationLoanSubsidyClaim c WHERE c.status = 'Approved' ORDER BY c.processedDate DESC")
    List<EducationLoanSubsidyClaim> findApprovedButNotCreditedClaims();
}

