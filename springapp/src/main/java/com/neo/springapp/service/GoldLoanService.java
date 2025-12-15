package com.neo.springapp.service;

import com.neo.springapp.model.GoldLoan;
import com.neo.springapp.model.Account;
import com.neo.springapp.model.EmiPayment;
import com.neo.springapp.repository.GoldLoanRepository;
import com.neo.springapp.repository.EmiPaymentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

@Service
public class GoldLoanService {

    private final GoldLoanRepository goldLoanRepository;
    private final GoldRateService goldRateService;
    private final AccountService accountService;
    private final EmiPaymentRepository emiPaymentRepository;

    public GoldLoanService(GoldLoanRepository goldLoanRepository, 
                          GoldRateService goldRateService,
                          AccountService accountService,
                          EmiPaymentRepository emiPaymentRepository) {
        this.goldLoanRepository = goldLoanRepository;
        this.goldRateService = goldRateService;
        this.accountService = accountService;
        this.emiPaymentRepository = emiPaymentRepository;
    }

    // Apply for gold loan
    public GoldLoan applyGoldLoan(GoldLoan goldLoan) {
        // Get current gold rate
        Double currentRate = goldRateService.getCurrentGoldRate().getRatePerGram();
        goldLoan.setGoldRatePerGram(currentRate);
        
        // Calculate gold value and loan amount
        goldLoan.calculateLoanAmount();
        
        // Generate loan account number
        goldLoan.generateLoanAccountNumber();
        
        // Set application date
        if (goldLoan.getApplicationDate() == null) {
            goldLoan.setApplicationDate(LocalDateTime.now());
        }
        
        // Set default interest rate (can be configured)
        if (goldLoan.getInterestRate() == null) {
            goldLoan.setInterestRate(12.0); // 12% per annum
        }
        
        // Set default tenure if not provided
        if (goldLoan.getTenure() == null) {
            goldLoan.setTenure(12); // 12 months default
        }
        
        return goldLoanRepository.save(goldLoan);
    }

    // Get all gold loans
    public List<GoldLoan> getAllGoldLoans() {
        return goldLoanRepository.findAll();
    }

    // Get gold loans by account number
    public List<GoldLoan> getGoldLoansByAccountNumber(String accountNumber) {
        return goldLoanRepository.findByAccountNumber(accountNumber);
    }

    // Get gold loan by ID
    public GoldLoan getGoldLoanById(Long id) {
        return goldLoanRepository.findById(id).orElse(null);
    }

    // Get gold loans by status
    public List<GoldLoan> getGoldLoansByStatus(String status) {
        return goldLoanRepository.findByStatus(status);
    }

