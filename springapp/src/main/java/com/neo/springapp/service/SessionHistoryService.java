package com.neo.springapp.service;

import com.neo.springapp.model.SessionHistory;
import com.neo.springapp.model.User;
import com.neo.springapp.model.Admin;
import com.neo.springapp.repository.SessionHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@SuppressWarnings("null")
public class SessionHistoryService {

    @Autowired
    private SessionHistoryRepository sessionHistoryRepository;

    /**
     * Record a user login session (savings account)
     */
    public SessionHistory recordUserLogin(User user, String loginLocation, String ipAddress, String deviceInfo, String loginMethod) {
        try {
            System.out.println("📝 Recording user login session for user ID: " + user.getId() + ", Email: " + user.getEmail());
            SessionHistory session = new SessionHistory();
            session.setUserType("USER");
            session.setUserId(user.getId());
            session.setEmail(user.getEmail());
            session.setAccountNumber(user.getAccountNumber());
            session.setUsername(user.getUsername());
            session.setLoginTime(LocalDateTime.now());
            session.setLoginLocation(loginLocation != null ? loginLocation : "Unknown");
            session.setIpAddress(ipAddress != null ? ipAddress : "Unknown");
            session.setDeviceInfo(deviceInfo != null ? deviceInfo : "Unknown");
            session.setLoginMethod(loginMethod != null ? loginMethod : "PASSWORD");
            session.setBrowserName(parseBrowserName(deviceInfo));
            session.setAccountType("SAVINGS");
            session.setStatus("ACTIVE");
            
            SessionHistory saved = sessionHistoryRepository.save(session);
            System.out.println("✅ User login session recorded successfully. Session ID: " + saved.getId());
            return saved;
        } catch (Exception e) {
            System.err.println("❌ Error recording user login session: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Record an admin login session
     */
    public SessionHistory recordAdminLogin(Admin admin, String loginLocation, String ipAddress, String deviceInfo, String loginMethod) {
        try {
            System.out.println("📝 Recording admin login session for admin ID: " + admin.getId() + ", Email: " + admin.getEmail());
            SessionHistory session = new SessionHistory();
            
            // Set userType based on admin's role (MANAGER or ADMIN)
            String adminRole = admin.getRole() != null ? admin.getRole() : "ADMIN";
            String userType = adminRole.equals("MANAGER") ? "MANAGER" : "ADMIN";
            session.setUserType(userType);
            
            session.setUserId(admin.getId());
            session.setEmail(admin.getEmail());
            session.setUsername(admin.getName() != null ? admin.getName() : admin.getEmail());
            session.setLoginTime(LocalDateTime.now());
            session.setLoginLocation(loginLocation != null ? loginLocation : "Unknown");
            session.setIpAddress(ipAddress != null ? ipAddress : "Unknown");
            session.setDeviceInfo(deviceInfo != null ? deviceInfo : "Unknown");
            session.setLoginMethod(loginMethod != null ? loginMethod : "PASSWORD");
            session.setBrowserName(parseBrowserName(deviceInfo));
            session.setAccountType(userType);
            session.setStatus("ACTIVE");
            
            SessionHistory saved = sessionHistoryRepository.save(session);
            System.out.println("✅ Admin login session recorded successfully. Session ID: " + saved.getId() + ", UserType: " + userType);
            return saved;
        } catch (Exception e) {
            System.err.println("❌ Error recording admin login session: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Record a current account login session
     */
    public SessionHistory recordCurrentAccountLogin(Long accountId, String email, String accountNumber, String ownerName,
                                                     String loginLocation, String ipAddress, String deviceInfo, String loginMethod) {
        try {
            System.out.println("📝 Recording current account login for account: " + accountNumber);
            SessionHistory session = new SessionHistory();
            session.setUserType("USER");
            session.setUserId(accountId);
            session.setEmail(email != null ? email : "");
            session.setAccountNumber(accountNumber);
            session.setUsername(ownerName != null ? ownerName : "Current Account User");
            session.setLoginTime(LocalDateTime.now());
            session.setLoginLocation(loginLocation != null ? loginLocation : "Unknown");
            session.setIpAddress(ipAddress != null ? ipAddress : "Unknown");
            session.setDeviceInfo(deviceInfo != null ? deviceInfo : "Unknown");
            session.setLoginMethod(loginMethod != null ? loginMethod : "PASSWORD");
            session.setBrowserName(parseBrowserName(deviceInfo));
            session.setAccountType("CURRENT");
            session.setStatus("ACTIVE");

            SessionHistory saved = sessionHistoryRepository.save(session);
            System.out.println("✅ Current account login session recorded. Session ID: " + saved.getId());
            return saved;
        } catch (Exception e) {
            System.err.println("❌ Error recording current account login: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Record a salary account login session
     */
    public SessionHistory recordSalaryAccountLogin(Long accountId, String email, String accountNumber, String employeeName,
                                                    String loginLocation, String ipAddress, String deviceInfo, String loginMethod) {
        try {
            System.out.println("📝 Recording salary account login for account: " + accountNumber);
            SessionHistory session = new SessionHistory();
            session.setUserType("USER");
            session.setUserId(accountId);
            session.setEmail(email != null ? email : "");
            session.setAccountNumber(accountNumber);
            session.setUsername(employeeName != null ? employeeName : "Salary Account User");
            session.setLoginTime(LocalDateTime.now());
            session.setLoginLocation(loginLocation != null ? loginLocation : "Unknown");
            session.setIpAddress(ipAddress != null ? ipAddress : "Unknown");
            session.setDeviceInfo(deviceInfo != null ? deviceInfo : "Unknown");
            session.setLoginMethod(loginMethod != null ? loginMethod : "PASSWORD");
            session.setBrowserName(parseBrowserName(deviceInfo));
            session.setAccountType("SALARY");
            session.setStatus("ACTIVE");

            SessionHistory saved = sessionHistoryRepository.save(session);
            System.out.println("✅ Salary account login session recorded. Session ID: " + saved.getId());
            return saved;
        } catch (Exception e) {
            System.err.println("❌ Error recording salary account login: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Parse browser name from User-Agent string
     */
    public String parseBrowserName(String userAgent) {
        if (userAgent == null || userAgent.isEmpty() || userAgent.equals("Unknown")) {
            return "Unknown";
        }
        String ua = userAgent.toLowerCase();
        if (ua.contains("edg/") || ua.contains("edge/")) return "Edge";
        if (ua.contains("opr/") || ua.contains("opera")) return "Opera";
        if (ua.contains("brave")) return "Brave";
        if (ua.contains("vivaldi")) return "Vivaldi";
        if (ua.contains("chrome") && !ua.contains("edg") && !ua.contains("opr")) return "Chrome";
        if (ua.contains("firefox")) return "Firefox";
        if (ua.contains("safari") && !ua.contains("chrome")) return "Safari";
        if (ua.contains("msie") || ua.contains("trident")) return "IE";
        return "Other";
    }

    /**
     * Record logout and calculate session duration
     */
    public SessionHistory recordLogout(Long sessionId, String sessionDuration) {
        SessionHistory session = sessionHistoryRepository.findById(sessionId)
            .orElseThrow(() -> new RuntimeException("Session not found"));
        
        session.setLogoutTime(LocalDateTime.now());
        session.setSessionDuration(sessionDuration);
        session.setStatus("LOGGED_OUT");
        
        return sessionHistoryRepository.save(session);
    }

    /**
     * Update session with logout time and duration
     */
    public SessionHistory updateSessionLogout(Long userId, String userType, String sessionDuration) {
        // Find the most recent active session for this user
        List<SessionHistory> activeSessions = sessionHistoryRepository.findByUserIdAndUserTypeOrderByLoginTimeDesc(userId, userType);
        
        if (activeSessions != null && !activeSessions.isEmpty()) {
            SessionHistory session = activeSessions.stream()
                .filter(s -> s.getLogoutTime() == null)
                .findFirst()
                .orElse(null);
            
            if (session != null) {
                session.setLogoutTime(LocalDateTime.now());
                session.setSessionDuration(sessionDuration);
                session.setStatus("LOGGED_OUT");
                return sessionHistoryRepository.save(session);
            }
        }
        return null;
    }

    /**
     * Get all session history
     */
    public List<SessionHistory> getAllSessionHistory() {
        return sessionHistoryRepository.findAllByOrderByLoginTimeDesc();
    }

    /**
     * Get active sessions
     */
    public List<SessionHistory> getActiveSessions() {
        return sessionHistoryRepository.findActiveSessions();
    }

    /**
     * Get sessions by user type
     */
    public List<SessionHistory> getSessionsByUserType(String userType) {
        return sessionHistoryRepository.findByUserTypeOrderByLoginTimeDesc(userType);
    }

    /**
     * Calculate session duration in HH:MM:SS format
     */
    public String calculateSessionDuration(LocalDateTime loginTime, LocalDateTime logoutTime) {
        if (loginTime == null || logoutTime == null) {
            return "00:00:00";
        }
        
        Duration duration = Duration.between(loginTime, logoutTime);
        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.getSeconds() % 60;
        
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}

