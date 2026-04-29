package com.neo.springapp.controller;

import com.neo.springapp.model.Investment;
import com.neo.springapp.model.MutualFundForeclosure;
import com.neo.springapp.service.InvestmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/investments")
public class InvestmentController {

    @Autowired
    private InvestmentService investmentService;

    // Create new investment application
    @PostMapping
    public ResponseEntity<Map<String, Object>> createInvestment(@RequestBody Investment investment) {
        Map<String, Object> response = investmentService.createInvestment(investment);
        return ResponseEntity.ok(response);
    }

    // Get all investments (admin/manager)
    @GetMapping
    public ResponseEntity<List<Investment>> getAllInvestments() {
        List<Investment> investments = investmentService.getAllInvestments();
        return ResponseEntity.ok(investments);
    }

    // Get investments by account number (user)
    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<List<Investment>> getInvestmentsByAccountNumber(@PathVariable String accountNumber) {
        List<Investment> investments = investmentService.getInvestmentsByAccountNumber(accountNumber);
        return ResponseEntity.ok(investments);
    }

    // Get investment by ID
    @GetMapping("/{id}")
    public ResponseEntity<Investment> getInvestmentById(@PathVariable Long id) {
        return investmentService.getInvestmentById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    // Get pending investments (admin)
    @GetMapping("/pending")
    public ResponseEntity<List<Investment>> getPendingInvestments() {
        List<Investment> investments = investmentService.getPendingInvestments();
        return ResponseEntity.ok(investments);
    }

    // Get investments by status
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Investment>> getInvestmentsByStatus(@PathVariable String status) {
        List<Investment> investments = investmentService.getInvestmentsByStatus(status);
        return ResponseEntity.ok(investments);
    }

    // Approve investment (admin)
    @PutMapping("/{id}/approve")
    public ResponseEntity<Map<String, Object>> approveInvestment(
            @PathVariable Long id,
            @RequestParam String approvedBy) {
        Map<String, Object> response = investmentService.approveInvestment(id, approvedBy);
        return ResponseEntity.ok(response);
    }

    // Reject investment (admin)
    @PutMapping("/{id}/reject")
    public ResponseEntity<Map<String, Object>> rejectInvestment(
            @PathVariable Long id,
            @RequestParam String rejectedBy,
            @RequestParam(required = false) String reason) {
        Map<String, Object> response = investmentService.rejectInvestment(id, rejectedBy, reason);
        return ResponseEntity.ok(response);
    }

    // Update investment (manager/admin)
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateInvestment(
            @PathVariable Long id,
            @RequestBody Investment investmentDetails) {
        Map<String, Object> response = investmentService.updateInvestment(id, investmentDetails);
        return ResponseEntity.ok(response);
    }

    // ========== FORECLOSURE ENDPOINTS ==========

    // Calculate foreclosure details
    @GetMapping("/foreclosure/calculate/{investmentId}")
    public ResponseEntity<Map<String, Object>> calculateForeclosure(@PathVariable Long investmentId) {
        Map<String, Object> response = investmentService.calculateForeclosure(investmentId);
        return ResponseEntity.ok(response);
    }

    // Send OTP to registered email for foreclosure (reason included in email)
    @PostMapping("/foreclosure/request-otp")
    public ResponseEntity<Map<String, Object>> requestForeclosureOtp(
            @RequestParam Long investmentId,
            @RequestParam(required = false) String requestReason) {
        Map<String, Object> response = investmentService.requestForeclosureOtp(investmentId, requestReason == null ? "" : requestReason);
        return ResponseEntity.ok(response);
    }

    // Request foreclosure (user) – requires OTP from request-otp
    @PostMapping("/foreclosure/request")
    public ResponseEntity<Map<String, Object>> requestForeclosure(
            @RequestParam Long investmentId,
            @RequestParam(required = false) String requestReason,
            @RequestParam String otp) {
        Map<String, Object> response = investmentService.requestForeclosure(investmentId, requestReason == null ? "" : requestReason, otp);
        return ResponseEntity.ok(response);
    }

    // Approve foreclosure (admin)
    @PutMapping("/foreclosure/{foreclosureId}/approve")
    public ResponseEntity<Map<String, Object>> approveForeclosure(
            @PathVariable Long foreclosureId,
            @RequestParam String approvedBy) {
        Map<String, Object> response = investmentService.approveForeclosure(foreclosureId, approvedBy);
        return ResponseEntity.ok(response);
    }

    // Reject foreclosure (admin)
    @PutMapping("/foreclosure/{foreclosureId}/reject")
    public ResponseEntity<Map<String, Object>> rejectForeclosure(
            @PathVariable Long foreclosureId,
            @RequestParam String rejectedBy,
            @RequestParam(required = false) String reason) {
        Map<String, Object> response = investmentService.rejectForeclosure(foreclosureId, rejectedBy, reason);
        return ResponseEntity.ok(response);
    }

    // Get foreclosure history by account (user)
    @GetMapping("/foreclosure/account/{accountNumber}")
    public ResponseEntity<List<MutualFundForeclosure>> getForeclosureHistoryByAccount(
            @PathVariable String accountNumber) {
        List<MutualFundForeclosure> foreclosures = investmentService.getForeclosureHistoryByAccount(accountNumber);
        return ResponseEntity.ok(foreclosures);
    }

    // Get all pending foreclosures (admin)
    @GetMapping("/foreclosure/pending")
    public ResponseEntity<List<MutualFundForeclosure>> getPendingForeclosures() {
        List<MutualFundForeclosure> foreclosures = investmentService.getPendingForeclosures();
        return ResponseEntity.ok(foreclosures);
    }

    // Get all foreclosures (admin)
    @GetMapping("/foreclosure/all")
    public ResponseEntity<List<MutualFundForeclosure>> getAllForeclosures() {
        List<MutualFundForeclosure> foreclosures = investmentService.getAllForeclosures();
        return ResponseEntity.ok(foreclosures);
    }

    // Get foreclosure by ID
    @GetMapping("/foreclosure/{id}")
    public ResponseEntity<MutualFundForeclosure> getForeclosureById(@PathVariable Long id) {
        Optional<MutualFundForeclosure> foreclosure = investmentService.getForeclosureById(id);
        return foreclosure.map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}

