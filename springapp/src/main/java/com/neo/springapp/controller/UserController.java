package com.neo.springapp.controller;

import com.neo.springapp.model.User;
import com.neo.springapp.model.Account;
import com.neo.springapp.service.UserService;
import com.neo.springapp.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseEntity.BodyBuilder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:4200")
public class UserController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private AccountService accountService;

    // Authentication endpoint
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
                
                // Simple password check (in real app, use proper password hashing)
                if (password.equals(user.getPassword())) {
                    // Reset failed login attempts on successful login
                    user.setFailedLoginAttempts(0);
                    user.setAccountLocked(false);
                    user.setLastFailedLoginTime(null);
                    userService.saveUser(user);
                    
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("user", user);
                    response.put("message", "Authentication successful");
                    System.out.println("Authentication successful for user: " + user.getUsername());
                    return ResponseEntity.ok(response);
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
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User userDetails) {
        User updatedUser = userService.updateUser(id, userDetails);
        return updatedUser != null ? ResponseEntity.ok(updatedUser) : ResponseEntity.notFound().build();
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
                        // Reset password
                        user.setPassword(newPassword);
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
}