    // Approve or reject gold loan
    public GoldLoan approveGoldLoan(Long id, String status, String approvedBy, Map<String, Object> goldDetails) {
        GoldLoan goldLoan = goldLoanRepository.findById(id).orElse(null);
        if (goldLoan != null) {
            goldLoan.setStatus(status);
            goldLoan.setApprovalDate(LocalDateTime.now());
            goldLoan.setApprovedBy(approvedBy);
            
            // If approved, save gold details provided by admin
            if ("Approved".equals(status) && goldDetails != null) {
                // Save gold items description
                if (goldDetails.containsKey("goldItems")) {
                    goldLoan.setGoldItems(goldDetails.get("goldItems").toString());
                }
                
                // Save gold description
                if (goldDetails.containsKey("goldDescription")) {
                    goldLoan.setGoldDescription(goldDetails.get("goldDescription").toString());
                }
                
                // Save verified purity
                if (goldDetails.containsKey("goldPurity")) {
                    goldLoan.setGoldPurity(goldDetails.get("goldPurity").toString());
                } else {
                    goldLoan.setGoldPurity("22K"); // Default to 22K
                }
                
                // Save verified gold grams
                if (goldDetails.containsKey("verifiedGoldGrams")) {
                    try {
                        Double verifiedGrams = Double.valueOf(goldDetails.get("verifiedGoldGrams").toString());
                        goldLoan.setVerifiedGoldGrams(verifiedGrams);
                    } catch (Exception e) {
                        // If verification fails, use original grams
                        goldLoan.setVerifiedGoldGrams(goldLoan.getGoldGrams());
                    }
                } else {
                    goldLoan.setVerifiedGoldGrams(goldLoan.getGoldGrams());
                }
                
                // Recalculate verified gold value if verified grams differ
                Double verifiedValue = null;
                if (goldLoan.getVerifiedGoldGrams() != null && goldLoan.getGoldRatePerGram() != null) {
                    verifiedValue = goldLoan.getVerifiedGoldGrams() * goldLoan.getGoldRatePerGram();
                    goldLoan.setVerifiedGoldValue(verifiedValue);
                } else {
                    verifiedValue = goldLoan.getGoldValue();
                    goldLoan.setVerifiedGoldValue(verifiedValue);
                }
                
                // IMPORTANT: Recalculate loan amount as 75% of verified gold value
                // This ensures loan amount is based on actual verified gold, not user's input
                goldLoan.calculateLoanAmountFromVerified();
                Double recalculatedLoanAmount = goldLoan.getLoanAmount();
                System.out.println("✅ Recalculated loan amount: ₹" + recalculatedLoanAmount + 
                                 " (75% of verified gold value: ₹" + verifiedValue + ")");
                
                // Calculate interest automatically if not provided
                if (goldLoan.getInterestRate() == null) {
                    goldLoan.setInterestRate(12.0); // Default 12% per annum
                    System.out.println("✅ Auto-set interest rate: 12% per annum");
                }
                
                // Calculate EMI automatically based on recalculated loan amount, interest rate, and tenure
                Double calculatedEMI = goldLoan.calculateEMI();
                if (calculatedEMI > 0) {
                    System.out.println("✅ Auto-calculated EMI: ₹" + calculatedEMI + 
                                     " (Loan Amount: ₹" + recalculatedLoanAmount + 
                                     ", Interest Rate: " + goldLoan.getInterestRate() + "% p.a., " +
                                     "Tenure: " + goldLoan.getTenure() + " months)");
                }
                
                // Save verification notes
                if (goldDetails.containsKey("verificationNotes")) {
                    goldLoan.setVerificationNotes(goldDetails.get("verificationNotes").toString());
                }
                
                // Save storage location
                if (goldDetails.containsKey("storageLocation")) {
                    goldLoan.setStorageLocation(goldDetails.get("storageLocation").toString());
                }
                
                // Credit the recalculated loan amount (75% of verified gold value) to user's account
                Account account = accountService.getAccountByNumber(goldLoan.getAccountNumber());
                if (account != null) {
                    accountService.creditBalance(goldLoan.getAccountNumber(), recalculatedLoanAmount);
                    System.out.println("✅ Loan amount ₹" + recalculatedLoanAmount + 
                                     " credited to account: " + goldLoan.getAccountNumber());
                } else {
                    System.out.println("⚠️ Account not found: " + goldLoan.getAccountNumber());
                }
                
                // Set EMI start date
                goldLoan.setEmiStartDate(LocalDate.now());
                
                // Generate EMI schedule for approved loan
                try {
                    List<EmiPayment> emiSchedule = generateEmiSchedule(goldLoan);
                    System.out.println("✅ EMI schedule generated for gold loan: " + emiSchedule.size() + " installments");
                } catch (Exception emiException) {
                    System.err.println("⚠️ Error generating EMI schedule for gold loan: " + emiException.getMessage());
                }
                
                // Reset terms acceptance (user needs to accept after admin approval)
                goldLoan.setTermsAccepted(false);
                goldLoan.setTermsAcceptedDate(null);
                goldLoan.setTermsAcceptedBy(null);
            }
            
            return goldLoanRepository.save(goldLoan);
        }
        return null;
    }

    // Accept terms and conditions by user
    public GoldLoan acceptTerms(Long id, String acceptedBy) {
        GoldLoan goldLoan = goldLoanRepository.findById(id).orElse(null);
        if (goldLoan != null && "Approved".equals(goldLoan.getStatus())) {
            goldLoan.setTermsAccepted(true);
            goldLoan.setTermsAcceptedDate(LocalDateTime.now());
            goldLoan.setTermsAcceptedBy(acceptedBy);
            return goldLoanRepository.save(goldLoan);
        }
        return null;
    }

    // Get gold loan by loan account number
    public GoldLoan getGoldLoanByLoanAccountNumber(String loanAccountNumber) {
        return goldLoanRepository.findByLoanAccountNumber(loanAccountNumber).orElse(null);
    }

