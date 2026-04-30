package com.neo.springapp.service;

import com.neo.springapp.model.FixedDeposit;
import com.neo.springapp.model.Account;
import com.neo.springapp.model.Transaction;
import com.neo.springapp.repository.FixedDepositRepository;
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
public class FixedDepositService {

    @Autowired
    private FixedDepositRepository fixedDepositRepository;

    @Autowired
    private AccountService accountService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private OtpService otpService;

    @Autowired
    private UserService userService;

    private static final String OTP_REASON_FD_FORECLOSURE = "FD (Fixed Deposit) Withdrawal / Foreclosure";

    /**
     * Send OTP to user's registered email for FD foreclosure. Reason included in email.
     */
    public Map<String, Object> requestForeclosureOtp(Long fdId) {
        Map<String, Object> response = new HashMap<>();
        try {
            FixedDeposit fd = fixedDepositRepository.findById(fdId)
                .orElseThrow(() -> new RuntimeException("Fixed Deposit not found"));
            String userEmail = fd.getUserEmail();
            if (userEmail == null || !userEmail.contains("@")) {
                Optional<com.neo.springapp.model.User> u = userService.getUserByAccountNumber(fd.getAccountNumber());
                userEmail = u.map(com.neo.springapp.model.User::getEmail).orElse(null);
            }
            if (userEmail == null || userEmail.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "User email not found. Cannot send OTP.");
                return response;
            }
            String key = "FD_FORECLOSURE:" + fdId;
            System.out.println("OTP API HIT: /api/fixed-deposits/" + fdId + "/foreclosure/request-otp");
            System.out.println("Email: " + userEmail);
            otpService.sendOtpForKey(userEmail, key, OTP_REASON_FD_FORECLOSURE);
            response.put("success", true);
            response.put("message", "OTP sent to your registered email.");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to send OTP: " + e.getMessage());
        }
        return response;
    }

    /**
     * Calculate maturity amount for FD
     */
    public Double calculateMaturityAmount(Double principal, Double interestRate, Integer tenure) {
        if (principal == null || interestRate == null || tenure == null) {
            return 0.0;
        }
        // Simple interest calculation: A = P(1 + r*t)
        // For compound interest: A = P(1 + r/100)^t
        double rate = interestRate / 100.0;
        double years = tenure / 12.0;
        double maturityAmount = principal * Math.pow(1 + rate, years);
        return Math.round(maturityAmount * 100.0) / 100.0;
    }

    /**
     * Calculate interest amount
     */
    public Double calculateInterestAmount(Double principal, Double interestRate, Integer tenure) {
        Double maturityAmount = calculateMaturityAmount(principal, interestRate, tenure);
        return maturityAmount - principal;
    }

    /**
     * Create new FD application
     */
    @Transactional
    public Map<String, Object> createFixedDeposit(FixedDeposit fixedDeposit) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Validate account
            Account account = accountService.getAccountByNumber(fixedDeposit.getAccountNumber());
            if (account == null) {
                response.put("success", false);
                response.put("message", "Account not found");
                return response;
            }

            // Set user details
            fixedDeposit.setUserName(account.getName());
            fixedDeposit.setUserEmail(account.getPhone()); // Using phone as email placeholder if needed
            
            // Set start date if not provided
            if (fixedDeposit.getStartDate() == null) {
                fixedDeposit.setStartDate(LocalDate.now());
            }

            // Calculate maturity date
            if (fixedDeposit.getMaturityDate() == null && fixedDeposit.getTenure() != null) {
                fixedDeposit.setMaturityDate(fixedDeposit.getStartDate().plusMonths(fixedDeposit.getTenure()));
            }

            // Calculate years and set interest rate based on years if not provided
            if (fixedDeposit.getTenure() != null) {
                Integer years = (int) Math.ceil(fixedDeposit.getTenure() / 12.0);
                fixedDeposit.setYears(years);
                // Set interest rate based on years if not already set
                if (fixedDeposit.getInterestRate() == null || fixedDeposit.getInterestRate() == 0.0) {
                    fixedDeposit.setInterestRate(calculateInterestRateByYears(years));
                }
            }

            // Calculate maturity amount and interest
            if (fixedDeposit.getPrincipalAmount() != null && fixedDeposit.getInterestRate() != null && fixedDeposit.getTenure() != null) {
                Double maturityAmount = calculateMaturityAmount(
                    fixedDeposit.getPrincipalAmount(),
                    fixedDeposit.getInterestRate(),
                    fixedDeposit.getTenure()
                );
                fixedDeposit.setMaturityAmount(maturityAmount);
                
                Double interestAmount = calculateInterestAmount(
                    fixedDeposit.getPrincipalAmount(),
                    fixedDeposit.getInterestRate(),
                    fixedDeposit.getTenure()
                );
                fixedDeposit.setInterestAmount(interestAmount);
            }

            fixedDeposit.setStatus("PENDING");
            fixedDeposit.setApplicationDate(LocalDateTime.now());

            FixedDeposit savedFD = fixedDepositRepository.save(fixedDeposit);

            response.put("success", true);
            response.put("message", "Fixed Deposit application submitted successfully");
            response.put("fixedDeposit", savedFD);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to create fixed deposit: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * Approve FD application
     */
    @Transactional
    public Map<String, Object> approveFixedDeposit(Long fdId, String approvedBy, String approvalReason) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            FixedDeposit fixedDeposit = fixedDepositRepository.findById(fdId)
                .orElseThrow(() -> new RuntimeException("Fixed Deposit not found"));

            if (!"PENDING".equals(fixedDeposit.getStatus())) {
                response.put("success", false);
                response.put("message", "Fixed Deposit is not in pending status");
                return response;
            }

            // Check if account exists and is not closed
            Account account = accountService.getAccountByNumber(fixedDeposit.getAccountNumber());
            if (account == null) {
                response.put("success", false);
                response.put("message", "Account not found for number: " + fixedDeposit.getAccountNumber());
                return response;
            }
            
            if ("CLOSED".equalsIgnoreCase(account.getStatus())) {
                response.put("success", false);
                response.put("message", "Cannot create Fixed Deposit for a closed account. Account number: " + fixedDeposit.getAccountNumber());
                return response;
            }

            // Check account balance
            Double currentBalance = account.getBalance();
            if (currentBalance == null || currentBalance < fixedDeposit.getPrincipalAmount()) {
                response.put("success", false);
                response.put("message", "Insufficient balance. Required: ₹" + fixedDeposit.getPrincipalAmount() + ", Available: ₹" + currentBalance);
                return response;
            }

            // Debit amount from account
            Double balanceBefore = currentBalance;
            Double newBalance = accountService.debitBalance(fixedDeposit.getAccountNumber(), fixedDeposit.getPrincipalAmount());
            
            if (newBalance == null) {
                response.put("success", false);
                response.put("message", "Failed to debit amount from account");
                return response;
            }

            // Calculate years and set interest rate if not set
            if (fixedDeposit.getTenure() != null) {
                Integer years = (int) Math.ceil(fixedDeposit.getTenure() / 12.0);
                fixedDeposit.setYears(years);
                // Set interest rate based on years if not already set
                if (fixedDeposit.getInterestRate() == null || fixedDeposit.getInterestRate() == 0.0) {
                    fixedDeposit.setInterestRate(calculateInterestRateByYears(years));
                }
                // Recalculate maturity amount with correct interest rate
                if (fixedDeposit.getPrincipalAmount() != null) {
                    Double maturityAmount = calculateMaturityAmount(
                        fixedDeposit.getPrincipalAmount(),
                        fixedDeposit.getInterestRate(),
                        fixedDeposit.getTenure()
                    );
                    fixedDeposit.setMaturityAmount(maturityAmount);
                    
                    Double interestAmount = calculateInterestAmount(
                        fixedDeposit.getPrincipalAmount(),
                        fixedDeposit.getInterestRate(),
                        fixedDeposit.getTenure()
                    );
                    fixedDeposit.setInterestAmount(interestAmount);
                }
            }

            // Update FD
            fixedDeposit.setStatus("ACTIVE");
            fixedDeposit.setApprovalDate(LocalDateTime.now());
            fixedDeposit.setApprovedBy(approvedBy);
            fixedDeposit.setBalanceBefore(balanceBefore);
            fixedDeposit.setBalanceAfter(newBalance);
            fixedDeposit.setLastInterestCreditDate(null); // Initialize for monthly interest tracking

            // Create transaction record
            Transaction transaction = new Transaction();
            transaction.setAccountNumber(fixedDeposit.getAccountNumber());
            transaction.setMerchant("Fixed Deposit");
            transaction.setAmount(fixedDeposit.getPrincipalAmount());
            transaction.setType("Debit");
            transaction.setDescription("Fixed Deposit - " + fixedDeposit.getFdAccountNumber() + " | Tenure: " + fixedDeposit.getTenure() + " months | Interest: " + fixedDeposit.getInterestRate() + "%");
            transaction.setDate(LocalDateTime.now());
            transaction.setStatus("Completed");
            transaction.setBalance(newBalance);
            
            Transaction savedTransaction = transactionService.saveTransaction(transaction);
            fixedDeposit.setTransactionId(savedTransaction.getId().toString());

            // Save approval reason if provided
            if (approvalReason != null && !approvalReason.trim().isEmpty()) {
                fixedDeposit.setRemarks("Approval Reason: " + approvalReason);
            }

            FixedDeposit savedFD = fixedDepositRepository.save(fixedDeposit);

            response.put("success", true);
            response.put("message", "Fixed Deposit approved successfully");
            response.put("fixedDeposit", savedFD);
            response.put("transaction", savedTransaction);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to approve fixed deposit: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * Reject FD application
     */
    public Map<String, Object> rejectFixedDeposit(Long fdId, String rejectedBy, String reason) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            FixedDeposit fixedDeposit = fixedDepositRepository.findById(fdId)
                .orElseThrow(() -> new RuntimeException("Fixed Deposit not found"));

            if (!"PENDING".equals(fixedDeposit.getStatus())) {
                response.put("success", false);
                response.put("message", "Fixed Deposit is not in pending status");
                return response;
            }

            fixedDeposit.setStatus("REJECTED");
            fixedDeposit.setRejectionReason(reason);
            fixedDeposit.setApprovalDate(LocalDateTime.now());
            fixedDeposit.setApprovedBy(rejectedBy);

            FixedDeposit savedFD = fixedDepositRepository.save(fixedDeposit);

            response.put("success", true);
            response.put("message", "Fixed Deposit rejected");
            response.put("fixedDeposit", savedFD);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to reject fixed deposit: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * Process FD maturity
     */
    @Transactional
    public Map<String, Object> processMaturity(Long fdId, String processedBy) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            FixedDeposit fixedDeposit = fixedDepositRepository.findById(fdId)
                .orElseThrow(() -> new RuntimeException("Fixed Deposit not found"));

            if (!"ACTIVE".equals(fixedDeposit.getStatus())) {
                response.put("success", false);
                response.put("message", "Fixed Deposit is not active");
                return response;
            }

            if (fixedDeposit.getMaturityDate().isAfter(LocalDate.now())) {
                response.put("success", false);
                response.put("message", "Fixed Deposit has not matured yet");
                return response;
            }

            // Check if account is closed
            Account account = accountService.getAccountByNumber(fixedDeposit.getAccountNumber());
            if (account == null) {
                response.put("success", false);
                response.put("message", "Account not found for number: " + fixedDeposit.getAccountNumber());
                return response;
            }
            
            if ("CLOSED".equalsIgnoreCase(account.getStatus())) {
                response.put("success", false);
                response.put("message", "Cannot credit FD maturity to a closed account. Account number: " + fixedDeposit.getAccountNumber());
                return response;
            }

            // Credit maturity amount to account
            Double newBalance = accountService.creditBalance(fixedDeposit.getAccountNumber(), fixedDeposit.getMaturityAmount());
            
            if (newBalance == null) {
                response.put("success", false);
                response.put("message", "Failed to credit amount to account");
                return response;
            }

            // Create transaction record
            Transaction transaction = new Transaction();
            transaction.setAccountNumber(fixedDeposit.getAccountNumber());
            transaction.setMerchant("Fixed Deposit Maturity");
            transaction.setAmount(fixedDeposit.getMaturityAmount());
            transaction.setType("Credit");
            transaction.setDescription("FD Maturity - " + fixedDeposit.getFdAccountNumber() + " | Principal: ₹" + fixedDeposit.getPrincipalAmount() + ", Interest: ₹" + fixedDeposit.getInterestAmount());
            transaction.setDate(LocalDateTime.now());
            transaction.setStatus("Completed");
            transaction.setBalance(newBalance);
            
            transactionService.saveTransaction(transaction);

            // Update FD
            fixedDeposit.setStatus("MATURED");
            fixedDeposit.setIsMatured(true);
            fixedDeposit.setMaturityProcessedDate(LocalDateTime.now());
            fixedDeposit.setMaturityProcessedBy(processedBy);

            FixedDeposit savedFD = fixedDepositRepository.save(fixedDeposit);

            response.put("success", true);
            response.put("message", "Fixed Deposit maturity processed successfully");
            response.put("fixedDeposit", savedFD);
            response.put("maturityAmount", fixedDeposit.getMaturityAmount());
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to process maturity: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * Get all FDs by account number
     */
    public List<FixedDeposit> getFixedDepositsByAccountNumber(String accountNumber) {
        return fixedDepositRepository.findByAccountNumber(accountNumber);
    }

    /**
     * Get all pending FDs
     */
    public List<FixedDeposit> getPendingFixedDeposits() {
        return fixedDepositRepository.findByStatus("PENDING");
    }

    /**
     * Get FD by ID
     */
    public Optional<FixedDeposit> getFixedDepositById(Long id) {
        return fixedDepositRepository.findById(id);
    }

    /**
     * Get FD by FD account number
     */
    public Optional<FixedDeposit> getFixedDepositByFdAccountNumber(String fdAccountNumber) {
        return fixedDepositRepository.findByFdAccountNumber(fdAccountNumber);
    }

    /**
     * Update FD (for manager/admin updates)
     */
    public Map<String, Object> updateFixedDeposit(Long fdId, FixedDeposit fdDetails) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            FixedDeposit fixedDeposit = fixedDepositRepository.findById(fdId)
                .orElseThrow(() -> new RuntimeException("Fixed Deposit not found"));

            // Update allowed fields
            if (fdDetails.getRemarks() != null) {
                fixedDeposit.setRemarks(fdDetails.getRemarks());
            }
            if (fdDetails.getStatus() != null) {
                fixedDeposit.setStatus(fdDetails.getStatus());
            }

            FixedDeposit savedFD = fixedDepositRepository.save(fixedDeposit);

            response.put("success", true);
            response.put("message", "Fixed Deposit updated successfully");
            response.put("fixedDeposit", savedFD);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to update fixed deposit: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * Get all FDs (for admin/manager)
     */
    public List<FixedDeposit> getAllFixedDeposits() {
        return fixedDepositRepository.findAll();
    }

    /**
     * Get FDs by status
     */
    public List<FixedDeposit> getFixedDepositsByStatus(String status) {
        return fixedDepositRepository.findByStatus(status);
    }

    /**
     * Get matured FDs that need processing
     */
    public List<FixedDeposit> getMaturedFixedDeposits() {
        return fixedDepositRepository.findByIsMaturedFalseAndMaturityDateBefore(LocalDate.now());
    }

    /**
     * Calculate interest rate based on years (up to 8% for 5 years)
     */
    public Double calculateInterestRateByYears(Integer years) {
        if (years == null || years <= 0) {
            return 4.0; // Default minimum rate
        }
        if (years >= 5) {
            return 8.0; // Maximum rate at 5 years
        }
        // Linear progression: 4% at 1 year, 8% at 5 years
        double rate = 4.0 + (years - 1) * 1.0;
        return Math.min(rate, 8.0); // Cap at 8%
    }

    /**
     * Process monthly interest credit for active FDs
     * This should be called by a scheduled task every month
     */
    @Transactional
    public Map<String, Object> processMonthlyInterestCredit(Long fdId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            FixedDeposit fixedDeposit = fixedDepositRepository.findById(fdId)
                .orElseThrow(() -> new RuntimeException("Fixed Deposit not found"));

            if (!"ACTIVE".equals(fixedDeposit.getStatus())) {
                response.put("success", false);
                response.put("message", "Fixed Deposit is not active");
                return response;
            }

            // Check if FD has matured
            if (fixedDeposit.getMaturityDate() != null && fixedDeposit.getMaturityDate().isBefore(LocalDate.now())) {
                response.put("success", false);
                response.put("message", "Fixed Deposit has matured. Please process maturity first.");
                return response;
            }

            // Check if interest was already credited this month
            LocalDate today = LocalDate.now();
            if (fixedDeposit.getLastInterestCreditDate() != null) {
                LocalDate lastCredit = fixedDeposit.getLastInterestCreditDate();
                if (lastCredit.getYear() == today.getYear() && lastCredit.getMonth() == today.getMonth()) {
                    response.put("success", false);
                    response.put("message", "Interest already credited for this month");
                    return response;
                }
            }

            // Check if account is closed
            Account account = accountService.getAccountByNumber(fixedDeposit.getAccountNumber());
            if (account == null) {
                response.put("success", false);
                response.put("message", "Account not found for number: " + fixedDeposit.getAccountNumber());
                return response;
            }
            
            if ("CLOSED".equalsIgnoreCase(account.getStatus())) {
                response.put("success", false);
                response.put("message", "Cannot credit FD interest to a closed account. Account number: " + fixedDeposit.getAccountNumber());
                return response;
            }

            // Calculate monthly interest
            // Monthly interest = (Principal * Annual Rate) / 12
            Double monthlyInterest = (fixedDeposit.getPrincipalAmount() * fixedDeposit.getInterestRate()) / (12 * 100);
            monthlyInterest = Math.round(monthlyInterest * 100.0) / 100.0;

            // Credit interest to user account
            Double newBalance = accountService.creditBalance(fixedDeposit.getAccountNumber(), monthlyInterest);
            
            if (newBalance == null) {
                response.put("success", false);
                response.put("message", "Failed to credit interest to account");
                return response;
            }

            // Create transaction record
            Transaction transaction = new Transaction();
            transaction.setAccountNumber(fixedDeposit.getAccountNumber());
            transaction.setMerchant("FD Monthly Interest");
            transaction.setAmount(monthlyInterest);
            transaction.setType("Credit");
            transaction.setDescription("FD Monthly Interest - " + fixedDeposit.getFdAccountNumber() + " | Interest Rate: " + fixedDeposit.getInterestRate() + "%");
            transaction.setDate(LocalDateTime.now());
            transaction.setStatus("Completed");
            transaction.setBalance(newBalance);
            
            transactionService.saveTransaction(transaction);

            // Update FD interest tracking
            fixedDeposit.setLastInterestCreditDate(today);
            fixedDeposit.setMonthsInterestCredited(fixedDeposit.getMonthsInterestCredited() + 1);
            fixedDeposit.setTotalInterestCredited(fixedDeposit.getTotalInterestCredited() + monthlyInterest);

            FixedDeposit savedFD = fixedDepositRepository.save(fixedDeposit);

            response.put("success", true);
            response.put("message", "Monthly interest credited successfully");
            response.put("fixedDeposit", savedFD);
            response.put("monthlyInterest", monthlyInterest);
            response.put("totalInterestCredited", savedFD.getTotalInterestCredited());
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to process monthly interest: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * Process monthly interest credit for all active FDs
     * This should be called by a scheduled task every month
     */
    @Transactional
    public Map<String, Object> processAllMonthlyInterestCredits() {
        Map<String, Object> response = new HashMap<>();
        List<FixedDeposit> activeFDs = fixedDepositRepository.findByStatus("ACTIVE");
        int successCount = 0;
        int failureCount = 0;
        List<String> errors = new java.util.ArrayList<>();

        for (FixedDeposit fd : activeFDs) {
            // Skip if FD has matured
            if (fd.getMaturityDate() != null && fd.getMaturityDate().isBefore(LocalDate.now())) {
                continue;
            }

            Map<String, Object> result = processMonthlyInterestCredit(fd.getId());
            if ((Boolean) result.get("success")) {
                successCount++;
            } else {
                failureCount++;
                errors.add("FD " + fd.getFdAccountNumber() + ": " + result.get("message"));
            }
        }

        response.put("success", true);
        response.put("message", "Processed monthly interest for " + successCount + " FDs");
        response.put("successCount", successCount);
        response.put("failureCount", failureCount);
        response.put("errors", errors);
        
        return response;
    }

    /**
     * Request FD withdrawal with cheque deposit
     */
    @Transactional
    public Map<String, Object> requestWithdrawal(Long fdId, Double withdrawalAmount, String requestedBy) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            FixedDeposit fixedDeposit = fixedDepositRepository.findById(fdId)
                .orElseThrow(() -> new RuntimeException("Fixed Deposit not found"));

            if (!"ACTIVE".equals(fixedDeposit.getStatus())) {
                response.put("success", false);
                response.put("message", "Fixed Deposit is not active");
                return response;
            }

            if (withdrawalAmount == null || withdrawalAmount <= 0) {
                response.put("success", false);
                response.put("message", "Invalid withdrawal amount");
                return response;
            }

            if (withdrawalAmount > fixedDeposit.getPrincipalAmount()) {
                response.put("success", false);
                response.put("message", "Withdrawal amount cannot exceed principal amount");
                return response;
            }

            // Set withdrawal request
            fixedDeposit.setWithdrawalRequested(true);
            fixedDeposit.setWithdrawalRequestDate(LocalDateTime.now());
            fixedDeposit.setWithdrawalRequestedBy(requestedBy);
            fixedDeposit.setWithdrawalAmount(withdrawalAmount);
            fixedDeposit.setWithdrawalStatus("PENDING");

            FixedDeposit savedFD = fixedDepositRepository.save(fixedDeposit);

            response.put("success", true);
            response.put("message", "Withdrawal request submitted successfully. Waiting for admin approval.");
            response.put("fixedDeposit", savedFD);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to request withdrawal: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * Approve withdrawal request and process cheque deposit
     */
    @Transactional
    public Map<String, Object> approveWithdrawal(Long fdId, String chequeNumber, String approvedBy) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            FixedDeposit fixedDeposit = fixedDepositRepository.findById(fdId)
                .orElseThrow(() -> new RuntimeException("Fixed Deposit not found"));

            if (!"PENDING".equals(fixedDeposit.getWithdrawalStatus())) {
                response.put("success", false);
                response.put("message", "Withdrawal request is not pending");
                return response;
            }

            if (fixedDeposit.getWithdrawalAmount() == null || fixedDeposit.getWithdrawalAmount() <= 0) {
                response.put("success", false);
                response.put("message", "Invalid withdrawal amount");
                return response;
            }

            // Check if account is closed
            Account account = accountService.getAccountByNumber(fixedDeposit.getAccountNumber());
            if (account == null) {
                response.put("success", false);
                response.put("message", "Account not found for number: " + fixedDeposit.getAccountNumber());
                return response;
            }
            
            if ("CLOSED".equalsIgnoreCase(account.getStatus())) {
                response.put("success", false);
                response.put("message", "Cannot credit FD withdrawal to a closed account. Account number: " + fixedDeposit.getAccountNumber());
                return response;
            }

            // Credit withdrawal amount to user account (cheque deposit)
            Double newBalance = accountService.creditBalance(fixedDeposit.getAccountNumber(), fixedDeposit.getWithdrawalAmount());
            
            if (newBalance == null) {
                response.put("success", false);
                response.put("message", "Failed to credit amount to account");
                return response;
            }

            // Create transaction record for cheque deposit
            Transaction transaction = new Transaction();
            transaction.setAccountNumber(fixedDeposit.getAccountNumber());
            transaction.setMerchant("FD Withdrawal - Cheque Deposit");
            transaction.setAmount(fixedDeposit.getWithdrawalAmount());
            transaction.setType("Credit");
            transaction.setDescription("FD Withdrawal - " + fixedDeposit.getFdAccountNumber() + " | Cheque: " + chequeNumber + " | Amount: ₹" + fixedDeposit.getWithdrawalAmount());
            transaction.setDate(LocalDateTime.now());
            transaction.setStatus("Completed");
            transaction.setBalance(newBalance);
            
            transactionService.saveTransaction(transaction);

            // Update FD
            fixedDeposit.setWithdrawalStatus("PROCESSED");
            fixedDeposit.setWithdrawalChequeNumber(chequeNumber);
            fixedDeposit.setApprovedBy(approvedBy);
            
            // If full amount withdrawn, close FD
            if (fixedDeposit.getWithdrawalAmount().equals(fixedDeposit.getPrincipalAmount())) {
                fixedDeposit.setStatus("CLOSED");
            } else {
                // Update principal amount if partial withdrawal
                fixedDeposit.setPrincipalAmount(fixedDeposit.getPrincipalAmount() - fixedDeposit.getWithdrawalAmount());
                // Recalculate maturity amount with new principal
                if (fixedDeposit.getInterestRate() != null && fixedDeposit.getTenure() != null) {
                    Double newMaturityAmount = calculateMaturityAmount(
                        fixedDeposit.getPrincipalAmount(),
                        fixedDeposit.getInterestRate(),
                        fixedDeposit.getTenure()
                    );
                    fixedDeposit.setMaturityAmount(newMaturityAmount);
                }
            }

            FixedDeposit savedFD = fixedDepositRepository.save(fixedDeposit);

            response.put("success", true);
            response.put("message", "Withdrawal approved and processed successfully");
            response.put("fixedDeposit", savedFD);
            response.put("transaction", transaction);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to approve withdrawal: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * Reject withdrawal request
     */
    public Map<String, Object> rejectWithdrawal(Long fdId, String rejectedBy, String reason) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            FixedDeposit fixedDeposit = fixedDepositRepository.findById(fdId)
                .orElseThrow(() -> new RuntimeException("Fixed Deposit not found"));

            if (!"PENDING".equals(fixedDeposit.getWithdrawalStatus())) {
                response.put("success", false);
                response.put("message", "Withdrawal request is not pending");
                return response;
            }

            fixedDeposit.setWithdrawalStatus("REJECTED");
            fixedDeposit.setApprovedBy(rejectedBy);
            fixedDeposit.setRemarks(reason != null ? reason : "Withdrawal request rejected");

            FixedDeposit savedFD = fixedDepositRepository.save(fixedDeposit);

            response.put("success", true);
            response.put("message", "Withdrawal request rejected");
            response.put("fixedDeposit", savedFD);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to reject withdrawal: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * Get FD history (all FDs for an account or all FDs for admin)
     */
    public List<FixedDeposit> getFDHistory(String accountNumber) {
        if (accountNumber != null && !accountNumber.isEmpty()) {
            return fixedDepositRepository.findByAccountNumber(accountNumber);
        }
        return fixedDepositRepository.findAll();
    }

    /**
     * Get FD history with status filter
     */
    public List<FixedDeposit> getFDHistoryByStatus(String accountNumber, String status) {
        if (accountNumber != null && !accountNumber.isEmpty()) {
            return fixedDepositRepository.findByAccountNumberAndStatus(accountNumber, status);
        }
        return fixedDepositRepository.findByStatus(status);
    }

    /**
     * Calculate premature closure amount for FD
     */
    @Transactional
    public Map<String, Object> calculatePrematureClosure(Long fdId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            FixedDeposit fixedDeposit = fixedDepositRepository.findById(fdId)
                .orElseThrow(() -> new RuntimeException("Fixed Deposit not found"));

            if (!"ACTIVE".equals(fixedDeposit.getStatus())) {
                response.put("success", false);
                response.put("message", "Only active Fixed Deposits can be foreclosed");
                return response;
            }

            if (fixedDeposit.getStartDate() == null) {
                response.put("success", false);
                response.put("message", "FD start date not found");
                return response;
            }

            // Calculate months elapsed
            LocalDate today = LocalDate.now();
            long monthsElapsed = java.time.temporal.ChronoUnit.MONTHS.between(fixedDeposit.getStartDate(), today);
            if (monthsElapsed < 0) {
                monthsElapsed = 0;
            }

            // Calculate interest earned so far (simple interest for premature closure)
            // Interest = Principal * Rate * (Months/12)
            Double interestEarned = (fixedDeposit.getPrincipalAmount() * fixedDeposit.getInterestRate() * monthsElapsed) / (12 * 100);
            interestEarned = Math.round(interestEarned * 100.0) / 100.0;

            // Penalty: 1% of principal if closed before 6 months, 0.5% if closed after 6 months
            Double penaltyRate = monthsElapsed < 6 ? 1.0 : 0.5;
            Double penalty = (fixedDeposit.getPrincipalAmount() * penaltyRate) / 100;
            penalty = Math.round(penalty * 100.0) / 100.0;

            // Premature closure amount = Principal + Interest - Penalty
            Double closureAmount = fixedDeposit.getPrincipalAmount() + interestEarned - penalty;
            closureAmount = Math.round(closureAmount * 100.0) / 100.0;

            response.put("success", true);
            response.put("principalAmount", fixedDeposit.getPrincipalAmount());
            response.put("interestEarned", interestEarned);
            response.put("penalty", penalty);
            response.put("penaltyRate", penaltyRate);
            response.put("closureAmount", closureAmount);
            response.put("monthsElapsed", monthsElapsed);
            response.put("remainingMonths", fixedDeposit.getTenure() - (int)monthsElapsed);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to calculate premature closure: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * Process premature closure of FD. If otp is provided, verifies OTP and sets foreclosureOtpVerified.
     */
    @Transactional
    public Map<String, Object> processPrematureClosure(Long fdId, String closedBy, String otp) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (otp != null && !otp.trim().isEmpty()) {
                String key = "FD_FORECLOSURE:" + fdId;
                if (!otpService.verifyOtpByKey(key, otp)) {
                    response.put("success", false);
                    response.put("message", "Invalid or expired OTP. Please request a new OTP.");
                    return response;
                }
            }
            return doProcessPrematureClosure(fdId, closedBy, (otp != null && !otp.trim().isEmpty()));
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to process premature closure: " + e.getMessage());
            return response;
        }
    }

    @Transactional
    public Map<String, Object> processPrematureClosure(Long fdId, String closedBy) {
        return doProcessPrematureClosure(fdId, closedBy, false);
    }

    private Map<String, Object> doProcessPrematureClosure(Long fdId, String closedBy, boolean otpVerified) {
        Map<String, Object> response = new HashMap<>();
        try {
            FixedDeposit fixedDeposit = fixedDepositRepository.findById(fdId)
                .orElseThrow(() -> new RuntimeException("Fixed Deposit not found"));

            if (!"ACTIVE".equals(fixedDeposit.getStatus())) {
                response.put("success", false);
                response.put("message", "Only active Fixed Deposits can be foreclosed");
                return response;
            }

            // Calculate closure amount
            Map<String, Object> calculation = calculatePrematureClosure(fdId);
            if (!(Boolean) calculation.get("success")) {
                return calculation;
            }

            Double closureAmount = (Double) calculation.get("closureAmount");
            Double penalty = (Double) calculation.get("penalty");
            Double interestEarned = (Double) calculation.get("interestEarned");

            // Check if account is closed
            Account account = accountService.getAccountByNumber(fixedDeposit.getAccountNumber());
            if (account == null) {
                response.put("success", false);
                response.put("message", "Account not found for number: " + fixedDeposit.getAccountNumber());
                return response;
            }
            
            if ("CLOSED".equalsIgnoreCase(account.getStatus())) {
                response.put("success", false);
                response.put("message", "Cannot credit FD closure amount to a closed account. Account number: " + fixedDeposit.getAccountNumber());
                return response;
            }

            // Credit closure amount to user account
            Double newBalance = accountService.creditBalance(fixedDeposit.getAccountNumber(), closureAmount);
            
            if (newBalance == null) {
                response.put("success", false);
                response.put("message", "Failed to credit amount to account");
                return response;
            }

            // Create transaction record
            Transaction transaction = new Transaction();
            transaction.setAccountNumber(fixedDeposit.getAccountNumber());
            transaction.setMerchant("FD Premature Closure");
            transaction.setAmount(closureAmount);
            transaction.setType("Credit");
            transaction.setDescription("FD Premature Closure - " + fixedDeposit.getFdAccountNumber() + 
                " | Principal: ₹" + fixedDeposit.getPrincipalAmount() + 
                " | Interest: ₹" + interestEarned + 
                " | Penalty: ₹" + penalty);
            transaction.setDate(LocalDateTime.now());
            transaction.setStatus("Completed");
            transaction.setBalance(newBalance);
            
            transactionService.saveTransaction(transaction);

            // Update FD
            fixedDeposit.setStatus("PREMATURE_CLOSED");
            fixedDeposit.setIsPrematureClosure(true);
            fixedDeposit.setClosureDate(LocalDate.now());
            fixedDeposit.setPrematureClosureAmount(closureAmount);
            fixedDeposit.setPrematureClosurePenalty(penalty);
            fixedDeposit.setClosedBy(closedBy);
            fixedDeposit.setTransactionId(transaction.getId().toString());
            fixedDeposit.setForeclosureOtpVerified(otpVerified);

            FixedDeposit savedFD = fixedDepositRepository.save(fixedDeposit);

            response.put("success", true);
            response.put("message", "FD premature closure processed successfully");
            response.put("fixedDeposit", savedFD);
            response.put("closureAmount", closureAmount);
            response.put("penalty", penalty);
            response.put("interestEarned", interestEarned);
            response.put("transaction", transaction);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to process premature closure: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * Increase FD amount (top-up)
     */
    @Transactional
    public Map<String, Object> increaseFDAmount(Long fdId, Double additionalAmount, String requestedBy) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            FixedDeposit fixedDeposit = fixedDepositRepository.findById(fdId)
                .orElseThrow(() -> new RuntimeException("Fixed Deposit not found"));

            if (!"ACTIVE".equals(fixedDeposit.getStatus())) {
                response.put("success", false);
                response.put("message", "Only active Fixed Deposits can be topped up");
                return response;
            }

            if (additionalAmount == null || additionalAmount <= 0) {
                response.put("success", false);
                response.put("message", "Invalid amount. Amount must be greater than 0");
                return response;
            }

            // Check if account exists and is not closed
            Account account = accountService.getAccountByNumber(fixedDeposit.getAccountNumber());
            if (account == null) {
                response.put("success", false);
                response.put("message", "Account not found for number: " + fixedDeposit.getAccountNumber());
                return response;
            }
            
            if ("CLOSED".equalsIgnoreCase(account.getStatus())) {
                response.put("success", false);
                response.put("message", "Cannot top-up FD for a closed account. Account number: " + fixedDeposit.getAccountNumber());
                return response;
            }

            // Check account balance
            Double currentBalance = account.getBalance();
            if (currentBalance == null || currentBalance < additionalAmount) {
                response.put("success", false);
                response.put("message", "Insufficient balance. Required: ₹" + additionalAmount + ", Available: ₹" + currentBalance);
                return response;
            }

            // Debit amount from account
            Double newBalance = accountService.debitBalance(fixedDeposit.getAccountNumber(), additionalAmount);
            
            if (newBalance == null) {
                response.put("success", false);
                response.put("message", "Failed to debit amount from account");
                return response;
            }

            // Create transaction record
            Transaction transaction = new Transaction();
            transaction.setAccountNumber(fixedDeposit.getAccountNumber());
            transaction.setMerchant("FD Top-up");
            transaction.setAmount(additionalAmount);
            transaction.setType("Debit");
            transaction.setDescription("FD Top-up - " + fixedDeposit.getFdAccountNumber() + " | Amount: ₹" + additionalAmount);
            transaction.setDate(LocalDateTime.now());
            transaction.setStatus("Completed");
            transaction.setBalance(newBalance);
            
            transactionService.saveTransaction(transaction);

            // Update FD principal and recalculate maturity amount
            Double newPrincipal = fixedDeposit.getPrincipalAmount() + additionalAmount;
            fixedDeposit.setPrincipalAmount(newPrincipal);
            
            // Recalculate maturity amount with new principal
            if (fixedDeposit.getInterestRate() != null && fixedDeposit.getTenure() != null) {
                Double newMaturityAmount = calculateMaturityAmount(
                    newPrincipal,
                    fixedDeposit.getInterestRate(),
                    fixedDeposit.getTenure()
                );
                fixedDeposit.setMaturityAmount(newMaturityAmount);
                fixedDeposit.setInterestAmount(newMaturityAmount - newPrincipal);
            }

            FixedDeposit savedFD = fixedDepositRepository.save(fixedDeposit);

            response.put("success", true);
            response.put("message", "FD amount increased successfully");
            response.put("fixedDeposit", savedFD);
            response.put("additionalAmount", additionalAmount);
            response.put("newPrincipal", newPrincipal);
            response.put("newMaturityAmount", savedFD.getMaturityAmount());
            response.put("transaction", transaction);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to increase FD amount: " + e.getMessage());
        }
        
        return response;
    }
}

