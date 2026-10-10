package com.neo.springapp.controller;

import com.neo.springapp.service.AccountRecoveryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/account-recovery")
public class AccountRecoveryController {
    private static final String VERIFICATION_ERROR =
            "We couldn't verify those details. Check them and try again.";
    private final AccountRecoveryService accountRecoveryService;

    public AccountRecoveryController(AccountRecoveryService accountRecoveryService) {
        this.accountRecoveryService = accountRecoveryService;
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verify(@RequestBody(required = false) Map<String, String> request) {
        if (request == null || !hasIdentityFields(request)) {
            return ResponseEntity.badRequest().body(error(VERIFICATION_ERROR));
        }

        Optional<String> maskedName = accountRecoveryService.verify(
                request.get("accountType"), request.get("customerId"), request.get("accountNumber"), request.get("dob"));
        if (maskedName.isEmpty()) return ResponseEntity.badRequest().body(error(VERIFICATION_ERROR));

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("maskedName", maskedName.get());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(@RequestBody(required = false) Map<String, String> request) {
        if (request == null || !hasIdentityFields(request)) {
            return ResponseEntity.badRequest().body(error(VERIFICATION_ERROR));
        }
        String newPassword = request.get("newPassword");
        if (newPassword == null || newPassword.length() < 8 || newPassword.length() > 128) {
            return ResponseEntity.badRequest().body(error("Password must be between 8 and 128 characters."));
        }

        boolean reset = accountRecoveryService.resetPassword(
                request.get("accountType"), request.get("customerId"), request.get("accountNumber"),
                request.get("dob"), newPassword);
        if (!reset) return ResponseEntity.badRequest().body(error(VERIFICATION_ERROR));

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Password reset successfully. You can now log in.");
        return ResponseEntity.ok(response);
    }

    private static boolean hasIdentityFields(Map<String, String> request) {
        return hasText(request.get("accountType")) && hasText(request.get("customerId"))
                && hasText(request.get("accountNumber")) && hasText(request.get("dob"));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static Map<String, Object> error(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }
}
