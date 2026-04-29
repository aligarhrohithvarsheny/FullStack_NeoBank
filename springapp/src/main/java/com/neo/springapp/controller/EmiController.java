package com.neo.springapp.controller;

import com.neo.springapp.model.EmiPayment;
import com.neo.springapp.service.EmiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/emis")
public class EmiController {

    @Autowired
    private EmiService emiService;

    // Get all EMIs by account number (user)
    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<List<EmiPayment>> getEmisByAccountNumber(@PathVariable String accountNumber) {
        List<EmiPayment> emis = emiService.getEmisByAccountNumber(accountNumber);
        return ResponseEntity.ok(emis);
    }

    // Get EMIs by loan ID
    @GetMapping("/loan/{loanId}")
    public ResponseEntity<List<EmiPayment>> getEmisByLoanId(@PathVariable Long loanId) {
        List<EmiPayment> emis = emiService.getEmisByLoanId(loanId);
        return ResponseEntity.ok(emis);
    }

    // Get EMIs by loan account number
    @GetMapping("/loan-account/{loanAccountNumber}")
    public ResponseEntity<List<EmiPayment>> getEmisByLoanAccountNumber(@PathVariable String loanAccountNumber) {
        List<EmiPayment> emis = emiService.getEmisByLoanAccountNumber(loanAccountNumber);
        return ResponseEntity.ok(emis);
    }

    // Get EMI by ID
    @GetMapping("/{id}")
    public ResponseEntity<EmiPayment> getEmiById(@PathVariable Long id) {
        EmiPayment emi = emiService.getEmisByLoanId(id).stream()
            .filter(e -> e.getId().equals(id))
            .findFirst()
            .orElse(null);
        
        if (emi != null) {
            return ResponseEntity.ok(emi);
        }
        return ResponseEntity.notFound().build();
    }

    // Get EMI details with loan information
    @GetMapping("/{id}/details")
    public ResponseEntity<Map<String, Object>> getEmiDetails(@PathVariable Long id) {
        Map<String, Object> details = emiService.getEmiDetails(id);
        return ResponseEntity.ok(details);
    }

    // Pay EMI (user)
    @PostMapping("/{id}/pay")
    public ResponseEntity<Map<String, Object>> payEmi(
            @PathVariable Long id,
            @RequestParam String accountNumber) {
        Map<String, Object> response = emiService.payEmi(id, accountNumber);
        return ResponseEntity.ok(response);
    }

    // Get pending EMIs for a loan
    @GetMapping("/loan/{loanId}/pending")
    public ResponseEntity<List<EmiPayment>> getPendingEmisByLoanId(@PathVariable Long loanId) {
        List<EmiPayment> emis = emiService.getPendingEmisByLoanId(loanId);
        return ResponseEntity.ok(emis);
    }

    // Get overdue EMIs (admin)
    @GetMapping("/overdue")
    public ResponseEntity<List<EmiPayment>> getOverdueEmis() {
        List<EmiPayment> emis = emiService.getOverdueEmis();
        return ResponseEntity.ok(emis);
    }

    // Get upcoming EMIs (due in next 7 days)
    @GetMapping("/upcoming")
    public ResponseEntity<List<EmiPayment>> getUpcomingEmis() {
        List<EmiPayment> emis = emiService.getUpcomingEmis();
        return ResponseEntity.ok(emis);
    }

    // Get next due EMI for a loan
    @GetMapping("/loan/{loanId}/next-due")
    public ResponseEntity<EmiPayment> getNextDueEmi(@PathVariable Long loanId) {
        EmiPayment emi = emiService.getNextDueEmi(loanId);
        if (emi != null) {
            return ResponseEntity.ok(emi);
        }
        return ResponseEntity.notFound().build();
    }

    // Get all EMIs (admin/manager)
    @GetMapping
    public ResponseEntity<List<EmiPayment>> getAllEmis() {
        List<EmiPayment> allEmis = emiService.getAllEmis();
        return ResponseEntity.ok(allEmis);
    }

    // Get EMI summary for account (user dashboard)
    @GetMapping("/account/{accountNumber}/summary")
    public ResponseEntity<Map<String, Object>> getEmiSummary(@PathVariable String accountNumber) {
        List<EmiPayment> allEmis = emiService.getEmisByAccountNumber(accountNumber);
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalEmis", allEmis.size());
        summary.put("paidEmis", allEmis.stream().filter(e -> "Paid".equals(e.getStatus())).count());
        summary.put("pendingEmis", allEmis.stream().filter(e -> "Pending".equals(e.getStatus())).count());
        summary.put("overdueEmis", allEmis.stream()
            .filter(e -> "Pending".equals(e.getStatus()) && 
                        e.getDueDate().isBefore(java.time.LocalDate.now()))
            .count());
        
        double totalPendingAmount = allEmis.stream()
            .filter(e -> "Pending".equals(e.getStatus()))
            .mapToDouble(EmiPayment::getTotalAmount)
            .sum();
        summary.put("totalPendingAmount", totalPendingAmount);
        
        double nextDueAmount = allEmis.stream()
            .filter(e -> "Pending".equals(e.getStatus()))
            .min((e1, e2) -> e1.getDueDate().compareTo(e2.getDueDate()))
            .map(EmiPayment::getTotalAmount)
            .orElse(0.0);
        summary.put("nextDueAmount", nextDueAmount);
        
        return ResponseEntity.ok(summary);
    }
}
