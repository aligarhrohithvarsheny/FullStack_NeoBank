package com.neo.springapp.service;

import com.neo.springapp.model.User;
import com.neo.springapp.model.UserLoginHistory;
import com.neo.springapp.repository.UserLoginHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserLoginHistoryService {

    @Autowired
    private UserLoginHistoryRepository loginHistoryRepository;

    /**
     * Record a successful login
     */
    public UserLoginHistory recordLogin(User user, String loginLocation, String ipAddress, String deviceInfo, String loginMethod) {
        try {
            System.out.println("📝 Creating login history record for user ID: " + user.getId() + ", Email: " + user.getEmail());
            
            UserLoginHistory history = new UserLoginHistory();
            history.setUser(user);
            history.setLoginDate(LocalDateTime.now());
            history.setLoginTime(LocalDateTime.now());
            history.setLoginLocation(loginLocation != null ? loginLocation : "Unknown");
            history.setIpAddress(ipAddress != null ? ipAddress : "Unknown");
            history.setDeviceInfo(deviceInfo != null ? deviceInfo : "Unknown");
            history.setLoginMethod(loginMethod != null ? loginMethod : "PASSWORD");
            history.setStatus("SUCCESS");
            
            UserLoginHistory saved = loginHistoryRepository.save(history);
            System.out.println("✅ Login history saved successfully. History ID: " + saved.getId());
            return saved;
        } catch (Exception e) {
            System.err.println("❌ Error saving login history: " + e.getMessage());
            e.printStackTrace();
            throw e; // Re-throw to be caught by controller
        }
    }

    /**
     * Record a failed login attempt
     */
    public UserLoginHistory recordFailedLogin(User user, String loginLocation, String ipAddress, String deviceInfo, String loginMethod) {
        UserLoginHistory history = new UserLoginHistory();
        history.setUser(user);
        history.setLoginDate(LocalDateTime.now());
        history.setLoginTime(LocalDateTime.now());
        history.setLoginLocation(loginLocation != null ? loginLocation : "Unknown");
        history.setIpAddress(ipAddress != null ? ipAddress : "Unknown");
        history.setDeviceInfo(deviceInfo != null ? deviceInfo : "Unknown");
        history.setLoginMethod(loginMethod != null ? loginMethod : "PASSWORD");
        history.setStatus("FAILED");
        
        return loginHistoryRepository.save(history);
    }

    /**
     * Get all login history for a user
     */
    public List<UserLoginHistory> getUserLoginHistory(Long userId) {
        return loginHistoryRepository.findByUserIdOrderByLoginTimeDesc(userId);
    }

    /**
     * Get all login history for a user by account number
     */
    public List<UserLoginHistory> getUserLoginHistoryByAccountNumber(String accountNumber) {
        return loginHistoryRepository.findByAccountNumberOrderByLoginTimeDesc(accountNumber);
    }

    /**
     * Get all login history (for admin dashboard)
     */
    public List<UserLoginHistory> getAllLoginHistory() {
        return loginHistoryRepository.findAllByOrderByLoginTimeDesc();
    }

    /**
     * Get recent login history (last N records)
     */
    public List<UserLoginHistory> getRecentLoginHistory(int limit) {
        List<UserLoginHistory> all = loginHistoryRepository.findAllByOrderByLoginTimeDesc();
        return all.stream().limit(limit).collect(java.util.stream.Collectors.toList());
    }
}

