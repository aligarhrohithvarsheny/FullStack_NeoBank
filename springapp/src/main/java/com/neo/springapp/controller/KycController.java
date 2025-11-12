package com.neo.springapp.controller;

import com.neo.springapp.model.KycRequest;
import com.neo.springapp.service.KycService;
import com.neo.springapp.service.EmailService;
import com.neo.springapp.service.OtpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/kyc")
@CrossOrigin(origins = "http://localhost:4200")
public class KycController {

    @Autowired
    private KycService kycService;
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private OtpService otpService;

    // Check if user has existing KYC requests (to determine if OTP is needed)
    @GetMapping("/check-existing/{userAccountNumber}")
    public ResponseEntity<Map<String, Object>> checkExistingKycRequests(@PathVariable String userAccountNumber) {
        Map<String, Object> response = new HashMap<>();
        boolean hasExisting = kycService.hasExistingKycRequests(userAccountNumber);
        response.put("hasExisting", hasExisting);
        response.put("requiresOtp", hasExisting);
        return ResponseEntity.ok(response);
    }

    // Send OTP for KYC update request
    @PostMapping("/send-otp")
    public ResponseEntity<Map<String, Object>> sendKycOtp(@RequestBody Map<String, String> request) {
        try {
            String userEmail = request.get("userEmail");
            String userAccountNumber = request.get("userAccountNumber");
            
            if (userEmail == null || userEmail.trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Email is required");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Check if user has existing KYC requests
            if (!kycService.hasExistingKycRequests(userAccountNumber)) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "This is your first KYC request. OTP is not required.");
                response.put("requiresOtp", false);
                return ResponseEntity.badRequest().body(response);
            }
            
            // Validate email format
            if (!userEmail.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Invalid email format");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Generate and send OTP
            String otp = otpService.generateOtp();
            otpService.storeOtp(userEmail, otp);
            
            // Send KYC update OTP via email
            boolean emailSent = emailService.sendKycUpdateOtpEmail(userEmail, otp);
            
            if (emailSent) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "OTP has been sent to your email. Please check and enter the OTP.");
                System.out.println("✅ KYC update OTP sent to email: " + userEmail);
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Failed to send OTP. Please try again.");
                System.out.println("❌ Failed to send KYC update OTP to email: " + userEmail);
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to send OTP: " + e.getMessage());
            System.out.println("KYC update OTP error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // Basic CRUD operations
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createKycRequest(@RequestBody Map<String, Object> request) {
        try {
            // Check if OTP is required (subsequent request)
            String userAccountNumber = (String) request.get("userAccountNumber");
            String userEmail = (String) request.get("userEmail");
            boolean hasExisting = kycService.hasExistingKycRequests(userAccountNumber);
            
            // If this is a subsequent request, verify OTP
            if (hasExisting) {
                String otp = (String) request.get("otp");
                if (otp == null || otp.trim().isEmpty()) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("requiresOtp", true);
                    response.put("message", "OTP is required for subsequent KYC update requests");
                    return ResponseEntity.badRequest().body(response);
                }
                
                // Verify OTP
                if (!otpService.verifyOtp(userEmail, otp)) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("requiresOtp", true);
                    response.put("message", "Invalid or expired OTP. Please try again.");
                    System.out.println("❌ Invalid OTP for KYC update: " + userEmail);
                    return ResponseEntity.badRequest().body(response);
                }
            }
            
            // Create KYC request
            KycRequest kycRequest = new KycRequest();
            kycRequest.setPanNumber((String) request.get("panNumber"));
            kycRequest.setName((String) request.get("name"));
            kycRequest.setUserId((String) request.get("userId"));
            kycRequest.setUserName((String) request.get("userName"));
            kycRequest.setUserEmail(userEmail);
            kycRequest.setUserAccountNumber(userAccountNumber);
            kycRequest.setStatus("Pending");
            
            KycRequest savedRequest = kycService.saveKycRequest(kycRequest);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("kycRequest", savedRequest);
            response.put("message", "KYC request submitted successfully");
            System.out.println("✅ KYC request created: " + savedRequest.getId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to create KYC request: " + e.getMessage());
            System.out.println("❌ KYC request creation error: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<KycRequest> getKycRequestById(@PathVariable Long id) {
        Optional<KycRequest> request = kycService.getKycRequestById(id);
        return request.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/all")
    public ResponseEntity<Page<KycRequest>> getAllKycRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "submittedDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Page<KycRequest> requests = kycService.getAllKycRequestsWithPagination(page, size, sortBy, sortDir);
        return ResponseEntity.ok(requests);
    }

    // Create new KYC request with user details
    @PostMapping("/request")
    public ResponseEntity<KycRequest> requestKyc(
            @RequestParam String panNumber, 
            @RequestParam String name,
            @RequestParam String userId,
            @RequestParam String userName,
            @RequestParam String userEmail,
            @RequestParam String userAccountNumber) {
        try {
            KycRequest request = kycService.createKycRequest(panNumber, name, userId, userName, userEmail, userAccountNumber);
            return ResponseEntity.ok(request);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // Approve KYC by admin
    @PutMapping("/approve/{id}")
    public ResponseEntity<KycRequest> approveKyc(@PathVariable Long id, @RequestParam String adminName) {
        KycRequest approvedRequest = kycService.approveKyc(id, adminName);
        return approvedRequest != null ? ResponseEntity.ok(approvedRequest) : ResponseEntity.notFound().build();
    }

    // Reject KYC by admin
    @PutMapping("/reject/{id}")
    public ResponseEntity<KycRequest> rejectKyc(@PathVariable Long id, @RequestParam String adminName) {
        KycRequest rejectedRequest = kycService.rejectKyc(id, adminName);
        return rejectedRequest != null ? ResponseEntity.ok(rejectedRequest) : ResponseEntity.notFound().build();
    }

    // Get KYC status by PAN number
    @GetMapping("/status/{panNumber}")
    public ResponseEntity<KycRequest> getStatusByPanNumber(@PathVariable String panNumber) {
        Optional<KycRequest> request = kycService.getKycStatusByPanNumber(panNumber);
        return request.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    // Get KYC request by user account number
    @GetMapping("/account/{userAccountNumber}")
    public ResponseEntity<KycRequest> getKycRequestByAccountNumber(@PathVariable String userAccountNumber) {
        Optional<KycRequest> request = kycService.getKycRequestByUserAccountNumber(userAccountNumber);
        return request.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    // Get all KYC requests by user account number
    @GetMapping("/account/{userAccountNumber}/all")
    public ResponseEntity<List<KycRequest>> getAllKycRequestsByAccountNumber(@PathVariable String userAccountNumber) {
        List<KycRequest> requests = kycService.getAllKycRequestsByUserAccountNumber(userAccountNumber);
        return ResponseEntity.ok(requests);
    }

    // Status-based operations
    @GetMapping("/status/{status}")
    public ResponseEntity<List<KycRequest>> getKycRequestsByStatus(@PathVariable String status) {
        List<KycRequest> requests = kycService.getKycRequestsByStatus(status);
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/status/{status}/paginated")
    public ResponseEntity<Page<KycRequest>> getKycRequestsByStatusWithPagination(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<KycRequest> requests = kycService.getKycRequestsByStatusWithPagination(status, page, size);
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/pending")
    public ResponseEntity<List<KycRequest>> getPendingKycRequestsForReview() {
        List<KycRequest> requests = kycService.getPendingKycRequestsForReview();
        return ResponseEntity.ok(requests);
    }

    // Search operations
    @GetMapping("/search")
    public ResponseEntity<Page<KycRequest>> searchKycRequests(
            @RequestParam String searchTerm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<KycRequest> requests = kycService.searchKycRequests(searchTerm, page, size);
        return ResponseEntity.ok(requests);
    }

    // Statistics endpoints
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getKycRequestStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRequests", kycService.getTotalKycRequestsCount());
        stats.put("pendingRequests", kycService.getKycRequestsCountByStatus("Pending"));
        stats.put("approvedRequests", kycService.getKycRequestsCountByStatus("Approved"));
        stats.put("rejectedRequests", kycService.getKycRequestsCountByStatus("Rejected"));
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/recent")
    public ResponseEntity<List<KycRequest>> getRecentKycRequests(@RequestParam(defaultValue = "5") int limit) {
        List<KycRequest> requests = kycService.getRecentKycRequests(limit);
        return ResponseEntity.ok(requests);
    }

    // Admin operations
    @GetMapping("/admin/{adminName}")
    public ResponseEntity<List<KycRequest>> getKycRequestsByApprovedBy(@PathVariable String adminName) {
        List<KycRequest> requests = kycService.getKycRequestsByApprovedBy(adminName);
        return ResponseEntity.ok(requests);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteKycRequest(@PathVariable Long id) {
        try {
            kycService.deleteKycRequest(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
