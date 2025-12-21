package com.neo.springapp.controller;

import com.neo.springapp.model.Admin;
import com.neo.springapp.service.AdminService;
import com.neo.springapp.service.UserLoginHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admins")
public class AdminController {

    @Autowired
    private AdminService adminService;
    
    @Autowired
    private UserLoginHistoryService loginHistoryService;

    @PostMapping("/create")
    public Admin createAdmin(@RequestBody Admin admin) {
        // When creating new admin employee, set profileComplete to false
        if (admin.getProfileComplete() == null) {
            admin.setProfileComplete(false);
        }
        return adminService.saveAdmin(admin);
    }
    
    /**
     * Check if admin profile is complete
     */
    @GetMapping("/profile-complete/{email}")
    public ResponseEntity<Map<String, Object>> checkProfileComplete(@PathVariable String email) {
        try {
            Admin admin = adminService.getAdminByEmail(email);
            if (admin == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Admin not found");
                return ResponseEntity.notFound().build();
            }
            
            // Create safe admin response (exclude password)
            Map<String, Object> adminResponse = createSafeAdminResponse(admin);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("profileComplete", admin.getProfileComplete() != null ? admin.getProfileComplete() : false);
            response.put("admin", adminResponse);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error checking profile: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Mark admin profile as complete
     */
    @PutMapping("/profile-complete/{email}")
    public ResponseEntity<Map<String, Object>> markProfileComplete(@PathVariable String email) {
        try {
            Admin admin = adminService.getAdminByEmail(email);
            if (admin == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Admin not found");
                return ResponseEntity.notFound().build();
            }
            
            admin.setProfileComplete(true);
            Admin updatedAdmin = adminService.updateAdmin(admin.getId(), admin);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Profile marked as complete");
            response.put("admin", updatedAdmin);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error updating profile: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Create default manager account (for initial setup)
     * This endpoint creates a default manager if no manager exists
     */
    @PostMapping("/create-default-manager")
    public ResponseEntity<Map<String, Object>> createDefaultManager() {
        try {
            // Check if any manager exists
            List<Admin> allAdmins = adminService.getAllAdmins();
            boolean managerExists = allAdmins.stream()
                .anyMatch(a -> "MANAGER".equalsIgnoreCase(a.getRole()));
            
            if (managerExists) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "A manager account already exists");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Create default manager
            Admin manager = new Admin();
            manager.setName("Manager");
            manager.setEmail("manager@neobank.com");
            manager.setPassword("manager123"); // Will be encrypted by service
            manager.setRole("MANAGER");
            manager.setEmployeeId("MGR001");
            
            Admin savedManager = adminService.saveAdmin(manager);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Default manager account created successfully");
            response.put("admin", savedManager);
            response.put("loginDetails", Map.of(
                "email", "manager@neobank.com",
                "password", "manager123",
                "role", "MANAGER"
            ));
            
            System.out.println("✅ Default manager account created:");
            System.out.println("   Email: manager@neobank.com");
            System.out.println("   Password: manager123");
            System.out.println("   Role: MANAGER");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error creating default manager: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get all admin employees (ADMIN role only, excluding MANAGER)
     */
    @GetMapping("/all")
    public ResponseEntity<List<Admin>> getAllAdmins() {
        List<Admin> allAdmins = adminService.getAllAdmins();
        // Filter to only return ADMIN role (exclude MANAGER)
        List<Admin> adminEmployees = allAdmins.stream()
            .filter(a -> "ADMIN".equalsIgnoreCase(a.getRole()))
            .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(adminEmployees);
    }

    @GetMapping("/{id}")
    public Admin getAdminById(@PathVariable Long id) {
        return adminService.getAdminById(id);
    }

    @PutMapping("/update/{id}")
    public Admin updateAdmin(@PathVariable Long id, @RequestBody Admin adminDetails) {
        return adminService.updateAdmin(id, adminDetails);
    }

    /**
     * Get admin profile by email
     */
    @GetMapping("/profile/{email}")
    public ResponseEntity<Admin> getAdminProfile(@PathVariable String email) {
        Admin admin = adminService.getAdminByEmail(email);
        if (admin != null) {
            return ResponseEntity.ok(admin);
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Update admin profile by email
     */
    @PutMapping("/profile/{email}")
    public ResponseEntity<Map<String, Object>> updateAdminProfile(
            @PathVariable String email, 
            @RequestBody Admin adminDetails) {
        Map<String, Object> response = new HashMap<>();
        try {
            Admin existingAdmin = adminService.getAdminByEmail(email);
            if (existingAdmin == null) {
                response.put("success", false);
                response.put("message", "Admin not found");
                return ResponseEntity.notFound().build();
            }
            
            Admin updatedAdmin = adminService.updateAdmin(existingAdmin.getId(), adminDetails);
            if (updatedAdmin != null) {
                response.put("success", true);
                response.put("message", "Profile updated successfully");
                response.put("admin", updatedAdmin);
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Failed to update profile");
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error updating profile: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> loginAdmin(@RequestBody Map<String, String> credentials) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Log incoming request details
            System.out.println("==========================================");
            System.out.println("[ADMIN-LOGIN] Request received at " + java.time.LocalDateTime.now());
            System.out.println("[ADMIN-LOGIN] Request body: " + credentials);
            System.out.println("[ADMIN-LOGIN] Request body keys: " + (credentials != null ? credentials.keySet() : "null"));
            System.out.println("==========================================");
            
            // Validate request body is not null
            if (credentials == null) {
                System.err.println("[ADMIN-LOGIN] ERROR: Request body is null");
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Request body is required. Please send JSON with email and password.");
                return ResponseEntity.badRequest().body(response);
            }
            
            String email = credentials.get("email");
            String password = credentials.get("password");
            String requestedRole = credentials.get("role"); // ADMIN or MANAGER (optional)
            
            System.out.println("[ADMIN-LOGIN] Extracted - Email: " + email + ", Password: " + (password != null ? "***" : "null") + ", Role: " + requestedRole);
            
            if (email == null || password == null || email.isEmpty() || password.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Email and password are required");
                return ResponseEntity.badRequest().body(response);
            }
            
            Admin admin = adminService.getAdminByEmail(email);
            if (admin == null) {
                System.out.println("❌ Admin not found for email: " + email);
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Invalid email or password");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
            System.out.println("✅ Admin found - Email: " + admin.getEmail() + ", AccountLocked: " + admin.getAccountLocked() + ", FailedAttempts: " + admin.getFailedLoginAttempts());
            
            if (admin.getAccountLocked() != null && admin.getAccountLocked()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("accountLocked", true);
                response.put("message", "Account is locked due to 3 failed login attempts. Please contact manager to unlock.");
                System.out.println("🔒 Login blocked - account locked for: " + email);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
            System.out.println("🔍 Attempting password verification...");
            Admin loggedInAdmin = adminService.login(email, password);
            if (loggedInAdmin != null) {
                // Check if the user's role matches the requested role
                String userRole = loggedInAdmin.getRole() != null ? loggedInAdmin.getRole() : "ADMIN";
                
                if (requestedRole != null && !requestedRole.equals(userRole)) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("message", "Invalid role. This account is registered as " + userRole);
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
                }
                
                // Create a safe admin response (exclude password)
                Map<String, Object> adminResponse = createSafeAdminResponse(loggedInAdmin);
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("role", userRole);
                response.put("admin", adminResponse);
                response.put("profileComplete", loggedInAdmin.getProfileComplete() != null ? loggedInAdmin.getProfileComplete() : false);
                response.put("message", (userRole.equals("MANAGER") ? "Manager" : "Admin") + " login successful");
                System.out.println((userRole.equals("MANAGER") ? "Manager" : "Admin") + " login successful for: " + email);
                System.out.println("Profile complete: " + (loggedInAdmin.getProfileComplete() != null ? loggedInAdmin.getProfileComplete() : false));
                return ResponseEntity.ok(response);
            } else {
                // After failed login, check if account got locked
                Admin adminAfterFailed = adminService.getAdminByEmail(email);
                if (adminAfterFailed != null) {
                    // Check if account is now locked
                    if (adminAfterFailed.getAccountLocked() != null && adminAfterFailed.getAccountLocked()) {
                        Map<String, Object> response = new HashMap<>();
                        response.put("success", false);
                        response.put("accountLocked", true);
                        response.put("failedAttempts", adminAfterFailed.getFailedLoginAttempts() != null ? adminAfterFailed.getFailedLoginAttempts() : 3);
                        response.put("message", "Account is locked due to 3 failed login attempts. Please contact manager to unlock.");
                        System.out.println("🔒 Login blocked - account locked after failed attempt for: " + email);
                        System.out.println("   Failed attempts: " + adminAfterFailed.getFailedLoginAttempts());
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
                    }
                    
                    // Account not locked yet, show remaining attempts
                    int failedAttempts = adminAfterFailed.getFailedLoginAttempts() != null ? adminAfterFailed.getFailedLoginAttempts() : 0;
                    int remainingAttempts = 3 - failedAttempts;
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("failedAttempts", failedAttempts);
                    response.put("remainingAttempts", remainingAttempts);
                    response.put("message", "Invalid email or password. " + remainingAttempts + " attempt(s) remaining before account lock.");
                    System.out.println("❌ Login failed for: " + email + " (Attempt " + failedAttempts + "/3)");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
                }
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Invalid email or password");
                System.out.println("❌ Login failed - admin not found for: " + email);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
        } catch (HttpMessageNotReadableException e) {
            long totalTime = System.currentTimeMillis() - startTime;
            System.err.println("==========================================");
            System.err.println("[ADMIN-LOGIN] HttpMessageNotReadableException after " + totalTime + "ms");
            System.err.println("[ADMIN-LOGIN] Error message: " + e.getMessage());
            System.err.println("[ADMIN-LOGIN] Root cause: " + (e.getRootCause() != null ? e.getRootCause().getMessage() : "null"));
            System.err.println("[ADMIN-LOGIN] Most specific cause: " + (e.getMostSpecificCause() != null ? e.getMostSpecificCause().getMessage() : "null"));
            e.printStackTrace();
            System.err.println("==========================================");
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Invalid request format. Please ensure Content-Type is application/json and request body is valid JSON.");
            response.put("error", "Request body could not be parsed. Expected JSON format: {\"email\":\"...\",\"password\":\"...\"}");
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            long totalTime = System.currentTimeMillis() - startTime;
            System.err.println("==========================================");
            System.err.println("[ADMIN-LOGIN] Exception after " + totalTime + "ms");
            System.err.println("[ADMIN-LOGIN] Error type: " + e.getClass().getName());
            System.err.println("[ADMIN-LOGIN] Error message: " + e.getMessage());
            e.printStackTrace();
            System.err.println("==========================================");
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "An error occurred during login: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get all user login history (for admin dashboard)
     */
    @GetMapping("/login-history")
    public ResponseEntity<List<Map<String, Object>>> getAllLoginHistory() {
        try {
            System.out.println("🔍 Fetching all login history");
            List<com.neo.springapp.model.UserLoginHistory> history = loginHistoryService.getAllLoginHistory();
            System.out.println("📊 Found " + history.size() + " total login history records");
            
            // Convert to DTO format for frontend
            List<Map<String, Object>> historyList = history.stream().map(h -> {
                Map<String, Object> item = new HashMap<>();
                item.put("id", h.getId());
                if (h.getUser() != null) {
                    item.put("userId", h.getUser().getId());
                    item.put("userName", h.getUser().getUsername());
                    item.put("userEmail", h.getUser().getEmail());
                    item.put("accountNumber", h.getUser().getAccountNumber());
                } else {
                    item.put("userId", null);
                    item.put("userName", "Unknown");
                    item.put("userEmail", "Unknown");
                    item.put("accountNumber", "Unknown");
                }
                item.put("loginDate", h.getLoginDate());
                item.put("loginTime", h.getLoginTime());
                item.put("loginLocation", h.getLoginLocation());
                item.put("ipAddress", h.getIpAddress());
                item.put("deviceInfo", h.getDeviceInfo());
                item.put("loginMethod", h.getLoginMethod());
                item.put("status", h.getStatus());
                return item;
            }).collect(java.util.stream.Collectors.toList());
            
            System.out.println("✅ Returning " + historyList.size() + " login history records");
            return ResponseEntity.ok(historyList);
        } catch (Exception e) {
            System.err.println("❌ Error fetching all login history: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Save admin feature access permissions
     */
    @PostMapping("/feature-access")
    @SuppressWarnings("unchecked")
    public ResponseEntity<Map<String, Object>> saveFeatureAccess(@RequestBody Map<String, Object> request) {
        try {
            List<Map<String, Object>> features = (List<Map<String, Object>>) request.get("features");
            
            // Save to database or return success
            // For now, we'll just return success (can be extended to save to database)
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Feature access updated successfully");
            response.put("features", features);
            
            System.out.println("Feature access updated: " + features.size() + " features");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error updating feature access: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get admin feature access permissions
     */
    @GetMapping("/feature-access")
    public ResponseEntity<Map<String, Object>> getFeatureAccess() {
        try {
            // Get from database or return default
            // For now, return empty list (can be extended to fetch from database)
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("features", new java.util.ArrayList<>());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error fetching feature access: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get login history for a specific user by account number
     */
    @GetMapping("/login-history/user/{accountNumber}")
    public ResponseEntity<List<Map<String, Object>>> getUserLoginHistory(@PathVariable String accountNumber) {
        try {
            List<com.neo.springapp.model.UserLoginHistory> history = loginHistoryService.getUserLoginHistoryByAccountNumber(accountNumber);
            
            // Convert to DTO format
            List<Map<String, Object>> historyList = history.stream().map(h -> {
                Map<String, Object> item = new HashMap<>();
                item.put("id", h.getId());
                item.put("userId", h.getUser().getId());
                item.put("userName", h.getUser().getUsername());
                item.put("userEmail", h.getUser().getEmail());
                item.put("accountNumber", h.getUser().getAccountNumber());
                item.put("loginDate", h.getLoginDate());
                item.put("loginTime", h.getLoginTime());
                item.put("loginLocation", h.getLoginLocation());
                item.put("ipAddress", h.getIpAddress());
                item.put("deviceInfo", h.getDeviceInfo());
                item.put("loginMethod", h.getLoginMethod());
                item.put("status", h.getStatus());
                return item;
            }).collect(java.util.stream.Collectors.toList());
            
            return ResponseEntity.ok(historyList);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get recent login history (last N records)
     */
    @GetMapping("/login-history/recent")
    public ResponseEntity<List<Map<String, Object>>> getRecentLoginHistory(@RequestParam(defaultValue = "50") int limit) {
        try {
            System.out.println("🔍 Fetching recent login history (limit: " + limit + ")");
            List<com.neo.springapp.model.UserLoginHistory> history = loginHistoryService.getRecentLoginHistory(limit);
            System.out.println("📊 Found " + history.size() + " login history records");
            
            // Convert to DTO format
            List<Map<String, Object>> historyList = history.stream().map(h -> {
                Map<String, Object> item = new HashMap<>();
                item.put("id", h.getId());
                if (h.getUser() != null) {
                    item.put("userId", h.getUser().getId());
                    item.put("userName", h.getUser().getUsername());
                    item.put("userEmail", h.getUser().getEmail());
                    item.put("accountNumber", h.getUser().getAccountNumber());
                } else {
                    item.put("userId", null);
                    item.put("userName", "Unknown");
                    item.put("userEmail", "Unknown");
                    item.put("accountNumber", "Unknown");
                }
                item.put("loginDate", h.getLoginDate());
                item.put("loginTime", h.getLoginTime());
                item.put("loginLocation", h.getLoginLocation());
                item.put("ipAddress", h.getIpAddress());
                item.put("deviceInfo", h.getDeviceInfo());
                item.put("loginMethod", h.getLoginMethod());
                item.put("status", h.getStatus());
                return item;
            }).collect(java.util.stream.Collectors.toList());
            
            System.out.println("✅ Returning " + historyList.size() + " login history records to admin");
            return ResponseEntity.ok(historyList);
        } catch (Exception e) {
            System.err.println("❌ Error fetching login history: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Helper method to create a safe admin response (exclude password)
     */
    private Map<String, Object> createSafeAdminResponse(Admin admin) {
        Map<String, Object> adminResponse = new HashMap<>();
        adminResponse.put("id", admin.getId());
        adminResponse.put("name", admin.getName());
        adminResponse.put("email", admin.getEmail());
        adminResponse.put("role", admin.getRole());
        adminResponse.put("pan", admin.getPan());
        adminResponse.put("employeeId", admin.getEmployeeId());
        adminResponse.put("address", admin.getAddress());
        adminResponse.put("aadharNumber", admin.getAadharNumber());
        adminResponse.put("mobileNumber", admin.getMobileNumber());
        adminResponse.put("qualifications", admin.getQualifications());
        adminResponse.put("profileComplete", admin.getProfileComplete() != null ? admin.getProfileComplete() : false);
        adminResponse.put("accountLocked", admin.getAccountLocked() != null ? admin.getAccountLocked() : false);
        adminResponse.put("failedLoginAttempts", admin.getFailedLoginAttempts() != null ? admin.getFailedLoginAttempts() : 0);
        adminResponse.put("lastFailedLoginTime", admin.getLastFailedLoginTime());
        adminResponse.put("createdAt", admin.getCreatedAt());
        adminResponse.put("lastUpdated", admin.getLastUpdated());
        return adminResponse;
    }
}
