package com.neo.springapp.controller;

import com.neo.springapp.model.GoldLoan;
import com.neo.springapp.model.GoldRate;
import com.neo.springapp.model.Transaction;
import com.neo.springapp.service.GoldLoanService;
import com.neo.springapp.service.GoldRateService;
import com.neo.springapp.service.AccountService;
import com.neo.springapp.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/gold-loans")
@CrossOrigin(origins = "http://localhost:4200")
public class GoldLoanController {

    @Autowired
    private GoldLoanService goldLoanService;
    
    @Autowired
    private GoldRateService goldRateService;
    
    @Autowired
    private AccountService accountService;
    
    @Autowired
    private TransactionService transactionService;

    // Apply for gold loan
    @PostMapping
    public ResponseEntity<Map<String, Object>> applyGoldLoan(@RequestBody GoldLoan goldLoan) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            GoldLoan savedLoan = goldLoanService.applyGoldLoan(goldLoan);
            response.put("success", true);
            response.put("message", "Gold loan application submitted successfully");
            response.put("goldLoan", savedLoan);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to apply for gold loan: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // Get all gold loans
    @GetMapping
    public ResponseEntity<List<GoldLoan>> getAllGoldLoans() {
        List<GoldLoan> loans = goldLoanService.getAllGoldLoans();
        return ResponseEntity.ok(loans);
    }

    // Get gold loans by account number
    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<List<GoldLoan>> getGoldLoansByAccountNumber(@PathVariable String accountNumber) {
        List<GoldLoan> loans = goldLoanService.getGoldLoansByAccountNumber(accountNumber);
        return ResponseEntity.ok(loans);
    }

    // Get gold loan by ID
    @GetMapping("/{id}")
    public ResponseEntity<GoldLoan> getGoldLoanById(@PathVariable Long id) {
        GoldLoan loan = goldLoanService.getGoldLoanById(id);
        if (loan != null) {
            return ResponseEntity.ok(loan);
        }
        return ResponseEntity.notFound().build();
    }

    // Get gold loans by status
    @GetMapping("/status/{status}")
    public ResponseEntity<List<GoldLoan>> getGoldLoansByStatus(@PathVariable String status) {
        List<GoldLoan> loans = goldLoanService.getGoldLoansByStatus(status);
        return ResponseEntity.ok(loans);
    }

    // Approve or reject gold loan
    @PutMapping("/{id}/approve")
    public ResponseEntity<Map<String, Object>> approveGoldLoan(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false, defaultValue = "Admin") String approvedBy,
            @RequestBody(required = false) Map<String, Object> goldDetails) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            GoldLoan approvedLoan = goldLoanService.approveGoldLoan(id, status, approvedBy, goldDetails);
            
            if (approvedLoan != null) {
                // If approved, create transaction record
                if ("Approved".equals(status)) {
                    Transaction loanTransaction = new Transaction();
                    loanTransaction.setMerchant("Gold Loan Disbursement");
                    loanTransaction.setAmount(approvedLoan.getLoanAmount());
                    loanTransaction.setType("Loan Credit");
                    loanTransaction.setAccountNumber(approvedLoan.getAccountNumber());
                    loanTransaction.setDescription("Gold Loan Approved: " + approvedLoan.getLoanAccountNumber() + 
                        " | Gold: " + approvedLoan.getGoldGrams() + " grams | Loan ID: " + approvedLoan.getId());
                    loanTransaction.setDate(LocalDateTime.now());
                    loanTransaction.setStatus("Completed");
                    
                    // Get updated balance
                    Double currentBalance = accountService.getBalanceByAccountNumber(approvedLoan.getAccountNumber());
                    loanTransaction.setBalance(currentBalance != null ? currentBalance : approvedLoan.getCurrentBalance() + approvedLoan.getLoanAmount());
                    
                    transactionService.saveTransaction(loanTransaction);
                }
                
                response.put("success", true);
                response.put("message", "Gold loan " + status.toLowerCase() + " successfully");
                response.put("goldLoan", approvedLoan);
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Gold loan not found");
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to process gold loan: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // Accept terms and conditions by user
    @PutMapping("/{id}/accept-terms")
    public ResponseEntity<Map<String, Object>> acceptTerms(
            @PathVariable Long id,
            @RequestParam String acceptedBy) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            GoldLoan loan = goldLoanService.acceptTerms(id, acceptedBy);
            
            if (loan != null) {
                response.put("success", true);
                response.put("message", "Terms and conditions accepted successfully");
                response.put("goldLoan", loan);
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Gold loan not found");
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to accept terms: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // Calculate loan amount for given grams (helper endpoint)
    @GetMapping("/calculate")
    public ResponseEntity<Map<String, Object>> calculateLoanAmount(@RequestParam Double grams) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            GoldRate currentRate = goldRateService.getCurrentGoldRate();
            Double goldValue = grams * currentRate.getRatePerGram();
            Double loanAmount = goldValue * 0.75; // 75% of gold value
            
            response.put("success", true);
            response.put("grams", grams);
            response.put("goldRatePerGram", currentRate.getRatePerGram());
            response.put("goldValue", goldValue);
            response.put("loanAmount", loanAmount);
            response.put("loanPercentage", 75.0);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to calculate loan amount: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}

