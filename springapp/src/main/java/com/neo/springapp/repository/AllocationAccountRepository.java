package com.neo.springapp.repository;

import com.neo.springapp.entity.AllocationAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for AllocationAccount
 * Manages persistence of linked bank accounts
 */
@Repository
public interface AllocationAccountRepository extends JpaRepository<AllocationAccount, Long> {
    
    // Find by allocation ID
    @Query("SELECT a FROM AllocationAccount a WHERE a.allocationId = :allocationId")
    Optional<AllocationAccount> findByAllocationId(@Param("allocationId") Long allocationId);
    
    // Find by account number (unique)
    @Query("SELECT a FROM AllocationAccount a WHERE a.accountNumber = :accountNumber")
    Optional<AllocationAccount> findByAccountNumber(@Param("accountNumber") String accountNumber);
    
    // Find by IFSC code
    @Query("SELECT a FROM AllocationAccount a WHERE a.ifscCode = :ifscCode")
    List<AllocationAccount> findByIfscCode(@Param("ifscCode") String ifscCode);
    
    // Find by cheque number
    @Query("SELECT a FROM AllocationAccount a WHERE a.chequeNumber = :chequeNumber")
    Optional<AllocationAccount> findByChequeNumber(@Param("chequeNumber") String chequeNumber);
    
    // Find verified accounts
    @Query("SELECT a FROM AllocationAccount a WHERE a.verificationStatus = 'VERIFIED' AND a.accountStatus = 'ACTIVE'")
    List<AllocationAccount> findVerifiedAccounts();
    
    // Find pending verification accounts
    @Query("SELECT a FROM AllocationAccount a WHERE a.verificationStatus = 'PENDING' ORDER BY a.linkedAt DESC")
    List<AllocationAccount> findPendingVerificationAccounts();
    
    // Find accounts by city and branch
        @Query("SELECT a FROM AllocationAccount a WHERE LOWER(a.city) = LOWER(:city) " +
            "AND (:branch IS NULL OR :branch = '' OR LOWER(a.branchName) = LOWER(:branch)) " +
            "ORDER BY a.linkedAt DESC")
    List<AllocationAccount> findByLocationAndBranch(
        @Param("city") String city, 
        @Param("branch") String branch
    );
    
    // Find accounts by manager (through allocation)
    @Query("SELECT a FROM AllocationAccount a WHERE a.allocationId IN (SELECT fa.id FROM FundsAllocation fa WHERE fa.managerId = :managerId)")
    List<AllocationAccount> findByManagerId(@Param("managerId") Long managerId);
    
    // Find all active accounts with verified status
    @Query("SELECT a FROM AllocationAccount a WHERE a.accountStatus = 'ACTIVE' AND a.verificationStatus = 'VERIFIED' ORDER BY a.linkedAt DESC")
    List<AllocationAccount> findAllActiveVerifiedAccounts();
    
    // Count by cheque status
    @Query("SELECT COUNT(a) FROM AllocationAccount a WHERE a.chequeStatus = :chequeStatus")
    Long countByChequeStatus(@Param("chequeStatus") String chequeStatus);
    
    // Find accounts with collected charges
    @Query("SELECT a FROM AllocationAccount a WHERE a.totalChargesCollected > 0 ORDER BY a.totalChargesCollected DESC")
    List<AllocationAccount> findAccountsWithCharges();
}
