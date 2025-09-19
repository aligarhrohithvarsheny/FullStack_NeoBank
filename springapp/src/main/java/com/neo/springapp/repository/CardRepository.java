package com.neo.springapp.repository;

import com.neo.springapp.model.Card;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {
    
    // JPQL Query to find cards by account number
    @Query("SELECT c FROM Card c WHERE c.accountNumber = :accountNumber")
    List<Card> findByAccountNumber(@Param("accountNumber") String accountNumber);
    
    // JPQL Query to find cards by user email
    @Query("SELECT c FROM Card c WHERE c.userEmail = :userEmail")
    List<Card> findByUserEmail(@Param("userEmail") String userEmail);
    
    // JPQL Query to find cards by card type with pagination
    @Query("SELECT c FROM Card c WHERE c.cardType = :cardType ORDER BY c.issueDate DESC")
    Page<Card> findByCardTypeOrderByIssueDateDesc(@Param("cardType") String cardType, Pageable pageable);
    
    // JPQL Query to find cards by status with pagination
    @Query("SELECT c FROM Card c WHERE c.status = :status ORDER BY c.issueDate DESC")
    Page<Card> findByStatusOrderByIssueDateDesc(@Param("status") String status, Pageable pageable);
    
    // JPQL Query to find blocked cards
    @Query("SELECT c FROM Card c WHERE c.blocked = true ORDER BY c.issueDate DESC")
    Page<Card> findBlockedCards(Pageable pageable);
    
    // JPQL Query to find deactivated cards
    @Query("SELECT c FROM Card c WHERE c.deactivated = true ORDER BY c.issueDate DESC")
    Page<Card> findDeactivatedCards(Pageable pageable);
    
    // JPQL Query to find active cards
    @Query("SELECT c FROM Card c WHERE c.status = 'Active' AND c.blocked = false AND c.deactivated = false ORDER BY c.issueDate DESC")
    Page<Card> findActiveCards(Pageable pageable);
    
    // JPQL Query to find cards expiring soon
    @Query("SELECT c FROM Card c WHERE c.expiryDateTime <= :expiryDate ORDER BY c.expiryDateTime ASC")
    List<Card> findCardsExpiringSoon(@Param("expiryDate") java.time.LocalDateTime expiryDate);
    
    // JPQL Query to get card statistics
    @Query("SELECT COUNT(c), SUM(CASE WHEN c.status = 'Active' THEN 1 ELSE 0 END), SUM(CASE WHEN c.blocked = true THEN 1 ELSE 0 END) FROM Card c")
    Object[] getCardStatistics();
    
    // Search queries
    @Query("SELECT c FROM Card c WHERE " +
           "LOWER(c.userName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.userEmail) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.accountNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.cardNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.cardType) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Card> searchCards(@Param("searchTerm") String searchTerm, Pageable pageable);
    
    // Statistics queries
    @Query("SELECT COUNT(c) FROM Card c WHERE c.status = :status")
    Long countByStatus(@Param("status") String status);
    
    @Query("SELECT COUNT(c) FROM Card c WHERE c.pinSet = :pinSet")
    Long countByPinSet(@Param("pinSet") boolean pinSet);
    
    // Recent cards
    @Query("SELECT c FROM Card c ORDER BY c.issueDate DESC")
    List<Card> findRecentCards(Pageable pageable);
    
    // Cards by user
    @Query("SELECT c FROM Card c WHERE c.userName = :userName ORDER BY c.issueDate DESC")
    List<Card> findByUserName(@Param("userName") String userName);
}
