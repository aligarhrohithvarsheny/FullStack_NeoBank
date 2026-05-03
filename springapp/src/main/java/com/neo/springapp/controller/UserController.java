package com.neo.springapp.controller;

import com.neo.springapp.model.User;
import com.neo.springapp.model.Account;
import com.neo.springapp.model.Admin;
import com.neo.springapp.service.UserService;
import com.neo.springapp.service.AdminAuditService;
import com.neo.springapp.service.AccountService;
import com.neo.springapp.service.PasswordService;
import com.neo.springapp.service.OtpService;
import com.neo.springapp.service.EmailService;
import com.neo.springapp.service.QrCodeService;
import com.neo.springapp.service.ProfileUpdateService;
import com.neo.springapp.model.ProfileUpdateRequest;
import com.neo.springapp.model.ProfileUpdateHistory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;

@RestController
@RequestMapping("/api/users")
@SuppressWarnings("null")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private AdminAuditService adminAuditService;

    @Value("${spring.web.cors.allowed-origins:}")
    private String allowedOrigins;
    
    @Autowired
    private AccountService accountService;
    
    @Autowired
    private PasswordService passwordService;
    
    @Autowired
    private OtpService otpService;
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private QrCodeService qrCodeService;
    
    @Autowired
    private ProfileUpdateService profileUpdateService;
    
    @Autowired
    private com.neo.springapp.service.AccountTrackingService accountTrackingService;
    
    @Autowired
    private com.neo.springapp.service.PdfService pdfService;
    
    @Autowired
    private com.neo.springapp.service.AdminService adminService;
    
    @Autowired
    private com.neo.springapp.repository.NetBankingServiceControlRepository netBankingServiceControlRepository;
    
    @Autowired
    private com.neo.springapp.service.UserLoginHistoryService loginHistoryService;
    
    @Autowired
    private com.neo.springapp.service.SessionHistoryService sessionHistoryService;

    @Autowired
    private com.neo.springapp.service.FraudAlertService fraudAlertService;

    @Autowired
    private com.neo.springapp.service.AiSecurityService aiSecurityService;
    
    /**
     * Helper method to create a safe user response object (avoid circular references and large byte arrays)
     */
    private Map<String, Object> createUserResponse(User user) {
        Map<String, Object> userResponse = new HashMap<>();
        userResponse.put("id", user.getId());
        userResponse.put("username", user.getUsername());
        userResponse.put("email", user.getEmail());
        userResponse.put("accountNumber", user.getAccountNumber());
        userResponse.put("status", user.getStatus());
        userResponse.put("accountLocked", user.isAccountLocked());
        userResponse.put("failedLoginAttempts", user.getFailedLoginAttempts());
        userResponse.put("joinDate", user.getJoinDate());
        
        // Add signature status fields (without the large byte array)
        userResponse.put("signatureStatus", user.getSignatureStatus());
        userResponse.put("signatureType", user.getSignatureType());
        userResponse.put("signatureName", user.getSignatureName());
        userResponse.put("signatureSubmittedDate", user.getSignatureSubmittedDate());
        userResponse.put("signatureReviewedDate", user.getSignatureReviewedDate());
        userResponse.put("signatureReviewedBy", user.getSignatureReviewedBy());
        userResponse.put("signatureRejectionReason", user.getSignatureRejectionReason());
        
        // Add profile photo metadata (without the large byte array)
        userResponse.put("profilePhotoType", user.getProfilePhotoType());
        userResponse.put("profilePhotoName", user.getProfilePhotoName());
        
        // Add account details if available (avoid circular references)
        if (user.getAccount() != null) {
            Map<String, Object> accountData = new HashMap<>();
            accountData.put("id", user.getAccount().getId());
            accountData.put("name", user.getAccount().getName());
            accountData.put("phone", user.getAccount().getPhone());
            accountData.put("balance", user.getAccount().getBalance());
            accountData.put("accountType", user.getAccount().getAccountType());
            accountData.put("aadharNumber", user.getAccount().getAadharNumber());
            accountData.put("pan", user.getAccount().getPan());
            accountData.put("address", user.getAccount().getAddress());
            accountData.put("dob", user.getAccount().getDob());
            accountData.put("occupation", user.getAccount().getOccupation());
            accountData.put("income", user.getAccount().getIncome());
            accountData.put("customerId", user.getAccount().getCustomerId());
            userResponse.put("account", accountData);
        }
        
        return userResponse;
    }

    // Authentication endpoint - Step 1: Verify password and send OTP
    @PostMapping("/authenticate")
    public ResponseEntity<Map<String, Object>> authenticateUser(
            @RequestBody Map<String, String> credentials,
            @RequestHeader(value = "X-Forwarded-For", required = false) String forwardedFor,
            @RequestHeader(value = "User-Agent", required = false) String userAgent) {
        try {
            String email = credentials.get("email");
            String password = credentials.get("password");
            
            System.out.println("Authentication attempt for email: " + email);
            
            if (email == null || password == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Email and password are required");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Check if Savings Account net banking is enabled
            try {
                var savingsControl = netBankingServiceControlRepository.findByServiceType("SAVINGS_ACCOUNT");
                if (savingsControl.isPresent() && !savingsControl.get().getEnabled()) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("netBankingDisabled", true);
                    response.put("message", "Net Banking for Savings Account is currently disabled by the administrator. Please try again later.");
                    return ResponseEntity.badRequest().body(response);
                }
            } catch (Exception e) {
                System.err.println("Error checking net banking status: " + e.getMessage());
            }
            
            // Check if this is an admin login first
            Admin admin = adminService.login(email, password);
            if (admin != null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("role", "ADMIN");
                response.put("admin", admin);
                response.put("message", "Admin login successful");
                System.out.println("Admin login successful for: " + email);
                return ResponseEntity.ok(response);
            }
            
            // Check if admin exists with this email (for better error messaging)
            Admin existingAdmin = adminService.getAdminByEmail(email);
            if (existingAdmin != null) {
                // Admin exists but password is wrong
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Invalid email or password");
                System.out.println("Admin exists but password incorrect for email: " + email);
                return ResponseEntity.badRequest().body(response);
            }
            
            Optional<User> userOpt = userService.findByEmail(email);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                System.out.println("User found: " + user.getUsername() + ", Status: " + user.getStatus());
                System.out.println("Account locked: " + user.isAccountLocked());
                System.out.println("Failed login attempts: " + user.getFailedLoginAttempts());
                
                // SECURITY: Check if account status is APPROVED
                if (!"APPROVED".equalsIgnoreCase(user.getStatus())) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("accountNotApproved", true);
                    if ("PENDING".equalsIgnoreCase(user.getStatus())) {
                        response.put("message", "Your account is pending approval. Please wait for admin authorization.");
                    } else if ("CLOSED".equalsIgnoreCase(user.getStatus())) {
                        response.put("accountClosed", true);
                        response.put("message", "Your account has been closed by the bank. Login is not allowed.");
                    } else {
                        response.put("message", "Your account is not in a state that allows login. Current status: " + user.getStatus());
                    }
                    System.out.println("Login blocked: account status is " + user.getStatus() + " for email: " + email);
                    return ResponseEntity.badRequest().body(response);
                }
                
                // SECURITY: Check if account status is CLOSED (additional check)
                if ("CLOSED".equalsIgnoreCase(user.getStatus())) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("accountClosed", true);
                    response.put("message", "Your account has been closed by the bank. Login is not allowed.");
                    System.out.println("Login blocked: user account is CLOSED for email: " + email);
                    return ResponseEntity.badRequest().body(response);
                }
                
                // SECURITY: Check if password has been set after approval
                if (!user.isPasswordSet()) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("passwordNotSet", true);
                    response.put("requiresPasswordSetup", true);
                    response.put("message", "Your account has been approved, but you must set a new password before you can login. Please set your password first.");
                    System.out.println("Login blocked: password not set for approved account: " + email);
                    return ResponseEntity.badRequest().body(response);
                }
                
                // Debug: Check password format
                String storedPassword = user.getPassword();
                if (storedPassword != null) {
                    System.out.println("Stored password format check - Length: " + storedPassword.length() + ", Contains colon: " + storedPassword.contains(":"));
                    System.out.println("Is encrypted format: " + passwordService.isEncrypted(storedPassword));
                } else {
                    System.out.println("WARNING: Stored password is NULL!");
                }
                
                // Check if account is locked
                if (user.isAccountLocked()) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("accountLocked", true);
                    response.put("message", "Account is locked due to multiple failed login attempts. Please use the unlock feature.");
                    System.out.println("Account locked for user: " + user.getUsername());
                    return ResponseEntity.badRequest().body(response);
                }
                
                // Check if password is in correct format, if not, re-encrypt it
                if (storedPassword != null && !passwordService.isEncrypted(storedPassword)) {
                    System.out.println("WARNING: Password is not in encrypted format. Re-encrypting...");
                    // This shouldn't happen, but if it does, we can't verify the old password
                    // So we'll treat it as invalid
                    System.out.println("Password format invalid - cannot verify. User needs to reset password.");
                }
                
                // Use encrypted password verification
                boolean passwordValid = passwordService.verifyPassword(password, storedPassword);
                System.out.println("Password verification result: " + passwordValid);
                
                if (passwordValid) {
                    // Check per-customer net banking status
                    try {
                        if (user.getAccountNumber() != null) {
                            Account userAccount = accountService.getAccountByNumber(user.getAccountNumber());
                            if (userAccount != null && userAccount.getNetBankingEnabled() != null && !userAccount.getNetBankingEnabled()) {
                                Map<String, Object> response = new HashMap<>();
                                response.put("success", false);
                                response.put("netBankingDisabled", true);
                                response.put("message", "Net Banking for your account has been disabled by the administrator. Please contact the bank for assistance.");
                                return ResponseEntity.badRequest().body(response);
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Error checking per-customer net banking status: " + e.getMessage());
                    }

                    // Password is correct - generate and send OTP
                    String otp = otpService.generateOtp();
                    otpService.storeOtp(email, otp);
                    
                    // Send OTP via email
                    boolean emailSent = emailService.sendOtpEmail(email, otp);
                    
                    if (emailSent) {
                        Map<String, Object> response = new HashMap<>();
                        response.put("success", true);
                        response.put("requiresOtp", true);
                        response.put("message", "Password verified. OTP has been sent to your email. Please enter the OTP to complete login.");
                        System.out.println("Password verified for user: " + user.getUsername() + ". OTP sent to email.");
                        return ResponseEntity.ok(response);
                    } else {
                        Map<String, Object> response = new HashMap<>();
                        response.put("success", false);
                        response.put("message", "Password verified but failed to send OTP. Please try again.");
                        System.out.println("Password verified but OTP email failed for user: " + user.getUsername());
                        return ResponseEntity.badRequest().body(response);
                    }
                } else {
                    // Increment failed login attempts
                    user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
                    user.setLastFailedLoginTime(LocalDateTime.now());
                    
                    // AI Security: Analyze failed login attempt
                    try {
                        String clientIpAi = forwardedFor != null ? forwardedFor.split(",")[0].trim() : "Unknown";
                        String deviceInfoAi = userAgent != null ? userAgent : "Unknown";
                        aiSecurityService.analyzeLoginAttempt(user.getEmail(), "USER", clientIpAi, userAgent, deviceInfoAi, null, false);
                    } catch (Exception aiEx) {
                        System.err.println("AI Security analysis error: " + aiEx.getMessage());
                    }
                    
                    // Lock account after 3 failed attempts and record fraud alert for manager
                    if (user.getFailedLoginAttempts() >= 3) {
                        user.setAccountLocked(true);
                        userService.saveUser(user);
                        String clientIp = forwardedFor != null ? forwardedFor.split(",")[0].trim() : "Unknown";
                        String deviceInfo = userAgent != null ? userAgent : "Unknown";
                        try {
                            fraudAlertService.recordLoginFraud(user.getEmail(), user.getUsername(), com.neo.springapp.model.FraudAlert.SourceType.USER, clientIp, "IP: " + clientIp, deviceInfo);
                        } catch (Exception e) {
                            System.err.println("Failed to record login fraud alert: " + e.getMessage());
                        }
                        Map<String, Object> response = new HashMap<>();
                        response.put("success", false);
                        response.put("accountLocked", true);
                        response.put("message", "Account locked due to 3 failed login attempts. Please use the unlock feature.");
                        System.out.println("Account locked for user after 3 failed attempts: " + user.getUsername());
                        return ResponseEntity.badRequest().body(response);
                    } else {
                        userService.saveUser(user);
                        
                        Map<String, Object> response = new HashMap<>();
                        response.put("success", false);
                        response.put("failedAttempts", user.getFailedLoginAttempts());
                        response.put("message", "Invalid password. " + (3 - user.getFailedLoginAttempts()) + " attempts remaining.");
                        System.out.println("Password mismatch for user: " + user.getUsername() + " (Attempt: " + user.getFailedLoginAttempts() + "/3)");
                        return ResponseEntity.badRequest().body(response);
                    }
                }
            } else {
                // Neither admin nor user exists with this email
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Account not found. Please check your email or register for a new account.");
                System.out.println("Account not found for email: " + email);
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Authentication failed: " + e.getMessage());
            System.out.println("Authentication error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // SECURITY: Set password for approved accounts (required before first login)
    @PostMapping("/set-password")
    public ResponseEntity<Map<String, Object>> setPasswordForApprovedAccount(
            @RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String newPassword = request.get("newPassword");
            String confirmPassword = request.get("confirmPassword");
            
            if (email == null || newPassword == null || confirmPassword == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Email, new password and confirm password are required");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Validate password match
            if (!newPassword.equals(confirmPassword)) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Passwords do not match");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Validate password strength (minimum 8 characters, at least 1 uppercase, 1 lowercase, 1 number)
            if (newPassword.length() < 8) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Password must be at least 8 characters long");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (!newPassword.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$")) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Password must contain at least one uppercase letter, one lowercase letter, and one number");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Check if user exists and is APPROVED
            Optional<User> userOpt = userService.findByEmail(email);
            if (!userOpt.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "User account not found");
                return ResponseEntity.badRequest().body(response);
            }
            
            User user = userOpt.get();
            
            // SECURITY: Only allow password setup for APPROVED accounts that haven't set password
            if (!"APPROVED".equalsIgnoreCase(user.getStatus())) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Your account must be approved before setting a password");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Encrypt and set the password
            String encryptedPassword = passwordService.encryptPassword(newPassword);
            user.setPassword(encryptedPassword);
            user.setPasswordSet(true);
            user.setFailedLoginAttempts(0);
            user.setAccountLocked(false);
            
            // Save updated user
            userService.saveUser(user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Password set successfully! You can now login with your email and new password.");
            System.out.println("✅ Password set successfully for approved account: " + email);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to set password: " + e.getMessage());
            System.out.println("❌ Error setting password: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // OTP Verification endpoint - Step 2: Verify OTP and complete login
    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, Object>> verifyOtp(@RequestBody Map<String, String> request, 
                                                          @RequestHeader(value = "X-Forwarded-For", required = false) String forwardedFor,
                                                          @RequestHeader(value = "User-Agent", required = false) String userAgent) {
        try {
            String email = request.get("email");
            String otp = request.get("otp");
            String loginMethod = request.get("loginMethod"); // PASSWORD, GRAPHICAL_PASSWORD
            
            // Normalize email to lowercase and trim
            if (email != null) {
                email = email.toLowerCase().trim();
            }
            
            // Trim OTP to remove any whitespace
            if (otp != null) {
                otp = otp.trim();
            }
            
            System.out.println("🔐 OTP verification attempt for email: " + email);
            System.out.println("   OTP received (length: " + (otp != null ? otp.length() : 0) + "): " + (otp != null ? "***" + otp.substring(Math.max(0, otp.length() - 2)) : "null"));
            
            if (email == null || email.isEmpty() || otp == null || otp.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Email and OTP are required");
                System.out.println("❌ OTP verification failed: Missing email or OTP");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Validate OTP format (should be 6 digits)
            if (!otp.matches("\\d{6}")) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Invalid OTP format. OTP must be 6 digits.");
                System.out.println("❌ OTP verification failed: Invalid format - " + otp);
                return ResponseEntity.badRequest().body(response);
            }
            
            // Get client IP address
            String clientIp = forwardedFor != null ? forwardedFor.split(",")[0].trim() : 
                             request.get("ipAddress") != null ? request.get("ipAddress") : "Unknown";
            
            // Get device info from User-Agent header or request body
            String deviceInfo = request.get("deviceInfo") != null ? request.get("deviceInfo") :
                               userAgent != null ? userAgent : "Unknown";
            
            // Get location (can be enhanced with IP geolocation service)
            String location = request.get("location") != null ? request.get("location") : 
                             "IP: " + clientIp;
            
            // Verify OTP (email and otp are already normalized/trimmed)
            boolean otpValid = otpService.verifyOtp(email, otp);
            
            if (!otpValid) {
                // Get stored OTP for debugging (if exists)
                String storedOtp = otpService.getStoredOtp(email);
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                if (storedOtp == null) {
                    response.put("message", "Invalid or expired OTP. Please request a new OTP.");
                } else {
                    response.put("message", "Invalid OTP. Please check and try again.");
                }
                System.out.println("❌ OTP verification failed for email: " + email);
                return ResponseEntity.badRequest().body(response);
            }
            
            // OTP is valid - proceed with login
            {
                // OTP is valid - complete login
                Optional<User> userOpt = userService.findByEmail(email);
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    
                    // Reset failed login attempts on successful login
                    user.setFailedLoginAttempts(0);
                    user.setAccountLocked(false);
                    user.setLastFailedLoginTime(null);
                    userService.saveUser(user);
                    
                    // Record login history
                    try {
                        System.out.println("📝 Recording login history for user: " + user.getEmail());
                        com.neo.springapp.model.UserLoginHistory history = loginHistoryService.recordLogin(user, location, clientIp, deviceInfo, 
                                                       loginMethod != null ? loginMethod : "PASSWORD");
                        System.out.println("✅ Login history recorded successfully. ID: " + (history != null ? history.getId() : "null"));
                        
                        // Also record in SessionHistory
                        sessionHistoryService.recordUserLogin(user, location, clientIp, deviceInfo, 
                            loginMethod != null ? loginMethod : "PASSWORD");
                    } catch (Exception e) {
                        System.err.println("❌ Error recording login history: " + e.getMessage());
                        e.printStackTrace();
                        // Don't fail login if history recording fails
                    }
                    
                    // Send login notification email for security purposes
                    LocalDateTime loginTime = LocalDateTime.now();
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    String formattedTimestamp = loginTime.format(formatter);
                    emailService.sendLoginNotificationEmail(user.getEmail(), user.getUsername(), formattedTimestamp);
                    
                    // Create a safe user response object (avoid circular references and large byte arrays)
                    Map<String, Object> userResponse = createUserResponse(user);
                    
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("user", userResponse);
                    response.put("role", "USER");
                    response.put("message", "Login successful");
                    System.out.println("OTP verified and login successful for user: " + user.getUsername());
                    return ResponseEntity.ok(response);
                } else {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("message", "Account not found. Please try logging in again.");
                    System.out.println("User not found after OTP verification: " + email);
                    return ResponseEntity.badRequest().body(response);
                }
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "OTP verification failed: " + e.getMessage());
            System.out.println("OTP verification error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // Graphical Password Authentication endpoint
    @PostMapping("/authenticate-graphical")
    public ResponseEntity<Map<String, Object>> authenticateWithGraphicalPassword(@RequestBody Map<String, Object> credentials) {
        try {
            String email = (String) credentials.get("email");
            Object graphicalPasswordObj = credentials.get("graphicalPassword");
            
            System.out.println("Graphical password authentication attempt for email: " + email);
            
            if (email == null || graphicalPasswordObj == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Email and graphical password are required");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Convert graphical password to array
            List<Integer> graphicalPassword;
            if (graphicalPasswordObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Integer> tempList = (List<Integer>) graphicalPasswordObj;
                graphicalPassword = tempList;
            } else if (graphicalPasswordObj instanceof String) {
                // Parse JSON string
                ObjectMapper mapper = new ObjectMapper();
                graphicalPassword = Arrays.asList(mapper.readValue((String) graphicalPasswordObj, Integer[].class));
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Invalid graphical password format");
                return ResponseEntity.badRequest().body(response);
            }
            
            Optional<User> userOpt = userService.findByEmail(email);
            if (!userOpt.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "User not found");
                return ResponseEntity.badRequest().body(response);
            }
            
            User user = userOpt.get();
            // Block login if user account is closed
            if ("CLOSED".equalsIgnoreCase(user.getStatus())) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("accountClosed", true);
                response.put("message", "Your account has been closed by the bank. Login is not allowed.");
                return ResponseEntity.badRequest().body(response);
            }

            // SECURITY: Check if account status is APPROVED
            if (!"APPROVED".equalsIgnoreCase(user.getStatus())) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("accountNotApproved", true);
                if ("PENDING".equalsIgnoreCase(user.getStatus())) {
                    response.put("message", "Your account is pending approval. Please wait for admin authorization.");
                } else {
                    response.put("message", "Your account is not in a state that allows login. Current status: " + user.getStatus());
                }
                return ResponseEntity.badRequest().body(response);
            }

            // SECURITY: Check if password has been set after approval
            if (!user.isPasswordSet()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("requiresPasswordSetup", true);
                response.put("message", "Your account has been approved, but you must set a new password before you can login. Please set your password first.");
                return ResponseEntity.badRequest().body(response);
            }

            // Check if account is locked
            if (user.isAccountLocked()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("accountLocked", true);
                response.put("message", "Account is locked. Please use the unlock feature.");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Check if user has a graphical password set
            if (user.getGraphicalPassword() == null || user.getGraphicalPassword().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Graphical password not set. Please use regular password login or set up graphical password.");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Verify graphical password
            ObjectMapper mapper = new ObjectMapper();
            List<Integer> storedPassword = Arrays.asList(mapper.readValue(user.getGraphicalPassword(), Integer[].class));
            
            boolean passwordMatches = storedPassword.equals(graphicalPassword);
            
            if (passwordMatches) {
                // Graphical password is correct - generate and send OTP
                String otp = otpService.generateOtp();
                otpService.storeOtp(email, otp);
                
                boolean emailSent = emailService.sendOtpEmail(email, otp);
                
                if (emailSent) {
                    // Reset failed login attempts
                    user.setFailedLoginAttempts(0);
                    userService.saveUser(user);
                    
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("requiresOtp", true);
                    response.put("message", "Graphical password verified. OTP has been sent to your email.");
                    System.out.println("Graphical password verified for user: " + user.getUsername());
                    return ResponseEntity.ok(response);
                } else {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("message", "Password verified but failed to send OTP. Please try again.");
                    return ResponseEntity.badRequest().body(response);
                }
            } else {
                // Increment failed login attempts
                user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
                user.setLastFailedLoginTime(LocalDateTime.now());
                
                if (user.getFailedLoginAttempts() >= 3) {
                    user.setAccountLocked(true);
                    userService.saveUser(user);
                    String deviceInfo = (String) credentials.get("deviceInfo");
                    if (deviceInfo == null) deviceInfo = "Unknown";
                    try {
                        fraudAlertService.recordLoginFraud(user.getEmail(), user.getUsername(), com.neo.springapp.model.FraudAlert.SourceType.USER, "Unknown", "Graphical login", deviceInfo);
                    } catch (Exception e) {
                        System.err.println("Failed to record login fraud alert: " + e.getMessage());
                    }
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("accountLocked", true);
                    response.put("message", "Account locked due to 3 failed attempts.");
                    return ResponseEntity.badRequest().body(response);
                } else {
                    userService.saveUser(user);
                    
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("failedAttempts", user.getFailedLoginAttempts());
                    response.put("message", "Invalid graphical password. " + (3 - user.getFailedLoginAttempts()) + " attempts remaining.");
                    return ResponseEntity.badRequest().body(response);
                }
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Graphical password authentication failed: " + e.getMessage());
            System.out.println("Graphical password authentication error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    // Set/Update Graphical Password endpoint
    @PostMapping("/set-graphical-password")
    public ResponseEntity<Map<String, Object>> setGraphicalPassword(@RequestBody Map<String, Object> request) {
        try {
            String email = (String) request.get("email");
            String password = (String) request.get("password"); // Regular password for verification
            Object graphicalPasswordObj = request.get("graphicalPassword");
            
            if (email == null || password == null || graphicalPasswordObj == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Email, password, and graphical password are required");
                return ResponseEntity.badRequest().body(response);
            }
            
            Optional<User> userOpt = userService.findByEmail(email);
            if (!userOpt.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "User not found");
                return ResponseEntity.badRequest().body(response);
            }
            
            User user = userOpt.get();
            
            // Verify regular password first
            if (!passwordService.verifyPassword(password, user.getPassword())) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Invalid password. Please enter your correct password.");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Convert graphical password to JSON string
            ObjectMapper mapper = new ObjectMapper();
            String graphicalPasswordJson;
            if (graphicalPasswordObj instanceof List) {
                graphicalPasswordJson = mapper.writeValueAsString(graphicalPasswordObj);
            } else if (graphicalPasswordObj instanceof String) {
                // Validate JSON format
                mapper.readValue((String) graphicalPasswordObj, Integer[].class);
                graphicalPasswordJson = (String) graphicalPasswordObj;
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Invalid graphical password format");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Save graphical password
            user.setGraphicalPassword(graphicalPasswordJson);
            userService.saveUser(user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Graphical password set successfully");
            System.out.println("Graphical password set for user: " + user.getUsername());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to set graphical password: " + e.getMessage());
            System.out.println("Set graphical password error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // Resend OTP endpoint
    @PostMapping("/resend-otp")
    public ResponseEntity<Map<String, Object>> resendOtp(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            
            System.out.println("Resend OTP request for email: " + email);
            
            if (email == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Email is required");
                return ResponseEntity.badRequest().body(response);
            }
            
            Optional<User> userOpt = userService.findByEmail(email);
            if (!userOpt.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "User not found");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Generate and send new OTP
            String otp = otpService.generateOtp();
            otpService.storeOtp(email, otp);
            
            boolean emailSent = emailService.sendOtpEmail(email, otp);
            
            if (emailSent) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "OTP has been resent to your email");
                System.out.println("OTP resent to email: " + email);
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Failed to resend OTP. Please try again.");
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to resend OTP: " + e.getMessage());
            System.out.println("Resend OTP error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // Send OTP for signup email verification (no existing user required)
    @PostMapping("/send-signup-otp")
    public ResponseEntity<Map<String, Object>> sendSignupOtp(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String phone = request.get("phone");
            Map<String, Object> response = new HashMap<>();

            if (email == null || email.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Email is required");
                return ResponseEntity.badRequest().body(response);
            }

            email = email.trim().toLowerCase();

            // Check existing user state:
            // - allow OTP if account is approved but password is not set yet (first-time activation)
            // - reject for fully registered accounts
            Optional<User> existingUserOpt = userService.findByEmail(email);
            if (existingUserOpt.isPresent()) {
                User existingUser = existingUserOpt.get();
                boolean approved = "APPROVED".equalsIgnoreCase(existingUser.getStatus());
                boolean requiresPasswordSetup = !existingUser.isPasswordSet();

                if (!(approved && requiresPasswordSetup)) {
                    response.put("success", false);
                    response.put("message", "This email is already registered. Please login instead.");
                    response.put("errorType", "EMAIL_EXISTS");
                    return ResponseEntity.badRequest().body(response);
                }
            }

            // Check if phone already registered.
            // For first-time activation users, allow their own already-linked phone number.
            if (phone != null && !phone.trim().isEmpty()) {
                String normalizedPhone = phone.trim();
                if (!accountService.isPhoneUnique(normalizedPhone)) {
                    boolean allowExistingActivationPhone = false;
                    if (existingUserOpt.isPresent()) {
                        User existingUser = existingUserOpt.get();
                        boolean approved = "APPROVED".equalsIgnoreCase(existingUser.getStatus());
                        boolean requiresPasswordSetup = !existingUser.isPasswordSet();
                        boolean matchesUserPhone = normalizedPhone.equals(existingUser.getPhone());
                        boolean matchesAccountPhone = existingUser.getAccount() != null
                                && normalizedPhone.equals(existingUser.getAccount().getPhone());
                        allowExistingActivationPhone = approved && requiresPasswordSetup
                                && (matchesUserPhone || matchesAccountPhone);
                    }

                    if (!allowExistingActivationPhone) {
                        response.put("success", false);
                        response.put("message", "This phone number is already registered.");
                        response.put("errorType", "PHONE_EXISTS");
                        return ResponseEntity.badRequest().body(response);
                    }
                }
            }

            // Generate and store OTP
            String otp = otpService.generateOtp();
            otpService.storeOtp(email, otp);

            // Send OTP email
            boolean sent = emailService.sendOtpEmail(email, otp);
            if (sent) {
                response.put("success", true);
                response.put("message", "OTP sent to " + email);
            } else {
                response.put("success", true);
                response.put("message", "OTP generated. Please check your email.");
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to send OTP: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // Verify OTP for signup email verification
    @PostMapping("/verify-signup-otp")
    public ResponseEntity<Map<String, Object>> verifySignupOtp(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String otp = request.get("otp");
            Map<String, Object> response = new HashMap<>();

            if (email == null || otp == null) {
                response.put("success", false);
                response.put("message", "Email and OTP are required");
                return ResponseEntity.badRequest().body(response);
            }

            boolean valid = otpService.verifyOtp(email.trim().toLowerCase(), otp.trim());
            if (valid) {
                response.put("success", true);
                response.put("message", "Email verified successfully");
            } else {
                response.put("success", false);
                response.put("message", "Invalid or expired OTP. Please try again.");
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "OTP verification failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // Basic CRUD operations
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createUser(@RequestBody User user) {
        try {
            System.out.println("=== USER CREATION REQUEST ===");
            System.out.println("Email: " + user.getEmail());
            System.out.println("Username: " + user.getUsername());
            
            Map<String, Object> response = new HashMap<>();

            String emailNorm = UserService.normalizeEmail(user.getEmail());
            if (emailNorm == null) {
                response.put("success", false);
                response.put("message", "Email is required");
                response.put("errorType", "EMAIL_REQUIRED");
                return ResponseEntity.badRequest().body(response);
            }
            user.setEmail(emailNorm);
            
            // Validate unique fields (case-insensitive)
            if (!userService.isEmailUnique(emailNorm)) {
                System.out.println("❌ Email already exists: " + emailNorm);
                response.put("success", false);
                response.put("message", "Email address is already registered. Please use a different email or try logging in.");
                response.put("errorType", "EMAIL_EXISTS");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (user.getPan() != null && !user.getPan().isEmpty() && !userService.isPanUnique(user.getPan())) {
                System.out.println("❌ PAN already exists: " + user.getPan());
                response.put("success", false);
                response.put("message", "PAN number is already registered. Please check your details.");
                response.put("errorType", "PAN_EXISTS");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (user.getAadhar() != null && !user.getAadhar().isEmpty() && !userService.isAadharUnique(user.getAadhar())) {
                System.out.println("❌ Aadhar already exists: " + user.getAadhar());
                response.put("success", false);
                response.put("message", "Aadhar number is already registered. Please check your details.");
                response.put("errorType", "AADHAR_EXISTS");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Validate phone number if account is provided with phone
            if (user.getAccount() != null && user.getAccount().getPhone() != null && !user.getAccount().getPhone().isEmpty()) {
                if (!accountService.isPhoneUnique(user.getAccount().getPhone())) {
                    System.out.println("❌ Phone number already exists: " + user.getAccount().getPhone());
                    response.put("success", false);
                    response.put("message", "Mobile number is already registered. Another account exists with this mobile number.");
                    response.put("errorType", "PHONE_EXISTS");
                    return ResponseEntity.badRequest().body(response);
                }
            }
            
            // Encrypt password before saving
            if (user.getPassword() != null && !passwordService.isEncrypted(user.getPassword())) {
                user.setPassword(passwordService.encryptPassword(user.getPassword()));
            }
            User savedUser = userService.saveUser(user);
            System.out.println("✅ User created successfully: " + savedUser.getEmail());
            
            // Generate tracking ID and create tracking record
            String aadharNumber = savedUser.getAadhar();
            String mobileNumber = savedUser.getPhone();
            if (aadharNumber != null && !aadharNumber.isEmpty() && mobileNumber != null && !mobileNumber.isEmpty()) {
                try {
                    com.neo.springapp.model.AccountTracking tracking = accountTrackingService.createTracking(savedUser, aadharNumber, mobileNumber);
                    System.out.println("✅ Tracking ID generated: " + tracking.getTrackingId());
                    
                    // Send tracking ID email to user
                    emailService.sendAccountTrackingEmail(
                        savedUser.getEmail(), 
                        savedUser.getUsername() != null ? savedUser.getUsername() : savedUser.getEmail(),
                        tracking.getTrackingId(),
                        aadharNumber,
                        tracking.getStatus()
                    );
                    
                    response.put("trackingId", tracking.getTrackingId());
                    response.put("message", "Account created successfully! Tracking ID has been sent to your email. Please wait for admin approval.");
                } catch (Exception e) {
                    System.out.println("⚠️ Failed to create tracking or send email: " + e.getMessage());
                    e.printStackTrace();
                    // Continue even if tracking fails
                    response.put("message", "Account created successfully! Please wait for admin approval.");
                }
            } else {
                response.put("message", "Account created successfully! Please wait for admin approval.");
            }
            
            response.put("success", true);
            response.put("user", savedUser);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.out.println("❌ User creation failed: " + e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Account creation failed. Please try again.");
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        Optional<User> user = userService.getUserById(id);
        return user.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<User> getUserByUsername(@PathVariable String username) {
        Optional<User> user = userService.getUserByUsername(username);
        return user.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    // Test endpoint to check data persistence
    @GetMapping("/test-persistence")
    public ResponseEntity<Map<String, Object>> testDataPersistence() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Get total user count
            long userCount = userService.getTotalUsersCount();
            response.put("totalUsers", userCount);
            
            // Get all users
            List<User> allUsers = userService.getAllUsers();
            response.put("allUsers", allUsers);
            
            // Get total account count
            long accountCount = accountService.getTotalAccountsCount();
            response.put("totalAccounts", accountCount);
            
            response.put("success", true);
            response.put("message", "Data persistence test successful");
            
            System.out.println("=== DATA PERSISTENCE TEST ===");
            System.out.println("Total Users: " + userCount);
            System.out.println("Total Accounts: " + accountCount);
            System.out.println("Users: " + allUsers.size());
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Data persistence test failed: " + e.getMessage());
            System.out.println("Data persistence test failed: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<User> getUserByAccountNumber(@PathVariable String accountNumber) {
        Optional<User> user = userService.getUserByAccountNumber(accountNumber);
        return user.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<User> getUserByEmail(@PathVariable String email) {
        Optional<User> user = userService.getUserByEmail(email);
        return user.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<Map<String, Object>> updateUser(@PathVariable Long id, @RequestBody User userDetails) {
        try {
            // Enforce document upload for sensitive updates if there is a pending required audit
            try {
                java.util.List<com.neo.springapp.model.AdminAuditLog> audits = adminAuditService.getAuditHistory("USER", id);
                boolean hasPendingRequired = audits.stream().anyMatch(a -> Boolean.TRUE.equals(a.getDocumentRequired()) && !Boolean.TRUE.equals(a.getDocumentUploaded()));
                if (hasPendingRequired) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("message", "A signed document upload is required before applying updates to this user.");
                    return ResponseEntity.status(400).body(response);
                }
            } catch (Exception ex) {
                System.out.println("⚠️ Audit check failed: " + ex.getMessage());
            }
        User updatedUser = userService.updateUser(id, userDetails);
            if (updatedUser != null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("user", updatedUser);
                response.put("message", "User updated successfully");
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "User not found");
                return ResponseEntity.status(404).body(response);
            }
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to update user: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // Dedicated endpoint for updating email
    @PutMapping("/update-email/{id}")
    public ResponseEntity<Map<String, Object>> updateEmail(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            String newEmail = request.get("email");
            
            if (newEmail == null || newEmail.trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Email is required");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Validate email format (basic validation)
            if (!newEmail.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Invalid email format");
                return ResponseEntity.badRequest().body(response);
            }
            
            Optional<User> userOpt = userService.getUserById(id);
            if (!userOpt.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "User not found");
                return ResponseEntity.status(404).body(response);
            }
            
            User user = userOpt.get();

            String normalizedNew = UserService.normalizeEmail(newEmail);
            if (normalizedNew == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Email is required");
                return ResponseEntity.badRequest().body(response);
            }

            String normalizedCurrent = UserService.normalizeEmail(user.getEmail());
            if (normalizedNew.equals(normalizedCurrent)) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "New email is the same as current email");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (!userService.isEmailUnique(normalizedNew, user.getId())) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Email already exists. Please use a different email.");
                return ResponseEntity.badRequest().body(response);
            }
            
            user.setEmail(normalizedNew);
            User updatedUser = userService.saveUser(user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("user", updatedUser);
            response.put("message", "Email updated successfully");
            System.out.println("✅ Email updated successfully for user ID: " + id);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to update email: " + e.getMessage());
            System.out.println("❌ Email update failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }



    // Root endpoint for getting all users (for frontend compatibility)
    // Excludes large byte arrays (profile photos and signatures) to prevent memory issues
    @GetMapping("")
    public ResponseEntity<List<Map<String, Object>>> getAllUsersSimple() {
        List<User> users = userService.getAllUsers();
        // Use createUserResponse to exclude large byte arrays and prevent OutOfMemoryError
        List<Map<String, Object>> userResponses = users.stream()
            .map(this::createUserResponse)
            .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(userResponses);
    }

    // Pagination and sorting
    @GetMapping("/all")
    public ResponseEntity<Page<User>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "joinDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        // Limit maximum page size to prevent OutOfMemoryError
        // Allow up to 100 records per page to balance performance and memory usage
        int maxSize = 100;
        if (size > maxSize) {
            size = maxSize;
        }
        // Ensure page is not negative
        if (page < 0) {
            page = 0;
        }
        Page<User> users = userService.getAllUsersWithPagination(page, size, sortBy, sortDir);
        // Note: This endpoint returns Page<User> which may include byte arrays
        // For admin dashboard, use /api/users/admin/all instead which excludes byte arrays
        return ResponseEntity.ok(users);
    }

    // Status-based operations
    @GetMapping("/status/{status}")
    public ResponseEntity<List<User>> getUsersByStatus(@PathVariable String status) {
        List<User> users = userService.getUsersByStatus(status);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/status/{status}/paginated")
    public ResponseEntity<Page<User>> getUsersByStatusWithPagination(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<User> users = userService.getUsersByStatusWithPagination(status, page, size);
        return ResponseEntity.ok(users);
    }

    @PutMapping("/approve/{id}")
    public ResponseEntity<Map<String, Object>> approveUser(@PathVariable Long id) {
        try {
            // Server-side enforcement: ensure no pending required document for this user
            try {
                java.util.List<com.neo.springapp.model.AdminAuditLog> audits = adminAuditService.getAuditHistory("USER", id);
                boolean hasPendingRequired = audits.stream().anyMatch(a -> Boolean.TRUE.equals(a.getDocumentRequired()) && !Boolean.TRUE.equals(a.getDocumentUploaded()));
                if (hasPendingRequired) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("message", "A signed document upload is required before approval.");
                    return ResponseEntity.status(400).body(response);
                }
            } catch (Exception ex) {
                // If audit service fails, log and continue to avoid blocking due to audit errors
                System.out.println("⚠️ Audit check failed: " + ex.getMessage());
            }
            System.out.println("=== USER APPROVAL REQUEST ===");
            System.out.println("User ID: " + id);
            
            User approvedUser = userService.approveUser(id, "Admin"); // Use default admin name
            
            if (approvedUser != null) {
                // Update tracking status to ADMIN_APPROVED
                try {
                    Optional<com.neo.springapp.model.AccountTracking> trackingOpt = accountTrackingService.getTrackingByUserId(id);
                    if (trackingOpt.isPresent()) {
                        com.neo.springapp.model.AccountTracking tracking = trackingOpt.get();
                        accountTrackingService.updateTrackingStatus(tracking.getId(), "ADMIN_APPROVED", "Admin");
                        
                        // Send email notification about approval
                        emailService.sendAccountTrackingEmail(
                            approvedUser.getEmail(),
                            approvedUser.getUsername() != null ? approvedUser.getUsername() : approvedUser.getEmail(),
                            tracking.getTrackingId(),
                            tracking.getAadharNumber(),
                            "ADMIN_APPROVED"
                        );
                        System.out.println("✅ Tracking status updated to ADMIN_APPROVED and email sent");
                    }
                } catch (Exception e) {
                    System.out.println("⚠️ Failed to update tracking status: " + e.getMessage());
                    // Continue even if tracking update fails
                }
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("user", approvedUser);
                response.put("message", "User approved successfully");
                response.put("accountNumber", approvedUser.getAccountNumber());
                
                System.out.println("✅ User approved successfully: " + approvedUser.getUsername());
                System.out.println("Account Number: " + approvedUser.getAccountNumber());
                
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "User not found or already approved");
                System.out.println("❌ User not found or already approved: " + id);
                return ResponseEntity.status(404).body(response);
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "User approval failed: " + e.getMessage());
            System.out.println("❌ User approval failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PutMapping("/close/{id}")
    public ResponseEntity<?> closeUserAccount(
            @PathVariable Long id,
            @RequestParam String adminName,
            @RequestParam(required = false) String adminAccountNumber) {
        try {
            // Enforce document upload before closing account
            try {
                java.util.List<com.neo.springapp.model.AdminAuditLog> audits = adminAuditService.getAuditHistory("USER", id);
                boolean hasPendingRequired = audits.stream().anyMatch(a -> Boolean.TRUE.equals(a.getDocumentRequired()) && !Boolean.TRUE.equals(a.getDocumentUploaded()));
                if (hasPendingRequired) {
                    Map<String, Object> error = new HashMap<>();
                    error.put("success", false);
                    error.put("message", "A signed document upload is required before closing this account.");
                    return ResponseEntity.status(400).body(error);
                }
            } catch (Exception ex) {
                System.out.println("⚠️ Audit check failed: " + ex.getMessage());
            }
            User closedUser = userService.closeUserAccount(id, adminName, adminAccountNumber);
            return closedUser != null ? ResponseEntity.ok(closedUser) : ResponseEntity.notFound().build();
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // Search operations
    @GetMapping("/search")
    public ResponseEntity<Page<User>> searchUsers(
            @RequestParam String searchTerm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<User> users = userService.searchUsers(searchTerm, page, size);
        return ResponseEntity.ok(users);
    }

    // Filter operations
    @GetMapping("/filter/income")
    public ResponseEntity<List<User>> getUsersByIncomeRange(
            @RequestParam Double minIncome,
            @RequestParam Double maxIncome) {
        List<User> users = userService.getUsersByIncomeRange(minIncome, maxIncome);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/filter/occupation/{occupation}")
    public ResponseEntity<List<User>> getUsersByOccupation(@PathVariable String occupation) {
        List<User> users = userService.getUsersByOccupation(occupation);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/filter/account-type/{accountType}")
    public ResponseEntity<List<User>> getUsersByAccountType(@PathVariable String accountType) {
        List<User> users = userService.getUsersByAccountType(accountType);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/filter/join-date")
    public ResponseEntity<List<User>> getUsersByJoinDateRange(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        // Parse dates and call service
        LocalDateTime start = LocalDateTime.parse(startDate);
        LocalDateTime end = LocalDateTime.parse(endDate);
        List<User> users = userService.getUsersByJoinDateRange(start, end);
        return ResponseEntity.ok(users);
    }

    // Statistics endpoints
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getUserStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userService.getTotalUsersCount());
        stats.put("pendingUsers", userService.getUsersCountByStatus("PENDING"));
        stats.put("approvedUsers", userService.getUsersCountByStatus("APPROVED"));
        stats.put("closedUsers", userService.getUsersCountByStatus("CLOSED"));
        stats.put("averageIncome", userService.getAverageIncome());
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/recent")
    public ResponseEntity<List<User>> getRecentUsers(@RequestParam(defaultValue = "5") int limit) {
        List<User> users = userService.getRecentUsers(limit);
        return ResponseEntity.ok(users);
    }

    // Validation endpoints
    @GetMapping("/validate/email/{email}")
    public ResponseEntity<Map<String, Boolean>> validateEmail(@PathVariable String email) {
        Map<String, Boolean> response = new HashMap<>();
        response.put("isUnique", userService.isEmailUnique(email));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/validate/pan/{pan}")
    public ResponseEntity<Map<String, Boolean>> validatePan(@PathVariable String pan) {
        Map<String, Boolean> response = new HashMap<>();
        response.put("isUnique", userService.isPanUnique(pan));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/validate/aadhar/{aadhar}")
    public ResponseEntity<Map<String, Boolean>> validateAadhar(@PathVariable String aadhar) {
        Map<String, Boolean> response = new HashMap<>();
        response.put("isUnique", userService.isAadharUnique(aadhar));
        return ResponseEntity.ok(response);
    }

    // Account unlock endpoint
    @PostMapping("/unlock-account")
    public ResponseEntity<Map<String, Object>> unlockAccount(@RequestBody Map<String, String> unlockData) {
        try {
            String email = unlockData.get("email");
            String aadharFirst4 = unlockData.get("aadharFirst4");
            String dob = unlockData.get("dob");
            
            System.out.println("Account unlock attempt for email: " + email);
            System.out.println("Aadhar first 4 digits: " + aadharFirst4);
            System.out.println("DOB: " + dob);
            
            if (email == null || aadharFirst4 == null || dob == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Email, first 4 digits of Aadhar, and date of birth are required");
                return ResponseEntity.badRequest().body(response);
            }
            
            Optional<User> userOpt = userService.findByEmail(email);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                // Block login if user account is closed
                if ("CLOSED".equalsIgnoreCase(user.getStatus())) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("accountClosed", true);
                    response.put("message", "Your account has been closed by the bank. Login is not allowed.");
                    System.out.println("OTP verification blocked: user account is CLOSED for email: " + email);
                    return ResponseEntity.badRequest().body(response);
                }

                // Check if account is actually locked
                if (!user.isAccountLocked()) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("message", "Account is not locked");
                    return ResponseEntity.badRequest().body(response);
                }
                
                // Get user's account details for verification
                Account account = user.getAccount();
                if (account == null) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("message", "Account details not found. Please contact support.");
                    return ResponseEntity.badRequest().body(response);
                }
                
                // Verify first 4 digits of Aadhar and DOB
                String userAadhar = account.getAadharNumber();
                String userDob = account.getDob();
                
                if (userAadhar != null && userDob != null && 
                    userAadhar.startsWith(aadharFirst4) && userDob.equals(dob)) {
                    
                    // Unlock the account
                    user.setAccountLocked(false);
                    user.setFailedLoginAttempts(0);
                    user.setLastFailedLoginTime(null);
                    userService.saveUser(user);
                    
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("message", "Account unlocked successfully. You can now login.");
                    System.out.println("Account unlocked successfully for user: " + user.getUsername());
                    return ResponseEntity.ok(response);
                } else {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("message", "Invalid verification details. Please check your Aadhar number and date of birth.");
                    System.out.println("Invalid unlock details for user: " + user.getUsername());
                    return ResponseEntity.badRequest().body(response);
                }
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "User not found");
                System.out.println("User not found for unlock request: " + email);
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Account unlock failed: " + e.getMessage());
            System.out.println("Account unlock error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String aadharFirst4 = request.get("aadharFirst4");
            String newPassword = request.get("newPassword");

            System.out.println("Password reset request for email: " + email);

            // Validate input
            if (email == null || email.trim().isEmpty() || 
                aadharFirst4 == null || aadharFirst4.trim().isEmpty() ||
                newPassword == null || newPassword.trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Email, Aadhar first 4 digits, and new password are required");
                return ResponseEntity.badRequest().body(response);
            }

            // Validate Aadhar first 4 digits format
            if (!aadharFirst4.matches("\\d{4}")) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Aadhar first 4 digits must be exactly 4 numbers");
                return ResponseEntity.badRequest().body(response);
            }

            // Validate password length
            if (newPassword.length() < 6) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Password must be at least 6 characters long");
                return ResponseEntity.badRequest().body(response);
            }

            // Find user by email
            Optional<User> userOpt = userService.findByEmail(email);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                
                // Check if user has an account with Aadhar information
                if (user.getAccount() != null && user.getAccount().getAadharNumber() != null) {
                    String userAadhar = user.getAccount().getAadharNumber();
                    
                    // Check if the first 4 digits match
                    if (userAadhar.length() >= 4 && userAadhar.substring(0, 4).equals(aadharFirst4)) {
                        // Reset password with encryption
                        user.setPassword(passwordService.encryptPassword(newPassword));
                        userService.saveUser(user);
                        
                        Map<String, Object> response = new HashMap<>();
                        response.put("success", true);
                        response.put("message", "Password reset successfully");
                        System.out.println("✅ Password reset successful for user: " + email);
                        return ResponseEntity.ok(response);
                    } else {
                        Map<String, Object> response = new HashMap<>();
                        response.put("success", false);
                        response.put("message", "Aadhar verification failed. Please check the first 4 digits");
                        System.out.println("❌ Aadhar verification failed for user: " + email);
                        return ResponseEntity.badRequest().body(response);
                    }
                } else {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("message", "User account not found or Aadhar not linked");
                    System.out.println("❌ User account or Aadhar not found for: " + email);
                    return ResponseEntity.badRequest().body(response);
                }
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "User not found with this email address");
                System.out.println("❌ User not found for password reset: " + email);
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Password reset failed: " + e.getMessage());
            System.out.println("Password reset error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // Send OTP for password reset
    @PostMapping("/send-reset-otp")
    public ResponseEntity<Map<String, Object>> sendResetOtp(@RequestBody Map<String, String> request) {
        long startTime = System.currentTimeMillis();
        String email = null;
        
        try {
            // Step 1: Extract and validate email input
            email = request.get("email");
            System.out.println("[SEND-RESET-OTP] Request received at " + LocalDateTime.now() + " for email: " + email);
            
            if (email == null || email.trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Email is required");
                System.out.println("[SEND-RESET-OTP] Validation failed: Email is null or empty");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Create final variable for lambda (email is trimmed and validated)
            final String finalEmail = email.trim();
            
            // Step 2: Validate email format
            if (!finalEmail.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Invalid email format");
                System.out.println("[SEND-RESET-OTP] Validation failed: Invalid email format for: " + finalEmail);
                return ResponseEntity.badRequest().body(response);
            }
            
            System.out.println("[SEND-RESET-OTP] Email format validated. Checking user existence...");
            
            // Step 3: Check if user exists (with timeout protection)
            Optional<User> userOpt;
            try {
                long dbStartTime = System.currentTimeMillis();
                userOpt = userService.findByEmail(finalEmail);
                long dbTime = System.currentTimeMillis() - dbStartTime;
                System.out.println("[SEND-RESET-OTP] Database query completed in " + dbTime + "ms");
                
                if (dbTime > 5000) {
                    System.err.println("[SEND-RESET-OTP] WARNING: Database query took " + dbTime + "ms (slow!)");
                }
            } catch (Exception dbException) {
                System.err.println("[SEND-RESET-OTP] Database error: " + dbException.getMessage());
                dbException.printStackTrace();
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Database error. Please try again.");
                return ResponseEntity.internalServerError().body(response);
            }
            
            if (!userOpt.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "User not found with this email address");
                System.out.println("[SEND-RESET-OTP] User not found for email: " + finalEmail);
                return ResponseEntity.badRequest().body(response);
            }
            
            System.out.println("[SEND-RESET-OTP] User found. Generating OTP...");
            
            // Step 4: Generate/store/send OTP through centralized service (synchronous, no fake success).
            System.out.println("OTP API HIT: /api/users/send-reset-otp");
            System.out.println("Email: " + finalEmail);
            otpService.sendOtp(finalEmail, "RESET_PASSWORD");
            System.out.println("[SEND-RESET-OTP] OTP generated, stored, and email sent for: " + finalEmail);

            // Step 5: Return success only after email dispatch succeeds.
            long totalTime = System.currentTimeMillis() - startTime;
            System.out.println("[SEND-RESET-OTP] Request completed in " + totalTime + "ms. Returning success response.");
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "OTP has been sent to your email. Please check and enter the OTP.");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            long totalTime = System.currentTimeMillis() - startTime;
            System.err.println("[SEND-RESET-OTP] CRITICAL ERROR after " + totalTime + "ms: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "An error occurred. Please try again.");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // Reset password with OTP verification
    @PostMapping("/reset-password-with-otp")
    public ResponseEntity<Map<String, Object>> resetPasswordWithOtp(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String otp = request.get("otp");
            String newPassword = request.get("newPassword");

            System.out.println("Password reset with OTP request for email: " + email);

            // Validate input
            if (email == null || email.trim().isEmpty() || 
                otp == null || otp.trim().isEmpty() ||
                newPassword == null || newPassword.trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Email, OTP, and new password are required");
                return ResponseEntity.badRequest().body(response);
            }

            // Validate OTP format
            if (!otp.matches("\\d{6}")) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "OTP must be exactly 6 digits");
                return ResponseEntity.badRequest().body(response);
            }

            // Validate password length
            if (newPassword.length() < 6) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Password must be at least 6 characters long");
                return ResponseEntity.badRequest().body(response);
            }

            // Verify OTP
            if (!otpService.verifyOtp(email, otp)) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Invalid or expired OTP. Please try again.");
                System.out.println("❌ Invalid OTP for password reset: " + email);
                return ResponseEntity.badRequest().body(response);
            }

            // Find user by email
            Optional<User> userOpt = userService.findByEmail(email);
            if (!userOpt.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "User not found with this email address");
                System.out.println("❌ User not found for password reset: " + email);
                return ResponseEntity.badRequest().body(response);
            }

            User user = userOpt.get();
            
            // Reset password with encryption
            user.setPassword(passwordService.encryptPassword(newPassword));
            userService.saveUser(user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Password reset successfully");
            System.out.println("✅ Password reset successful with OTP for user: " + email);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Password reset failed: " + e.getMessage());
            System.out.println("Password reset error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // QR Code Login endpoints
    
    /**
     * Generate QR code for login
     * @return QR code image and token
     */
    @PostMapping("/generate-qr-login")
    public ResponseEntity<Map<String, Object>> generateQrLogin() {
        try {
            String token = qrCodeService.generateQrSession();
            
            // Generate login URL from environment variable
            String frontendUrl = (allowedOrigins != null && !allowedOrigins.trim().isEmpty()) 
                ? allowedOrigins.split(",")[0].trim() 
                : "";
            String loginUrl = frontendUrl + "/website/user?qrToken=" + token;
            
            String qrCodeImage = qrCodeService.generateQrCodeImage(token, loginUrl);
            
            if (qrCodeImage == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Failed to generate QR code");
                return ResponseEntity.badRequest().body(response);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("qrToken", token);
            response.put("qrCodeImage", qrCodeImage);
            response.put("expiresIn", 300); // 5 minutes in seconds
            System.out.println("✅ QR code generated successfully. Token: " + token);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to generate QR code: " + e.getMessage());
            System.out.println("❌ QR code generation error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Check QR login status (for polling from desktop)
     * @param token QR session token
     * @return Status of QR login
     */
    @GetMapping("/check-qr-login-status/{token}")
    public ResponseEntity<Map<String, Object>> checkQrLoginStatus(@PathVariable String token) {
        try {
            QrCodeService.QrSessionData session = qrCodeService.getQrSession(token);
            
            if (session == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("status", "EXPIRED");
                response.put("message", "QR code expired or invalid");
                return ResponseEntity.ok(response);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("status", session.getStatus());
            response.put("token", token);
            
            if ("LOGGED_IN".equals(session.getStatus()) && session.getUserData() != null) {
                response.put("user", session.getUserData());
                response.put("message", "Login successful");
            } else if ("SCANNED".equals(session.getStatus())) {
                response.put("message", "QR code scanned. Please complete login on mobile device.");
            } else {
                response.put("message", "Waiting for QR code scan...");
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to check QR status: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Complete login via QR code (called from mobile after scanning)
     * @param request Contains qrToken, email, password, and optionally otp
     * @return Login result
     */
    @PostMapping("/complete-qr-login")
    public ResponseEntity<Map<String, Object>> completeQrLogin(@RequestBody Map<String, String> request) {
        try {
            String qrToken = request.get("qrToken");
            String email = request.get("email");
            String password = request.get("password");
            String otp = request.get("otp");
            
            System.out.println("QR login attempt - Token: " + qrToken + ", Email: " + email);
            
            if (qrToken == null || email == null || password == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "QR token, email, and password are required");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Verify QR session exists and is valid
            QrCodeService.QrSessionData session = qrCodeService.getQrSession(qrToken);
            if (session == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "QR code expired or invalid. Please generate a new QR code.");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Mark as scanned
            qrCodeService.updateQrSession(qrToken, "SCANNED", null);
            
            // Authenticate user
            Optional<User> userOpt = userService.findByEmail(email);
            if (!userOpt.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "User not found");
                return ResponseEntity.badRequest().body(response);
            }
            
            User user = userOpt.get();
            
            // Check if account is locked
            if (user.isAccountLocked()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("accountLocked", true);
                response.put("message", "Account is locked. Please use the unlock feature.");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Verify password
            if (!passwordService.verifyPassword(password, user.getPassword())) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Invalid password");
                return ResponseEntity.badRequest().body(response);
            }
            
            // If OTP is provided, verify it
            if (otp != null && !otp.isEmpty()) {
                if (!otpService.verifyOtp(email, otp)) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("message", "Invalid or expired OTP");
                    return ResponseEntity.badRequest().body(response);
                }
            } else {
                // Generate and send OTP for QR login
                String generatedOtp = otpService.generateOtp();
                otpService.storeOtp(email, generatedOtp);
                emailService.sendOtpEmail(email, generatedOtp);
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("requiresOtp", true);
                response.put("message", "OTP has been sent to your email. Please enter the OTP to complete login.");
                return ResponseEntity.ok(response);
            }
            
            // Check if account is approved
            if (!"APPROVED".equals(user.getStatus())) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Account not approved yet. Please wait for admin approval.");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Login successful - update QR session
            user.setFailedLoginAttempts(0);
            user.setAccountLocked(false);
            user.setLastFailedLoginTime(null);
            userService.saveUser(user);
            
            // Record login history
            try {
                String clientIp = request.get("ipAddress") != null ? request.get("ipAddress") : "Unknown";
                String deviceInfo = request.get("deviceInfo") != null ? request.get("deviceInfo") : "QR Code Login";
                String location = request.get("location") != null ? request.get("location") : "IP: " + clientIp;
                loginHistoryService.recordLogin(user, location, clientIp, deviceInfo, "QR_CODE");
            } catch (Exception e) {
                System.err.println("Error recording QR login history: " + e.getMessage());
            }
            
            // Send login notification email
            LocalDateTime loginTime = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedTimestamp = loginTime.format(formatter);
            emailService.sendLoginNotificationEmail(user.getEmail(), user.getUsername(), formattedTimestamp);
            
            // Update QR session with user data
            qrCodeService.updateQrSession(qrToken, "LOGGED_IN", user);
            
            // Create a safe user response object (avoid circular references and large byte arrays)
            Map<String, Object> userResponse = createUserResponse(user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("user", userResponse);
            response.put("message", "Login successful via QR code");
            System.out.println("✅ QR login successful for user: " + user.getUsername());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "QR login failed: " + e.getMessage());
            System.out.println("❌ QR login error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // Upload profile photo
    @PostMapping("/{userId}/upload-profile-photo")
    public ResponseEntity<Map<String, Object>> uploadProfilePhoto(
            @PathVariable Long userId,
            @RequestParam("profilePhoto") MultipartFile profilePhoto) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Validate file size (max 5MB)
            long maxSize = 5 * 1024 * 1024; // 5MB in bytes
            if (profilePhoto.getSize() > maxSize) {
                response.put("success", false);
                response.put("message", "Profile photo size exceeds 5MB limit");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Validate file type
            String contentType = profilePhoto.getContentType();
            if (contentType == null || (!contentType.equals("image/jpeg") && 
                !contentType.equals("image/jpg") && 
                !contentType.equals("image/png") && 
                !contentType.equals("application/pdf"))) {
                response.put("success", false);
                response.put("message", "Profile photo must be JPEG, PNG, or PDF format");
                return ResponseEntity.badRequest().body(response);
            }
            
            Optional<User> userOpt = userService.getUserById(userId);
            if (!userOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "User not found");
                return ResponseEntity.notFound().build();
            }
            
            User user = userOpt.get();
            user.setProfilePhoto(profilePhoto.getBytes());
            user.setProfilePhotoType(profilePhoto.getContentType());
            user.setProfilePhotoName(profilePhoto.getOriginalFilename());
            
            User savedUser = userService.saveUser(user);
            
            response.put("success", true);
            response.put("message", "Profile photo uploaded successfully");
            response.put("user", savedUser);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to upload profile photo: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // Get profile photo
    @GetMapping("/{userId}/profile-photo")
    public ResponseEntity<byte[]> getProfilePhoto(@PathVariable Long userId) {
        Optional<User> userOpt = userService.getUserById(userId);
        if (!userOpt.isPresent() || userOpt.get().getProfilePhoto() == null) {
            return ResponseEntity.notFound().build();
        }
        
        User user = userOpt.get();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
            user.getProfilePhotoType() != null ? user.getProfilePhotoType() : "image/jpeg"));
        headers.setContentDispositionFormData("inline", 
            user.getProfilePhotoName() != null ? user.getProfilePhotoName() : "profile_photo.jpg");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(user.getProfilePhoto());
    }

    // Upload signature
    @PostMapping("/{userId}/upload-signature")
    public ResponseEntity<Map<String, Object>> uploadSignature(
            @PathVariable Long userId,
            @RequestParam("signature") MultipartFile signature) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Validate file size (max 5MB)
            long maxSize = 5 * 1024 * 1024; // 5MB in bytes
            if (signature.getSize() > maxSize) {
                response.put("success", false);
                response.put("message", "Signature size exceeds 5MB limit");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Validate file type
            String contentType = signature.getContentType();
            if (contentType == null || (!contentType.equals("image/jpeg") && 
                !contentType.equals("image/jpg") && 
                !contentType.equals("image/png") && 
                !contentType.equals("application/pdf"))) {
                response.put("success", false);
                response.put("message", "Signature must be JPEG, PNG, or PDF format");
                return ResponseEntity.badRequest().body(response);
            }
            
            Optional<User> userOpt = userService.getUserById(userId);
            if (!userOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "User not found");
                return ResponseEntity.notFound().build();
            }
            
            User user = userOpt.get();
            user.setSignature(signature.getBytes());
            user.setSignatureType(signature.getContentType());
            user.setSignatureName(signature.getOriginalFilename());
            user.setSignatureStatus("PENDING");
            user.setSignatureSubmittedDate(LocalDateTime.now());
            
            User savedUser = userService.saveUser(user);
            
            response.put("success", true);
            response.put("message", "Signature uploaded successfully. Waiting for admin approval.");
            response.put("user", savedUser);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to upload signature: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // Get signature
    @GetMapping("/{userId}/signature")
    public ResponseEntity<byte[]> getSignature(@PathVariable Long userId) {
        Optional<User> userOpt = userService.getUserById(userId);
        if (!userOpt.isPresent() || userOpt.get().getSignature() == null) {
            return ResponseEntity.notFound().build();
        }
        
        User user = userOpt.get();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
            user.getSignatureType() != null ? user.getSignatureType() : "image/jpeg"));
        headers.setContentDispositionFormData("inline", 
            user.getSignatureName() != null ? user.getSignatureName() : "signature.jpg");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(user.getSignature());
    }

    // Admin: Get all users with pending signatures
    @GetMapping("/pending-signatures")
    public ResponseEntity<List<User>> getUsersWithPendingSignatures() {
        List<User> users = userService.getUsersWithPendingSignatures();
        return ResponseEntity.ok(users);
    }

    // Admin: Approve signature
    @PutMapping("/{userId}/approve-signature")
    public ResponseEntity<Map<String, Object>> approveSignature(
            @PathVariable Long userId,
            @RequestParam String adminName) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Optional<User> userOpt = userService.getUserById(userId);
            if (!userOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "User not found");
                return ResponseEntity.notFound().build();
            }
            
            User user = userOpt.get();
            if (user.getSignature() == null) {
                response.put("success", false);
                response.put("message", "User has no signature uploaded");
                return ResponseEntity.badRequest().body(response);
            }
            
            user.setSignatureStatus("APPROVED");
            user.setSignatureReviewedDate(LocalDateTime.now());
            user.setSignatureReviewedBy(adminName);
            user.setSignatureRejectionReason(null);
            
            User savedUser = userService.saveUser(user);
            
            response.put("success", true);
            response.put("message", "Signature approved successfully");
            response.put("user", savedUser);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to approve signature: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // Admin: Reject signature
    @PutMapping("/{userId}/reject-signature")
    public ResponseEntity<Map<String, Object>> rejectSignature(
            @PathVariable Long userId,
            @RequestParam String adminName,
            @RequestParam(required = false) String rejectionReason) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Optional<User> userOpt = userService.getUserById(userId);
            if (!userOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "User not found");
                return ResponseEntity.notFound().build();
            }
            
            User user = userOpt.get();
            if (user.getSignature() == null) {
                response.put("success", false);
                response.put("message", "User has no signature uploaded");
                return ResponseEntity.badRequest().body(response);
            }
            
            user.setSignatureStatus("REJECTED");
            user.setSignatureReviewedDate(LocalDateTime.now());
            user.setSignatureReviewedBy(adminName);
            user.setSignatureRejectionReason(rejectionReason != null ? rejectionReason : "Signature does not meet requirements");
            
            User savedUser = userService.saveUser(user);
            
            response.put("success", true);
            response.put("message", "Signature rejected");
            response.put("user", savedUser);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to reject signature: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // Generate and download passbook
    @GetMapping("/{userId}/passbook")
    public ResponseEntity<byte[]> generatePassbook(@PathVariable Long userId) {
        try {
            Optional<User> userOpt = userService.getUserById(userId);
            if (!userOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            User user = userOpt.get();
            Account account = user.getAccount();
            if (account == null) {
                return ResponseEntity.badRequest().build();
            }
            
            // Get current balance
            Double currentBalance = accountService.getBalanceByAccountNumber(user.getAccountNumber());
            if (currentBalance == null) {
                currentBalance = account.getBalance() != null ? account.getBalance() : 0.0;
            }
            
            // Generate passbook PDF
            byte[] pdfBytes = pdfService.generatePassbook(userId, user, account, currentBalance);
            
            // Send passbook PDF to user's email
            if (user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
                try {
                    String userName = account != null && account.getName() != null ? account.getName() : user.getUsername();
                    boolean emailSent = emailService.sendPassbookEmail(
                        user.getEmail(),
                        user.getAccountNumber(),
                        userName,
                        pdfBytes
                    );
                    System.out.println("Passbook email sent: " + emailSent);
                } catch (Exception emailException) {
                    System.err.println("Error sending passbook email: " + emailException.getMessage());
                    // Continue even if email fails - still return PDF for download
                }
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", 
                "NeoBank_Passbook_" + user.getAccountNumber() + "_" + 
                java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + ".pdf");
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);
                    
        } catch (Exception e) {
            System.err.println("Error generating passbook: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    // Admin: Full user management - Update any user field
    @PutMapping("/admin/update-full/{id}")
    public ResponseEntity<Map<String, Object>> updateUserFull(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updateData) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Optional<User> userOpt = userService.getUserById(id);
            if (!userOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "User not found");
                return ResponseEntity.notFound().build();
            }
            
            User user = userOpt.get();
            Account account = user.getAccount();
            
            // Update user fields
            if (updateData.containsKey("username")) {
                user.setUsername((String) updateData.get("username"));
            }
            if (updateData.containsKey("email")) {
                String newEmail = (String) updateData.get("email");
                String normalizedNew = UserService.normalizeEmail(newEmail);
                if (normalizedNew == null) {
                    response.put("success", false);
                    response.put("message", "Email is invalid or empty");
                    return ResponseEntity.badRequest().body(response);
                }
                String normalizedCurrent = UserService.normalizeEmail(user.getEmail());
                if (!normalizedNew.equals(normalizedCurrent) && !userService.isEmailUnique(normalizedNew, user.getId())) {
                    response.put("success", false);
                    response.put("message", "Email already exists");
                    return ResponseEntity.badRequest().body(response);
                }
                user.setEmail(normalizedNew);
            }
            if (updateData.containsKey("status")) {
                user.setStatus((String) updateData.get("status"));
            }
            if (updateData.containsKey("accountNumber")) {
                user.setAccountNumber((String) updateData.get("accountNumber"));
            }
            if (updateData.containsKey("accountLocked")) {
                user.setAccountLocked((Boolean) updateData.get("accountLocked"));
            }
            if (updateData.containsKey("failedLoginAttempts")) {
                user.setFailedLoginAttempts(((Number) updateData.get("failedLoginAttempts")).intValue());
            }
            
            // Update or create account
            if (account == null) {
                account = new Account();
                account.setCreatedAt(LocalDateTime.now());
            }
            
            // Update account fields
            if (updateData.containsKey("name")) {
                account.setName((String) updateData.get("name"));
            }
            if (updateData.containsKey("phone")) {
                account.setPhone((String) updateData.get("phone"));
            }
            if (updateData.containsKey("address")) {
                account.setAddress((String) updateData.get("address"));
            }
            if (updateData.containsKey("dob")) {
                account.setDob((String) updateData.get("dob"));
            }
            if (updateData.containsKey("age")) {
                account.setAge(((Number) updateData.get("age")).intValue());
            }
            if (updateData.containsKey("occupation")) {
                account.setOccupation((String) updateData.get("occupation"));
            }
            if (updateData.containsKey("income")) {
                account.setIncome(((Number) updateData.get("income")).doubleValue());
            }
            if (updateData.containsKey("accountType")) {
                account.setAccountType((String) updateData.get("accountType"));
            }
            if (updateData.containsKey("balance")) {
                account.setBalance(((Number) updateData.get("balance")).doubleValue());
            }
            if (updateData.containsKey("pan")) {
                account.setPan((String) updateData.get("pan"));
            }
            if (updateData.containsKey("aadharNumber")) {
                account.setAadharNumber((String) updateData.get("aadharNumber"));
            }
            if (updateData.containsKey("customerId")) {
                String newCustomerId = (String) updateData.get("customerId");
                String currentCustomerId = account.getCustomerId();

                // If unchanged (including legacy non‑9‑digit values), skip validation and leave as is
                if (newCustomerId != null && newCustomerId.equals(currentCustomerId)) {
                    // Do nothing – keep existing value even if it doesn't match the new format
                } else if (newCustomerId != null && !newCustomerId.trim().isEmpty()) {
                    // Validate new value must be exactly 9 digits and unique
                    if (newCustomerId.matches("\\d{9}")) {
                        Account existingByCustomerId = accountService.getAccountByCustomerId(newCustomerId);
                        if (existingByCustomerId == null || existingByCustomerId.getId().equals(account.getId())) {
                            account.setCustomerId(newCustomerId.trim());
                        } else {
                            response.put("success", false);
                            response.put("message", "Customer ID already exists. Each customer must have a unique ID.");
                            return ResponseEntity.badRequest().body(response);
                        }
                    } else {
                        response.put("success", false);
                        response.put("message", "Customer ID must be exactly 9 digits.");
                        return ResponseEntity.badRequest().body(response);
                    }
                } else {
                    // Explicitly cleared in UI
                    account.setCustomerId(null);
                }
            }
            if (updateData.containsKey("accountStatus")) {
                account.setStatus((String) updateData.get("accountStatus"));
            }
            
            account.setLastUpdated(LocalDateTime.now());
            account.setAccountNumber(user.getAccountNumber());
            
            // Save account
            Account savedAccount = accountService.saveAccount(account);
            user.setAccount(savedAccount);
            
            // Save user
            User savedUser = userService.saveUser(user);
            
            response.put("success", true);
            response.put("message", "User updated successfully");
            response.put("user", savedUser);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to update user: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // Admin: Delete user
    @DeleteMapping("/admin/delete/{id}")
    public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Optional<User> userOpt = userService.getUserById(id);
            if (!userOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "User not found");
                return ResponseEntity.notFound().build();
            }
            
            userService.deleteUser(id);
            
            response.put("success", true);
            response.put("message", "User deleted successfully");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to delete user: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // Admin: Get all users with full details (excluding large byte arrays to prevent memory issues)
    // Supports pagination for better performance: ?page=0&size=50
    @GetMapping("/admin/all")
    public ResponseEntity<?> getAllUsersForAdmin(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        if (page != null && size != null && page >= 0 && size > 0) {
            // Paginated response - reduces lag with many users
            int maxSize = Math.min(size, 200);
            org.springframework.data.domain.Page<User> userPage = userService.getAllUsersWithPagination(page, maxSize, "joinDate", "desc");
            List<Map<String, Object>> userResponses = userPage.getContent().stream()
                .map(this::createUserResponse)
                .collect(Collectors.toList());
            Map<String, Object> response = new HashMap<>();
            response.put("content", userResponses);
            response.put("totalElements", userPage.getTotalElements());
            response.put("totalPages", userPage.getTotalPages());
            response.put("number", userPage.getNumber());
            response.put("size", userPage.getSize());
            return ResponseEntity.ok(response);
        }
        // Non-paginated (legacy) - for backward compatibility
        List<User> users = userService.getAllUsers();
        List<Map<String, Object>> userResponses = users.stream()
            .map(this::createUserResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(userResponses);
    }

    // ========== ADMIN DUPLICATE DETAILS MANAGEMENT ==========

    /**
     * Admin endpoint to view all duplicate details (email, phone, PAN, Aadhar)
     * Shows which details are used by multiple accounts
     */
    @GetMapping("/admin/duplicate-details")
    public ResponseEntity<Map<String, Object>> getDuplicateDetails() {
        try {
            List<User> allUsers = userService.getAllUsers();
            Map<String, Object> duplicates = new HashMap<>();
            
            // Track duplicate emails
            Map<String, java.util.List<Map<String, Object>>> emailDuplicates = new HashMap<>();
            Map<String, java.util.List<Map<String, Object>>> phoneDuplicates = new HashMap<>();
            Map<String, java.util.List<Map<String, Object>>> panDuplicates = new HashMap<>();
            Map<String, java.util.List<Map<String, Object>>> aadharDuplicates = new HashMap<>();
            
            for (User user : allUsers) {
                // Check email
                if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                    String email = user.getEmail().toLowerCase();
                    emailDuplicates.computeIfAbsent(email, k -> new java.util.ArrayList<>())
                        .add(createDetailMap(user, "EMAIL"));
                }
                
                // Check phone
                if (user.getPhone() != null && !user.getPhone().isEmpty()) {
                    String phone = user.getPhone();
                    phoneDuplicates.computeIfAbsent(phone, k -> new java.util.ArrayList<>())
                        .add(createDetailMap(user, "PHONE"));
                }
                
                // Check PAN
                if (user.getPan() != null && !user.getPan().isEmpty()) {
                    String pan = user.getPan();
                    panDuplicates.computeIfAbsent(pan, k -> new java.util.ArrayList<>())
                        .add(createDetailMap(user, "PAN"));
                }
                
                // Check Aadhar
                if (user.getAadhar() != null && !user.getAadhar().isEmpty()) {
                    String aadhar = user.getAadhar();
                    aadharDuplicates.computeIfAbsent(aadhar, k -> new java.util.ArrayList<>())
                        .add(createDetailMap(user, "AADHAR"));
                }
            }
            
            // Filter to only include duplicates (count > 1)
            Map<String, java.util.List<Map<String, Object>>> emailDups = new HashMap<>();
            emailDuplicates.forEach((key, value) -> {
                if (value.size() > 1) {
                    emailDups.put(key, value);
                }
            });
            
            Map<String, java.util.List<Map<String, Object>>> phoneDups = new HashMap<>();
            phoneDuplicates.forEach((key, value) -> {
                if (value.size() > 1) {
                    phoneDups.put(key, value);
                }
            });
            
            Map<String, java.util.List<Map<String, Object>>> panDups = new HashMap<>();
            panDuplicates.forEach((key, value) -> {
                if (value.size() > 1) {
                    panDups.put(key, value);
                }
            });
            
            Map<String, java.util.List<Map<String, Object>>> aadharDups = new HashMap<>();
            aadharDuplicates.forEach((key, value) -> {
                if (value.size() > 1) {
                    aadharDups.put(key, value);
                }
            });
            
            duplicates.put("success", true);
            duplicates.put("emailDuplicates", emailDups);
            duplicates.put("phoneDuplicates", phoneDups);
            duplicates.put("panDuplicates", panDups);
            duplicates.put("aadharDuplicates", aadharDups);
            duplicates.put("totalDuplicateEmails", emailDups.size());
            duplicates.put("totalDuplicatePhones", phoneDups.size());
            duplicates.put("totalDuplicatePans", panDups.size());
            duplicates.put("totalDuplicateAadhars", aadharDups.size());
            
            System.out.println("✅ Duplicate details retrieved - Emails: " + emailDups.size() + 
                             ", Phones: " + phoneDups.size() + ", PANs: " + panDups.size() + 
                             ", Aadhars: " + aadharDups.size());
            
            return ResponseEntity.ok(duplicates);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to retrieve duplicate details: " + e.getMessage());
            System.out.println("❌ Error retrieving duplicates: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Helper method to create detail map for duplicate tracking
     */
    private Map<String, Object> createDetailMap(User user, String detailType) {
        Map<String, Object> detail = new HashMap<>();
        detail.put("userId", user.getId());
        detail.put("username", user.getUsername());
        detail.put("email", user.getEmail());
        detail.put("joinDate", user.getJoinDate());
        detail.put("status", user.getStatus());
        
        if (user.getAccount() != null) {
            detail.put("phone", user.getAccount().getPhone());
            detail.put("pan", user.getAccount().getPan());
            detail.put("aadhar", user.getAccount().getAadharNumber());
            detail.put("name", user.getAccount().getName());
            detail.put("accountNumber", user.getAccount().getAccountNumber());
        }
        
        detail.put("detailType", detailType);
        return detail;
    }

    /**
     * Admin endpoint to update/resolve user details and fix duplicates
     */
    @PostMapping("/admin/update-user-details")
    public ResponseEntity<Map<String, Object>> updateUserDetailsForAdmin(
            @RequestParam Long userId,
            @RequestBody Map<String, String> updates) {
        try {
            Optional<User> userOpt = userService.getUserById(userId);
            if (!userOpt.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "User not found");
                return ResponseEntity.badRequest().body(response);
            }
            
            User user = userOpt.get();
            Map<String, Object> response = new HashMap<>();
            java.util.List<String> changedFields = new java.util.ArrayList<>();
            
            // Update email if provided
            if (updates.containsKey("email") && !updates.get("email").isEmpty()) {
                String newEmail = UserService.normalizeEmail(updates.get("email"));
                if (newEmail == null) {
                    response.put("success", false);
                    response.put("message", "Invalid email");
                    return ResponseEntity.badRequest().body(response);
                }
                String currentNorm = UserService.normalizeEmail(user.getEmail());
                if (!newEmail.equals(currentNorm)) {
                    if (!userService.isEmailUnique(newEmail, user.getId())) {
                        response.put("success", false);
                        response.put("message", "Email already in use by another account");
                        return ResponseEntity.badRequest().body(response);
                    }
                    user.setEmail(newEmail);
                    changedFields.add("email");
                }
            }
            
            // Update phone if provided
            if (updates.containsKey("phone") && !updates.get("phone").isEmpty() && user.getAccount() != null) {
                String newPhone = updates.get("phone");
                if (!newPhone.equals(user.getAccount().getPhone())) {
                    // Check if new phone is unique
                    if (!accountService.isPhoneUnique(newPhone)) {
                        response.put("success", false);
                        response.put("message", "Phone number already in use by another account");
                        return ResponseEntity.badRequest().body(response);
                    }
                    user.getAccount().setPhone(newPhone);
                    changedFields.add("phone");
                }
            }
            
            // Update PAN if provided
            if (updates.containsKey("pan") && !updates.get("pan").isEmpty() && user.getAccount() != null) {
                String newPan = updates.get("pan");
                if (!newPan.equals(user.getAccount().getPan())) {
                    // Check if new PAN is unique
                    if (!userService.isPanUnique(newPan)) {
                        response.put("success", false);
                        response.put("message", "PAN already in use by another account");
                        return ResponseEntity.badRequest().body(response);
                    }
                    user.getAccount().setPan(newPan);
                    changedFields.add("pan");
                }
            }
            
            // Update Aadhar if provided
            if (updates.containsKey("aadhar") && !updates.get("aadhar").isEmpty() && user.getAccount() != null) {
                String newAadhar = updates.get("aadhar");
                if (!newAadhar.equals(user.getAccount().getAadharNumber())) {
                    // Check if new Aadhar is unique
                    if (!userService.isAadharUnique(newAadhar)) {
                        response.put("success", false);
                        response.put("message", "Aadhar already in use by another account");
                        return ResponseEntity.badRequest().body(response);
                    }
                    user.getAccount().setAadharNumber(newAadhar);
                    changedFields.add("aadhar");
                }
            }
            
            // Save if changes made
            if (!changedFields.isEmpty()) {
                userService.saveUser(user);
                response.put("success", true);
                response.put("message", "User details updated successfully. Changed fields: " + String.join(", ", changedFields));
                response.put("changedFields", changedFields);
                System.out.println("✅ Admin updated user " + userId + " fields: " + String.join(", ", changedFields));
            } else {
                response.put("success", true);
                response.put("message", "No changes made");
                response.put("changedFields", new java.util.ArrayList<>());
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to update user details: " + e.getMessage());
            System.out.println("❌ Error updating user details: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // ========== PROFILE UPDATE ENDPOINTS ==========

    // Request profile update (address or phone) with OTP
    @PostMapping("/profile-update/request")
    public ResponseEntity<Map<String, Object>> requestProfileUpdate(
            @RequestParam Long userId,
            @RequestParam String field,
            @RequestParam String newValue) {
        Map<String, Object> response = profileUpdateService.requestProfileUpdate(userId, field, newValue);
        return ResponseEntity.ok(response);
    }

    // Verify OTP and submit profile update request
    @PostMapping("/profile-update/verify-otp")
    public ResponseEntity<Map<String, Object>> verifyOtpAndSubmitRequest(
            @RequestParam Long requestId,
            @RequestParam String otp) {
        Map<String, Object> response = profileUpdateService.verifyOtpAndSubmitRequest(requestId, otp);
        return ResponseEntity.ok(response);
    }

    // Resend OTP for profile update request
    @PostMapping("/profile-update/resend-otp")
    public ResponseEntity<Map<String, Object>> resendOtp(@RequestParam Long requestId) {
        Map<String, Object> response = profileUpdateService.resendOtp(requestId);
        return ResponseEntity.ok(response);
    }

    // Get update requests by user ID
    @GetMapping("/profile-update/requests/{userId}")
    public ResponseEntity<List<ProfileUpdateRequest>> getUpdateRequestsByUser(@PathVariable Long userId) {
        List<ProfileUpdateRequest> requests = profileUpdateService.getUpdateRequestsByUser(userId);
        return ResponseEntity.ok(requests);
    }

    // Get update history by user ID
    @GetMapping("/profile-update/history/{userId}")
    public ResponseEntity<List<ProfileUpdateHistory>> getUpdateHistoryByUser(@PathVariable Long userId) {
        List<ProfileUpdateHistory> history = profileUpdateService.getUpdateHistoryByUser(userId);
        return ResponseEntity.ok(history);
    }

    // Get update history by account number
    @GetMapping("/profile-update/history/account/{accountNumber}")
    public ResponseEntity<List<ProfileUpdateHistory>> getUpdateHistoryByAccount(@PathVariable String accountNumber) {
        List<ProfileUpdateHistory> history = profileUpdateService.getUpdateHistoryByAccount(accountNumber);
        return ResponseEntity.ok(history);
    }
}
