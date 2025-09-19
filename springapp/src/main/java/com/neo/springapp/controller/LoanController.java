package com.neo.springapp.controller;

import com.neo.springapp.model.Loan;
import com.neo.springapp.service.LoanService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/loans")
@CrossOrigin(origins = "http://localhost:4200") // ✅ allow Angular frontend
public class LoanController {

    private final LoanService loanService;

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

    // Get loan by ID
    @GetMapping("/{id}")
    public Loan getLoanById(@PathVariable Long id) {
        return loanService.getLoanById(id);
    }

    // Update loan status (e.g. Admin Approval)
    @PutMapping("/{id}/approve")
    public Loan approveLoan(@PathVariable Long id, @RequestParam String status) {
        // status should be "Approved" or "Rejected"
        return loanService.approveLoan(id, status);
    }

    // Get loans by account number (for user to see their loan applications)
    @GetMapping("/user/{accountNumber}")
    public List<Loan> getLoansByAccountNumber(@PathVariable String accountNumber) {
        return loanService.getLoansByAccountNumber(accountNumber);
    }

}
