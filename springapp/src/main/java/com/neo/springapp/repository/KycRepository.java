package com.neo.springapp.repository;

import com.neo.springapp.model.KycRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface KycRepository extends JpaRepository<KycRequest, Long> {
    
    // Basic queries
    Optional<KycRequest> findByPanNumber(String panNumber);
    Optional<KycRequest> findByUserId(String userId);
    Optional<KycRequest> findByUserAccountNumber(String userAccountNumber);
    List<KycRequest> findByStatus(String status);
    Page<KycRequest> findByStatus(String status, Pageable pageable);
    
    // Search queries
    @Query("SELECT k FROM KycRequest k WHERE " +
           "LOWER(k.userName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(k.userEmail) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(k.userAccountNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(k.panNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(k.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<KycRequest> searchKycRequests(@Param("searchTerm") String searchTerm, Pageable pageable);
    
    // Date range queries
    @Query("SELECT k FROM KycRequest k WHERE k.submittedDate BETWEEN :startDate AND :endDate")
    List<KycRequest> findBySubmittedDateRange(@Param("startDate") LocalDateTime startDate, 
                                             @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT k FROM KycRequest k WHERE k.approvedDate BETWEEN :startDate AND :endDate")
    List<KycRequest> findByApprovedDateRange(@Param("startDate") LocalDateTime startDate, 
                                            @Param("endDate") LocalDateTime endDate);
    
    // Statistics queries
    @Query("SELECT COUNT(k) FROM KycRequest k WHERE k.status = :status")
    Long countByStatus(@Param("status") String status);
    
    // Recent requests
    @Query("SELECT k FROM KycRequest k ORDER BY k.submittedDate DESC")
    List<KycRequest> findRecentRequests(Pageable pageable);
    
    // Pending requests for admin review
    @Query("SELECT k FROM KycRequest k WHERE k.status = 'Pending' ORDER BY k.submittedDate ASC")
    List<KycRequest> findPendingRequestsForReview();
    
    // Admin queries
    @Query("SELECT k FROM KycRequest k WHERE k.approvedBy = :adminName")
    List<KycRequest> findByApprovedBy(@Param("adminName") String adminName);
}
