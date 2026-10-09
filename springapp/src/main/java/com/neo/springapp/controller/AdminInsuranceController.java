package com.neo.springapp.controller;

import com.neo.springapp.model.InsuranceApplication;
import com.neo.springapp.model.InsuranceClaim;
import com.neo.springapp.model.InsurancePolicy;
import com.neo.springapp.model.GuestInsuranceApplication;
import com.neo.springapp.service.InsuranceService;
import com.neo.springapp.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminInsuranceController {

    @Autowired
    private InsuranceService insuranceService;

    @Autowired
    private AdminService adminService;

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
        return ResponseEntity.ok(insuranceService.getApplicationsAwaitingReview());
    }

    @GetMapping("/insurance/applications/all")
    public ResponseEntity<List<InsuranceApplication>> getAllInsuranceApplications() {
        return ResponseEntity.ok(insuranceService.getAllApplications());
    }

    @PutMapping("/insurance/applications/{id}/premium")
    public ResponseEntity<?> updateApprovedInsurancePremium(
            @PathVariable Long id, @RequestBody Map<String, Object> request) {
        try {
            Double amount = request.get("premiumAmount") == null
                    ? null : Double.valueOf(request.get("premiumAmount").toString());
            String type = request.get("premiumType") == null
                    ? null : request.get("premiumType").toString();
            InsuranceApplication application = insuranceService.updateInsurancePremium(id, amount, type);
            return ResponseEntity.ok(Map.of("success", true, "application", application,
                    "message", "Application premium updated"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message",
                    e.getMessage() == null ? "Unable to update application premium" : e.getMessage()));
        }
    }

    @GetMapping("/insurance/payments/{paymentId}/receipt")
    public ResponseEntity<?> downloadInsurancePaymentReceipt(@PathVariable Long paymentId) {
        try {
            byte[] receipt = insuranceService.generateInsurancePaymentReceipt(paymentId);
            return ResponseEntity.ok()
                    .header("Content-Type", "application/pdf")
                    .header("Content-Disposition", "attachment; filename=insurance-receipt-" + paymentId + ".pdf")
                    .body(receipt);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message",
                    e.getMessage() == null ? "Unable to generate payment receipt" : e.getMessage()));
        }
    }

    @GetMapping("/insurance/guest-applications/pending")
    public ResponseEntity<?> getPendingGuestApplications(
            @RequestHeader("X-Admin-Email") String adminEmail,
            @RequestHeader("X-Admin-Password") String adminPassword) {
        if (!adminService.verifyInsuranceReviewer(adminEmail, adminPassword)) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Admin verification failed"));
        }
        return ResponseEntity.ok(insuranceService.getPendingGuestApplications());
    }

    @GetMapping("/insurance/guest-applications/approved")
    public ResponseEntity<?> getApprovedGuestApplications(
            @RequestHeader("X-Admin-Email") String adminEmail,
            @RequestHeader("X-Admin-Password") String adminPassword) {
        if (!adminService.verifyInsuranceReviewer(adminEmail, adminPassword)) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Admin verification failed"));
        }
        return ResponseEntity.ok(insuranceService.getApprovedGuestApplications());
    }

    @PutMapping("/insurance/guest-applications/{id}/premium")
    public ResponseEntity<?> updateApprovedGuestInsurancePremium(
            @PathVariable Long id,
            @RequestHeader("X-Admin-Email") String adminEmail,
            @RequestHeader("X-Admin-Password") String adminPassword,
            @RequestBody Map<String, Object> request) {
        if (!adminService.verifyInsuranceReviewer(adminEmail, adminPassword)) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Admin verification failed"));
        }
        try {
            Double amount = request.get("premiumAmount") == null
                    ? null : Double.valueOf(request.get("premiumAmount").toString());
            String type = request.get("premiumType") == null
                    ? null : request.get("premiumType").toString();
            GuestInsuranceApplication application =
                    insuranceService.updateGuestInsurancePremium(id, amount, type);
            return ResponseEntity.ok(Map.of("success", true, "applicationNumber",
                    application.getApplicationNumber(), "email", application.getEmail(),
                    "message", "Application premium updated"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message",
                    e.getMessage() == null ? "Unable to update guest premium" : e.getMessage()));
        }
    }

    @GetMapping("/insurance/guest-payments/{paymentId}/receipt")
    public ResponseEntity<?> downloadGuestInsurancePaymentReceipt(
            @PathVariable Long paymentId,
            @RequestHeader("X-Admin-Email") String adminEmail,
            @RequestHeader("X-Admin-Password") String adminPassword) {
        if (!adminService.verifyInsuranceReviewer(adminEmail, adminPassword)) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Admin verification failed"));
        }
        try {
            byte[] receipt = insuranceService.generateGuestInsurancePaymentReceipt(paymentId);
            return ResponseEntity.ok()
                    .header("Content-Type", "application/pdf")
                    .header("Content-Disposition", "attachment; filename=guest-insurance-receipt-" + paymentId + ".pdf")
                    .body(receipt);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message",
                    e.getMessage() == null ? "Unable to generate guest payment receipt" : e.getMessage()));
        }
    }

    @PostMapping("/insurance/guest-applications/{id}/review")
    public ResponseEntity<?> reviewGuestApplication(
            @PathVariable Long id,
            @RequestHeader("X-Admin-Email") String adminEmail,
            @RequestHeader("X-Admin-Password") String adminPassword,
            @RequestBody Map<String, Object> request) {
        try {
            if (!adminService.verifyInsuranceReviewer(adminEmail, adminPassword)) {
                return ResponseEntity.status(401).body(Map.of("success", false, "message", "Admin verification failed"));
            }
            if (!(request.get("approve") instanceof Boolean approve)) {
                throw new IllegalArgumentException("approve must be true or false");
            }
            String remark = request.get("remark") == null ? null : String.valueOf(request.get("remark"));
            var reviewedApplication = insuranceService.reviewGuestApplication(id, approve, remark);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "email", reviewedApplication.getEmail(),
                    "message", approve ? "Insurance application approved" : "Insurance application rejected"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message",
                    e.getMessage() == null ? "Unable to review insurance application" : e.getMessage()));
        }
    }

    @GetMapping("/insurance/guest-claims/pending")
    public ResponseEntity<?> getPendingGuestInsuranceClaims(
            @RequestHeader("X-Admin-Email") String adminEmail,
            @RequestHeader("X-Admin-Password") String adminPassword) {
        if (!adminService.verifyInsuranceReviewer(adminEmail, adminPassword)) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Admin verification failed"));
        }
        return ResponseEntity.ok(insuranceService.getPendingGuestInsuranceClaims());
    }

    @PostMapping("/insurance/guest-claims/{id}/review")
    public ResponseEntity<?> reviewGuestInsuranceClaim(
            @PathVariable Long id,
            @RequestHeader("X-Admin-Email") String adminEmail,
            @RequestHeader("X-Admin-Password") String adminPassword,
            @RequestBody Map<String, Object> request) {
        try {
            if (!adminService.verifyInsuranceReviewer(adminEmail, adminPassword)) {
                return ResponseEntity.status(401).body(Map.of("success", false, "message", "Admin verification failed"));
            }
            if (!(request.get("approve") instanceof Boolean approve)) {
                throw new IllegalArgumentException("approve must be true or false");
            }
            String remark = request.get("remark") == null ? null : String.valueOf(request.get("remark"));
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "claim", insuranceService.reviewGuestInsuranceClaim(id, approve, remark),
                    "message", approve ? "Guest insurance claim approved" : "Guest insurance claim rejected"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message",
                    e.getMessage() == null ? "Unable to review guest claim" : e.getMessage()));
        }
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

            InsuranceApplication application = insuranceService.assignPolicyToVerifiedAccount(accountNumber, policyId, premiumType, remark, customerName, payload);
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

    @GetMapping("/insurance/accounts/verify")
    public ResponseEntity<?> verifyInsuranceAccount(@RequestParam String accountNumber,
                                                    @RequestParam(required = false) String customerName) {
        try { return ResponseEntity.ok(insuranceService.verifyLinkedAccount(accountNumber, customerName)); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("valid", false, "message", e.getMessage())); }
    }

    @PutMapping("/insurance/applications/{id}")
    public ResponseEntity<?> editInsuranceApplication(@PathVariable Long id, @RequestBody Map<String, Object> updates) {
        try { return ResponseEntity.ok(Map.of("success", true, "application", insuranceService.editApplication(id, updates))); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage())); }
    }

    @PostMapping("/insurance/applications/{id}/renew")
    public ResponseEntity<?> renewInsuranceApplication(@PathVariable Long id,
                                                        @RequestParam(required = false) String renewedBy) {
        try { return ResponseEntity.ok(Map.of("success", true, "application", insuranceService.renewApplication(id, renewedBy))); }
        catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage())); }
    }

    @PostMapping(value = "/insurance/applications/{id}/documents", consumes = "multipart/form-data")
    public ResponseEntity<?> uploadInsuranceDocuments(@PathVariable Long id,
                                                       @RequestParam("files") List<MultipartFile> files) {
        try {
            Path directory = Paths.get("uploads/insurance");
            Files.createDirectories(directory);
            List<String> paths = new java.util.ArrayList<>();
            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) continue;
                String original = file.getOriginalFilename() == null ? "document" : file.getOriginalFilename();
                String safe = original.replaceAll("[^A-Za-z0-9._-]", "_");
                Path target = directory.resolve(id + "_" + System.currentTimeMillis() + "_" + safe).normalize();
                Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
                paths.add(target.toString());
            }
            InsuranceApplication application = insuranceService.editApplication(id, Map.of("vehicleDocumentPaths", String.join(",", paths)));
            return ResponseEntity.ok(Map.of("success", true, "application", application, "files", paths));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage())); }
    }

    @GetMapping("/insurance/applications/{id}/certificate")
    public ResponseEntity<byte[]> downloadAdminCertificate(@PathVariable Long id) {
        byte[] pdf = insuranceService.generatePolicyCertificatePdf(id);
        return ResponseEntity.ok().header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=insurance-certificate-" + id + ".pdf").body(pdf);
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
