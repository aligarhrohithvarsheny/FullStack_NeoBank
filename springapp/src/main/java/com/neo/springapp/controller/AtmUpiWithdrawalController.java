package com.neo.springapp.controller;

import com.neo.springapp.service.AtmUpiWithdrawalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/atm/upi-withdrawals")
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"})
public class AtmUpiWithdrawalController {

    @Autowired private AtmUpiWithdrawalService withdrawalService;

    @PostMapping("/requests")
    public ResponseEntity<Map<String, Object>> createRequest(@RequestBody Map<String, Object> body) {
        try {
            Map<String, Object> result = withdrawalService.createRequest(
                    stringValue(body.get("atmId")),
                    stringValue(body.get("accountNumber")),
                    stringValue(body.get("accountType")),
                    numberValue(body.get("amount")));
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(error(ex.getMessage()));
        }
    }

    @GetMapping("/requests/{token}")
    public ResponseEntity<Map<String, Object>> getRequest(@PathVariable String token,
                                                          @RequestParam String accountNumber) {
        try {
            return ResponseEntity.ok(withdrawalService.getRequest(token, accountNumber));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(error(ex.getMessage()));
        }
    }

    @PostMapping("/requests/{token}/confirm")
    public ResponseEntity<Map<String, Object>> confirmRequest(@PathVariable String token,
                                                              @RequestBody Map<String, String> body) {
        try {
            Map<String, Object> result = withdrawalService.confirmRequest(
                    token, body.get("accountNumber"), body.get("pin"));
            return Boolean.TRUE.equals(result.get("success"))
                    ? ResponseEntity.ok(result) : ResponseEntity.badRequest().body(result);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(error(ex.getMessage()));
        }
    }

    @PostMapping("/requests/{token}/cancel")
    public ResponseEntity<Map<String, Object>> cancelRequest(@PathVariable String token,
                                                             @RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(withdrawalService.cancelRequest(token, body.get("accountNumber")));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(error(ex.getMessage()));
        }
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private Double numberValue(Object value) {
        if (value instanceof Number number) return number.doubleValue();
        if (value instanceof String text) {
            try {
                return Double.valueOf(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private Map<String, Object> error(String message) {
        return Map.of("success", false, "message", message);
    }
}
