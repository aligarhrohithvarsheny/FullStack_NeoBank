package com.neo.springapp.service;

import com.neo.springapp.model.Investment;
import com.neo.springapp.model.Account;
import com.neo.springapp.model.Transaction;
import com.neo.springapp.model.MutualFundForeclosure;
import com.neo.springapp.repository.InvestmentRepository;
import com.neo.springapp.repository.MutualFundForeclosureRepository;
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
@SuppressWarnings("null")
public class InvestmentService {

    @Autowired
    private InvestmentRepository investmentRepository;

    @Autowired
    private AccountService accountService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private MutualFundForeclosureRepository foreclosureRepository;

    @Autowired
    private OtpService otpService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private UserService userService;

    private static final String OTP_REASON_MF_FORECLOSURE = "Mutual Fund Foreclosure request";

    /**
     * Send OTP to user's registered email for foreclosure request. Reason is included in the email.
     */
    public Map<String, Object> requestForeclosureOtp(Long investmentId, String requestReason) {
        Map<String, Object> response = new HashMap<>();
        try {
            Optional<Investment> invOpt = investmentRepository.findById(investmentId);
            if (!invOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "Investment not found");
                return response;
            }
            Investment inv = invOpt.get();
            String userEmail = inv.getUserEmail();
            if (userEmail == null || !userEmail.contains("@")) {
                userEmail = userService.getUserByAccountNumber(inv.getAccountNumber())
                    .map(u -> u.getEmail()).orElse(null);
            }
            if (userEmail == null || userEmail.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "User email not found. Cannot send OTP.");
                return response;
            }
            String otp = otpService.generateOtp();
            String key = "MF_FORECLOSURE:" + investmentId;
            otpService.storeOtpForKey(key, otp);
            boolean sent = emailService.sendOtpEmailWithReason(userEmail, otp, OTP_REASON_MF_FORECLOSURE);
            if (!sent) {
                response.put("success", false);
                response.put("message", "Failed to send OTP. Please try again.");
                return response;
            }
            response.put("success", true);
            response.put("message", "OTP sent to your registered email.");
            response.put("key", key);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to send OTP: " + e.getMessage());
        }
        return response;
    }

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

    /**
     * Calculate foreclosure fine based on investment amount
     * Fine ranges from Rs 500 to Rs 10000 based on investment amount
     */
    public Double calculateForeclosureFine(Double investmentAmount) {
        if (investmentAmount == null || investmentAmount <= 0) {
            return 0.0;
        }
        
        // Fine calculation: 500 to 10000 based on investment amount
        // For amounts <= 10000: fine = 500
        // For amounts > 10000: fine = 500 + (amount - 10000) * 0.05, capped at 10000
        Double fine = 500.0;
        if (investmentAmount > 10000) {
            fine = 500.0 + (investmentAmount - 10000) * 0.05;
            if (fine > 10000) {
                fine = 10000.0;
            }
        }
        return fine;
    }

    /**
     * Calculate foreclosure details (fine and amount to be credited)
     */
    public Map<String, Object> calculateForeclosure(Long investmentId) {
        Map<String, Object> result = new HashMap<>();
        
        Investment investment = investmentRepository.findById(investmentId)
            .orElse(null);
        
        if (investment == null) {
            result.put("success", false);
            result.put("message", "Investment not found");
            return result;
        }

        if (!"ACTIVE".equals(investment.getStatus()) && !"APPROVED".equals(investment.getStatus())) {
            result.put("success", false);
            result.put("message", "Only active or approved investments can be foreclosed");
            return result;
        }

        // Check if there's already a pending foreclosure request
        List<MutualFundForeclosure> pendingForeclosures = foreclosureRepository
            .findByInvestmentIdAndStatus(investmentId, "PENDING");
        if (!pendingForeclosures.isEmpty()) {
            result.put("success", false);
            result.put("message", "A foreclosure request is already pending for this investment");
            return result;
        }

        // Use current value if available, otherwise use investment amount
        Double currentValue = investment.getCurrentValue() != null ? 
            investment.getCurrentValue() : investment.getInvestmentAmount();
        
        // Calculate fine
        Double fine = calculateForeclosureFine(investment.getInvestmentAmount());
        
        // Calculate amount to be credited (current value - fine)
        Double foreclosureAmount = currentValue - fine;
        if (foreclosureAmount < 0) {
            foreclosureAmount = 0.0;
        }

        result.put("success", true);
        result.put("investmentId", investmentId);
        result.put("investmentAmount", investment.getInvestmentAmount());
        result.put("currentValue", currentValue);
        result.put("fine", fine);
        result.put("foreclosureAmount", foreclosureAmount);
        
        return result;
    }

    /**
     * Request foreclosure for an investment after OTP verification.
     * @param otp OTP sent to user's registered email (required).
     */
    @Transactional
    public Map<String, Object> requestForeclosure(Long investmentId, String requestReason, String otp) {
        Map<String, Object> response = new HashMap<>();
        try {
            String key = "MF_FORECLOSURE:" + investmentId;
            if (otp == null || otp.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "OTP is required.");
                return response;
            }
            if (!otpService.verifyOtpByKey(key, otp)) {
                response.put("success", false);
                response.put("message", "Invalid or expired OTP. Please request a new OTP.");
                return response;
            }
            Map<String, Object> calculation = calculateForeclosure(investmentId);
            if (!Boolean.TRUE.equals(calculation.get("success"))) {
                return calculation;
            }
            Investment investment = investmentRepository.findById(investmentId)
                .orElseThrow(() -> new RuntimeException("Investment not found"));
            MutualFundForeclosure foreclosure = new MutualFundForeclosure();
            foreclosure.setInvestmentId(investmentId);
            foreclosure.setAccountNumber(investment.getAccountNumber());
            foreclosure.setUserName(investment.getUserName());
            foreclosure.setFundName(investment.getFundName());
            foreclosure.setFundCategory(investment.getFundCategory());
            foreclosure.setInvestmentAmount(investment.getInvestmentAmount());
            foreclosure.setCurrentValue((Double) calculation.get("currentValue"));
            foreclosure.setUnits(investment.getUnits());
            foreclosure.setFine((Double) calculation.get("fine"));
            foreclosure.setForeclosureAmount((Double) calculation.get("foreclosureAmount"));
            foreclosure.setStatus("PENDING");
            foreclosure.setRequestReason(requestReason);
            foreclosure.setOtpVerified(true);
            MutualFundForeclosure savedForeclosure = foreclosureRepository.save(foreclosure);
            response.put("success", true);
            response.put("message", "Foreclosure request submitted successfully");
            response.put("foreclosure", savedForeclosure);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to request foreclosure: " + e.getMessage());
        }
        return response;
    }

    /**
     * Approve foreclosure request and credit amount to user account
     */
    @Transactional
    public Map<String, Object> approveForeclosure(Long foreclosureId, String approvedBy) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            MutualFundForeclosure foreclosure = foreclosureRepository.findById(foreclosureId)
                .orElseThrow(() -> new RuntimeException("Foreclosure request not found"));

            if (!"PENDING".equals(foreclosure.getStatus())) {
                response.put("success", false);
                response.put("message", "Foreclosure request is not in pending status");
                return response;
            }

            // Get account balance before credit
            Double balanceBefore = accountService.getBalanceByAccountNumber(foreclosure.getAccountNumber());
            if (balanceBefore == null) {
                response.put("success", false);
                response.put("message", "Account not found");
                return response;
            }

            // Credit foreclosure amount to user account
            Double newBalance = accountService.creditBalance(
                foreclosure.getAccountNumber(), 
                foreclosure.getForeclosureAmount()
            );
            
            if (newBalance == null) {
                response.put("success", false);
                response.put("message", "Failed to credit amount to account");
                return response;
            }

            // Create transaction record
            Transaction transaction = new Transaction();
            transaction.setAccountNumber(foreclosure.getAccountNumber());
            transaction.setMerchant("Mutual Fund Foreclosure");
            transaction.setAmount(foreclosure.getForeclosureAmount());
            transaction.setType("Credit");
            transaction.setDescription("Mutual Fund Foreclosure - " + foreclosure.getFundName() + 
                " (Fine: ₹" + foreclosure.getFine() + ")");
            transaction.setDate(LocalDateTime.now());
            transaction.setStatus("Completed");
            transaction.setBalance(newBalance);
            
            Transaction savedTransaction = transactionService.saveTransaction(transaction);

            // Update investment status to CLOSED
            Investment investment = investmentRepository.findById(foreclosure.getInvestmentId())
                .orElse(null);
            if (investment != null) {
                investment.setStatus("CLOSED");
                investmentRepository.save(investment);
            }

            // Update foreclosure
            foreclosure.setStatus("COMPLETED");
            foreclosure.setApprovalDate(LocalDateTime.now());
            foreclosure.setApprovedBy(approvedBy);
            foreclosure.setCompletionDate(LocalDateTime.now());
            foreclosure.setTransactionId(savedTransaction.getId().toString());
            foreclosure.setBalanceBefore(balanceBefore);
            foreclosure.setBalanceAfter(newBalance);

            MutualFundForeclosure savedForeclosure = foreclosureRepository.save(foreclosure);

            response.put("success", true);
            response.put("message", "Foreclosure approved and amount credited successfully");
            response.put("foreclosure", savedForeclosure);
            response.put("transaction", savedTransaction);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to approve foreclosure: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * Reject foreclosure request
     */
    public Map<String, Object> rejectForeclosure(Long foreclosureId, String rejectedBy, String reason) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            MutualFundForeclosure foreclosure = foreclosureRepository.findById(foreclosureId)
                .orElseThrow(() -> new RuntimeException("Foreclosure request not found"));

            if (!"PENDING".equals(foreclosure.getStatus())) {
                response.put("success", false);
                response.put("message", "Foreclosure request is not in pending status");
                return response;
            }

            foreclosure.setStatus("REJECTED");
            foreclosure.setApprovalDate(LocalDateTime.now());
            foreclosure.setApprovedBy(rejectedBy);
            foreclosure.setRejectionReason(reason);

            MutualFundForeclosure savedForeclosure = foreclosureRepository.save(foreclosure);

            response.put("success", true);
            response.put("message", "Foreclosure request rejected");
            response.put("foreclosure", savedForeclosure);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to reject foreclosure: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * Get foreclosure history by account number
     */
    public List<MutualFundForeclosure> getForeclosureHistoryByAccount(String accountNumber) {
        return foreclosureRepository.findByAccountNumber(accountNumber);
    }

    /**
     * Get all pending foreclosure requests (for admin)
     */
    public List<MutualFundForeclosure> getPendingForeclosures() {
        return foreclosureRepository.findByStatus("PENDING");
    }

    /**
     * Get all foreclosures (for admin)
     */
    public List<MutualFundForeclosure> getAllForeclosures() {
        return foreclosureRepository.findAll();
    }

    /**
     * Get foreclosure by ID
     */
    public Optional<MutualFundForeclosure> getForeclosureById(Long id) {
        return foreclosureRepository.findById(id);
    }
}

