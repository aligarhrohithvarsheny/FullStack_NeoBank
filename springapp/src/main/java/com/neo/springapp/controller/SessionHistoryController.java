package com.neo.springapp.controller;

import com.neo.springapp.model.SessionHistory;
import com.neo.springapp.service.SessionHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/session-history")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {org.springframework.web.bind.annotation.RequestMethod.GET, 
    org.springframework.web.bind.annotation.RequestMethod.POST, org.springframework.web.bind.annotation.RequestMethod.PUT, 
    org.springframework.web.bind.annotation.RequestMethod.DELETE, org.springframework.web.bind.annotation.RequestMethod.OPTIONS})
public class SessionHistoryController {

    @Autowired
    private SessionHistoryService sessionHistoryService;

    /**
     * Get all session history (for manager dashboard)
     */
    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAllSessionHistory() {
        try {
            System.out.println("🔍 Fetching all session history...");
            List<SessionHistory> sessions = sessionHistoryService.getAllSessionHistory();
            System.out.println("📊 Found " + sessions.size() + " session history records");
            
            List<Map<String, Object>> sessionList = sessions.stream().map(s -> {
                Map<String, Object> item = new HashMap<>();
                item.put("id", s.getId());
                item.put("userType", s.getUserType());
                item.put("userId", s.getUserId());
                item.put("email", s.getEmail());
                item.put("accountNumber", s.getAccountNumber());
                item.put("username", s.getUsername());
                item.put("loginTime", s.getLoginTime());
                item.put("logoutTime", s.getLogoutTime());
                item.put("sessionDuration", s.getSessionDuration());
                item.put("loginLocation", s.getLoginLocation());
                item.put("ipAddress", s.getIpAddress());
                item.put("deviceInfo", s.getDeviceInfo());
                item.put("loginMethod", s.getLoginMethod());
                item.put("browserName", s.getBrowserName());
                item.put("accountType", s.getAccountType());
                item.put("status", s.getStatus());
                return item;
            }).collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("sessions", sessionList);
            response.put("total", sessionList.size());
            
            System.out.println("✅ Returning " + sessionList.size() + " session history records");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("❌ Error fetching session history: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error fetching session history: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Get active sessions
     */
    @GetMapping("/active")
    public ResponseEntity<Map<String, Object>> getActiveSessions() {
        try {
            List<SessionHistory> sessions = sessionHistoryService.getActiveSessions();
            
            List<Map<String, Object>> sessionList = sessions.stream().map(s -> {
                Map<String, Object> item = new HashMap<>();
                item.put("id", s.getId());
                item.put("userType", s.getUserType());
                item.put("userId", s.getUserId());
                item.put("email", s.getEmail());
                item.put("accountNumber", s.getAccountNumber());
                item.put("username", s.getUsername());
                item.put("loginTime", s.getLoginTime());
                item.put("loginLocation", s.getLoginLocation());
                item.put("ipAddress", s.getIpAddress());
                item.put("deviceInfo", s.getDeviceInfo());
                item.put("loginMethod", s.getLoginMethod());
                item.put("browserName", s.getBrowserName());
                item.put("accountType", s.getAccountType());
                item.put("status", s.getStatus());
                return item;
            }).collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("sessions", sessionList);
            response.put("total", sessionList.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error fetching active sessions: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Record logout and update session
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> recordLogout(@RequestBody Map<String, Object> logoutData) {
        try {
            Long userId = Long.parseLong(logoutData.get("userId").toString());
            String userType = logoutData.get("userType").toString();
            String sessionDuration = logoutData.get("sessionDuration") != null ? 
                logoutData.get("sessionDuration").toString() : "00:00:00";
            
            SessionHistory session = sessionHistoryService.updateSessionLogout(userId, userType, sessionDuration);
            
            Map<String, Object> response = new HashMap<>();
            if (session != null) {
                response.put("success", true);
                response.put("message", "Logout recorded successfully");
            } else {
                response.put("success", false);
                response.put("message", "No active session found");
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error recording logout: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Get sessions by user type
     */
    @GetMapping("/by-type/{userType}")
    public ResponseEntity<Map<String, Object>> getSessionsByUserType(@PathVariable String userType) {
        try {
            List<SessionHistory> sessions = sessionHistoryService.getSessionsByUserType(userType);
            
            List<Map<String, Object>> sessionList = sessions.stream().map(s -> {
                Map<String, Object> item = new HashMap<>();
                item.put("id", s.getId());
                item.put("userType", s.getUserType());
                item.put("userId", s.getUserId());
                item.put("email", s.getEmail());
                item.put("accountNumber", s.getAccountNumber());
                item.put("username", s.getUsername());
                item.put("loginTime", s.getLoginTime());
                item.put("logoutTime", s.getLogoutTime());
                item.put("sessionDuration", s.getSessionDuration());
                item.put("loginLocation", s.getLoginLocation());
                item.put("ipAddress", s.getIpAddress());
                item.put("deviceInfo", s.getDeviceInfo());
                item.put("loginMethod", s.getLoginMethod());
                item.put("browserName", s.getBrowserName());
                item.put("accountType", s.getAccountType());
                item.put("status", s.getStatus());
                return item;
            }).collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("sessions", sessionList);
            response.put("total", sessionList.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error fetching sessions: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}

