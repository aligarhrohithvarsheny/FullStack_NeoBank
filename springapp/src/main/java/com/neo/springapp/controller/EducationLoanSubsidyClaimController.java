package com.neo.springapp.controller;

import com.neo.springapp.model.EducationLoanSubsidyClaim;
import com.neo.springapp.service.EducationLoanSubsidyClaimService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/education-loan-subsidy-claims")
public class EducationLoanSubsidyClaimController {

    @Autowired
    private EducationLoanSubsidyClaimService subsidyClaimService;

    /**
     * Create a new subsidy claim request
     */
    @PostMapping("/create")
    public ResponseEntity<?> createClaim(@RequestBody Map<String, Object> request) {
        try {
            Long loanId = Long.parseLong(request.get("loanId").toString());
            String childAadharNumber = request.get("childAadharNumber") != null ? request.get("childAadharNumber").toString() : "";
            String userNotes = request.get("userNotes") != null ? request.get("userNotes").toString() : "";
            
            EducationLoanSubsidyClaim claim = subsidyClaimService.createClaim(loanId, childAadharNumber, userNotes);
            return ResponseEntity.ok(claim);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Get all claims
     */
    @GetMapping
    public ResponseEntity<List<EducationLoanSubsidyClaim>> getAllClaims() {
        return ResponseEntity.ok(subsidyClaimService.getAllClaims());
    }

    /**
     * Get claim by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<EducationLoanSubsidyClaim> getClaimById(@PathVariable Long id) {
        try {
            EducationLoanSubsidyClaim claim = subsidyClaimService.getClaimById(id);
            return ResponseEntity.ok(claim);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get claims by account number
     */
    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<List<EducationLoanSubsidyClaim>> getClaimsByAccountNumber(@PathVariable String accountNumber) {
        return ResponseEntity.ok(subsidyClaimService.getClaimsByAccountNumber(accountNumber));
    }

    /**
     * Get pending claims
     */
    @GetMapping("/pending")
    public ResponseEntity<List<EducationLoanSubsidyClaim>> getPendingClaims() {
        return ResponseEntity.ok(subsidyClaimService.getPendingClaims());
    }

    /**
     * Approve claim (admin can edit amount)
     */
    @PutMapping("/{id}/approve")
    public ResponseEntity<?> approveClaim(
            @PathVariable Long id,
            @RequestParam(required = false) Double approvedAmount,
            @RequestParam String adminName,
            @RequestParam(required = false) String adminNotes) {
        try {
            EducationLoanSubsidyClaim claim = subsidyClaimService.getClaimById(id);
            Double amount = approvedAmount != null ? approvedAmount : claim.getCalculatedSubsidyAmount();
            String notes = adminNotes != null ? adminNotes : "";
            
            EducationLoanSubsidyClaim approvedClaim = subsidyClaimService.approveClaim(id, amount, adminName, notes);
            return ResponseEntity.ok(approvedClaim);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Reject claim
     */
    @PutMapping("/{id}/reject")
    public ResponseEntity<?> rejectClaim(
            @PathVariable Long id,
            @RequestParam String adminName,
            @RequestParam(required = false) String rejectionReason) {
        try {
            String reason = rejectionReason != null ? rejectionReason : "Claim rejected by admin";
            EducationLoanSubsidyClaim rejectedClaim = subsidyClaimService.rejectClaim(id, adminName, reason);
            return ResponseEntity.ok(rejectedClaim);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Credit subsidy to user account
     */
    @PutMapping("/{id}/credit")
    public ResponseEntity<?> creditSubsidy(@PathVariable Long id, @RequestParam String adminName) {
        try {
            Map<String, Object> result = subsidyClaimService.creditSubsidyToAccount(id, adminName);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Update approved amount
     */
    @PutMapping("/{id}/update-amount")
    public ResponseEntity<?> updateApprovedAmount(
            @PathVariable Long id,
            @RequestParam Double newAmount,
            @RequestParam String adminName) {
        try {
            EducationLoanSubsidyClaim claim = subsidyClaimService.updateApprovedAmount(id, newAmount, adminName);
            return ResponseEntity.ok(claim);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}




