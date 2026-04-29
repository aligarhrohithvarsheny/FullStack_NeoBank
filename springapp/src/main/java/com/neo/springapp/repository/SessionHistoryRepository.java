package com.neo.springapp.repository;

import com.neo.springapp.model.SessionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SessionHistoryRepository extends JpaRepository<SessionHistory, Long> {
    
    // Find all session history ordered by login time
    List<SessionHistory> findAllByOrderByLoginTimeDesc();
    
    // Find active sessions (no logout time)
    @Query("SELECT s FROM SessionHistory s WHERE s.logoutTime IS NULL ORDER BY s.loginTime DESC")
    List<SessionHistory> findActiveSessions();
    
    // Find sessions by user type
    List<SessionHistory> findByUserTypeOrderByLoginTimeDesc(String userType);
    
    // Find sessions by user ID and type
    List<SessionHistory> findByUserIdAndUserTypeOrderByLoginTimeDesc(Long userId, String userType);
    
    // Find recent sessions (last N records)
    @Query("SELECT s FROM SessionHistory s ORDER BY s.loginTime DESC")
    List<SessionHistory> findRecentSessions();
}

