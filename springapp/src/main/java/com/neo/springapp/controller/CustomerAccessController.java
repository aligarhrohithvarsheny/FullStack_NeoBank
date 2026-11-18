package com.neo.springapp.controller;

import com.neo.springapp.service.CustomerAccessService;
import com.neo.springapp.service.UserSessionTokenService.SessionPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/customer-access")
public class CustomerAccessController {
    private final CustomerAccessService customerAccessService;

    public CustomerAccessController(CustomerAccessService customerAccessService) {
        this.customerAccessService = customerAccessService;
    }

    @PostMapping("/activation-codes")
    public ResponseEntity<?> issueActivationCode(@RequestBody(required = false) Map<String, String> request,
                                                 Authentication authentication) {
        SessionPrincipal admin = adminPrincipal(authentication);
        if (admin == null) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Admin access is required."));
        try {
            if (request == null) throw new IllegalArgumentException("Enter an account type and customer ID.");
            CustomerAccessService.IssuedCode issued = customerAccessService.issueActivationCode(
                    request.get("accountType"), request.get("customerId"), admin.accountNumber());
            return ResponseEntity.ok(Map.of(
                    "activationCode", issued.activationCode(),
                    "accountType", issued.accountType(),
                    "expiresAt", issued.expiresAt()));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
        }
    }

    @GetMapping("/activation-codes/history")
    public ResponseEntity<?> getActivationCodeHistory(Authentication authentication) {
        if (adminPrincipal(authentication) == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Admin access is required."));
        }
        return ResponseEntity.ok(customerAccessService.getActivationCodeHistory());
    }

    @PostMapping("/registration/verify-code")
    public ResponseEntity<?> verifyCode(@RequestBody(required = false) Map<String, String> request) {
        try {
            if (request == null) throw new IllegalArgumentException("Registration could not be verified.");
            customerAccessService.verifyActivationCode(request.get("customerId"), request.get("activationCode"));
            return ResponseEntity.ok(Map.of("success", true));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", exception.getMessage()));
        }
    }

    @PostMapping("/registration/verify-details")
    public ResponseEntity<?> verifyDetails(@RequestBody(required = false) Map<String, String> request) {
        try {
            if (request == null) throw new IllegalArgumentException("Registration could not be verified.");
            customerAccessService.verifyRegistrationDetails(request.get("customerId"), request.get("email"),
                    request.get("aadhaar"), request.get("activationCode"));
            return ResponseEntity.ok(Map.of("success", true));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", exception.getMessage()));
        }
    }

    @PostMapping("/registration/activate")
    public ResponseEntity<?> activate(@RequestBody(required = false) CustomerAccessService.ActivationRequest request) {
        try {
            return ResponseEntity.ok(customerAccessService.activateOnlineAccess(request));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", exception.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody(required = false) Map<String, String> request) {
        if (request == null) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Customer ID and password are required."));
        return ResponseEntity.ok(customerAccessService.authenticate(request.get("customerId"), request.get("password")));
    }

    private SessionPrincipal adminPrincipal(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof SessionPrincipal principal)
                || !"ADMIN".equals(principal.scope())) return null;
        return principal;
    }
}
