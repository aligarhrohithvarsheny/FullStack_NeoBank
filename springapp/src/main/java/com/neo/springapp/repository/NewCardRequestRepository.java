package com.neo.springapp.repository;

import com.neo.springapp.model.NewCardRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NewCardRequestRepository extends JpaRepository<NewCardRequest, Long> {
    
    // Basic queries
    List<NewCardRequest> findByUserId(String userId);
    List<NewCardRequest> findByAccountNumber(String accountNumber);
    List<NewCardRequest> findByStatus(String status);
    Page<NewCardRequest> findByStatus(String status, Pageable pageable);
    
    // Card type queries
    List<NewCardRequest> findByCardType(String cardType);
    List<NewCardRequest> findByCardTypeAndStatus(String cardType, String status);
    
    // Search queries
    @Query("SELECT n FROM NewCardRequest n WHERE " +
           "LOWER(n.userName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(n.userEmail) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(n.accountNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(n.cardType) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<NewCardRequest> searchRequests(@Param("searchTerm") String searchTerm, Pageable pageable);
    
    // Date range queries
    @Query("SELECT n FROM NewCardRequest n WHERE n.requestDate BETWEEN :startDate AND :endDate")
    List<NewCardRequest> findByRequestDateRange(@Param("startDate") LocalDateTime startDate, 
                                               @Param("endDate") LocalDateTime endDate);
    
    // Statistics queries
    @Query("SELECT COUNT(n) FROM NewCardRequest n WHERE n.status = :status")
    Long countByStatus(@Param("status") String status);
    
    // Recent requests
    @Query("SELECT n FROM NewCardRequest n ORDER BY n.requestDate DESC")
    List<NewCardRequest> findRecentRequests(Pageable pageable);
    
    // Pending requests for admin review
    @Query("SELECT n FROM NewCardRequest n WHERE n.status = 'Pending' ORDER BY n.requestDate ASC")
    List<NewCardRequest> findPendingRequestsForReview();
}
