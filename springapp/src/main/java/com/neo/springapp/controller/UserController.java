package com.neo.springapp.controller;

import com.neo.springapp.model.User;
import com.neo.springapp.model.Account;
import com.neo.springapp.model.Admin;
import com.neo.springapp.service.UserService;
import com.neo.springapp.service.AccountService;
import com.neo.springapp.service.PasswordService;
import com.neo.springapp.service.OtpService;
import com.neo.springapp.service.EmailService;
import com.neo.springapp.service.QrCodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

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
    private com.neo.springapp.service.AccountTrackingService accountTrackingService;
    
    @Autowired
    private com.neo.springapp.service.PdfService pdfService;
    
    @Autowired
    private com.neo.springapp.service.AdminService adminService;
    
    @Autowired
    private com.neo.springapp.service.UserLoginHistoryService loginHistoryService;
    
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
            userResponse.put("account", accountData);
        }
        
        return userResponse;
    }

    // Authentication endpoint - Step 1: Verify password and send OTP
    @PostMapping("/authenticate")
    public ResponseEntity<Map<String, Object>> authenticateUser(@RequestBody Map<String, String> credentials) {
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
                    
                    // Lock account after 3 failed attempts
                    if (user.getFailedLoginAttempts() >= 3) {
                        user.setAccountLocked(true);
                        userService.saveUser(user);
                        
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

    // Basic CRUD operations
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createUser(@RequestBody User user) {
        try {
            System.out.println("=== USER CREATION REQUEST ===");
            System.out.println("Email: " + user.getEmail());
            System.out.println("Username: " + user.getUsername());
            
            Map<String, Object> response = new HashMap<>();
            
            // Validate unique fields
            if (!userService.isEmailUnique(user.getEmail())) {
                System.out.println("❌ Email already exists: " + user.getEmail());
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
            
            // Check if email is the same
            if (newEmail.equals(user.getEmail())) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "New email is the same as current email");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Check if email is unique
            if (!userService.isEmailUnique(newEmail)) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Email already exists. Please use a different email.");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Update email
            user.setEmail(newEmail);
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
    @GetMapping("")
    public ResponseEntity<List<User>> getAllUsersSimple() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    // Pagination and sorting
    @GetMapping("/all")
    public ResponseEntity<Page<User>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "joinDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Page<User> users = userService.getAllUsersWithPagination(page, size, sortBy, sortDir);
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
    public ResponseEntity<User> closeUserAccount(@PathVariable Long id, @RequestParam String adminName) {
        User closedUser = userService.closeUserAccount(id, adminName);
        return closedUser != null ? ResponseEntity.ok(closedUser) : ResponseEntity.notFound().build();
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
            
            // Step 4: Generate and store OTP (fast, in-memory operation)
            String otp = otpService.generateOtp();
            otpService.storeOtp(finalEmail, otp);
            System.out.println("[SEND-RESET-OTP] OTP generated and stored for email: " + finalEmail);
            
            // Create final variable for lambda (OTP is generated and stored)
            final String finalOtp = otp;
            
            // Step 5: Send email ASYNCHRONOUSLY (truly non-blocking)
            // Return success immediately, send email in background without waiting
            System.out.println("[SEND-RESET-OTP] Initiating async email send (non-blocking)...");
            
            CompletableFuture.runAsync(() -> {
                try {
                    System.out.println("[SEND-RESET-OTP-ASYNC] Starting email send for: " + finalEmail);
                    long emailStartTime = System.currentTimeMillis();
                    boolean emailSent = emailService.sendPasswordResetOtpEmail(finalEmail, finalOtp);
                    long emailTime = System.currentTimeMillis() - emailStartTime;
                    System.out.println("[SEND-RESET-OTP-ASYNC] Email send completed in " + emailTime + "ms. Success: " + emailSent);
                    
                    if (!emailSent) {
                        System.err.println("[SEND-RESET-OTP-ASYNC] WARNING: Email send failed, but OTP is stored. User can still use OTP.");
                    }
                } catch (Exception e) {
                    System.err.println("[SEND-RESET-OTP-ASYNC] Email send error: " + e.getMessage());
                    e.printStackTrace();
                    System.err.println("[SEND-RESET-OTP-ASYNC] OTP is still stored and valid despite email error.");
                }
            });
            
            // Step 6: Return success response immediately (OTP is stored, email is being sent in background)
            long totalTime = System.currentTimeMillis() - startTime;
            System.out.println("[SEND-RESET-OTP] Request completed in " + totalTime + "ms. Returning success response immediately.");
            
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
                if (!newEmail.equals(user.getEmail()) && !userService.isEmailUnique(newEmail)) {
                    response.put("success", false);
                    response.put("message", "Email already exists");
                    return ResponseEntity.badRequest().body(response);
                }
                user.setEmail(newEmail);
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

    // Admin: Get all users with full details
    @GetMapping("/admin/all")
    public ResponseEntity<List<User>> getAllUsersForAdmin() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }
}
