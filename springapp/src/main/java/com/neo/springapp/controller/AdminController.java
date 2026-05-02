package com.neo.springapp.controller;

import com.neo.springapp.model.Admin;
import com.neo.springapp.model.ProfileUpdateRequest;
import com.neo.springapp.model.AdminProfileUpdateRequest;
import com.neo.springapp.model.SalaryAccount;
import com.neo.springapp.model.SalaryTransaction;
import com.neo.springapp.model.SalaryNormalTransaction;
import com.neo.springapp.service.AdminService;
import com.neo.springapp.service.UserLoginHistoryService;
import com.neo.springapp.service.ProfileUpdateService;
import com.neo.springapp.service.SalaryAccountService;
import com.neo.springapp.service.FaceAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admins")
@SuppressWarnings("null")
public class AdminController {

    private static final String ADMIN_PROFILE_PHOTO_DIR = "uploads/admin-profile-photos";

    @Autowired
    private AdminService adminService;
    
    @Autowired
    private UserLoginHistoryService loginHistoryService;
    
    @Autowired
    private com.neo.springapp.service.AdminLoginHistoryService adminLoginHistoryService;
    
    @Autowired
    private com.neo.springapp.service.SessionHistoryService sessionHistoryService;
    
    @Autowired
    private ProfileUpdateService profileUpdateService;

    @Autowired
    private com.neo.springapp.service.AdminProfileUpdateService adminProfileUpdateService;

    @Autowired
    private com.neo.springapp.service.AccountService accountService;

    @Autowired
    private com.neo.springapp.service.SavingsUpiService savingsUpiService;

    @Autowired
    private com.neo.springapp.service.FraudAlertService fraudAlertService;

    @Autowired
    private com.neo.springapp.service.AiSecurityService aiSecurityService;

    @Autowired(required = false)
    private com.neo.springapp.service.BranchAccountService branchAccountService;

    @Autowired
    private SalaryAccountService salaryAccountService;

    @Autowired
    private FaceAuthService faceAuthService;

