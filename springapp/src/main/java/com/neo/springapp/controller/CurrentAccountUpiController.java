package com.neo.springapp.controller;

import com.neo.springapp.model.CurrentAccountUpiPayment;
import com.neo.springapp.service.CurrentAccountUpiService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/current-accounts/upi")
public class CurrentAccountUpiController {

    private final CurrentAccountUpiService upiService;

    public CurrentAccountUpiController(CurrentAccountUpiService upiService) {
        this.upiService = upiService;
    }

    // ==================== UPI ID Setup ====================

    @PostMapping("/setup/{accountNumber}")
    public ResponseEntity<Map<String, Object>> setupUpi(
            @PathVariable String accountNumber,
            @RequestParam(required = false) String upiId) {
        try {
            Map<String, Object> result = upiService.setupUpiId(accountNumber, upiId);
            if (Boolean.TRUE.equals(result.get("success"))) {
                result.put("message", "UPI ID set up successfully");
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        } catch (RuntimeException e) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @PutMapping("/update/{accountNumber}")
    public ResponseEntity<Map<String, Object>> updateUpi(
            @PathVariable String accountNumber,
            @RequestParam String upiId) {
        try {
            Map<String, Object> result = upiService.updateUpiId(accountNumber, upiId);
            if (Boolean.TRUE.equals(result.get("success"))) {
                result.put("message", "UPI ID updated successfully");
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        } catch (RuntimeException e) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @PutMapping("/toggle/{accountNumber}")
    public ResponseEntity<Map<String, Object>> toggleUpi(@PathVariable String accountNumber) {
        Map<String, Object> response = new HashMap<>();
        try {
            var account = upiService.toggleUpi(accountNumber);
            response.put("success", true);
            response.put("upiEnabled", account.getUpiEnabled());
            response.put("message", "UPI " + (Boolean.TRUE.equals(account.getUpiEnabled()) ? "enabled" : "disabled"));
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ==================== QR Code Generation ====================

    @GetMapping("/qrcode/{accountNumber}")
    public ResponseEntity<Map<String, Object>> generateQrCode(
            @PathVariable String accountNumber,
            @RequestParam(required = false) Double amount) {
        try {
            Map<String, Object> result = upiService.generateQrCode(accountNumber, amount);
            result.put("success", true);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // ==================== Payment Processing ====================

    @PostMapping("/payment/process")
    public ResponseEntity<Map<String, Object>> processPayment(@RequestBody CurrentAccountUpiPayment payment) {
        try {
            Map<String, Object> result = upiService.processUpiPayment(payment);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // ==================== Transaction History ====================

    @GetMapping("/payments/{accountNumber}")
    public ResponseEntity<List<CurrentAccountUpiPayment>> getPayments(@PathVariable String accountNumber) {
        return ResponseEntity.ok(upiService.getPaymentsByAccount(accountNumber));
    }

    @GetMapping("/payments/{accountNumber}/paginated")
    public ResponseEntity<Page<CurrentAccountUpiPayment>> getPaymentsPaginated(
            @PathVariable String accountNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(upiService.getPaymentsPaginated(accountNumber, page, size));
    }

    // ==================== Stats ====================

    @GetMapping("/stats/user/{accountNumber}")
    public ResponseEntity<Map<String, Object>> getUserStats(@PathVariable String accountNumber) {
        try {
            return ResponseEntity.ok(upiService.getUserUpiStats(accountNumber));
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/stats/admin")
    public ResponseEntity<Map<String, Object>> getAdminStats() {
        return ResponseEntity.ok(upiService.getAdminUpiStats());
    }

    @GetMapping("/payments/recent")
    public ResponseEntity<List<CurrentAccountUpiPayment>> getRecentPayments() {
        return ResponseEntity.ok(upiService.getRecentPayments());
    }

    // ==================== UPI Verification ====================

    @GetMapping("/verify/{upiId}")
    public ResponseEntity<Map<String, Object>> verifyUpiId(@PathVariable String upiId) {
        Map<String, Object> result = upiService.verifyUpiId(upiId);
        return ResponseEntity.ok(result);
    }

    // ==================== QR Code for Linked UPI ====================

    @GetMapping("/qrcode/generate")
    public ResponseEntity<Map<String, Object>> generateQrCodeForUpi(
            @RequestParam String upiId,
            @RequestParam String accountNumber,
            @RequestParam(required = false) Double amount) {
        try {
            Map<String, Object> result = upiService.generateQrCodeForUpi(upiId, amount, accountNumber);
            result.put("success", true);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // ==================== Search by Transaction ID ====================

    @GetMapping("/search/txn/{txnId}")
    public ResponseEntity<Map<String, Object>> searchByTxnId(@PathVariable String txnId) {
        Map<String, Object> result = upiService.searchByTxnId(txnId);
        return ResponseEntity.ok(result);
    }

    // ==================== Cross-Customer UPI Payment ====================

    @PostMapping("/payment/cross-pay")
    public ResponseEntity<Map<String, Object>> crossCustomerPayment(@RequestBody Map<String, Object> request) {
        try {
            String senderAccount = (String) request.get("senderAccountNumber");
            String receiverUpiId = (String) request.get("receiverUpiId");
            Double amount = Double.parseDouble(request.get("amount").toString());
            String note = (String) request.getOrDefault("note", "");
            Map<String, Object> result = upiService.processCrossCustomerPayment(senderAccount, receiverUpiId, amount, note);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
