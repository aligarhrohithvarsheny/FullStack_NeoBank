package com.neo.springapp.service;

import com.neo.springapp.model.Loan;
import com.neo.springapp.model.Account;
import com.neo.springapp.repository.LoanRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class LoanService {

    private final LoanRepository loanRepository;
    private final AccountService accountService;

    public LoanService(LoanRepository loanRepository, AccountService accountService) {
        this.loanRepository = loanRepository;
        this.accountService = accountService;
    }

    // Save new loan
    public Loan applyLoan(Loan loan) {
        // Generate loan account number
        String loanAccountNumber = generateLoanAccountNumber();
        loan.setLoanAccountNumber(loanAccountNumber);
        
        // Set application date if not already set
        if (loan.getApplicationDate() == null) {
            loan.setApplicationDate(LocalDateTime.now());
        }
        
        return loanRepository.save(loan);
    }

    // Get all loans
    public List<Loan> getAllLoans() {
        return loanRepository.findAll();
    }

    // Get loans by account number
    public List<Loan> getLoansByAccountNumber(String accountNumber) {
        return loanRepository.findByAccountNumber(accountNumber);
    }

    // Get loan by ID
    public Loan getLoanById(Long id) {
        return loanRepository.findById(id).orElse(null);
    }

    // Approve or reject loan
    public Loan approveLoan(Long id, String status) {
        Loan loan = loanRepository.findById(id).orElse(null);
        if (loan != null) {
            loan.setStatus(status);
            loan.setApprovalDate(LocalDateTime.now());
            
            // If approved, add loan amount to user's account balance
            if ("Approved".equals(status)) {
                Account account = accountService.getAccountByNumber(loan.getAccountNumber());
                if (account != null) {
                    accountService.creditBalance(loan.getAccountNumber(), loan.getAmount());
                }
            }
            
            return loanRepository.save(loan);
        }
        return null;
    }

    // Generate unique loan account number
    private String generateLoanAccountNumber() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String timestamp = now.format(formatter);
        return "LOAN" + timestamp;
    }

}
