package com.neo.springapp.repository;

import com.neo.springapp.model.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    
    // Basic queries
    Account findByPan(String pan);
    Account findByAccountNumber(String accountNumber);
    Account findByAadharNumber(String aadharNumber);
    List<Account> findByPhone(String phone);
    Account findByAadharVerificationReference(String verificationReference);
    
    // Status-based queries
    List<Account> findByStatus(String status);
    Page<Account> findByStatus(String status, Pageable pageable);
    
    // Account type queries
    List<Account> findByAccountType(String accountType);
    Page<Account> findByAccountType(String accountType, Pageable pageable);
    
    // KYC verification queries
    List<Account> findByKycVerified(boolean kycVerified);
    List<Account> findByVerifiedMatrix(boolean verifiedMatrix);
    
    // Income range queries
    @Query("SELECT a FROM Account a WHERE a.income BETWEEN :minIncome AND :maxIncome")
    List<Account> findByIncomeRange(@Param("minIncome") Double minIncome, @Param("maxIncome") Double maxIncome);
    
    // Balance range queries
    @Query("SELECT a FROM Account a WHERE a.balance BETWEEN :minBalance AND :maxBalance")
    List<Account> findByBalanceRange(@Param("minBalance") Double minBalance, @Param("maxBalance") Double maxBalance);
    
    // Occupation-based queries
    List<Account> findByOccupation(String occupation);
    
    // Search queries
    @Query("SELECT a FROM Account a WHERE " +
           "LOWER(a.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(a.accountNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(a.pan) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(a.aadharNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(a.phone) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Account> searchAccounts(@Param("searchTerm") String searchTerm, Pageable pageable);
    
    // Date range queries
    @Query("SELECT a FROM Account a WHERE a.createdAt BETWEEN :startDate AND :endDate")
    List<Account> findByCreatedDateRange(@Param("startDate") LocalDateTime startDate, 
                                        @Param("endDate") LocalDateTime endDate);
    
    // Statistics queries
    @Query("SELECT COUNT(a) FROM Account a WHERE a.status = :status")
    Long countByStatus(@Param("status") String status);
    
    @Query("SELECT COUNT(a) FROM Account a WHERE a.kycVerified = true")
    Long countKycVerifiedAccounts();
    
    @Query("SELECT COUNT(a) FROM Account a WHERE a.verifiedMatrix = true")
    Long countVerifiedMatrixAccounts();
    
    @Query("SELECT AVG(a.balance) FROM Account a WHERE a.status = :status")
    Double getAverageBalanceByStatus(@Param("status") String status);
    
    @Query("SELECT SUM(a.balance) FROM Account a WHERE a.status = :status")
    Double getTotalBalanceByStatus(@Param("status") String status);
    
    @Query("SELECT AVG(a.income) FROM Account a WHERE a.status = :status")
    Double getAverageIncomeByStatus(@Param("status") String status);
    
    // Recent accounts
    @Query("SELECT a FROM Account a ORDER BY a.createdAt DESC")
    List<Account> findRecentAccounts(Pageable pageable);
    
    // Aadhar verification queries
    @Query("SELECT a FROM Account a WHERE a.aadharVerified = false OR a.aadharVerificationStatus = 'PENDING'")
    List<Account> findByAadharVerifiedFalseOrAadharVerificationStatus(String status);
    
    List<Account> findByAadharVerificationStatus(String status);
}
