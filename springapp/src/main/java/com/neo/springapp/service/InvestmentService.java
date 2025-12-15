package com.neo.springapp.service;

import com.neo.springapp.model.Investment;
import com.neo.springapp.model.Account;
import com.neo.springapp.model.Transaction;
import com.neo.springapp.repository.InvestmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

@Service
public class InvestmentService {

    @Autowired
    private InvestmentRepository investmentRepository;

    @Autowired
    private AccountService accountService;

    @Autowired
    private TransactionService transactionService;

    /**
     * Create new investment application
     */
    @Transactional
    public Map<String, Object> createInvestment(Investment investment) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Validate account
            Account account = accountService.getAccountByNumber(investment.getAccountNumber());
            if (account == null) {
                response.put("success", false);
                response.put("message", "Account not found");
                return response;
            }

            // Set user details
            investment.setUserName(account.getName());
            investment.setUserEmail(account.getPhone()); // Using phone as email placeholder if needed
            
            // Set investment date if not provided
            if (investment.getInvestmentDate() == null) {
                investment.setInvestmentDate(LocalDate.now());
            }

            // Calculate units (simplified - assuming NAV of 100 for now)
            if (investment.getInvestmentAmount() != null && investment.getUnits() == null) {
                double nav = 100.0; // Net Asset Value (can be made dynamic)
                investment.setUnits(investment.getInvestmentAmount() / nav);
            }

            // Set initial current value same as investment amount
            if (investment.getCurrentValue() == null) {
                investment.setCurrentValue(investment.getInvestmentAmount());
            }

            investment.setStatus("PENDING");
            investment.setApplicationDate(LocalDateTime.now());

            Investment savedInvestment = investmentRepository.save(investment);

            response.put("success", true);
            response.put("message", "Investment application submitted successfully");
            response.put("investment", savedInvestment);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to create investment: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * Approve investment application
     */
    @Transactional
    public Map<String, Object> approveInvestment(Long investmentId, String approvedBy) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Investment investment = investmentRepository.findById(investmentId)
                .orElseThrow(() -> new RuntimeException("Investment not found"));

            if (!"PENDING".equals(investment.getStatus())) {
                response.put("success", false);
                response.put("message", "Investment is not in pending status");
                return response;
            }

            // Check account balance
            Double currentBalance = accountService.getBalanceByAccountNumber(investment.getAccountNumber());
            if (currentBalance == null || currentBalance < investment.getInvestmentAmount()) {
                response.put("success", false);
                response.put("message", "Insufficient balance. Required: ₹" + investment.getInvestmentAmount() + ", Available: ₹" + currentBalance);
                return response;
            }

            // Debit amount from account
            Double balanceBefore = currentBalance;
            Double newBalance = accountService.debitBalance(investment.getAccountNumber(), investment.getInvestmentAmount());
            
            if (newBalance == null) {
                response.put("success", false);
                response.put("message", "Failed to debit amount from account");
                return response;
            }

            // Update investment
            investment.setStatus("APPROVED");
            investment.setApprovalDate(LocalDateTime.now());
            investment.setApprovedBy(approvedBy);
            investment.setBalanceBefore(balanceBefore);
            investment.setBalanceAfter(newBalance);

            // Create transaction record
            Transaction transaction = new Transaction();
            transaction.setAccountNumber(investment.getAccountNumber());
            transaction.setMerchant("Mutual Fund Investment");
            transaction.setAmount(investment.getInvestmentAmount());
            transaction.setType("Debit");
            transaction.setDescription("Investment in " + investment.getFundName() + " - " + investment.getFundCategory());
            transaction.setDate(LocalDateTime.now());
            transaction.setStatus("Completed");
            transaction.setBalance(newBalance);
            
            Transaction savedTransaction = transactionService.saveTransaction(transaction);
            investment.setTransactionId(savedTransaction.getId().toString());

            Investment savedInvestment = investmentRepository.save(investment);

            response.put("success", true);
            response.put("message", "Investment approved successfully");
            response.put("investment", savedInvestment);
            response.put("transaction", savedTransaction);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to approve investment: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * Reject investment application
     */
    public Map<String, Object> rejectInvestment(Long investmentId, String rejectedBy, String reason) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Investment investment = investmentRepository.findById(investmentId)
                .orElseThrow(() -> new RuntimeException("Investment not found"));

            if (!"PENDING".equals(investment.getStatus())) {
                response.put("success", false);
                response.put("message", "Investment is not in pending status");
                return response;
            }

            investment.setStatus("REJECTED");
            investment.setRejectionReason(reason);
            investment.setApprovalDate(LocalDateTime.now());
            investment.setApprovedBy(rejectedBy);

            Investment savedInvestment = investmentRepository.save(investment);

            response.put("success", true);
            response.put("message", "Investment rejected");
            response.put("investment", savedInvestment);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to reject investment: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * Get all investments by account number
     */
    public List<Investment> getInvestmentsByAccountNumber(String accountNumber) {
        return investmentRepository.findByAccountNumber(accountNumber);
    }

    /**
     * Get all pending investments
     */
    public List<Investment> getPendingInvestments() {
        return investmentRepository.findByStatus("PENDING");
    }

    /**
     * Get investment by ID
     */
    public Optional<Investment> getInvestmentById(Long id) {
        return investmentRepository.findById(id);
    }

    /**
     * Update investment (for manager/admin updates)
     */
    public Map<String, Object> updateInvestment(Long investmentId, Investment investmentDetails) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Investment investment = investmentRepository.findById(investmentId)
                .orElseThrow(() -> new RuntimeException("Investment not found"));

            // Update allowed fields
            if (investmentDetails.getCurrentValue() != null) {
                investment.setCurrentValue(investmentDetails.getCurrentValue());
            }
            if (investmentDetails.getReturns() != null) {
                investment.setReturns(investmentDetails.getReturns());
            }
            if (investmentDetails.getProfitLoss() != null) {
                investment.setProfitLoss(investmentDetails.getProfitLoss());
            }
            if (investmentDetails.getStatus() != null) {
                investment.setStatus(investmentDetails.getStatus());
            }
            if (investmentDetails.getRemarks() != null) {
                investment.setRemarks(investmentDetails.getRemarks());
            }

            Investment savedInvestment = investmentRepository.save(investment);

            response.put("success", true);
            response.put("message", "Investment updated successfully");
            response.put("investment", savedInvestment);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to update investment: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * Get all investments (for admin/manager)
     */
    public List<Investment> getAllInvestments() {
        return investmentRepository.findAll();
    }

    /**
     * Get investments by status
     */
    public List<Investment> getInvestmentsByStatus(String status) {
        return investmentRepository.findByStatus(status);
    }
}

