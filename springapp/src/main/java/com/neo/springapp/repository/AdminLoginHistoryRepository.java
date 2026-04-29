package com.neo.springapp.repository;

import com.neo.springapp.model.AdminLoginHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminLoginHistoryRepository extends JpaRepository<AdminLoginHistory, Long> {
    
    // Find all login history for a specific admin
    List<AdminLoginHistory> findByAdminIdOrderByLoginTimeDesc(Long adminId);
    
    // Find all login history (for manager dashboard)
    List<AdminLoginHistory> findAllByOrderByLoginTimeDesc();
    
    // Find active sessions (no logout time)
    @Query("SELECT h FROM AdminLoginHistory h WHERE h.logoutTime IS NULL ORDER BY h.loginTime DESC")
    List<AdminLoginHistory> findActiveSessions();
    
    // Find recent login history (last N records)
    @Query("SELECT h FROM AdminLoginHistory h ORDER BY h.loginTime DESC")
    List<AdminLoginHistory> findRecentLogins();
}

