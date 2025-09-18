package com.neo.springapp.service;

import com.neo.springapp.model.Loan;
import com.neo.springapp.repository.LoanRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LoanService {

    private final LoanRepository loanRepository;

    public LoanService(LoanRepository loanRepository) {
        this.loanRepository = loanRepository;
    }

    // Save new loan
    public Loan applyLoan(Loan loan) {
        return loanRepository.save(loan);
    }

    // Get all loans
    public List<Loan> getAllLoans() {
        return loanRepository.findAll();
    }

    // Get loan by ID
    public Loan getLoanById(Long id) {
        return loanRepository.findById(id).orElse(null);
    }

    // Update loan status
    // Approve or reject loan
public Loan approveLoan(Long id, String status) {
    Loan loan = loanRepository.findById(id).orElse(null);
    if (loan != null) {
        loan.setStatus(status); // "Approved" or "Rejected"
        return loanRepository.save(loan);
    }
    return null;
}

}
