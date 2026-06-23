package com.neo.springapp.controller;

import com.neo.springapp.model.AccountTracking;
import com.neo.springapp.service.AccountTrackingService;
import com.neo.springapp.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tracking")
public class AccountTrackingController {

    @Autowired
    private AccountTrackingService accountTrackingService;

    @Autowired
    private AccountService accountService;

    private Map<String, Object> toSafeUserSummary(com.neo.springapp.model.User user) {
        if (user == null) {
            return null;
        }
        Map<String, Object> userSummary = new HashMap<>();
        userSummary.put("id", user.getId());
        userSummary.put("username", user.getUsername());
        userSummary.put("email", user.getEmail());
        userSummary.put("accountNumber", user.getAccountNumber());
        userSummary.put("status", user.getStatus());
        return userSummary;
    }

    private Map<String, Object> toSafeTracking(AccountTracking tracking) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", tracking.getId());
        response.put("trackingId", tracking.getTrackingId());
        response.put("aadharNumber", tracking.getAadharNumber());
        response.put("mobileNumber", tracking.getMobileNumber());
        response.put("status", tracking.getStatus());
        response.put("createdAt", tracking.getCreatedAt());
        response.put("updatedAt", tracking.getUpdatedAt());
        response.put("statusChangedAt", tracking.getStatusChangedAt());
        response.put("updatedBy", tracking.getUpdatedBy());
        response.put("user", toSafeUserSummary(tracking.getUser()));
        return response;
    }

    private Map<String, Object> toSafeTrackingPage(Page<AccountTracking> trackingPage) {
        Map<String, Object> response = new HashMap<>();
        response.put("content", trackingPage.getContent().stream()
            .map(this::toSafeTracking)
            .collect(Collectors.toList()));
        response.put("totalElements", trackingPage.getTotalElements());
        response.put("totalPages", trackingPage.getTotalPages());
        response.put("number", trackingPage.getNumber());
        response.put("size", trackingPage.getSize());
        return response;
    }

    /**
     * Get all tracking records with pagination
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllTracking(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Page<AccountTracking> tracking = accountTrackingService.getAllTracking(page, size, sortBy, sortDir);
        return ResponseEntity.ok(toSafeTrackingPage(tracking));
    }

    /**
     * Get tracking by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<AccountTracking> getTrackingById(@PathVariable Long id) {
        Optional<AccountTracking> tracking = accountTrackingService.getTrackingById(id);
        return tracking.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get tracking by tracking ID
     */
    @GetMapping("/tracking-id/{trackingId}")
    public ResponseEntity<AccountTracking> getTrackingByTrackingId(@PathVariable String trackingId) {
        Optional<AccountTracking> tracking = accountTrackingService.getTrackingByTrackingId(trackingId);
        return tracking.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get tracking by Aadhar number
     */
    @GetMapping("/aadhar/{aadharNumber}")
    public ResponseEntity<AccountTracking> getTrackingByAadharNumber(@PathVariable String aadharNumber) {
        Optional<AccountTracking> tracking = accountTrackingService.getTrackingByAadharNumber(aadharNumber);
        return tracking.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get tracking by mobile number
     */
    @GetMapping("/mobile/{mobileNumber}")
    public ResponseEntity<AccountTracking> getTrackingByMobileNumber(@PathVariable String mobileNumber) {
        Optional<AccountTracking> tracking = accountTrackingService.getTrackingByMobileNumber(mobileNumber);
        return tracking.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get tracking by Aadhar number and mobile number
     */
    @GetMapping("/track")
    public ResponseEntity<Map<String, Object>> getTrackingByAadharAndMobile(
            @RequestParam String aadharNumber,
            @RequestParam String mobileNumber) {
        try {
            Optional<AccountTracking> tracking = accountTrackingService.getTrackingByAadharAndMobile(aadharNumber, mobileNumber);
            if (tracking.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("tracking", toSafeTracking(tracking.get()));
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "No tracking record found with the provided Aadhar number and mobile number.");
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error retrieving tracking: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Get tracking by user ID
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<AccountTracking> getTrackingByUserId(@PathVariable Long userId) {
        Optional<AccountTracking> tracking = accountTrackingService.getTrackingByUserId(userId);
        return tracking.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get tracking by status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Map<String, Object>>> getTrackingByStatus(@PathVariable String status) {
        List<AccountTracking> tracking = accountTrackingService.getTrackingByStatus(status);
        return ResponseEntity.ok(tracking.stream().map(this::toSafeTracking).collect(Collectors.toList()));
    }

    /**
     * Get tracking by status with pagination
     */
    @GetMapping("/status/{status}/paginated")
    public ResponseEntity<Map<String, Object>> getTrackingByStatusWithPagination(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<AccountTracking> tracking = accountTrackingService.getTrackingByStatusWithPagination(status, page, size);
        return ResponseEntity.ok(toSafeTrackingPage(tracking));
    }

    /**
     * Search tracking records
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchTracking(
            @RequestParam String searchTerm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<AccountTracking> tracking = accountTrackingService.searchTracking(searchTerm, page, size);
        return ResponseEntity.ok(toSafeTrackingPage(tracking));
    }

    /**
     * Update tracking status (for admin)
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> updateTrackingStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false, defaultValue = "Admin") String updatedBy) {
        try {
            System.out.println("=== UPDATE TRACKING STATUS REQUEST ===");
            System.out.println("Tracking ID: " + id);
            System.out.println("New Status: " + status);
            System.out.println("Updated By: " + updatedBy);
            
            // Validate status
            if (status == null || status.trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Status cannot be empty");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Validate status values
            List<String> validStatuses = List.of("PENDING", "ADMIN_SEEN", "ADMIN_APPROVED", "ADMIN_SENT");
            if (!validStatuses.contains(status.toUpperCase())) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Invalid status. Valid values are: PENDING, ADMIN_SEEN, ADMIN_APPROVED, ADMIN_SENT");
                return ResponseEntity.badRequest().body(response);
            }
            
            AccountTracking updatedTracking = accountTrackingService.updateTrackingStatus(id, status.toUpperCase(), updatedBy);
            if (updatedTracking != null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("tracking", toSafeTracking(updatedTracking));
                response.put("message", "Tracking status updated successfully");
                System.out.println("✅ Tracking status updated successfully");
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Tracking record not found with ID: " + id);
                System.out.println("❌ Tracking record not found");
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            System.err.println("❌ Error updating tracking status: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            String errorMessage = e.getMessage() != null ? e.getMessage() : "Unknown error occurred";
            response.put("message", "Failed to update tracking status: " + errorMessage);
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Update tracking status by tracking ID
     */
    @PutMapping("/tracking-id/{trackingId}/status")
    public ResponseEntity<Map<String, Object>> updateTrackingStatusByTrackingId(
            @PathVariable String trackingId,
            @RequestParam String status,
            @RequestParam(required = false, defaultValue = "Admin") String updatedBy) {
        try {
            AccountTracking updatedTracking = accountTrackingService.updateTrackingStatusByTrackingId(trackingId, status, updatedBy);
            if (updatedTracking != null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("tracking", toSafeTracking(updatedTracking));
                response.put("message", "Tracking status updated successfully");
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Tracking record not found");
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to update tracking status: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Get recent tracking records
     */
    @GetMapping("/recent")
    public ResponseEntity<List<Map<String, Object>>> getRecentTracking(
            @RequestParam(defaultValue = "10") int limit) {
        List<AccountTracking> tracking = accountTrackingService.getRecentTracking(limit);
        return ResponseEntity.ok(tracking.stream().map(this::toSafeTracking).collect(Collectors.toList()));
    }

    /**
     * Verify Aadhar for a tracking record
     */
    @PostMapping("/{id}/verify-aadhar")
    public ResponseEntity<Map<String, Object>> verifyAadhar(
            @PathVariable Long id,
            @RequestParam(required = false) String verificationReference) {
        try {
            Optional<AccountTracking> trackingOpt = accountTrackingService.getTrackingById(id);
            if (trackingOpt.isPresent()) {
                AccountTracking tracking = trackingOpt.get();
                // Verify Aadhar in Account
                com.neo.springapp.model.Account account = accountService.verifyAadhar(
                    tracking.getAadharNumber(),
                    verificationReference,
                    "Admin"
                );
                if (account != null) {
                    // Update tracking status to approved
                    accountTrackingService.updateTrackingStatus(id, "ADMIN_APPROVED", "Admin");
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("message", "Aadhar verified successfully");
                    response.put("account", account);
                    response.put("tracking", accountTrackingService.getTrackingById(id).orElse(null));
                    return ResponseEntity.ok(response);
                } else {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("message", "Account not found for Aadhar: " + tracking.getAadharNumber());
                    return ResponseEntity.badRequest().body(response);
                }
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Tracking record not found");
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to verify Aadhar: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}

