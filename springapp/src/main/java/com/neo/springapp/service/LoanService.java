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
        
        // If PAN is provided but CIBIL info is not, calculate it
        if (loan.getPan() != null && !loan.getPan().trim().isEmpty()) {
            if (loan.getCibilScore() == null) {
                // This will be set by the controller before saving
                // We keep it here as a fallback
            }
        }
        
        return loanRepository.save(loan);
    }

    // Get all loans
    public List<Loan> getAllLoans() {
        return loanRepository.findAll();
    }

    // Get loans by account number (includes both applicant and child accounts for education loans)
    public List<Loan> getLoansByAccountNumber(String accountNumber) {
        // Get loans where user is the applicant
        List<Loan> applicantLoans = loanRepository.findByAccountNumber(accountNumber);
        
        // Get loans where user is the child (for education loans)
        List<Loan> childLoans = loanRepository.findByChildAccountNumber(accountNumber);
        
        // Combine both lists and remove duplicates
        java.util.Set<Loan> allLoans = new java.util.HashSet<>(applicantLoans);
        allLoans.addAll(childLoans);
        
        return new java.util.ArrayList<>(allLoans);
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
            
            // If approved, add loan amount to user's account balance and set EMI start date
            if ("Approved".equals(status)) {
                Account account = accountService.getAccountByNumber(loan.getAccountNumber());
                if (account != null) {
                    accountService.creditBalance(loan.getAccountNumber(), loan.getAmount());
                }
                
                // Set EMI start date to approval date (loan sanction date)
                java.time.LocalDate emiStartDate = java.time.LocalDate.now();
                loan.setEmiStartDate(emiStartDate);
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

    // Get loan by loan account number
    public Loan getLoanByLoanAccountNumber(String loanAccountNumber) {
        return loanRepository.findByLoanAccountNumber(loanAccountNumber).orElse(null);
    }

    // Calculate foreclosure amount
    public java.util.Map<String, Object> calculateForeclosure(String loanAccountNumber) {
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        
        Loan loan = getLoanByLoanAccountNumber(loanAccountNumber);
        if (loan == null) {
            result.put("success", false);
            result.put("message", "Loan not found");
            return result;
        }

        if (!"Approved".equals(loan.getStatus())) {
            result.put("success", false);
            result.put("message", "Only approved loans can be foreclosed");
            return result;
        }

        if ("Foreclosed".equals(loan.getStatus())) {
            result.put("success", false);
            result.put("message", "Loan is already foreclosed");
            return result;
        }

        // Calculate months elapsed since approval
        LocalDateTime approvalDate = loan.getApprovalDate();
        if (approvalDate == null) {
            approvalDate = loan.getApplicationDate();
        }
        
        long monthsElapsed = java.time.temporal.ChronoUnit.MONTHS.between(approvalDate, LocalDateTime.now());
        if (monthsElapsed < 0) {
            monthsElapsed = 0;
        }

        // Calculate EMI
        double principal = loan.getAmount();
        double annualRate = loan.getInterestRate();
        int tenure = loan.getTenure();
        double monthlyRate = annualRate / (12 * 100);
        
        double emi = 0;
        if (monthlyRate > 0) {
            emi = (principal * monthlyRate * Math.pow(1 + monthlyRate, tenure)) / 
                  (Math.pow(1 + monthlyRate, tenure) - 1);
        } else {
            emi = principal / tenure;
        }

        // Calculate total amount paid so far (EMI * months elapsed)
        double totalPaid = emi * monthsElapsed;
        
        // Calculate principal and interest paid
        double principalPaid = 0;
        double interestPaid = 0;
        double remainingPrincipal = principal;
        
        // Calculate principal and interest for each month
        for (int i = 0; i < monthsElapsed && i < tenure; i++) {
            double interestForMonth = remainingPrincipal * monthlyRate;
            double principalForMonth = emi - interestForMonth;
            principalPaid += principalForMonth;
            interestPaid += interestForMonth;
            remainingPrincipal -= principalForMonth;
        }

        // Remaining principal
        double remainingPrincipalAmount = Math.max(0, remainingPrincipal);
        
        // Calculate remaining interest (interest on remaining principal for remaining tenure)
        int remainingMonths = (int)(tenure - monthsElapsed);
        double remainingInterest = 0;
        if (remainingMonths > 0 && monthlyRate > 0) {
            // Calculate interest on remaining principal for remaining months
            double tempEMI = (remainingPrincipalAmount * monthlyRate * Math.pow(1 + monthlyRate, remainingMonths)) / 
                            (Math.pow(1 + monthlyRate, remainingMonths) - 1);
            double totalRemainingAmount = tempEMI * remainingMonths;
            remainingInterest = totalRemainingAmount - remainingPrincipalAmount;
        }

        // Foreclosure charges: 4% of remaining principal
        double foreclosureCharges = remainingPrincipalAmount * 0.04;
        
        // GST: 18% on foreclosure charges
        double gst = foreclosureCharges * 0.18;
        
        // Total foreclosure amount
        double totalForeclosureAmount = remainingPrincipalAmount + remainingInterest + foreclosureCharges + gst;

        result.put("success", true);
        result.put("loan", loan);
        result.put("monthsElapsed", monthsElapsed);
        result.put("emi", emi);
        result.put("totalPaid", totalPaid);
        result.put("principalPaid", principalPaid);
        result.put("interestPaid", interestPaid);
        result.put("remainingPrincipal", remainingPrincipalAmount);
        result.put("remainingInterest", remainingInterest);
        result.put("foreclosureCharges", foreclosureCharges);
        result.put("gst", gst);
        result.put("totalForeclosureAmount", totalForeclosureAmount);
        result.put("remainingMonths", remainingMonths);

        return result;
    }

    // Process foreclosure
    public Loan processForeclosure(String loanAccountNumber, String foreclosedBy) {
        Loan loan = getLoanByLoanAccountNumber(loanAccountNumber);
        if (loan == null) {
            return null;
        }

        // Calculate foreclosure details
        java.util.Map<String, Object> foreclosureCalc = calculateForeclosure(loanAccountNumber);
        if (!(Boolean) foreclosureCalc.get("success")) {
            return null;
        }

        // Update loan with foreclosure details
        loan.setStatus("Foreclosed");
        loan.setForeclosureDate(LocalDateTime.now());
        loan.setForeclosureAmount((Double) foreclosureCalc.get("totalForeclosureAmount"));
        loan.setForeclosureCharges((Double) foreclosureCalc.get("foreclosureCharges"));
        loan.setForeclosureGst((Double) foreclosureCalc.get("gst"));
        loan.setPrincipalPaid((Double) foreclosureCalc.get("principalPaid"));
        loan.setInterestPaid((Double) foreclosureCalc.get("interestPaid"));
        loan.setRemainingPrincipal((Double) foreclosureCalc.get("remainingPrincipal"));
        loan.setRemainingInterest((Double) foreclosureCalc.get("remainingInterest"));
        loan.setForeclosedBy(foreclosedBy);

        return loanRepository.save(loan);
    }

}
