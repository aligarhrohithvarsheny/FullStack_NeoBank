package com.neo.springapp.controller;

import com.neo.springapp.model.User;
import com.neo.springapp.model.Account;
import com.neo.springapp.service.UserService;
import com.neo.springapp.service.AccountService;
import com.neo.springapp.service.PasswordService;
import com.neo.springapp.service.OtpService;
import com.neo.springapp.service.EmailService;
import com.neo.springapp.service.QrCodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:3000", "http://frontend:80"})
public class UserController {

    @Autowired
    private UserService userService;
    
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
            
            Optional<User> userOpt = userService.findByEmail(email);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                System.out.println("User found: " + user.getUsername() + ", Status: " + user.getStatus());
                System.out.println("Account locked: " + user.isAccountLocked());
                System.out.println("Failed login attempts: " + user.getFailedLoginAttempts());
                
                // Check if account is locked
                if (user.isAccountLocked()) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("accountLocked", true);
                    response.put("message", "Account is locked due to multiple failed login attempts. Please use the unlock feature.");
                    System.out.println("Account locked for user: " + user.getUsername());
                    return ResponseEntity.badRequest().body(response);
                }
                
                // Use encrypted password verification
                if (passwordService.verifyPassword(password, user.getPassword())) {
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
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "User not found");
                System.out.println("User not found for email: " + email);
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
    public ResponseEntity<Map<String, Object>> verifyOtp(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String otp = request.get("otp");
            
            System.out.println("OTP verification attempt for email: " + email);
            
            if (email == null || otp == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Email and OTP are required");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Verify OTP
            if (otpService.verifyOtp(email, otp)) {
                // OTP is valid - complete login
                Optional<User> userOpt = userService.findByEmail(email);
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    
                    // Reset failed login attempts on successful login
                    user.setFailedLoginAttempts(0);
                    user.setAccountLocked(false);
                    user.setLastFailedLoginTime(null);
                    userService.saveUser(user);
                    
                    // Send login notification email for security purposes
                    LocalDateTime loginTime = LocalDateTime.now();
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    String formattedTimestamp = loginTime.format(formatter);
                    emailService.sendLoginNotificationEmail(user.getEmail(), user.getUsername(), formattedTimestamp);
                    
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("user", user);
                    response.put("message", "Login successful");
                    System.out.println("OTP verified and login successful for user: " + user.getUsername());
                    return ResponseEntity.ok(response);
                } else {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("message", "User not found");
                    System.out.println("User not found after OTP verification: " + email);
                    return ResponseEntity.badRequest().body(response);
                }
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Invalid or expired OTP. Please try again.");
                System.out.println("Invalid OTP for email: " + email);
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "OTP verification failed: " + e.getMessage());
            System.out.println("OTP verification error: " + e.getMessage());
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
            
            // Encrypt password before saving
            if (user.getPassword() != null && !passwordService.isEncrypted(user.getPassword())) {
                user.setPassword(passwordService.encryptPassword(user.getPassword()));
            }
            User savedUser = userService.saveUser(user);
            System.out.println("✅ User created successfully: " + savedUser.getEmail());
            
            response.put("success", true);
            response.put("message", "Account created successfully! Please wait for admin approval.");
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


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
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
        try {
            String email = request.get("email");
            
            System.out.println("Password reset OTP request for email: " + email);
            
            if (email == null || email.trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Email is required");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Validate email format
            if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Invalid email format");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Check if user exists
            Optional<User> userOpt = userService.findByEmail(email);
            if (!userOpt.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "User not found with this email address");
                System.out.println("❌ User not found for password reset OTP: " + email);
                return ResponseEntity.badRequest().body(response);
            }
            
            // Generate and send OTP
            String otp = otpService.generateOtp();
            otpService.storeOtp(email, otp);
            
            // Send password reset OTP via email
            boolean emailSent = emailService.sendPasswordResetOtpEmail(email, otp);
            
            if (emailSent) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "OTP has been sent to your email. Please check and enter the OTP.");
                System.out.println("✅ Password reset OTP sent to email: " + email);
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Failed to send OTP. Please try again.");
                System.out.println("❌ Failed to send password reset OTP to email: " + email);
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to send OTP: " + e.getMessage());
            System.out.println("Password reset OTP error: " + e.getMessage());
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
            
            // Generate login URL (adjust based on your frontend URL)
            String loginUrl = "http://localhost:4200/website/user?qrToken=" + token;
            // For production, use: "https://yourdomain.com/website/user?qrToken=" + token;
            
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
            
            // Send login notification email
            LocalDateTime loginTime = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedTimestamp = loginTime.format(formatter);
            emailService.sendLoginNotificationEmail(user.getEmail(), user.getUsername(), formattedTimestamp);
            
            // Update QR session with user data
            qrCodeService.updateQrSession(qrToken, "LOGGED_IN", user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("user", user);
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
}
