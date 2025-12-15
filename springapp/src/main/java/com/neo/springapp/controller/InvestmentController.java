package com.neo.springapp.controller;

import com.neo.springapp.model.Investment;
import com.neo.springapp.service.InvestmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/investments")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:3000"})
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
}

