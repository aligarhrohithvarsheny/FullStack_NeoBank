package com.neo.springapp.repository;

import com.neo.springapp.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // Basic queries
    Optional<User> findByUsername(String username);
    Optional<User> findByAccountNumber(String accountNumber);
    Optional<User> findByEmail(String email);
    
    // PAN and Aadhar queries through account relationship
    @Query("SELECT u FROM User u LEFT JOIN u.account a WHERE a.pan = :pan")
    Optional<User> findByPan(@Param("pan") String pan);
    
    @Query("SELECT u FROM User u LEFT JOIN u.account a WHERE a.aadharNumber = :aadhar")
    Optional<User> findByAadhar(@Param("aadhar") String aadhar);
    
    // Status-based queries
    List<User> findByStatus(String status);
    Page<User> findByStatus(String status, Pageable pageable);
    
    // Search queries
    @Query("SELECT u FROM User u LEFT JOIN u.account a WHERE " +
           "LOWER(a.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.accountNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(a.pan) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<User> searchUsers(@Param("searchTerm") String searchTerm, Pageable pageable);
    
    // Income range queries
    @Query("SELECT u FROM User u LEFT JOIN u.account a WHERE a.income BETWEEN :minIncome AND :maxIncome")
    List<User> findByIncomeRange(@Param("minIncome") Double minIncome, @Param("maxIncome") Double maxIncome);
    
    // Occupation-based queries
    @Query("SELECT u FROM User u LEFT JOIN u.account a WHERE a.occupation = :occupation")
    List<User> findByOccupation(@Param("occupation") String occupation);
    
    // Account type queries
    @Query("SELECT u FROM User u LEFT JOIN u.account a WHERE a.accountType = :accountType")
    List<User> findByAccountType(@Param("accountType") String accountType);
    
    // Date range queries
    @Query("SELECT u FROM User u WHERE u.joinDate BETWEEN :startDate AND :endDate")
    List<User> findByJoinDateRange(@Param("startDate") java.time.LocalDateTime startDate, 
                                   @Param("endDate") java.time.LocalDateTime endDate);
    
    // Statistics queries
    @Query("SELECT COUNT(u) FROM User u WHERE u.status = :status")
    Long countByStatus(@Param("status") String status);
    
    @Query("SELECT AVG(a.income) FROM User u LEFT JOIN u.account a WHERE u.status = 'APPROVED'")
    Double getAverageIncome();
    
    @Query("SELECT u FROM User u ORDER BY u.joinDate DESC")
    List<User> findRecentUsers(Pageable pageable);
}
