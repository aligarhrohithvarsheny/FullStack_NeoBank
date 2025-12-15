package com.neo.springapp.service;

import com.neo.springapp.model.EmiPayment;
import com.neo.springapp.model.Loan;
import com.neo.springapp.model.Transaction;
import com.neo.springapp.repository.EmiPaymentRepository;
import com.neo.springapp.repository.LoanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
public class EmiService {

    @Autowired
    private EmiPaymentRepository emiPaymentRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private AccountService accountService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private PdfService pdfService;

    @Autowired
    private EmailService emailService;

    /**
     * Calculate EMI amount using standard formula
     */
    public Double calculateEmi(Double principal, Double annualRate, Integer tenure) {
        if (principal == null || principal <= 0 || tenure == null || tenure <= 0) {
            return 0.0;
        }
        
        double monthlyRate = annualRate / (12 * 100);
        
        if (monthlyRate > 0) {
            double emi = (principal * monthlyRate * Math.pow(1 + monthlyRate, tenure)) / 
                        (Math.pow(1 + monthlyRate, tenure) - 1);
            return Math.round(emi * 100.0) / 100.0; // Round to 2 decimal places
        } else {
            return Math.round((principal / tenure) * 100.0) / 100.0;
        }
    }

    /**
     * Generate all EMI schedules for a loan
     */
    @Transactional
    public List<EmiPayment> generateEmiSchedule(Loan loan) {
        if (loan.getEmiStartDate() == null) {
            throw new IllegalArgumentException("EMI start date must be set for the loan");
        }

        List<EmiPayment> emiPayments = new ArrayList<>();
        Double principal = loan.getAmount();
        Double annualRate = loan.getInterestRate();
        Integer tenure = loan.getTenure();
        Double monthlyRate = annualRate / (12 * 100);
        
        // Calculate EMI amount
        Double emiAmount = calculateEmi(principal, annualRate, tenure);
        
        Double remainingPrincipal = principal;
        LocalDate currentDueDate = loan.getEmiStartDate();

        // Generate EMI schedule for each month
        for (int i = 1; i <= tenure; i++) {
            EmiPayment emi = new EmiPayment();
            emi.setLoanId(loan.getId());
            emi.setLoanAccountNumber(loan.getLoanAccountNumber());
            emi.setAccountNumber(loan.getAccountNumber());
            emi.setEmiNumber(i);
            emi.setDueDate(currentDueDate);
            emi.setStatus("Pending");
            
            // Calculate interest for this month
            Double interestForMonth = remainingPrincipal * monthlyRate;
            interestForMonth = Math.round(interestForMonth * 100.0) / 100.0;
            
            // Calculate principal for this month
            Double principalForMonth = emiAmount - interestForMonth;
            principalForMonth = Math.round(principalForMonth * 100.0) / 100.0;
            
            // Adjust last EMI to account for rounding
            if (i == tenure) {
                principalForMonth = remainingPrincipal;
                emiAmount = principalForMonth + interestForMonth;
            }
            
            emi.setPrincipalAmount(principalForMonth);
            emi.setInterestAmount(interestForMonth);
            emi.setTotalAmount(emiAmount);
            
            // Update remaining principal
            remainingPrincipal -= principalForMonth;
            remainingPrincipal = Math.max(0, Math.round(remainingPrincipal * 100.0) / 100.0);
            emi.setRemainingPrincipal(remainingPrincipal);
            
            emiPayments.add(emi);
            
            // Move to next month
            currentDueDate = currentDueDate.plusMonths(1);
        }

        // Save all EMIs
        return emiPaymentRepository.saveAll(emiPayments);
    }

    /**
     * Pay a specific EMI
     */
    @Transactional
    public Map<String, Object> payEmi(Long emiId, String accountNumber) {
        Map<String, Object> response = new HashMap<>();
        
        EmiPayment emi = emiPaymentRepository.findById(emiId)
            .orElseThrow(() -> new RuntimeException("EMI not found"));
        
        if (!"Pending".equals(emi.getStatus())) {
            response.put("success", false);
            response.put("message", "EMI is already paid or not available for payment");
            return response;
        }

        // Verify account number matches
        if (!accountNumber.equals(emi.getAccountNumber())) {
            response.put("success", false);
            response.put("message", "Account number mismatch");
            return response;
        }

        // Check account balance
        Double currentBalance = accountService.getBalanceByAccountNumber(accountNumber);
        if (currentBalance == null || currentBalance < emi.getTotalAmount()) {
            response.put("success", false);
            response.put("message", "Insufficient balance. Required: ₹" + emi.getTotalAmount() + ", Available: ₹" + currentBalance);
            response.put("requiredAmount", emi.getTotalAmount());
            response.put("availableBalance", currentBalance);
            return response;
        }

        // Debit amount from savings account
        Double balanceBefore = currentBalance;
        Double newBalance = accountService.debitBalance(accountNumber, emi.getTotalAmount());
        
        if (newBalance == null) {
            response.put("success", false);
            response.put("message", "Failed to debit amount from account");
            return response;
        }

        // Update EMI payment
        emi.setStatus("Paid");
        emi.setPaymentDate(LocalDateTime.now());
        emi.setBalanceBeforePayment(balanceBefore);
        emi.setBalanceAfterPayment(newBalance);
        
        // Create transaction record
        Transaction transaction = new Transaction();
        transaction.setAccountNumber(accountNumber);
        transaction.setMerchant("Loan EMI Payment");
        transaction.setAmount(emi.getTotalAmount());
        transaction.setType("Debit");
        transaction.setDescription("EMI Payment - " + emi.getLoanAccountNumber() + " | Installment #" + emi.getEmiNumber() + 
            " | Principal: ₹" + emi.getPrincipalAmount() + ", Interest: ₹" + emi.getInterestAmount());
        transaction.setDate(LocalDateTime.now());
        transaction.setStatus("Completed");
        transaction.setBalance(newBalance);
        
        Transaction savedTransaction = transactionService.saveTransaction(transaction);
        emi.setTransactionId(savedTransaction.getId().toString());
        
        // Generate PDF receipt
        try {
            pdfService.generateEmiReceipt(emi, getLoanByLoanId(emi.getLoanId()));
            // PDF path can be stored if needed
            emi.setPdfPath("emi_receipt_" + emi.getId() + ".pdf");
        } catch (Exception e) {
            System.err.println("Error generating EMI PDF: " + e.getMessage());
        }

        EmiPayment savedEmi = emiPaymentRepository.save(emi);

        // Check if loan is fully paid
        Loan loan = getLoanByLoanId(emi.getLoanId());
        if (loan != null) {
            Long paidCount = emiPaymentRepository.countPaidEmisByLoanId(loan.getId());
            Long totalCount = emiPaymentRepository.countTotalEmisByLoanId(loan.getId());
            
            if (paidCount != null && totalCount != null && paidCount.equals(totalCount)) {
                // Loan is fully paid
                loan.setStatus("Paid");
                loanRepository.save(loan);
            }
        }

        response.put("success", true);
        response.put("message", "EMI paid successfully");
        response.put("emi", savedEmi);
        response.put("transaction", savedTransaction);
        response.put("newBalance", newBalance);
        
        return response;
    }

