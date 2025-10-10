package com.neo.springapp.controller;

import com.neo.springapp.model.Loan;
import com.neo.springapp.model.Transaction;
import com.neo.springapp.service.LoanService;
import com.neo.springapp.service.AccountService;
import com.neo.springapp.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseEntity.BodyBuilder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/loans")
@CrossOrigin(origins = "http://localhost:4200") // ✅ allow Angular frontend
public class LoanController {

    private final LoanService loanService;
    
    @Autowired
    private AccountService accountService;
    
    @Autowired
    private TransactionService transactionService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    // Apply Loan
    @PostMapping
    public Loan applyLoan(@RequestBody Loan loan) {
        return loanService.applyLoan(loan);
    }

    // Get all loans
    @GetMapping
    public List<Loan> getAllLoans() {
        return loanService.getAllLoans();
    }

    // Get loans by account number
    @GetMapping("/account/{accountNumber}")
    public List<Loan> getLoansByAccountNumber(@PathVariable String accountNumber) {
        return loanService.getLoansByAccountNumber(accountNumber);
    }

    // Get loan by ID
    @GetMapping("/{id}")
    public Loan getLoanById(@PathVariable Long id) {
        return loanService.getLoanById(id);
    }

    // Update loan status (e.g. Admin Approval)
    @PutMapping("/{id}/approve")
    public ResponseEntity<Map<String, Object>> approveLoan(@PathVariable Long id, @RequestParam String status) {
        System.out.println("=== LOAN APPROVAL ===");
        System.out.println("Loan ID: " + id);
        System.out.println("Status: " + status);
        
        try {
        // status should be "Approved" or "Rejected"
            Loan approvedLoan = loanService.approveLoan(id, status);
            
            if (approvedLoan != null) {
                System.out.println("✅ Loan approved successfully: " + approvedLoan.getId());
                System.out.println("Account Number: " + approvedLoan.getAccountNumber());
                System.out.println("Amount: " + approvedLoan.getAmount());
                
                // If loan is approved, credit the amount to user's account
                if ("Approved".equals(status)) {
                    System.out.println("=== CREDITING LOAN AMOUNT TO USER ACCOUNT ===");
                    
                    // Credit the loan amount to user's account
                    accountService.creditBalance(approvedLoan.getAccountNumber(), approvedLoan.getAmount());
                    
                    // Create transaction record
                    Transaction loanTransaction = new Transaction();
                    loanTransaction.setMerchant("Loan Disbursement");
                    loanTransaction.setAmount(approvedLoan.getAmount());
                    loanTransaction.setType("Loan Credit");
                    loanTransaction.setAccountNumber(approvedLoan.getAccountNumber());
                    loanTransaction.setDescription("Loan Approved: " + approvedLoan.getLoanAccountNumber() + " - " + approvedLoan.getType() + " | Loan ID: " + approvedLoan.getId());
                    loanTransaction.setDate(LocalDateTime.now());
                    loanTransaction.setStatus("Completed");
                    
                    // Save transaction
                    Transaction savedTransaction = transactionService.saveTransaction(loanTransaction);
                    
                    System.out.println("✅ Loan amount credited to account: " + approvedLoan.getAccountNumber());
                    System.out.println("✅ Transaction created: " + savedTransaction.getId());
                    
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("loan", approvedLoan);
                    response.put("transaction", savedTransaction);
                    response.put("message", "Loan approved and amount credited successfully");
                    
                    return ResponseEntity.ok(response);
                } else {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("loan", approvedLoan);
                    response.put("message", "Loan status updated successfully");
                    
                    return ResponseEntity.ok(response);
                }
            } else {
                System.out.println("❌ Loan approval failed for ID: " + id);
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Loan not found or approval failed");
                
                return ((BodyBuilder) ResponseEntity.notFound()).body(response);
            }
        } catch (Exception e) {
            System.out.println("❌ Loan approval error: " + e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Loan approval failed: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }


}
