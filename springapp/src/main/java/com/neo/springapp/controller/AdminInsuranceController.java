package com.neo.springapp.controller;

import com.neo.springapp.model.InsuranceApplication;
import com.neo.springapp.model.InsuranceClaim;
import com.neo.springapp.model.InsurancePolicy;
import com.neo.springapp.service.InsuranceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminInsuranceController {

    @Autowired
    private InsuranceService insuranceService;

    // ===== Policy management =====

    // Spec-compatible path: /api/admin/create-policy
    @PostMapping("/create-policy")
    public ResponseEntity<?> createPolicyLegacy(@RequestBody InsurancePolicy policy) {
        return createPolicy(policy);
    }

    @PostMapping("/insurance/policies")
    public ResponseEntity<?> createPolicy(@RequestBody InsurancePolicy policy) {
        try {
            InsurancePolicy saved = insuranceService.createPolicy(policy);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("policy", saved);
            response.put("message", "Insurance policy created successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // Spec-compatible path: /api/admin/update-policy
    @PutMapping("/update-policy")
    public ResponseEntity<?> updatePolicyLegacy(@RequestParam Long id, @RequestBody InsurancePolicy policy) {
        return updatePolicy(id, policy);
    }

    @PutMapping("/insurance/policies/{id}")
    public ResponseEntity<?> updatePolicy(@PathVariable Long id, @RequestBody InsurancePolicy policy) {
        try {
            InsurancePolicy updated = insuranceService.updatePolicy(id, policy);
            if (updated == null) {
                return ResponseEntity.notFound().build();
            }
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("policy", updated);
            response.put("message", "Insurance policy updated successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // Spec-compatible path: /api/admin/delete-policy
    @DeleteMapping("/delete-policy")
    public ResponseEntity<?> deletePolicyLegacy(@RequestParam Long id) {
        return deletePolicy(id);
    }

    @DeleteMapping("/insurance/policies/{id}")
    public ResponseEntity<?> deletePolicy(@PathVariable Long id) {
        try {
            insuranceService.deletePolicy(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Insurance policy deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/insurance/policies")
    public ResponseEntity<List<InsurancePolicy>> getAllPolicies() {
        return ResponseEntity.ok(insuranceService.getActivePolicies());
    }

    // ===== Application approvals =====

    @GetMapping("/insurance/applications/pending")
    public ResponseEntity<List<InsuranceApplication>> getPendingApplications() {
        // include both pending approval and under-review for admin view
        java.util.List<InsuranceApplication> pending = new java.util.ArrayList<>();
        pending.addAll(insuranceService.getPendingApplications());
        pending.addAll(insuranceService.getUnderReviewApplications());
        pending.addAll(insuranceService.getApprovedApplications()); // approved but pending activation/payment
        return ResponseEntity.ok(pending);
    }

    @PostMapping("/insurance/assign-policy")
    public ResponseEntity<?> assignPolicyToAccount(@RequestBody Map<String, Object> payload) {
        try {
            String accountNumber = payload.get("accountNumber") != null ? payload.get("accountNumber").toString() : null;
            Long policyId = payload.get("policyId") != null ? Long.valueOf(payload.get("policyId").toString()) : null;
            String customerName = payload.get("customerName") != null ? payload.get("customerName").toString() : null;
            String premiumType = payload.get("premiumType") != null ? payload.get("premiumType").toString() : null;
            String remark = payload.get("remark") != null ? payload.get("remark").toString() : null;

            if (accountNumber == null || policyId == null) {
                throw new IllegalArgumentException("accountNumber and policyId are required");
            }

            InsuranceApplication application = insuranceService.assignPolicyToAccount(accountNumber, policyId, premiumType, remark, customerName);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("application", application);
            response.put("message", "Policy assigned to customer account successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/insurance/applications/{id}/approve")
    public ResponseEntity<?> approveApplication(@PathVariable Long id,
                                                @RequestParam(required = false) String remark) {
        try {
            InsuranceApplication application = insuranceService.approveApplication(id, remark);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("application", application);
            response.put("message", "Application approved successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/insurance/applications/{id}/reject")
    public ResponseEntity<?> rejectApplication(@PathVariable Long id,
                                               @RequestParam(required = false) String remark) {
        try {
            InsuranceApplication application = insuranceService.rejectApplication(id, remark);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("application", application);
            response.put("message", "Application rejected successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/insurance/applications/{id}/auto-debit/approve")
    public ResponseEntity<?> approveAutoDebit(@PathVariable Long id,
                                              @RequestParam(required = false) String remark) {
        try {
            InsuranceApplication application = insuranceService.approveAutoDebit(id, remark);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("application", application);
            response.put("message", "Auto-debit approved for application");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // ===== Claims management =====

    @GetMapping("/insurance/claims/pending")
    public ResponseEntity<List<InsuranceClaim>> getPendingClaims() {
        return ResponseEntity.ok(insuranceService.getPendingClaims());
    }

    @GetMapping("/insurance/claims/by-policy/{policyNumber}")
    public ResponseEntity<List<InsuranceClaim>> getClaimsByPolicyNumber(@PathVariable String policyNumber) {
        return ResponseEntity.ok(insuranceService.getClaimsByPolicyNumber(policyNumber));
    }

    @GetMapping("/insurance/claims/{id}/risk-score")
    public ResponseEntity<Map<String, Object>> getClaimRiskScore(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(insuranceService.getClaimRiskScore(id));
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/insurance/claims/{id}/approve")
    public ResponseEntity<?> approveClaim(@PathVariable Long id,
                                          @RequestParam(required = false) String remark) {
        try {
            InsuranceClaim claim = insuranceService.approveClaim(id, remark);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("claim", claim);
            response.put("message", "Claim approved successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/insurance/claims/{id}/reject")
    public ResponseEntity<?> rejectClaim(@PathVariable Long id,
                                         @RequestParam(required = false) String remark) {
        try {
            InsuranceClaim claim = insuranceService.rejectClaim(id, remark);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("claim", claim);
            response.put("message", "Claim rejected successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/insurance/claims/{id}/payout")
    public ResponseEntity<?> payoutClaim(@PathVariable Long id,
                                         @RequestParam(required = false) String adminAccountNumber,
                                         @RequestParam(required = false) String description) {
        try {
            InsuranceClaim claim = insuranceService.payoutClaim(id, adminAccountNumber, description);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("claim", claim);
            response.put("message", "Claim payout processed successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // ===== Admin dashboard stats =====

    @GetMapping("/insurance/dashboard-stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        return ResponseEntity.ok(insuranceService.getAdminDashboardStats());
    }
}