    // Calculate foreclosure amount for gold loan
    public Map<String, Object> calculateForeclosure(String loanAccountNumber) {
        Map<String, Object> result = new java.util.HashMap<>();
        
        GoldLoan goldLoan = getGoldLoanByLoanAccountNumber(loanAccountNumber);
        if (goldLoan == null) {
            result.put("success", false);
            result.put("message", "Gold loan not found");
            return result;
        }

        if (!"Approved".equals(goldLoan.getStatus())) {
            result.put("success", false);
            result.put("message", "Only approved gold loans can be foreclosed");
            return result;
        }

        if ("Foreclosed".equals(goldLoan.getStatus())) {
            result.put("success", false);
            result.put("message", "Gold loan is already foreclosed");
            return result;
        }

        // Calculate months elapsed since approval
        LocalDateTime approvalDate = goldLoan.getApprovalDate();
        if (approvalDate == null) {
            approvalDate = goldLoan.getApplicationDate();
        }
        
        long monthsElapsed = java.time.temporal.ChronoUnit.MONTHS.between(approvalDate, LocalDateTime.now());
        if (monthsElapsed < 0) {
            monthsElapsed = 0;
        }

        // Calculate EMI
        Double principal = goldLoan.getLoanAmount();
        Double annualRate = goldLoan.getInterestRate();
        Integer tenure = goldLoan.getTenure();
        Double monthlyRate = annualRate / (12 * 100);
        
        Double emi = 0.0;
        if (monthlyRate > 0) {
            emi = (principal * monthlyRate * Math.pow(1 + monthlyRate, tenure)) / 
                  (Math.pow(1 + monthlyRate, tenure) - 1);
        } else {
            emi = principal / tenure;
        }

        // Calculate total amount paid so far (EMI * months elapsed)
        Double totalPaid = emi * monthsElapsed;
        
        // Calculate principal and interest paid
        Double principalPaid = 0.0;
        Double interestPaid = 0.0;
        Double remainingPrincipal = principal;
        
        // Calculate principal and interest for each month
        for (int i = 0; i < monthsElapsed && i < tenure; i++) {
            Double interestForMonth = remainingPrincipal * monthlyRate;
            Double principalForMonth = emi - interestForMonth;
            principalPaid += principalForMonth;
            interestPaid += interestForMonth;
            remainingPrincipal -= principalForMonth;
        }

        // Remaining principal
        Double remainingPrincipalAmount = Math.max(0.0, remainingPrincipal);
        
        // Calculate remaining interest (interest on remaining principal for remaining tenure)
        int remainingMonths = (int)(tenure - monthsElapsed);
        Double remainingInterest = 0.0;
        if (remainingMonths > 0 && monthlyRate > 0) {
            // Calculate interest on remaining principal for remaining months
            Double tempEMI = (remainingPrincipalAmount * monthlyRate * Math.pow(1 + monthlyRate, remainingMonths)) / 
                            (Math.pow(1 + monthlyRate, remainingMonths) - 1);
            Double totalRemainingAmount = tempEMI * remainingMonths;
            remainingInterest = totalRemainingAmount - remainingPrincipalAmount;
        }

        // Foreclosure charges: 4% of remaining principal
        Double foreclosureCharges = remainingPrincipalAmount * 0.04;
        
        // GST: 18% on foreclosure charges
        Double gst = foreclosureCharges * 0.18;
        
        // Total foreclosure amount
        Double totalForeclosureAmount = remainingPrincipalAmount + remainingInterest + foreclosureCharges + gst;

        result.put("success", true);
        result.put("loan", goldLoan);
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

    // Process foreclosure for gold loan
    public GoldLoan processForeclosure(String loanAccountNumber, String foreclosedBy) {
        GoldLoan goldLoan = getGoldLoanByLoanAccountNumber(loanAccountNumber);
        if (goldLoan == null) {
            return null;
        }

        // Calculate foreclosure details
        Map<String, Object> foreclosureCalc = calculateForeclosure(loanAccountNumber);
        if (!(Boolean) foreclosureCalc.get("success")) {
            return null;
        }

        // Update gold loan with foreclosure details
        goldLoan.setStatus("Foreclosed");
        goldLoan.setForeclosureDate(LocalDateTime.now());
        // Use calculated amount if not already set (for edited amounts, it will be set separately)
        if (goldLoan.getForeclosureAmount() == null) {
            goldLoan.setForeclosureAmount((Double) foreclosureCalc.get("totalForeclosureAmount"));
        }
        goldLoan.setForeclosureCharges((Double) foreclosureCalc.get("foreclosureCharges"));
        goldLoan.setForeclosureGst((Double) foreclosureCalc.get("gst"));
        goldLoan.setPrincipalPaid((Double) foreclosureCalc.get("principalPaid"));
        goldLoan.setInterestPaid((Double) foreclosureCalc.get("interestPaid"));
        goldLoan.setRemainingPrincipal((Double) foreclosureCalc.get("remainingPrincipal"));
        goldLoan.setRemainingInterest((Double) foreclosureCalc.get("remainingInterest"));
        goldLoan.setForeclosedBy(foreclosedBy);

        return goldLoanRepository.save(goldLoan);
    }

    // Process foreclosure with custom amount
    public GoldLoan processForeclosureWithAmount(String loanAccountNumber, String foreclosedBy, Double foreclosureAmount) {
        GoldLoan goldLoan = getGoldLoanByLoanAccountNumber(loanAccountNumber);
        if (goldLoan == null) {
            return null;
        }

        // Calculate foreclosure details for reference
        Map<String, Object> foreclosureCalc = calculateForeclosure(loanAccountNumber);
        if (!(Boolean) foreclosureCalc.get("success")) {
            return null;
        }

        // Update gold loan with foreclosure details using custom amount
        goldLoan.setStatus("Foreclosed");
        goldLoan.setForeclosureDate(LocalDateTime.now());
        goldLoan.setForeclosureAmount(foreclosureAmount);
        goldLoan.setForeclosureCharges((Double) foreclosureCalc.get("foreclosureCharges"));
        goldLoan.setForeclosureGst((Double) foreclosureCalc.get("gst"));
        goldLoan.setPrincipalPaid((Double) foreclosureCalc.get("principalPaid"));
        goldLoan.setInterestPaid((Double) foreclosureCalc.get("interestPaid"));
        goldLoan.setRemainingPrincipal((Double) foreclosureCalc.get("remainingPrincipal"));
        // Calculate interest based on custom foreclosure amount
        Double remainingPrincipal = (Double) foreclosureCalc.get("remainingPrincipal");
        Double charges = (Double) foreclosureCalc.get("foreclosureCharges");
        Double gst = (Double) foreclosureCalc.get("gst");
        Double calculatedInterest = foreclosureAmount - remainingPrincipal - charges - gst;
        goldLoan.setRemainingInterest(Math.max(0.0, calculatedInterest));
        goldLoan.setForeclosedBy(foreclosedBy);

        return goldLoanRepository.save(goldLoan);
    }

    // Generate EMI schedule for gold loan
    public List<EmiPayment> generateEmiSchedule(GoldLoan goldLoan) {
        if (goldLoan.getEmiStartDate() == null) {
            throw new IllegalArgumentException("EMI start date must be set for the gold loan");
        }

        List<EmiPayment> emiPayments = new ArrayList<>();
        Double principal = goldLoan.getLoanAmount();
        Double annualRate = goldLoan.getInterestRate();
        Integer tenure = goldLoan.getTenure();
        Double monthlyRate = annualRate / (12 * 100);
        
        // Calculate EMI amount
        Double emiAmount = goldLoan.calculateEMI();
        
        Double remainingPrincipal = principal;
        LocalDate currentDueDate = goldLoan.getEmiStartDate();

        // Generate EMI schedule for each month
        for (int i = 1; i <= tenure; i++) {
            EmiPayment emi = new EmiPayment();
            emi.setLoanId(goldLoan.getId());
            emi.setLoanAccountNumber(goldLoan.getLoanAccountNumber());
            emi.setAccountNumber(goldLoan.getAccountNumber());
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
            remainingPrincipal = Math.max(0.0, Math.round(remainingPrincipal * 100.0) / 100.0);
            emi.setRemainingPrincipal(remainingPrincipal);
            
            emiPayments.add(emi);
            
            // Move to next month
            currentDueDate = currentDueDate.plusMonths(1);
        }

        // Save all EMIs
        return emiPaymentRepository.saveAll(emiPayments);
    }

    // Get EMI schedule for gold loan
    public List<EmiPayment> getEmiSchedule(String loanAccountNumber) {
        return emiPaymentRepository.findByLoanAccountNumberOrderByEmiNumberAsc(loanAccountNumber);
    }

    // Update gold loan interest rate
    public GoldLoan updateInterestRate(Long id, Double interestRate) {
        GoldLoan goldLoan = goldLoanRepository.findById(id).orElse(null);
        if (goldLoan != null) {
            goldLoan.setInterestRate(interestRate);
            return goldLoanRepository.save(goldLoan);
        }
        return null;
    }
}

