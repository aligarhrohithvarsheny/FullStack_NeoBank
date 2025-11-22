package com.neo.springapp.controller;

import com.neo.springapp.model.EmiPayment;
import com.neo.springapp.model.Loan;
import com.neo.springapp.service.EmiService;
import com.neo.springapp.service.LoanService;
import com.neo.springapp.service.PdfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/emis")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:3000", "http://frontend:80"})
public class EmiController {

    @Autowired
    private EmiService emiService;

    @Autowired
    private LoanService loanService;

    @Autowired
    private PdfService pdfService;

    /**
     * Generate EMI schedule for a loan
     */
    @PostMapping("/generate/{loanId}")
    public ResponseEntity<Map<String, Object>> generateEmiSchedule(@PathVariable Long loanId) {
        try {
            Loan loan = loanService.getLoanById(loanId);
            if (loan == null) {
                Map<String, Object> errorResponse = new java.util.HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Loan not found");
                return ResponseEntity.notFound().build();
            }

            if (!"Approved".equals(loan.getStatus())) {
                Map<String, Object> errorResponse = new java.util.HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "EMI schedule can only be generated for approved loans");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            List<EmiPayment> emiSchedule = emiService.generateEmiSchedule(loan);
            
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", true);
            response.put("message", "EMI schedule generated successfully");
            response.put("emiSchedule", emiSchedule);
            response.put("totalEmis", emiSchedule.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new java.util.HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error generating EMI schedule: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Pay a specific EMI
     */
    @PostMapping("/pay/{emiId}")
    public ResponseEntity<Map<String, Object>> payEmi(
            @PathVariable Long emiId,
            @RequestParam String accountNumber) {
        try {
            Map<String, Object> result = emiService.payEmi(emiId, accountNumber);
            
            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        } catch (Exception e) {
            Map<String, Object> errorResponse = new java.util.HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error processing EMI payment: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Get all EMIs for a loan
     */
    @GetMapping("/loan/{loanId}")
    public ResponseEntity<List<EmiPayment>> getEmisByLoanId(@PathVariable Long loanId) {
        List<EmiPayment> emis = emiService.getEmisByLoanId(loanId);
        return ResponseEntity.ok(emis);
    }

    /**
     * Get all EMIs for a loan account number
     */
    @GetMapping("/loan-account/{loanAccountNumber}")
    public ResponseEntity<List<EmiPayment>> getEmisByLoanAccountNumber(@PathVariable String loanAccountNumber) {
        List<EmiPayment> emis = emiService.getEmisByLoanAccountNumber(loanAccountNumber);
        return ResponseEntity.ok(emis);
    }

    /**
     * Get all EMIs for user's account
     */
    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<List<EmiPayment>> getEmisByAccountNumber(@PathVariable String accountNumber) {
        List<EmiPayment> emis = emiService.getEmisByAccountNumber(accountNumber);
        return ResponseEntity.ok(emis);
    }

    /**
     * Get pending EMIs for a loan
     */
    @GetMapping("/loan/{loanId}/pending")
    public ResponseEntity<List<EmiPayment>> getPendingEmisByLoanId(@PathVariable Long loanId) {
        List<EmiPayment> emis = emiService.getPendingEmisByLoanId(loanId);
        return ResponseEntity.ok(emis);
    }

    /**
     * Get next due EMI for a loan
     */
    @GetMapping("/loan/{loanId}/next-due")
    public ResponseEntity<EmiPayment> getNextDueEmi(@PathVariable Long loanId) {
        EmiPayment emi = emiService.getNextDueEmi(loanId);
        if (emi != null) {
            return ResponseEntity.ok(emi);
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Get EMI details with loan information
     */
    @GetMapping("/{emiId}")
    public ResponseEntity<Map<String, Object>> getEmiDetails(@PathVariable Long emiId) {
        try {
            Map<String, Object> details = emiService.getEmiDetails(emiId);
            return ResponseEntity.ok(details);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new java.util.HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "EMI not found: " + e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get overdue EMIs
     */
    @GetMapping("/overdue")
    public ResponseEntity<List<EmiPayment>> getOverdueEmis() {
        List<EmiPayment> emis = emiService.getOverdueEmis();
        return ResponseEntity.ok(emis);
    }

    /**
     * Get upcoming EMIs (due in next 7 days)
     */
    @GetMapping("/upcoming")
    public ResponseEntity<List<EmiPayment>> getUpcomingEmis() {
        List<EmiPayment> emis = emiService.getUpcomingEmis();
        return ResponseEntity.ok(emis);
    }

    /**
     * Download EMI receipt as PDF
     */
    @GetMapping("/{emiId}/receipt")
    public ResponseEntity<byte[]> downloadEmiReceipt(@PathVariable Long emiId) {
        try {
            EmiPayment emi = emiService.getEmiDetails(emiId).get("emi") != null ? 
                (EmiPayment) emiService.getEmiDetails(emiId).get("emi") : null;
            
            if (emi == null || !"Paid".equals(emi.getStatus())) {
                return ResponseEntity.notFound().build();
            }

            Loan loan = loanService.getLoanById(emi.getLoanId());
            if (loan == null) {
                return ResponseEntity.notFound().build();
            }

            byte[] pdfBytes = pdfService.generateEmiReceipt(emi, loan);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", 
                "EMI_Receipt_" + emi.getLoanAccountNumber() + "_" + emi.getEmiNumber() + ".pdf");
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);
                    
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Send EMI reminders (can be called by scheduler)
     */
    @PostMapping("/send-reminders")
    public ResponseEntity<Map<String, Object>> sendEmiReminders() {
        try {
            emiService.sendEmiReminders();
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", true);
            response.put("message", "EMI reminders sent successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new java.util.HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error sending reminders: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}

