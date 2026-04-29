package com.neo.springapp.controller;

import com.neo.springapp.model.SavingsUpiTransaction;
import com.neo.springapp.service.SavingsUpiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/savings-upi")
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"})
public class SavingsUpiController {

    @Autowired
    private SavingsUpiService savingsUpiService;

    // ── Enable UPI for a savings account ──────────────────────────────────────
    @PostMapping("/setup/{accountNumber}")
    public ResponseEntity<Map<String, Object>> setupUpi(@PathVariable String accountNumber) {
        return ResponseEntity.ok(savingsUpiService.setupUpi(accountNumber));
    }

    // ── Toggle UPI on/off ─────────────────────────────────────────────────────
    @PutMapping("/toggle/{accountNumber}")
    public ResponseEntity<Map<String, Object>> toggleUpi(@PathVariable String accountNumber) {
        return ResponseEntity.ok(savingsUpiService.toggleUpi(accountNumber));
    }

    // ── Set Transaction PIN ───────────────────────────────────────────────────
    @PostMapping("/set-pin/{accountNumber}")
    public ResponseEntity<Map<String, Object>> setPin(@PathVariable String accountNumber,
                                                       @RequestBody Map<String, String> body) {
        String pin = body.get("pin");
        return ResponseEntity.ok(savingsUpiService.setTransactionPin(accountNumber, pin));
    }

    // ── Verify a UPI ID (any account type) ────────────────────────────────────
    @GetMapping("/verify/{upiId}")
    public ResponseEntity<Map<String, Object>> verifyUpiId(@PathVariable String upiId) {
        return ResponseEntity.ok(savingsUpiService.verifyUpiId(upiId));
    }

    // ── Send money ────────────────────────────────────────────────────────────
    @PostMapping("/send/{accountNumber}")
    public ResponseEntity<Map<String, Object>> sendMoney(@PathVariable String accountNumber,
                                                          @RequestBody Map<String, Object> body) {
        String receiverUpiId = (String) body.get("receiverUpiId");
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        // Accept both 'transactionPin' (frontend) and 'pin' (legacy) field names
        String pin = body.containsKey("transactionPin")
                ? (String) body.get("transactionPin")
                : (String) body.get("pin");
        String remark = (String) body.getOrDefault("remark", "");
        return ResponseEntity.ok(savingsUpiService.sendMoney(accountNumber, receiverUpiId, amount, pin, remark));
    }

    // ── Forgot UPI PIN (reset via registered phone) ───────────────────────────
    @PostMapping("/forgot-pin/{accountNumber}")
    public ResponseEntity<Map<String, Object>> forgotPin(@PathVariable String accountNumber,
                                                          @RequestBody Map<String, String> body) {
        String phone = body.get("registeredPhone");
        String newPin = body.get("newPin");
        return ResponseEntity.ok(savingsUpiService.forgotPin(accountNumber, phone, newPin));
    }

    // ── Latest transactions after timestamp (real-time polling) ──────────────
    @GetMapping("/latest-transactions/{accountNumber}")
    public ResponseEntity<List<SavingsUpiTransaction>> latestTransactions(
            @PathVariable String accountNumber,
            @RequestParam(defaultValue = "0") long afterEpochMs) {
        java.time.LocalDateTime after = afterEpochMs > 0
                ? java.time.Instant.ofEpochMilli(afterEpochMs)
                        .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
                : java.time.LocalDateTime.now().minusSeconds(10);
        return ResponseEntity.ok(savingsUpiService.getLatestTransactions(accountNumber, after));
    }

    // ── Transaction history ───────────────────────────────────────────────────
    @GetMapping("/transactions/{accountNumber}")
    public ResponseEntity<List<SavingsUpiTransaction>> getTransactions(@PathVariable String accountNumber) {
        return ResponseEntity.ok(savingsUpiService.getTransactions(accountNumber));
    }

    // ── UPI status & info ─────────────────────────────────────────────────────
    @GetMapping("/status/{accountNumber}")
    public ResponseEntity<Map<String, Object>> getUpiStatus(@PathVariable String accountNumber) {
        return ResponseEntity.ok(savingsUpiService.getUpiStatus(accountNumber));
    }

    // ── Admin endpoints ───────────────────────────────────────────────────────
    @GetMapping("/admin/accounts")
    public ResponseEntity<List<Map<String, Object>>> adminListAccounts() {
        return ResponseEntity.ok(savingsUpiService.adminListAllUpiAccounts());
    }

    @PutMapping("/admin/toggle/{accountNumber}")
    public ResponseEntity<Map<String, Object>> adminToggle(@PathVariable String accountNumber,
                                                            @RequestParam boolean enable) {
        return ResponseEntity.ok(savingsUpiService.adminToggleUpi(accountNumber, enable));
    }

    @GetMapping("/admin/transactions")
    public ResponseEntity<List<SavingsUpiTransaction>> adminTransactions() {
        return ResponseEntity.ok(savingsUpiService.adminListAllTransactions());
    }

    @GetMapping("/admin/flagged")
    public ResponseEntity<List<SavingsUpiTransaction>> adminFlagged() {
        return ResponseEntity.ok(savingsUpiService.adminListFlaggedTransactions());
    }
}
