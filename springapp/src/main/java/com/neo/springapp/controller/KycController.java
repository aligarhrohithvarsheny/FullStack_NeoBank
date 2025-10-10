package com.neo.springapp.controller;

import com.neo.springapp.model.KycRequest;
import com.neo.springapp.service.KycService;
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

    // Basic CRUD operations
    @PostMapping("/create")
    public ResponseEntity<KycRequest> createKycRequest(@RequestBody KycRequest kycRequest) {
        try {
            KycRequest savedRequest = kycService.saveKycRequest(kycRequest);
            return ResponseEntity.ok(savedRequest);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
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
