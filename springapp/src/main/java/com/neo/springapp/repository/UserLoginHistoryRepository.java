package com.neo.springapp.repository;

import com.neo.springapp.model.UserLoginHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserLoginHistoryRepository extends JpaRepository<UserLoginHistory, Long> {
    
    // Find all login history for a specific user
    List<UserLoginHistory> findByUserIdOrderByLoginTimeDesc(Long userId);
    
    // Find all login history for a user by account number
    @Query("SELECT h FROM UserLoginHistory h WHERE h.user.accountNumber = :accountNumber ORDER BY h.loginTime DESC")
    List<UserLoginHistory> findByAccountNumberOrderByLoginTimeDesc(String accountNumber);
    
    // Find all login history (for admin dashboard)
    List<UserLoginHistory> findAllByOrderByLoginTimeDesc();
    
    // Find recent login history (last N records)
    @Query("SELECT h FROM UserLoginHistory h ORDER BY h.loginTime DESC")
    List<UserLoginHistory> findRecentLogins(java.util.List<Long> ids);
    
    // Find login history by date range
    @Query("SELECT h FROM UserLoginHistory h WHERE h.loginDate BETWEEN :startDate AND :endDate ORDER BY h.loginTime DESC")
    List<UserLoginHistory> findByDateRange(java.time.LocalDateTime startDate, java.time.LocalDateTime endDate);
}


