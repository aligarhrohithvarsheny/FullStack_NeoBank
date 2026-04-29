package com.neo.springapp.service;

import com.neo.springapp.model.Account;
import com.neo.springapp.model.EmiPayment;
import com.neo.springapp.model.Loan;
import com.neo.springapp.model.Transaction;
import com.neo.springapp.model.SalaryAccount;
import com.neo.springapp.model.SalaryNormalTransaction;
import com.neo.springapp.model.GoldLoan;
import com.neo.springapp.repository.EmiPaymentRepository;
import com.neo.springapp.repository.GoldLoanRepository;
import com.neo.springapp.repository.LoanRepository;
import com.neo.springapp.repository.SalaryAccountRepository;
import com.neo.springapp.repository.SalaryNormalTransactionRepository;
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
@SuppressWarnings("null")
public class EmiService {

    @Autowired
    private EmiPaymentRepository emiPaymentRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private GoldLoanRepository goldLoanRepository;

    @Autowired
    private AccountService accountService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private PdfService pdfService;

    @Autowired
    private EmailService emailService;

    @Autowired(required = false)
    private BranchAccountService branchAccountService;

    @Autowired
    private SalaryAccountRepository salaryAccountRepository;

    @Autowired
    private SalaryNormalTransactionRepository salaryNormalTransactionRepository;

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

