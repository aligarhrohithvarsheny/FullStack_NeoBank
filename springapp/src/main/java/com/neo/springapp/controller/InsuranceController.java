package com.neo.springapp.controller;

import com.neo.springapp.model.InsuranceApplication;
import com.neo.springapp.model.InsuranceClaim;
import com.neo.springapp.model.InsurancePayment;
import com.neo.springapp.model.InsurancePolicy;
import com.neo.springapp.service.InsuranceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class InsuranceController {

    @Autowired
    private InsuranceService insuranceService;

    // ===== Public / User-facing APIs =====

    @GetMapping("/insurance/policies")
    public ResponseEntity<List<InsurancePolicy>> getPolicies() {
        return ResponseEntity.ok(insuranceService.getActivePolicies());
    }

    // Compatibility with prompt: /api/policies/apply
    @PostMapping("/policies/apply")
    public ResponseEntity<?> applyPolicyLegacy(@RequestBody Map<String, Object> payload) {
        return applyPolicy(payload);
    }

    @PostMapping("/insurance/policies/apply")
    public ResponseEntity<?> applyPolicy(@RequestBody Map<String, Object> payload) {
        try {
            Long userId = payload.get("userId") != null
                    ? Long.valueOf(payload.get("userId").toString())
                    : null;
            Long policyId = payload.get("policyId") != null
                    ? Long.valueOf(payload.get("policyId").toString())
                    : null;
            String nomineeName = (String) payload.getOrDefault("nomineeName", "");
            String nomineeRelation = (String) payload.getOrDefault("nomineeRelation", "");
            String kycDocumentPath = (String) payload.getOrDefault("kycDocumentPath", "");
            String premiumType = (String) payload.getOrDefault("premiumType", "MONTHLY");
            Integer proposerAge = payload.get("proposerAge") != null
                    ? Integer.valueOf(payload.get("proposerAge").toString())
                    : null;
            String healthConditions = (String) payload.getOrDefault("healthConditions", "");
            String lifestyleHabits = (String) payload.getOrDefault("lifestyleHabits", "");
            Boolean hasExistingEmis = payload.get("hasExistingEmis") != null
                    ? Boolean.valueOf(payload.get("hasExistingEmis").toString())
                    : null;

            if (userId == null || policyId == null) {
                throw new IllegalArgumentException("userId and policyId are required");
            }

            InsuranceApplication application = insuranceService.applyForPolicy(
                    userId,
                    policyId,
                    nomineeName,
                    nomineeRelation,
                    kycDocumentPath,
                    premiumType,
                    proposerAge,
                    healthConditions,
                    lifestyleHabits,
                    hasExistingEmis
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("application", application);
            response.put("message", "Insurance application submitted successfully and is pending approval");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/insurance/applications/user/{userId}")
    public ResponseEntity<List<InsuranceApplication>> getUserApplications(@PathVariable Long userId) {
        return ResponseEntity.ok(insuranceService.getApplicationsForUser(userId));
    }

    @GetMapping("/insurance/applications/account/{accountNumber}")
    public ResponseEntity<List<InsuranceApplication>> getAccountApplications(@PathVariable String accountNumber) {
        return ResponseEntity.ok(insuranceService.getApplicationsForAccount(accountNumber));
    }

    @PostMapping("/insurance/payments")
    public ResponseEntity<?> payPremium(@RequestBody Map<String, Object> payload) {
        try {
            Long applicationId = payload.get("applicationId") != null
                    ? Long.valueOf(payload.get("applicationId").toString())
                    : null;
            Double amount = payload.get("amount") != null
                    ? Double.valueOf(payload.get("amount").toString())
                    : null;
            boolean autoDebitEnabled = payload.get("autoDebitEnabled") != null
                    && Boolean.parseBoolean(payload.get("autoDebitEnabled").toString());
            String merchant = (String) payload.getOrDefault("merchant", "Insurance Premium");

            if (applicationId == null || amount == null) {
                throw new IllegalArgumentException("applicationId and amount are required");
            }

            InsurancePayment payment = insuranceService.payPremium(
                    applicationId,
                    amount,
                    autoDebitEnabled,
                    merchant
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("payment", payment);
            response.put("message", "Premium payment successful");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/insurance/payments/user/{userId}")
    public ResponseEntity<List<InsurancePayment>> getUserPayments(@PathVariable Long userId) {
        return ResponseEntity.ok(insuranceService.getPaymentsForUser(userId));
    }

    @GetMapping("/insurance/payments/account/{accountNumber}")
    public ResponseEntity<List<InsurancePayment>> getAccountPayments(@PathVariable String accountNumber) {
        return ResponseEntity.ok(insuranceService.getPaymentsForAccount(accountNumber));
    }

    // Compatibility with prompt: /api/claims/request
    @PostMapping("/claims/request")
    public ResponseEntity<?> requestClaimLegacy(@RequestBody Map<String, Object> payload) {
        return requestClaim(payload);
    }

    @PostMapping("/insurance/claims/request")
    public ResponseEntity<?> requestClaim(@RequestBody Map<String, Object> payload) {
        try {
            Long applicationId = payload.get("applicationId") != null
                    ? Long.valueOf(payload.get("applicationId").toString())
                    : null;
            Double claimAmount = payload.get("claimAmount") != null
                    ? Double.valueOf(payload.get("claimAmount").toString())
                    : null;
            String documentsPath = (String) payload.getOrDefault("documentsPath", "");

            if (applicationId == null || claimAmount == null) {
                throw new IllegalArgumentException("applicationId and claimAmount are required");
            }

            InsuranceClaim claim = insuranceService.createClaim(applicationId, claimAmount, documentsPath);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("claim", claim);
            response.put("message", "Claim request submitted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/insurance/claims/user/{userId}")
    public ResponseEntity<List<InsuranceClaim>> getUserClaims(@PathVariable Long userId) {
        return ResponseEntity.ok(insuranceService.getClaimsForUser(userId));
    }

    @GetMapping("/insurance/claims/account/{accountNumber}")
    public ResponseEntity<List<InsuranceClaim>> getAccountClaims(@PathVariable String accountNumber) {
        return ResponseEntity.ok(insuranceService.getClaimsForAccount(accountNumber));
    }

    // Upcoming renewals (auto-renewal reminder helper)
    @GetMapping("/insurance/renewals/account/{accountNumber}")
    public ResponseEntity<List<InsurancePayment>> getUpcomingRenewals(
            @PathVariable String accountNumber,
            @RequestParam(defaultValue = "7") int daysAhead) {
        return ResponseEntity.ok(insuranceService.getUpcomingRenewalsForAccount(accountNumber, daysAhead));
    }

    // Simple policy certificate "PDF" download
    @GetMapping("/insurance/applications/{applicationId}/certificate")
    public ResponseEntity<byte[]> downloadCertificate(@PathVariable Long applicationId) {
        byte[] pdfBytes = insuranceService.generatePolicyCertificatePdf(applicationId);
        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=policy-certificate-" + applicationId + ".pdf")
                .body(pdfBytes);
    }

    // Lookup policy number and return linked application and user email (if any)
    @GetMapping("/insurance/policy/lookup/{policyNumber}")
    public ResponseEntity<?> lookupPolicy(@PathVariable String policyNumber) {
        try {
            Map<String, Object> data = insuranceService.lookupPolicyWithCustomer(policyNumber);
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", true);
            resp.putAll(data);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }
}