    /**
     * Get all EMIs for a loan
     */
    public List<EmiPayment> getEmisByLoanId(Long loanId) {
        return emiPaymentRepository.findByLoanIdOrderByEmiNumberAsc(loanId);
    }

    /**
     * Get all EMIs for a loan account number
     */
    public List<EmiPayment> getEmisByLoanAccountNumber(String loanAccountNumber) {
        return emiPaymentRepository.findByLoanAccountNumberOrderByEmiNumberAsc(loanAccountNumber);
    }

    /**
     * Get all EMIs for user's account
     */
    public List<EmiPayment> getEmisByAccountNumber(String accountNumber) {
        return emiPaymentRepository.findByAccountNumberOrderByDueDateDesc(accountNumber);
    }

    /**
     * Get pending EMIs for a loan
     */
    public List<EmiPayment> getPendingEmisByLoanId(Long loanId) {
        return emiPaymentRepository.findByLoanIdAndStatusOrderByDueDateAsc(loanId, "Pending");
    }

    /**
     * Get overdue EMIs
     */
    public List<EmiPayment> getOverdueEmis() {
        return emiPaymentRepository.findOverdueEmis(LocalDate.now());
    }

    /**
     * Get upcoming EMIs (due in next 7 days)
     */
    public List<EmiPayment> getUpcomingEmis() {
        LocalDate today = LocalDate.now();
        LocalDate nextWeek = today.plusDays(7);
        return emiPaymentRepository.findUpcomingEmis(today, nextWeek);
    }

    /**
     * Get EMI details with loan information
     */
    public Map<String, Object> getEmiDetails(Long emiId) {
        Map<String, Object> response = new HashMap<>();
        
        EmiPayment emi = emiPaymentRepository.findById(emiId)
            .orElseThrow(() -> new RuntimeException("EMI not found"));
        
        Loan loan = getLoanByLoanId(emi.getLoanId());
        
        response.put("emi", emi);
        response.put("loan", loan);
        
        // Calculate summary
        List<EmiPayment> allEmis = getEmisByLoanId(emi.getLoanId());
        long paidCount = allEmis.stream().filter(e -> "Paid".equals(e.getStatus())).count();
        long pendingCount = allEmis.stream().filter(e -> "Pending".equals(e.getStatus())).count();
        
        response.put("totalEmis", allEmis.size());
        response.put("paidEmis", paidCount);
        response.put("pendingEmis", pendingCount);
        
        return response;
    }

    /**
     * Get next due EMI for a loan
     */
    public EmiPayment getNextDueEmi(Long loanId) {
        List<EmiPayment> nextEmis = emiPaymentRepository.findNextDueEmi(loanId);
        return nextEmis.isEmpty() ? null : nextEmis.get(0);
    }

    /**
     * Send reminder for upcoming EMIs
     */
    public void sendEmiReminders() {
        List<EmiPayment> upcomingEmis = getUpcomingEmis();
        
        for (EmiPayment emi : upcomingEmis) {
            Loan loan = getLoanByLoanId(emi.getLoanId());
            if (loan != null && loan.getUserEmail() != null) {
                try {
                    emailService.sendEmiReminderEmail(
                        loan.getUserEmail(),
                        loan.getUserName(),
                        loan.getLoanAccountNumber(),
                        emi.getDueDate(),
                        emi.getTotalAmount(),
                        emi.getEmiNumber()
                    );
                } catch (Exception e) {
                    System.err.println("Error sending EMI reminder: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Get all EMIs (for admin/manager)
     */
    public List<EmiPayment> getAllEmis() {
        return emiPaymentRepository.findAll();
    }

    /**
     * Helper method to get loan by ID
     */
    private Loan getLoanByLoanId(Long loanId) {
        return loanRepository.findById(loanId).orElse(null);
    }
}