        // Prevent duplicate EMI generation — if EMIs already exist for this loan, return them
        List<EmiPayment> existingEmis = emiPaymentRepository.findByLoanIdOrderByEmiNumberAsc(loan.getId());
        if (existingEmis != null && !existingEmis.isEmpty()) {
            System.out.println("EMI schedule already exists for loan " + loan.getLoanAccountNumber() + " (" + existingEmis.size() + " EMIs). Skipping generation.");
            return existingEmis;
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

        // Check account balance - try regular accounts first, then salary accounts
        Double currentBalance = accountService.getBalanceByAccountNumber(accountNumber);
        boolean isSalaryAccount = false;
        SalaryAccount salaryAccount = null;
        if (currentBalance == null) {
            // Fallback: check salary_accounts table
            salaryAccount = salaryAccountRepository.findByAccountNumber(accountNumber);
            if (salaryAccount != null && salaryAccount.getBalance() != null) {
                currentBalance = salaryAccount.getBalance();
                isSalaryAccount = true;
            }
        }
        if (currentBalance == null || currentBalance < emi.getTotalAmount()) {
            response.put("success", false);
            response.put("message", "Insufficient balance. Required: ₹" + emi.getTotalAmount() + ", Available: ₹" + currentBalance);
            response.put("requiredAmount", emi.getTotalAmount());
            response.put("availableBalance", currentBalance);
            return response;
        }

        // Debit amount from account
        Double balanceBefore = currentBalance;
        Double newBalance;
        if (isSalaryAccount && salaryAccount != null) {
            // Debit from salary account
            Double updatedBal = salaryAccount.getBalance() - emi.getTotalAmount();
            salaryAccount.setBalance(updatedBal);
            salaryAccount.setUpdatedAt(LocalDateTime.now());
            salaryAccountRepository.save(salaryAccount);
            newBalance = updatedBal;
        } else {
            newBalance = accountService.debitBalance(accountNumber, emi.getTotalAmount());
        }

        if (newBalance == null) {
            response.put("success", false);
            response.put("message", "Failed to debit amount from account");
            return response;
        }

        // Map loan interest to manager branch account in real time (credit branch with interest portion)
        Double interestAmount = emi.getInterestAmount() != null ? emi.getInterestAmount() : 0.0;
        if (branchAccountService != null && interestAmount > 0) {
            String depositAccount = branchAccountService.getDepositAccountNumber();
            Account branchAccount = accountService.getAccountByNumber(depositAccount);
            if (branchAccount != null) {
                accountService.creditBalance(depositAccount, interestAmount);
                Double branchBalance = accountService.getBalanceByAccountNumber(depositAccount);
                Transaction creditTxn = new Transaction();
                creditTxn.setMerchant("Loan EMI Interest - " + accountNumber);
                creditTxn.setAmount(interestAmount);
                creditTxn.setType("Credit");
                creditTxn.setDescription("Loan interest (EMI #" + emi.getEmiNumber() + " from " + accountNumber + ")");
                creditTxn.setAccountNumber(depositAccount);
                creditTxn.setUserName("NeoBank");
                creditTxn.setSourceAccountNumber(accountNumber);
                creditTxn.setBalance(branchBalance != null ? branchBalance : interestAmount);
                creditTxn.setDate(LocalDateTime.now());
                creditTxn.setStatus("Completed");
                transactionService.saveTransaction(creditTxn);
            }
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

        // Also create salary normal transaction if it's a salary account
        if (isSalaryAccount && salaryAccount != null) {
            SalaryNormalTransaction salTxn = new SalaryNormalTransaction();
            salTxn.setSalaryAccountId(salaryAccount.getId());
            salTxn.setAccountNumber(accountNumber);
            salTxn.setType("Debit");
            salTxn.setAmount(emi.getTotalAmount());
            salTxn.setRemark("Gold Loan EMI Payment - " + emi.getLoanAccountNumber() + " | Installment #" + emi.getEmiNumber());
            salTxn.setPreviousBalance(balanceBefore);
            salTxn.setNewBalance(newBalance);
            salTxn.setStatus("Success");
            salaryNormalTransactionRepository.save(salTxn);
        }
        
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
     * Get all EMIs for user's account, excluding EMIs for foreclosed loans
     */
    public List<EmiPayment> getEmisByAccountNumber(String accountNumber) {
        List<EmiPayment> allEmis = emiPaymentRepository.findByAccountNumberOrderByDueDateDesc(accountNumber);

        // Filter out pending/overdue EMIs belonging to foreclosed loans
        List<EmiPayment> filtered = new ArrayList<>();
        Map<Long, String> loanStatusCache = new HashMap<>();

        for (EmiPayment emi : allEmis) {
            // Already paid EMIs always show (historical record)
            if ("Paid".equals(emi.getStatus())) {
                filtered.add(emi);
                continue;
            }

            // Cancelled EMIs from foreclosure — skip them
            if ("Cancelled".equals(emi.getStatus())) {
                continue;
            }

            // For pending EMIs, check if the loan is foreclosed (check both regular loans and gold loans)
            Long loanId = emi.getLoanId();
            String loanStatus = loanStatusCache.get(loanId);
            if (loanStatus == null) {
                Loan loan = loanRepository.findById(loanId).orElse(null);
                if (loan != null) {
                    loanStatus = loan.getStatus();
                } else {
                    // Check if it's a gold loan
                    GoldLoan goldLoan = goldLoanRepository.findById(loanId).orElse(null);
                    loanStatus = (goldLoan != null) ? goldLoan.getStatus() : "Unknown";
                }
                loanStatusCache.put(loanId, loanStatus);
            }

            if (!"Foreclosed".equals(loanStatus)) {
                filtered.add(emi);
            }
            // If loan is foreclosed, skip pending EMIs (they should have been cancelled)
        }

        return filtered;
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
     * Get all EMIs (for admin/manager) - enriched with customerId from account for admin display
     */
    public List<EmiPayment> getAllEmis() {
        List<EmiPayment> emis = emiPaymentRepository.findAll();
        for (EmiPayment emi : emis) {
            if (emi.getAccountNumber() != null) {
                Account account = accountService.getAccountByNumber(emi.getAccountNumber());
                if (account != null && account.getCustomerId() != null) {
                    emi.setCustomerId(account.getCustomerId());
                }
            }
        }
        return emis;
    }

    /**
     * Helper method to get loan by ID
     */
    private Loan getLoanByLoanId(Long loanId) {
        return loanRepository.findById(loanId).orElse(null);
    }
}

