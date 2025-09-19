package com.neo.springapp.controller;

import com.neo.springapp.model.User;
import com.neo.springapp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserService userService;

    // Basic CRUD operations
    @PostMapping("/create")
    public ResponseEntity<User> createUser(@RequestBody User user) {
        try {
            // Validate unique fields
            if (!userService.isEmailUnique(user.getEmail())) {
                return ResponseEntity.badRequest().build();
            }
            if (!userService.isPanUnique(user.getPan())) {
                return ResponseEntity.badRequest().build();
            }
            if (!userService.isAadharUnique(user.getAadhar())) {
                return ResponseEntity.badRequest().build();
            }
            
            User savedUser = userService.saveUser(user);
            return ResponseEntity.ok(savedUser);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
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
    public ResponseEntity<User> approveUser(@PathVariable Long id, @RequestParam String adminName) {
        User approvedUser = userService.approveUser(id, adminName);
        return approvedUser != null ? ResponseEntity.ok(approvedUser) : ResponseEntity.notFound().build();
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
}