    /**
     * Assign mandatory Customer ID (9 digits: PAN 4 + DOB 5) to all existing accounts
     * that don't have one. Call this to migrate existing data.
     */
    @PostMapping("/accounts/assign-customer-ids")
    public ResponseEntity<Map<String, Object>> assignCustomerIdsToExistingAccounts() {
        try {
            int count = accountService.assignCustomerIdsToExistingAccounts();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Assigned Customer ID to " + count + " account(s)");
            response.put("accountsUpdated", count);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to assign customer IDs: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

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

            // First-time profile completion is saved directly.
            boolean profileComplete = existingAdmin.getProfileComplete() != null && existingAdmin.getProfileComplete();
            if (!profileComplete) {
                Admin updatedAdmin = adminService.updateAdmin(existingAdmin.getId(), adminDetails);
                if (updatedAdmin != null) {
                    // Mark as complete on first successful save
                    updatedAdmin.setProfileComplete(true);
                    updatedAdmin = adminService.saveAdmin(updatedAdmin);

                    if (branchAccountService != null && updatedAdmin.getBranchAccountNumber() != null && !updatedAdmin.getBranchAccountNumber().trim().isEmpty()) {
                        branchAccountService.setBranchAccount(
                            updatedAdmin.getBranchAccountNumber(),
                            updatedAdmin.getBranchAccountName(),
                            updatedAdmin.getBranchAccountIfsc(),
                            updatedAdmin.getId()
                        );
                    }
                    response.put("success", true);
                    response.put("message", "Profile completed successfully");
                    response.put("admin", updatedAdmin);
                    return ResponseEntity.ok(response);
                } else {
                    response.put("success", false);
                    response.put("message", "Failed to update profile");
                    return ResponseEntity.badRequest().body(response);
                }
            }

            // Subsequent edits: create approval request for manager dashboard
            Map<String, Object> approvalResponse = adminProfileUpdateService.createProfileUpdateRequest(existingAdmin, adminDetails);
            if (Boolean.TRUE.equals(approvalResponse.get("success"))) {
                return ResponseEntity.ok(approvalResponse);
            } else {
                // If request creation failed, fall back to error response
                response.put("success", false);
                response.put("message", String.valueOf(approvalResponse.get("message")));
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error updating profile: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Get branch deposit account summary (for Manager Dashboard).
     * All user charges, loan interest, KYC/debit charges are deposited here.
     */
    @GetMapping("/branch-account")
    public ResponseEntity<Map<String, Object>> getBranchAccount() {
        if (branchAccountService == null) {
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("accountNumber", com.neo.springapp.service.BranchAccountService.DEFAULT_NEOBANK_ACCOUNT);
            fallback.put("accountName", "NeoBank Branch");
            fallback.put("balance", 0.0);
            fallback.put("isConfigured", false);
            return ResponseEntity.ok(fallback);
        }
        return ResponseEntity.ok(branchAccountService.getBranchAccountSummary());
    }

    /**
     * Get branch account transactions (all credits to branch) with optional date filter and search.
     * Query params: fromDate (yyyy-MM-dd), toDate (yyyy-MM-dd), search, page (default 0), size (default 20).
     */
    @GetMapping("/branch-account/transactions")
    public ResponseEntity<Map<String, Object>> getBranchAccountTransactions(
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        if (branchAccountService == null) {
            Map<String, Object> empty = new HashMap<>();
            empty.put("content", List.of());
            empty.put("totalElements", 0L);
            empty.put("totalPages", 0);
            return ResponseEntity.ok(empty);
        }
        LocalDate from = null;
        LocalDate to = null;
        if (fromDate != null && !fromDate.isBlank()) {
            try { from = LocalDate.parse(fromDate); } catch (DateTimeParseException ignored) {}
        }
        if (toDate != null && !toDate.isBlank()) {
            try { to = LocalDate.parse(toDate); } catch (DateTimeParseException ignored) {}
        }
        Map<String, Object> result = branchAccountService.getBranchAccountTransactions(
            null, from, to, search, page, size);
        return ResponseEntity.ok(result);
    }

    /**
     * Set or open branch account from Manager Dashboard.
     * Body: accountNumber, accountName (optional), ifscCode (optional), adminId (optional - for audit).
     */
    @PutMapping("/branch-account")
    public ResponseEntity<Map<String, Object>> setBranchAccount(@RequestBody Map<String, String> body) {
        Map<String, Object> response = new HashMap<>();
        if (branchAccountService == null) {
            response.put("success", false);
            response.put("message", "Branch account service not available");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }
        String accountNumber = body.get("accountNumber");
        String accountName = body.get("accountName");
        String ifscCode = body.get("ifscCode");
        Long adminId = null;
        if (body.containsKey("adminId") && body.get("adminId") != null && !body.get("adminId").trim().isEmpty()) {
            try {
                adminId = Long.parseLong(body.get("adminId").trim());
            } catch (NumberFormatException ignored) {}
        }
        Map<String, Object> result = branchAccountService.setBranchAccount(accountNumber, accountName, ifscCode, adminId);
        return ResponseEntity.ok(result);
    }

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> loginAdmin(
            @RequestBody Map<String, String> credentials,
            @RequestHeader(value = "X-Forwarded-For", required = false) String forwardedFor,
            @RequestHeader(value = "User-Agent", required = false) String userAgent) {
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
                
                // Record admin login history
                try {
                    String clientIp = forwardedFor != null ? forwardedFor.split(",")[0].trim() : "Unknown";
                    String deviceInfo = userAgent != null ? userAgent : "Unknown";
                    String location = "Unknown";
                    
                    System.out.println("🔐 Admin login detected - Recording session history for admin ID: " + loggedInAdmin.getId() + ", Email: " + loggedInAdmin.getEmail());
                    
                    // Record in AdminLoginHistory
                    try {
                        adminLoginHistoryService.recordLogin(loggedInAdmin, location, clientIp, deviceInfo, "PASSWORD");
                        System.out.println("✅ AdminLoginHistory recorded successfully");
                    } catch (Exception e1) {
                        System.err.println("❌ Error recording AdminLoginHistory: " + e1.getMessage());
                        e1.printStackTrace();
                    }
                    
                    // Record in SessionHistory
                    try {
                        sessionHistoryService.recordAdminLogin(loggedInAdmin, location, clientIp, deviceInfo, "PASSWORD");
                        System.out.println("✅ SessionHistory recorded successfully");
                    } catch (Exception e2) {
                        System.err.println("❌ Error recording SessionHistory: " + e2.getMessage());
                        e2.printStackTrace();
                        // Check if it's a table not found error
                        if (e2.getMessage() != null && (e2.getMessage().contains("Table") || e2.getMessage().contains("doesn't exist"))) {
                            System.err.println("⚠️ WARNING: session_history table may not exist. Please check database migrations.");
                        }
                    }
                } catch (Exception e) {
                    System.err.println("❌ Error recording admin login history: " + e.getMessage());
                    e.printStackTrace();
                    // Don't fail login if history recording fails
                }
                
                // Create a safe admin response (exclude password)
                Map<String, Object> adminResponse = createSafeAdminResponse(loggedInAdmin);
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("role", userRole);
                response.put("admin", adminResponse);
                response.put("profileComplete", loggedInAdmin.getProfileComplete() != null ? loggedInAdmin.getProfileComplete() : false);
                boolean faceRegistered = faceAuthService.hasFaceRegistered(email);
                response.put("faceAuthEnabled", faceAuthService.isFaceAuthEnabled());
                response.put("faceRegistered", faceRegistered);
                response.put("faceRequired", false);
                response.put("message", (userRole.equals("MANAGER") ? "Manager" : "Admin") + " login successful");
                System.out.println((userRole.equals("MANAGER") ? "Manager" : "Admin") + " login successful for: " + email);
                System.out.println("Profile complete: " + (loggedInAdmin.getProfileComplete() != null ? loggedInAdmin.getProfileComplete() : false));
                return ResponseEntity.ok(response);
            } else {
                // After failed login, check if account got locked
                Admin adminAfterFailed = adminService.getAdminByEmail(email);
                if (adminAfterFailed != null) {
                    // AI Security: Analyze failed admin login
                    try {
                        String aiIp = forwardedFor != null ? forwardedFor.split(",")[0].trim() : "Unknown";
                        aiSecurityService.analyzeLoginAttempt(email, "ADMIN", aiIp, userAgent, userAgent, null, false);
                    } catch (Exception aiEx) {
                        System.err.println("AI Security analysis error: " + aiEx.getMessage());
                    }
                    // Check if account is now locked - record fraud alert for manager
                    if (adminAfterFailed.getAccountLocked() != null && adminAfterFailed.getAccountLocked()) {
                        try {
                            String clientIp = forwardedFor != null ? forwardedFor.split(",")[0].trim() : "Unknown";
                            String deviceInfo = userAgent != null ? userAgent : "Unknown";
                            fraudAlertService.recordLoginFraud(adminAfterFailed.getEmail(), adminAfterFailed.getName(), com.neo.springapp.model.FraudAlert.SourceType.ADMIN, clientIp, "IP: " + clientIp, deviceInfo);
                        } catch (Exception e) {
                            System.err.println("Failed to record admin login fraud alert: " + e.getMessage());
                        }
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
            Throwable rootCause = e.getRootCause();
            System.err.println("[ADMIN-LOGIN] Root cause: " + (rootCause != null ? rootCause.getMessage() : "null"));
            Throwable mostSpecificCause = e.getMostSpecificCause();
            System.err.println("[ADMIN-LOGIN] Most specific cause: " + (mostSpecificCause != null ? mostSpecificCause.getMessage() : "null"));
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
     * Get admin feature access for a specific email (validated)
     * This endpoint validates the path variable to avoid corrupted/concatenated paths.
     */
    @GetMapping("/feature-access/{email}")
    public ResponseEntity<Map<String, Object>> getFeatureAccessForEmail(@PathVariable String email) {
        try {
            // Basic validation to reject obviously corrupted values
            if (email == null || email.isEmpty() || email.length() > 200 || email.contains("api") || email.contains("/")) {
                Map<String, Object> resp = new HashMap<>();
                resp.put("success", false);
                resp.put("message", "Invalid email parameter");
                System.err.println("[FEATURE-ACCESS] Rejected invalid email path: " + email);
                return ResponseEntity.badRequest().body(resp);
            }

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
     * Generate or regenerate an admin ID card.
     */
    @PostMapping("/id-card/{id}/generate")
    public ResponseEntity<Map<String, Object>> generateIdCard(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "Manager") String generatedBy) {
        Map<String, Object> response = new HashMap<>();
        try {
            Admin admin = adminService.generateIdCard(id, generatedBy);
            if (admin == null) {
                response.put("success", false);
                response.put("message", "Admin not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            response.put("success", true);
            response.put("message", "ID card generated successfully");
            response.put("admin", createSafeAdminResponse(admin));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error generating ID card: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Update editable ID card meta (designation/department).
     */
    @PutMapping("/id-card/{id}")
    public ResponseEntity<Map<String, Object>> updateIdCardMeta(
            @PathVariable Long id,
            @RequestParam(required = false) String designation,
            @RequestParam(required = false) String department,
            @RequestParam(required = false, defaultValue = "Manager") String updatedBy) {
        Map<String, Object> response = new HashMap<>();
        try {
            Admin admin = adminService.updateIdCardMeta(id, designation, department, updatedBy);
            if (admin == null) {
                response.put("success", false);
                response.put("message", "Admin not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            response.put("success", true);
            response.put("message", "ID card details updated successfully");
            response.put("admin", createSafeAdminResponse(admin));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error updating ID card details: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Save manager signature (drawn) for PVC card printing.
     */
    @PostMapping(value = "/id-card/{id}/signature", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> saveIdCardSignature(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        Map<String, Object> response = new HashMap<>();
        try {
            String signatureDataUrl = body != null && body.get("signatureDataUrl") != null ? String.valueOf(body.get("signatureDataUrl")) : null;
            String signedBy = body != null && body.get("signedBy") != null ? String.valueOf(body.get("signedBy")) : "Manager";

            if (signatureDataUrl == null || signatureDataUrl.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "signatureDataUrl is required");
                return ResponseEntity.badRequest().body(response);
            }
            // basic validation for data URL images
            String s = signatureDataUrl.trim();
            if (!(s.startsWith("data:image/png;base64,") || s.startsWith("data:image/jpeg;base64,") || s.startsWith("data:image/webp;base64,"))) {
                response.put("success", false);
                response.put("message", "Signature must be a base64 image data URL (png/jpg/webp)");
                return ResponseEntity.badRequest().body(response);
            }

            Admin admin = adminService.saveIdCardManagerSignature(id, s, signedBy);
            if (admin == null) {
                response.put("success", false);
                response.put("message", "Admin not found or invalid signature");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            response.put("success", true);
            response.put("message", "Signature saved successfully");
            response.put("admin", createSafeAdminResponse(admin));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error saving signature: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Lookup admin details by ID card number (for scan or manual entry).
     */
    @GetMapping("/id-card/lookup/{idCardNumber}")
    public ResponseEntity<Map<String, Object>> lookupByIdCardNumber(@PathVariable String idCardNumber) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (idCardNumber == null || idCardNumber.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "ID card number is required");
                return ResponseEntity.badRequest().body(response);
            }
            Admin admin = adminService.getAdminByIdCardNumber(idCardNumber);
            if (admin == null) {
                response.put("success", false);
                response.put("message", "ID card not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            response.put("success", true);
            response.put("admin", createSafeAdminResponse(admin));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error looking up ID card: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Upload an admin profile photo (used on PVC ID card).
     */
    @PostMapping(value = "/profile-photo/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadAdminProfilePhoto(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (file == null || file.isEmpty()) {
                response.put("success", false);
                response.put("message", "Please select a valid image file");
                return ResponseEntity.badRequest().body(response);
            }

            Admin admin = adminService.getAdminById(id);
            if (admin == null) {
                response.put("success", false);
                response.put("message", "Admin not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            String originalName = file.getOriginalFilename();
            String lower = (originalName != null ? originalName : "").toLowerCase();
            String ext = lower.contains(".") ? lower.substring(lower.lastIndexOf('.') + 1) : "";
            if (!(ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png") || ext.equals("webp"))) {
                response.put("success", false);
                response.put("message", "Only JPG, JPEG, PNG, or WEBP images are allowed");
                return ResponseEntity.badRequest().body(response);
            }

            // Create uploads directory if missing
            Path uploadDir = Paths.get(ADMIN_PROFILE_PHOTO_DIR).normalize();
            Files.createDirectories(uploadDir);

            String filename = "admin-" + id + "-" + System.currentTimeMillis() + "." + ext;
            Path target = uploadDir.resolve(filename).normalize();

            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            admin.setProfilePhotoPath(ADMIN_PROFILE_PHOTO_DIR + "/" + filename);
            Admin saved = adminService.saveAdmin(admin);

            response.put("success", true);
            response.put("message", "Profile photo uploaded successfully");
            response.put("admin", createSafeAdminResponse(saved));
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            response.put("success", false);
            response.put("message", "Failed to save profile photo: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error uploading profile photo: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Serve an admin profile photo by admin id.
     */
    @GetMapping("/profile-photo/{id}")
    public ResponseEntity<byte[]> getAdminProfilePhoto(@PathVariable Long id) {
        try {
            Admin admin = adminService.getAdminById(id);
            if (admin == null || admin.getProfilePhotoPath() == null || admin.getProfilePhotoPath().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            // Only use filename part from DB to prevent path traversal
            String storedPath = admin.getProfilePhotoPath();
            String filename = Paths.get(storedPath).getFileName().toString();
            Path filePath = Paths.get(ADMIN_PROFILE_PHOTO_DIR).resolve(filename).normalize();

            if (!Files.exists(filePath)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            String contentType = Files.probeContentType(filePath);
            MediaType mediaType;
            try {
                mediaType = contentType != null ? MediaType.parseMediaType(contentType) : MediaType.APPLICATION_OCTET_STREAM;
            } catch (Exception ignored) {
                mediaType = MediaType.APPLICATION_OCTET_STREAM;
            }

            byte[] bytes = Files.readAllBytes(filePath);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate, max-age=0")
                    .contentType(mediaType)
                    .body(bytes);
        } catch (Exception e) {
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
        // ID card meta
        adminResponse.put("idCardNumber", admin.getIdCardNumber());
        adminResponse.put("idCardGeneratedAt", admin.getIdCardGeneratedAt());
        adminResponse.put("idCardGeneratedCount", admin.getIdCardGeneratedCount());
        adminResponse.put("idCardLastUpdatedAt", admin.getIdCardLastUpdatedAt());
        adminResponse.put("idCardLastUpdatedBy", admin.getIdCardLastUpdatedBy());
        adminResponse.put("idCardDesignation", admin.getIdCardDesignation());
        adminResponse.put("idCardDepartment", admin.getIdCardDepartment());
        adminResponse.put("idCardValidTill", admin.getIdCardValidTill());
        adminResponse.put("profilePhotoPath", admin.getProfilePhotoPath());
        adminResponse.put("dateOfJoining", admin.getDateOfJoining());
        adminResponse.put("idCardManagerSignatureDataUrl", admin.getIdCardManagerSignatureDataUrl());
        adminResponse.put("idCardManagerSignedAt", admin.getIdCardManagerSignedAt());
        adminResponse.put("idCardManagerSignedBy", admin.getIdCardManagerSignedBy());
        adminResponse.put("branchAccountNumber", admin.getBranchAccountNumber());
        adminResponse.put("branchAccountName", admin.getBranchAccountName());
        adminResponse.put("branchAccountIfsc", admin.getBranchAccountIfsc());
        adminResponse.put("salaryAccountNumber", admin.getSalaryAccountNumber());
        adminResponse.put("salaryAccountLinked", admin.getSalaryAccountLinked() != null ? admin.getSalaryAccountLinked() : false);
        return adminResponse;
    }

    /**
     * Get all blocked admins
     */
    @GetMapping("/blocked")
    public ResponseEntity<List<Admin>> getBlockedAdmins() {
        List<Admin> blockedAdmins = adminService.getBlockedAdmins();
        return ResponseEntity.ok(blockedAdmins);
    }

    /**
     * Unblock an admin account
     */
    @PostMapping("/unblock/{id}")
    public ResponseEntity<Map<String, Object>> unblockAdmin(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            Admin admin = adminService.unblockAdmin(id);
            if (admin != null) {
                response.put("success", true);
                response.put("message", "Admin account unblocked successfully");
                response.put("admin", createSafeAdminResponse(admin));
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Admin not found");
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error unblocking admin: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Reset admin password
     */
    @PostMapping("/reset-password/{id}")
    public ResponseEntity<Map<String, Object>> resetAdminPassword(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String newPassword = request.get("newPassword");
            if (newPassword == null || newPassword.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "New password is required");
                return ResponseEntity.badRequest().body(response);
            }

            Admin admin = adminService.resetAdminPassword(id, newPassword);
            if (admin != null) {
                response.put("success", true);
                response.put("message", "Password reset successfully");
                response.put("admin", createSafeAdminResponse(admin));
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Admin not found");
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error resetting password: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ========== PROFILE UPDATE ADMIN ENDPOINTS ==========

    // Get all pending profile update requests
    @GetMapping("/profile-update/pending")
    public ResponseEntity<List<ProfileUpdateRequest>> getPendingProfileUpdates() {
        List<ProfileUpdateRequest> requests = profileUpdateService.getPendingUpdateRequests();
        return ResponseEntity.ok(requests);
    }

    // Get all profile update requests
    @GetMapping("/profile-update/all")
    public ResponseEntity<List<ProfileUpdateRequest>> getAllProfileUpdates() {
        List<ProfileUpdateRequest> requests = profileUpdateService.getAllUpdateRequests();
        return ResponseEntity.ok(requests);
    }

    // Approve profile update request
    @PutMapping("/profile-update/{requestId}/approve")
    public ResponseEntity<Map<String, Object>> approveProfileUpdate(
            @PathVariable Long requestId,
            @RequestParam String approvedBy) {
        Map<String, Object> response = profileUpdateService.approveProfileUpdate(requestId, approvedBy);
        return ResponseEntity.ok(response);
    }

    // Reject profile update request
    @PutMapping("/profile-update/{requestId}/reject")
    public ResponseEntity<Map<String, Object>> rejectProfileUpdate(
            @PathVariable Long requestId,
            @RequestParam String rejectedBy,
            @RequestParam(required = false) String reason) {
        Map<String, Object> response = profileUpdateService.rejectProfileUpdate(requestId, rejectedBy, reason);
        return ResponseEntity.ok(response);
    }

    // Get profile update request by ID
    @GetMapping("/profile-update/{requestId}")
    public ResponseEntity<ProfileUpdateRequest> getProfileUpdateRequest(@PathVariable Long requestId) {
        return profileUpdateService.getUpdateRequestById(requestId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    // ========== ADMIN EMPLOYEE PROFILE UPDATE (MANAGER APPROVAL) ==========

    /**
     * Get all pending admin profile update requests (for Manager Dashboard).
     */
    @GetMapping("/admin-profile-update/pending")
    public ResponseEntity<List<AdminProfileUpdateRequest>> getPendingAdminProfileUpdates() {
        List<AdminProfileUpdateRequest> requests = adminProfileUpdateService.getPendingRequests();
        return ResponseEntity.ok(requests);
    }

    /**
     * Get all admin profile update requests (for audit/history).
     */
    @GetMapping("/admin-profile-update/all")
    public ResponseEntity<List<AdminProfileUpdateRequest>> getAllAdminProfileUpdates() {
        List<AdminProfileUpdateRequest> requests = adminProfileUpdateService.getAllRequests();
        return ResponseEntity.ok(requests);
    }

    /**
     * Approve an admin profile update request and apply it immediately.
     */
    @PutMapping("/admin-profile-update/{requestId}/approve")
    public ResponseEntity<Map<String, Object>> approveAdminProfileUpdate(
            @PathVariable Long requestId,
            @RequestParam String approvedBy) {
        Map<String, Object> result = adminProfileUpdateService.approveProfileUpdate(requestId, approvedBy);
        HttpStatus status = Boolean.TRUE.equals(result.get("success")) ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(result);
    }

    /**
     * Reject an admin profile update request.
     */
    @PutMapping("/admin-profile-update/{requestId}/reject")
    public ResponseEntity<Map<String, Object>> rejectAdminProfileUpdate(
            @PathVariable Long requestId,
            @RequestParam String rejectedBy,
            @RequestParam(required = false) String reason) {
        Map<String, Object> result = adminProfileUpdateService.rejectProfileUpdate(requestId, rejectedBy, reason);
        HttpStatus status = Boolean.TRUE.equals(result.get("success")) ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(result);
    }

    /**
     * Get a specific admin profile update request by ID.
     */
    @GetMapping("/admin-profile-update/{requestId}")
    public ResponseEntity<AdminProfileUpdateRequest> getAdminProfileUpdateRequest(@PathVariable Long requestId) {
        return adminProfileUpdateService.getRequestById(requestId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    // ═══════════════════════════════════════════════════════════════
    // MANAGER PERMISSIONS — Admin Salary Account Linking & Monitoring
    // ═══════════════════════════════════════════════════════════════

    /**
     * Link an admin account with a NeoBank salary account (ONE TIME ONLY).
     * Once linked, the salary account number cannot be changed.
     */
    @PostMapping("/link-salary-account/{adminId}")
    public ResponseEntity<Map<String, Object>> linkSalaryAccount(
            @PathVariable Long adminId,
            @RequestBody Map<String, String> body) {
        Map<String, Object> response = new HashMap<>();
        try {
            String salaryAccountNumber = body.get("salaryAccountNumber");
            if (salaryAccountNumber == null || salaryAccountNumber.isBlank()) {
                response.put("success", false);
                response.put("message", "Salary account number is required");
                return ResponseEntity.badRequest().body(response);
            }

            Admin admin = adminService.getAdminById(adminId);
            if (admin == null) {
                response.put("success", false);
                response.put("message", "Admin not found");
                return ResponseEntity.badRequest().body(response);
            }

            // Enforce ONE TIME linking
            if (Boolean.TRUE.equals(admin.getSalaryAccountLinked())) {
                response.put("success", false);
                response.put("message", "Salary account already linked. This is a one-time operation and cannot be changed.");
                return ResponseEntity.badRequest().body(response);
            }

            // Verify the salary account exists
            SalaryAccount salaryAcc = salaryAccountService.getByAccountNumber(salaryAccountNumber);
            if (salaryAcc == null) {
                response.put("success", false);
                response.put("message", "Salary account not found with number: " + salaryAccountNumber);
                return ResponseEntity.badRequest().body(response);
            }

            // Link the accounts
            admin.setSalaryAccountNumber(salaryAccountNumber);
            admin.setSalaryAccountLinked(true);
            adminService.saveAdmin(admin);

            response.put("success", true);
            response.put("message", "Salary account linked successfully to admin " + admin.getName());
            response.put("admin", createSafeAdminResponse(admin));
            response.put("salaryAccount", salaryAcc);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to link salary account: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * View admin's linked salary account details (read-only for manager).
     */
    @GetMapping("/salary-account/{adminId}")
    public ResponseEntity<Map<String, Object>> getAdminSalaryAccount(@PathVariable Long adminId) {
        Map<String, Object> response = new HashMap<>();
        try {
            Admin admin = adminService.getAdminById(adminId);
            if (admin == null) {
                response.put("success", false);
                response.put("message", "Admin not found");
                return ResponseEntity.badRequest().body(response);
            }

            if (admin.getSalaryAccountNumber() == null || admin.getSalaryAccountNumber().isBlank()) {
                response.put("success", false);
                response.put("message", "No salary account linked for this admin");
                response.put("linked", false);
                return ResponseEntity.ok(response);
            }

            SalaryAccount salaryAcc = salaryAccountService.getByAccountNumber(admin.getSalaryAccountNumber());
            if (salaryAcc == null) {
                response.put("success", false);
                response.put("message", "Linked salary account not found");
                return ResponseEntity.ok(response);
            }

            response.put("success", true);
            response.put("linked", true);
            response.put("salaryAccount", salaryAcc);
            response.put("adminName", admin.getName());
            response.put("adminEmail", admin.getEmail());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error fetching salary account: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Monitor transactions for an admin's linked salary account (read-only).
     */
    @GetMapping("/salary-account/{adminId}/transactions")
    public ResponseEntity<Map<String, Object>> getAdminSalaryTransactions(@PathVariable Long adminId) {
        Map<String, Object> response = new HashMap<>();
        try {
            Admin admin = adminService.getAdminById(adminId);
            if (admin == null) {
                response.put("success", false);
                response.put("message", "Admin not found");
                return ResponseEntity.badRequest().body(response);
            }

            if (admin.getSalaryAccountNumber() == null || admin.getSalaryAccountNumber().isBlank()) {
                response.put("success", false);
                response.put("message", "No salary account linked for this admin");
                return ResponseEntity.ok(response);
            }

            SalaryAccount salaryAcc = salaryAccountService.getByAccountNumber(admin.getSalaryAccountNumber());
            if (salaryAcc == null) {
                response.put("success", false);
                response.put("message", "Linked salary account not found");
                return ResponseEntity.ok(response);
            }

            List<SalaryTransaction> salaryTxns = salaryAccountService.getTransactions(salaryAcc.getId());
            List<SalaryNormalTransaction> normalTxns = salaryAccountService.getNormalTransactions(salaryAcc.getId());

            response.put("success", true);
            response.put("adminName", admin.getName());
            response.put("accountNumber", salaryAcc.getAccountNumber());
            response.put("salaryTransactions", salaryTxns);
            response.put("normalTransactions", normalTxns);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error fetching transactions: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Generate salary report for an admin's linked salary account (read-only).
     */
    @GetMapping("/salary-account/{adminId}/report")
    public ResponseEntity<Map<String, Object>> generateSalaryReport(@PathVariable Long adminId) {
        Map<String, Object> response = new HashMap<>();
        try {
            Admin admin = adminService.getAdminById(adminId);
            if (admin == null) {
                response.put("success", false);
                response.put("message", "Admin not found");
                return ResponseEntity.badRequest().body(response);
            }

            if (admin.getSalaryAccountNumber() == null || admin.getSalaryAccountNumber().isBlank()) {
                response.put("success", false);
                response.put("message", "No salary account linked for this admin");
                return ResponseEntity.ok(response);
            }

            SalaryAccount salaryAcc = salaryAccountService.getByAccountNumber(admin.getSalaryAccountNumber());
            if (salaryAcc == null) {
                response.put("success", false);
                response.put("message", "Linked salary account not found");
                return ResponseEntity.ok(response);
            }

            List<SalaryTransaction> salaryTxns = salaryAccountService.getTransactions(salaryAcc.getId());

            // Calculate report data
            double totalSalaryCredited = salaryTxns.stream()
                    .filter(t -> "COMPLETED".equalsIgnoreCase(t.getStatus()))
                    .mapToDouble(t -> t.getSalaryAmount() != null ? t.getSalaryAmount() : 0)
                    .sum();
            long totalCredits = salaryTxns.stream()
                    .filter(t -> "COMPLETED".equalsIgnoreCase(t.getStatus()))
                    .count();
            String latestCredit = salaryTxns.isEmpty() ? null :
                    salaryTxns.stream()
                            .map(SalaryTransaction::getCreditDate)
                            .filter(d -> d != null)
                            .max(java.time.LocalDateTime::compareTo)
                            .map(Object::toString)
                            .orElse(null);

            response.put("success", true);
            response.put("adminName", admin.getName());
            response.put("adminEmail", admin.getEmail());
            response.put("accountNumber", salaryAcc.getAccountNumber());
            response.put("employeeName", salaryAcc.getEmployeeName());
            response.put("companyName", salaryAcc.getCompanyName());
            response.put("designation", salaryAcc.getDesignation());
            response.put("monthlySalary", salaryAcc.getMonthlySalary());
            response.put("currentBalance", salaryAcc.getBalance());
            response.put("totalSalaryCredited", totalSalaryCredited);
            response.put("totalCredits", totalCredits);
            response.put("latestCreditDate", latestCredit);
            response.put("accountStatus", salaryAcc.getStatus());
            response.put("transactions", salaryTxns);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error generating report: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  ADMIN UPI MANAGEMENT ENDPOINTS
    // ═══════════════════════════════════════════════════════════

    /**
     * List all savings UPI accounts.
     */
    @GetMapping("/upi/savings/accounts")
    public ResponseEntity<Map<String, Object>> adminListSavingsUpiAccounts() {
        Map<String, Object> response = new HashMap<>();
        try {
            response.put("success", true);
            response.put("accounts", savingsUpiService.adminListAllUpiAccounts());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error fetching UPI accounts: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Toggle (block/enable) a savings UPI account.
     */
    @PutMapping("/upi/savings/toggle/{accountNumber}")
    public ResponseEntity<Map<String, Object>> adminToggleSavingsUpi(
            @PathVariable String accountNumber,
            @RequestParam boolean enable) {
        Map<String, Object> response = new HashMap<>();
        try {
            response.put("success", true);
            response.put("result", savingsUpiService.adminToggleUpi(accountNumber, enable));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error toggling UPI: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * List all savings UPI transactions.
     */
    @GetMapping("/upi/savings/transactions")
    public ResponseEntity<Map<String, Object>> adminListSavingsUpiTransactions() {
        Map<String, Object> response = new HashMap<>();
        try {
            response.put("success", true);
            response.put("transactions", savingsUpiService.adminListAllTransactions());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error fetching UPI transactions: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * List flagged / fraudulent savings UPI transactions.
     */
    @GetMapping("/upi/savings/flagged")
    public ResponseEntity<Map<String, Object>> adminListFlaggedSavingsUpiTransactions() {
        Map<String, Object> response = new HashMap<>();
        try {
            response.put("success", true);
            response.put("transactions", savingsUpiService.adminListFlaggedTransactions());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error fetching flagged transactions: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
