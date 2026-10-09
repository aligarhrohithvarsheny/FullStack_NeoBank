package com.neo.springapp.controller;

import com.neo.springapp.model.InsuranceApplication;
import com.neo.springapp.model.InsuranceClaim;
import com.neo.springapp.model.InsurancePayment;
import com.neo.springapp.model.InsurancePolicy;
import com.neo.springapp.service.InsuranceService;
import com.neo.springapp.service.UserSessionTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    @PostMapping("/insurance/authenticate")
    public ResponseEntity<?> authenticateInsuranceCustomer(@RequestBody Map<String, String> credentials) {
        try {
            return ResponseEntity.ok(insuranceService.authenticateCustomer(
                    credentials.get("insuranceNumber"),
                    credentials.get("email"),
                    credentials.get("password")));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Invalid insurance credentials"));
        }
    }

    @PostMapping("/insurance/create-password")
    public ResponseEntity<?> createInsurancePassword(@RequestBody Map<String, String> credentials) {
        try {
            return ResponseEntity.ok(insuranceService.createInsurancePassword(
                    credentials.get("insuranceNumber"),
                    credentials.get("email"),
                    credentials.get("password")));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage() == null
                            ? "Unable to create insurance password" : e.getMessage()));
        }
    }

    @PostMapping("/insurance/guest-applications")
    public ResponseEntity<?> applyForInsuranceAsGuest(@RequestBody Map<String, Object> request) {
        try {
            Long policyId = request.get("policyId") == null
                    ? null : Long.valueOf(request.get("policyId").toString());
            var application = insuranceService.applyAsGuest(
                    String.valueOf(request.getOrDefault("applicantName", "")),
                    String.valueOf(request.getOrDefault("email", "")),
                    String.valueOf(request.getOrDefault("phone", "")),
                    policyId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "applicationNumber", application.getApplicationNumber(),
                    "message", "Insurance application submitted for admin review"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false,
                    "message", e.getMessage() == null ? "Unable to submit insurance application" : e.getMessage()));
        }
    }

    @GetMapping("/insurance/guest-applications/track")
    public ResponseEntity<?> trackGuestInsuranceApplications(@RequestParam String email) {
        try {
            return ResponseEntity.ok(insuranceService.trackGuestApplications(email));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/insurance/guest-account")
    public ResponseEntity<?> getGuestInsuranceAccount(
            @AuthenticationPrincipal UserSessionTokenService.SessionPrincipal principal) {
        if (principal == null || !"INSURANCE_GUEST".equals(principal.scope())) return unauthorized();
        return ResponseEntity.ok(insuranceService.getGuestApplicationForDashboard(principal.userId()));
    }

    @PostMapping("/insurance/guest-account/premium-payment")
    public ResponseEntity<?> payGuestInsurancePremium(
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal UserSessionTokenService.SessionPrincipal principal) {
        if (principal == null || !"INSURANCE_GUEST".equals(principal.scope())) return unauthorized();
        try {
            Map<String, Object> result = insuranceService.payGuestInsurancePremium(
                    principal.userId(), request.get("payerAccountNumber"), request.get("pin"));
            return Boolean.TRUE.equals(result.get("success"))
                    ? ResponseEntity.ok(result)
                    : ResponseEntity.badRequest().body(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message",
                    e.getMessage() == null ? "Unable to pay insurance premium" : e.getMessage()));
        }
    }

    @PostMapping("/insurance/guest-account/close")
    public ResponseEntity<?> closeGuestInsuranceAccount(
            @AuthenticationPrincipal UserSessionTokenService.SessionPrincipal principal) {
        if (principal == null || !"INSURANCE_GUEST".equals(principal.scope())) return unauthorized();
        try {
            return ResponseEntity.ok(insuranceService.closeGuestInsuranceApplication(principal.userId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message",
                    e.getMessage() == null ? "Unable to close insurance" : e.getMessage()));
        }
    }

    @GetMapping("/insurance/guest-account/certificate")
    public ResponseEntity<?> downloadGuestInsuranceCertificate(
            @AuthenticationPrincipal UserSessionTokenService.SessionPrincipal principal) {
        if (principal == null || !"INSURANCE_GUEST".equals(principal.scope())) return unauthorized();
        try {
            byte[] pdf = insuranceService.generateGuestInsuranceCertificate(principal.userId());
            return ResponseEntity.ok()
                    .header("Content-Type", "application/pdf")
                    .header("Content-Disposition", "attachment; filename=insurance-certificate.pdf")
                    .body(pdf);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message",
                    e.getMessage() == null ? "Unable to generate insurance certificate" : e.getMessage()));
        }
    }

    @PostMapping("/insurance/guest-account/claims")
    public ResponseEntity<?> createGuestInsuranceClaim(
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserSessionTokenService.SessionPrincipal principal) {
        if (principal == null || !"INSURANCE_GUEST".equals(principal.scope())) return unauthorized();
        try {
            java.math.BigDecimal amount = request.get("claimAmount") == null
                    ? null : new java.math.BigDecimal(request.get("claimAmount").toString());
            var claim = insuranceService.createGuestInsuranceClaim(
                    principal.userId(), amount,
                    String.valueOf(request.getOrDefault("reason", "")),
                    String.valueOf(request.getOrDefault("details", "")));
            return ResponseEntity.ok(Map.of("success", true, "claimNumber", claim.getClaimNumber(),
                    "message", "Claim submitted for admin review"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message",
                    e.getMessage() == null ? "Unable to submit insurance claim" : e.getMessage()));
        }
    }

    // Compatibility with prompt: /api/policies/apply
    @PostMapping("/policies/apply")
    public ResponseEntity<?> applyPolicyLegacy(@RequestBody Map<String, Object> payload,
                                                @AuthenticationPrincipal UserSessionTokenService.SessionPrincipal principal) {
        return applyPolicy(payload, principal);
    }

    @PostMapping("/insurance/policies/apply")
    public ResponseEntity<?> applyPolicy(@RequestBody Map<String, Object> payload,
                                         @AuthenticationPrincipal UserSessionTokenService.SessionPrincipal principal) {
        if (!isBankInsurancePrincipal(principal)) return unauthorized();
        try {
            Long userId = principal.userId();
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
    public ResponseEntity<?> getUserApplications(@PathVariable Long userId,
                                                  @AuthenticationPrincipal UserSessionTokenService.SessionPrincipal principal) {
        if (!isBankInsurancePrincipal(principal)) return unauthorized();
        if (!principal.userId().equals(userId)) return forbidden();
        return ResponseEntity.ok(insuranceService.getApplicationsForUser(userId));
    }

    @GetMapping("/insurance/applications/account/{accountNumber}")
    public ResponseEntity<?> getAccountApplications(@PathVariable String accountNumber,
                                                     @AuthenticationPrincipal UserSessionTokenService.SessionPrincipal principal) {
        if (!isBankInsurancePrincipal(principal)) return unauthorized();
        if (!principal.accountNumber().equals(accountNumber)) return forbidden();
        return ResponseEntity.ok(insuranceService.getApplicationsForAccount(accountNumber));
    }

    @PostMapping("/insurance/payments")
    public ResponseEntity<?> payPremium(@RequestBody Map<String, Object> payload,
                                        @AuthenticationPrincipal UserSessionTokenService.SessionPrincipal principal) {
        if (!isBankInsurancePrincipal(principal)) return unauthorized();
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
            if (!ownsApplication(principal, applicationId)) return forbidden();

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
    public ResponseEntity<?> getUserPayments(@PathVariable Long userId,
                                              @AuthenticationPrincipal UserSessionTokenService.SessionPrincipal principal) {
        if (!isBankInsurancePrincipal(principal)) return unauthorized();
        if (!principal.userId().equals(userId)) return forbidden();
        return ResponseEntity.ok(insuranceService.getPaymentsForUser(userId));
    }

    @GetMapping("/insurance/payments/account/{accountNumber}")
    public ResponseEntity<?> getAccountPayments(@PathVariable String accountNumber,
                                                 @AuthenticationPrincipal UserSessionTokenService.SessionPrincipal principal) {
        if (!isBankInsurancePrincipal(principal)) return unauthorized();
        if (!principal.accountNumber().equals(accountNumber)) return forbidden();
        return ResponseEntity.ok(insuranceService.getPaymentsForAccount(accountNumber));
    }

    // Compatibility with prompt: /api/claims/request
    @PostMapping("/claims/request")
    public ResponseEntity<?> requestClaimLegacy(@RequestBody Map<String, Object> payload,
                                                 @AuthenticationPrincipal UserSessionTokenService.SessionPrincipal principal) {
        return requestClaim(payload, principal);
    }

    @PostMapping("/insurance/claims/request")
    public ResponseEntity<?> requestClaim(@RequestBody Map<String, Object> payload,
                                          @AuthenticationPrincipal UserSessionTokenService.SessionPrincipal principal) {
        if (!isBankInsurancePrincipal(principal)) return unauthorized();
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
            if (!ownsApplication(principal, applicationId)) return forbidden();

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
    public ResponseEntity<?> getUserClaims(@PathVariable Long userId,
                                            @AuthenticationPrincipal UserSessionTokenService.SessionPrincipal principal) {
        if (!isBankInsurancePrincipal(principal)) return unauthorized();
        if (!principal.userId().equals(userId)) return forbidden();
        return ResponseEntity.ok(insuranceService.getClaimsForUser(userId));
    }

    @GetMapping("/insurance/claims/account/{accountNumber}")
    public ResponseEntity<?> getAccountClaims(@PathVariable String accountNumber,
                                               @AuthenticationPrincipal UserSessionTokenService.SessionPrincipal principal) {
        if (!isBankInsurancePrincipal(principal)) return unauthorized();
        if (!principal.accountNumber().equals(accountNumber)) return forbidden();
        return ResponseEntity.ok(insuranceService.getClaimsForAccount(accountNumber));
    }

    // Upcoming renewals (auto-renewal reminder helper)
    @GetMapping("/insurance/renewals/account/{accountNumber}")
    public ResponseEntity<?> getUpcomingRenewals(
            @PathVariable String accountNumber,
            @RequestParam(defaultValue = "7") int daysAhead,
            @AuthenticationPrincipal UserSessionTokenService.SessionPrincipal principal) {
        if (!isBankInsurancePrincipal(principal)) return unauthorized();
        if (!principal.accountNumber().equals(accountNumber)) return forbidden();
        return ResponseEntity.ok(insuranceService.getUpcomingRenewalsForAccount(accountNumber, daysAhead));
    }

    @PostMapping("/insurance/applications/{applicationId}/close")
    public ResponseEntity<?> requestPolicyClosure(
            @PathVariable Long applicationId,
            @AuthenticationPrincipal UserSessionTokenService.SessionPrincipal principal) {
        if (principal == null) return unauthorized();
        try {
            InsuranceApplication application = insuranceService.closePolicy(principal.userId(), applicationId);
            return ResponseEntity.ok(Map.of("success", true, "application", application,
                    "message", "Insurance policy closed"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // Simple policy certificate "PDF" download
    @GetMapping("/insurance/applications/{applicationId}/certificate")
    public ResponseEntity<?> downloadCertificate(
            @PathVariable Long applicationId,
            @AuthenticationPrincipal UserSessionTokenService.SessionPrincipal principal) {
        if (principal == null) return unauthorized();
        if (!ownsApplication(principal, applicationId)) return forbidden();
        byte[] pdfBytes = insuranceService.generatePolicyCertificatePdf(applicationId);
        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=policy-certificate-" + applicationId + ".pdf")
                .body(pdfBytes);
    }

    // Lookup policy number and return linked application and user email (if any)
    @GetMapping("/insurance/policy/lookup/{policyNumber}")
    public ResponseEntity<?> lookupPolicy(
            @PathVariable String policyNumber,
            @AuthenticationPrincipal UserSessionTokenService.SessionPrincipal principal) {
        try {
            Map<String, Object> data = insuranceService.lookupPolicyWithCustomer(policyNumber);
            InsuranceApplication application = (InsuranceApplication) data.get("application");
            if (application != null) {
                boolean currentUserOwnsApplication = isBankInsurancePrincipal(principal)
                        && principal.userId().equals(application.getUserId());
                data.put("isAssigned", true);
                data.put("isAssignedToCurrentUser", currentUserOwnsApplication);
                if (!currentUserOwnsApplication) {
                    data.remove("application");
                    data.remove("userEmail");
                    data.remove("accountNumber");
                }
            } else {
                data.put("isAssigned", false);
            }
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

    private boolean ownsApplication(UserSessionTokenService.SessionPrincipal principal, Long applicationId) {
        return isBankInsurancePrincipal(principal)
                && insuranceService.getApplicationsForUser(principal.userId()).stream()
                .anyMatch(application -> applicationId.equals(application.getId()));
    }

    private boolean isBankInsurancePrincipal(UserSessionTokenService.SessionPrincipal principal) {
        return principal != null && "USER".equals(principal.scope());
    }

    private ResponseEntity<Map<String, Object>> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("success", false, "message", "Insurance login is required"));
    }

    private ResponseEntity<Map<String, Object>> forbidden() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("success", false, "message", "This insurance record does not belong to your account"));
    }
}
