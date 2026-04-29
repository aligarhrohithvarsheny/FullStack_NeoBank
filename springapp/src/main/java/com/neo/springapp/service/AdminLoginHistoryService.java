package com.neo.springapp.service;

import com.neo.springapp.model.Admin;
import com.neo.springapp.model.AdminLoginHistory;
import com.neo.springapp.repository.AdminLoginHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminLoginHistoryService {

    @Autowired
    private AdminLoginHistoryRepository adminLoginHistoryRepository;

    /**
     * Record a successful admin login
     */
    public AdminLoginHistory recordLogin(Admin admin, String loginLocation, String ipAddress, String deviceInfo, String loginMethod) {
        try {
            System.out.println("📝 Creating admin login history record for admin ID: " + admin.getId() + ", Email: " + admin.getEmail());
            
            AdminLoginHistory history = new AdminLoginHistory();
            history.setAdmin(admin);
            history.setLoginDate(LocalDateTime.now());
            history.setLoginTime(LocalDateTime.now());
            history.setLoginLocation(loginLocation != null ? loginLocation : "Unknown");
            history.setIpAddress(ipAddress != null ? ipAddress : "Unknown");
            history.setDeviceInfo(deviceInfo != null ? deviceInfo : "Unknown");
            history.setLoginMethod(loginMethod != null ? loginMethod : "PASSWORD");
            history.setStatus("SUCCESS");
            
            AdminLoginHistory saved = adminLoginHistoryRepository.save(history);
            System.out.println("✅ Admin login history saved successfully. History ID: " + saved.getId());
            return saved;
        } catch (Exception e) {
            System.err.println("❌ Error saving admin login history: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Record a failed admin login attempt
     */
    public AdminLoginHistory recordFailedLogin(Admin admin, String loginLocation, String ipAddress, String deviceInfo, String loginMethod) {
        AdminLoginHistory history = new AdminLoginHistory();
        history.setAdmin(admin);
        history.setLoginDate(LocalDateTime.now());
        history.setLoginTime(LocalDateTime.now());
        history.setLoginLocation(loginLocation != null ? loginLocation : "Unknown");
        history.setIpAddress(ipAddress != null ? ipAddress : "Unknown");
        history.setDeviceInfo(deviceInfo != null ? deviceInfo : "Unknown");
        history.setLoginMethod(loginMethod != null ? loginMethod : "PASSWORD");
        history.setStatus("FAILED");
        
        return adminLoginHistoryRepository.save(history);
    }

    /**
     * Update admin session with logout time and duration
     */
    public AdminLoginHistory updateSessionLogout(Long adminId, String sessionDuration) {
        // Find the most recent active session for this admin
        List<AdminLoginHistory> activeSessions = adminLoginHistoryRepository.findByAdminIdOrderByLoginTimeDesc(adminId);
        
        if (activeSessions != null && !activeSessions.isEmpty()) {
            AdminLoginHistory session = activeSessions.stream()
                .filter(s -> s.getLogoutTime() == null)
                .findFirst()
                .orElse(null);
            
            if (session != null) {
                session.setLogoutTime(LocalDateTime.now());
                session.setSessionDuration(sessionDuration);
                return adminLoginHistoryRepository.save(session);
            }
        }
        return null;
    }

    /**
     * Get all admin login history
     */
    public List<AdminLoginHistory> getAllAdminLoginHistory() {
        return adminLoginHistoryRepository.findAllByOrderByLoginTimeDesc();
    }

    /**
     * Get login history for a specific admin
     */
    public List<AdminLoginHistory> getAdminLoginHistory(Long adminId) {
        return adminLoginHistoryRepository.findByAdminIdOrderByLoginTimeDesc(adminId);
    }

    /**
     * Get active admin sessions
     */
    public List<AdminLoginHistory> getActiveSessions() {
        return adminLoginHistoryRepository.findActiveSessions();
    }
}

