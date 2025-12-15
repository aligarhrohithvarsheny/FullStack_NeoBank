package com.neo.springapp.controller;

import com.neo.springapp.model.FixedDeposit;
import com.neo.springapp.service.FixedDepositService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/fixed-deposits")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:3000"})
public class FixedDepositController {

    @Autowired
    private FixedDepositService fixedDepositService;

    // Create new FD application
    @PostMapping
    public ResponseEntity<Map<String, Object>> createFixedDeposit(@RequestBody FixedDeposit fixedDeposit) {
        Map<String, Object> response = fixedDepositService.createFixedDeposit(fixedDeposit);
        return ResponseEntity.ok(response);
    }

    // Get all FDs (admin/manager)
    @GetMapping
    public ResponseEntity<List<FixedDeposit>> getAllFixedDeposits() {
        List<FixedDeposit> fixedDeposits = fixedDepositService.getAllFixedDeposits();
        return ResponseEntity.ok(fixedDeposits);
    }

    // Get FDs by account number (user)
    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<List<FixedDeposit>> getFixedDepositsByAccountNumber(@PathVariable String accountNumber) {
        List<FixedDeposit> fixedDeposits = fixedDepositService.getFixedDepositsByAccountNumber(accountNumber);
        return ResponseEntity.ok(fixedDeposits);
    }

    // Get FD by ID
    @GetMapping("/{id}")
    public ResponseEntity<FixedDeposit> getFixedDepositById(@PathVariable Long id) {
        return fixedDepositService.getFixedDepositById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    // Get FD by FD account number
    @GetMapping("/fd-account/{fdAccountNumber}")
    public ResponseEntity<FixedDeposit> getFixedDepositByFdAccountNumber(@PathVariable String fdAccountNumber) {
        return fixedDepositService.getFixedDepositByFdAccountNumber(fdAccountNumber)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    // Get pending FDs (admin)
    @GetMapping("/pending")
    public ResponseEntity<List<FixedDeposit>> getPendingFixedDeposits() {
        List<FixedDeposit> fixedDeposits = fixedDepositService.getPendingFixedDeposits();
        return ResponseEntity.ok(fixedDeposits);
    }

    // Get FDs by status
    @GetMapping("/status/{status}")
    public ResponseEntity<List<FixedDeposit>> getFixedDepositsByStatus(@PathVariable String status) {
        List<FixedDeposit> fixedDeposits = fixedDepositService.getFixedDepositsByStatus(status);
        return ResponseEntity.ok(fixedDeposits);
    }

    // Get matured FDs that need processing
    @GetMapping("/matured")
    public ResponseEntity<List<FixedDeposit>> getMaturedFixedDeposits() {
        List<FixedDeposit> fixedDeposits = fixedDepositService.getMaturedFixedDeposits();
        return ResponseEntity.ok(fixedDeposits);
    }

    // Approve FD (admin)
    @PutMapping("/{id}/approve")
    public ResponseEntity<Map<String, Object>> approveFixedDeposit(
            @PathVariable Long id,
            @RequestParam String approvedBy) {
        Map<String, Object> response = fixedDepositService.approveFixedDeposit(id, approvedBy);
        return ResponseEntity.ok(response);
    }

    // Reject FD (admin)
    @PutMapping("/{id}/reject")
    public ResponseEntity<Map<String, Object>> rejectFixedDeposit(
            @PathVariable Long id,
            @RequestParam String rejectedBy,
            @RequestParam(required = false) String reason) {
        Map<String, Object> response = fixedDepositService.rejectFixedDeposit(id, rejectedBy, reason);
        return ResponseEntity.ok(response);
    }

    // Process FD maturity (admin)
    @PutMapping("/{id}/process-maturity")
    public ResponseEntity<Map<String, Object>> processMaturity(
            @PathVariable Long id,
            @RequestParam String processedBy) {
        Map<String, Object> response = fixedDepositService.processMaturity(id, processedBy);
        return ResponseEntity.ok(response);
    }

    // Update FD (manager/admin)
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateFixedDeposit(
            @PathVariable Long id,
            @RequestBody FixedDeposit fdDetails) {
        Map<String, Object> response = fixedDepositService.updateFixedDeposit(id, fdDetails);
        return ResponseEntity.ok(response);
    }

    // Get FD history (all FDs for an account or all FDs)
    @GetMapping("/history")
    public ResponseEntity<List<FixedDeposit>> getFDHistory(
            @RequestParam(required = false) String accountNumber) {
        List<FixedDeposit> history = fixedDepositService.getFDHistory(accountNumber);
        return ResponseEntity.ok(history);
    }

    // Get FD history by status
    @GetMapping("/history/status/{status}")
    public ResponseEntity<List<FixedDeposit>> getFDHistoryByStatus(
            @PathVariable String status,
            @RequestParam(required = false) String accountNumber) {
        List<FixedDeposit> history = fixedDepositService.getFDHistoryByStatus(accountNumber, status);
        return ResponseEntity.ok(history);
    }

    // Request FD withdrawal with cheque deposit (user)
    @PostMapping("/{id}/withdrawal/request")
    public ResponseEntity<Map<String, Object>> requestWithdrawal(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        Double withdrawalAmount = ((Number) request.get("withdrawalAmount")).doubleValue();
        String requestedBy = (String) request.get("requestedBy");
        Map<String, Object> response = fixedDepositService.requestWithdrawal(id, withdrawalAmount, requestedBy);
        return ResponseEntity.ok(response);
    }

    // Approve withdrawal request and process cheque deposit (admin)
    @PutMapping("/{id}/withdrawal/approve")
    public ResponseEntity<Map<String, Object>> approveWithdrawal(
            @PathVariable Long id,
            @RequestParam String chequeNumber,
            @RequestParam String approvedBy) {
        Map<String, Object> response = fixedDepositService.approveWithdrawal(id, chequeNumber, approvedBy);
        return ResponseEntity.ok(response);
    }

    // Reject withdrawal request (admin)
    @PutMapping("/{id}/withdrawal/reject")
    public ResponseEntity<Map<String, Object>> rejectWithdrawal(
            @PathVariable Long id,
            @RequestParam String rejectedBy,
            @RequestParam(required = false) String reason) {
        Map<String, Object> response = fixedDepositService.rejectWithdrawal(id, rejectedBy, reason);
        return ResponseEntity.ok(response);
    }

    // Process monthly interest credit for a specific FD (admin/manual trigger)
    @PostMapping("/{id}/interest/credit")
    public ResponseEntity<Map<String, Object>> processMonthlyInterest(
            @PathVariable Long id) {
        Map<String, Object> response = fixedDepositService.processMonthlyInterestCredit(id);
        return ResponseEntity.ok(response);
    }

    // Process monthly interest credit for all active FDs (admin/manual trigger)
    @PostMapping("/interest/credit-all")
    public ResponseEntity<Map<String, Object>> processAllMonthlyInterest() {
        Map<String, Object> response = fixedDepositService.processAllMonthlyInterestCredits();
        return ResponseEntity.ok(response);
    }
}

