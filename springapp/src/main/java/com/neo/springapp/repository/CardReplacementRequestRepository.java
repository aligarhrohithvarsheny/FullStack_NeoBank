package com.neo.springapp.repository;

import com.neo.springapp.model.CardReplacementRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CardReplacementRequestRepository extends JpaRepository<CardReplacementRequest, Long> {
    
    // Basic queries
    List<CardReplacementRequest> findByUserId(String userId);
    List<CardReplacementRequest> findByAccountNumber(String accountNumber);
    List<CardReplacementRequest> findByStatus(String status);
    Page<CardReplacementRequest> findByStatus(String status, Pageable pageable);
    
    // Search queries
    @Query("SELECT c FROM CardReplacementRequest c WHERE " +
           "LOWER(c.userName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.userEmail) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.accountNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.currentCardNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<CardReplacementRequest> searchRequests(@Param("searchTerm") String searchTerm, Pageable pageable);
    
    // Date range queries
    @Query("SELECT c FROM CardReplacementRequest c WHERE c.requestDate BETWEEN :startDate AND :endDate")
    List<CardReplacementRequest> findByRequestDateRange(@Param("startDate") LocalDateTime startDate, 
                                                        @Param("endDate") LocalDateTime endDate);
    
    // Statistics queries
    @Query("SELECT COUNT(c) FROM CardReplacementRequest c WHERE c.status = :status")
    Long countByStatus(@Param("status") String status);
    
    // Recent requests
    @Query("SELECT c FROM CardReplacementRequest c ORDER BY c.requestDate DESC")
    List<CardReplacementRequest> findRecentRequests(Pageable pageable);
    
    // Pending requests for admin review
    @Query("SELECT c FROM CardReplacementRequest c WHERE c.status = 'Pending' ORDER BY c.requestDate ASC")
    List<CardReplacementRequest> findPendingRequestsForReview();
}
