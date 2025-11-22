package com.neo.springapp.service;

import com.neo.springapp.model.GoldLoan;
import com.neo.springapp.model.Account;
import com.neo.springapp.repository.GoldLoanRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class GoldLoanService {

    private final GoldLoanRepository goldLoanRepository;
    private final GoldRateService goldRateService;
    private final AccountService accountService;

    public GoldLoanService(GoldLoanRepository goldLoanRepository, 
                          GoldRateService goldRateService,
                          AccountService accountService) {
        this.goldLoanRepository = goldLoanRepository;
        this.goldRateService = goldRateService;
        this.accountService = accountService;
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
                if (goldLoan.getVerifiedGoldGrams() != null && goldLoan.getGoldRatePerGram() != null) {
                    goldLoan.setVerifiedGoldValue(goldLoan.getVerifiedGoldGrams() * goldLoan.getGoldRatePerGram());
                } else {
                    goldLoan.setVerifiedGoldValue(goldLoan.getGoldValue());
                }
                
                // Save verification notes
                if (goldDetails.containsKey("verificationNotes")) {
                    goldLoan.setVerificationNotes(goldDetails.get("verificationNotes").toString());
                }
                
                // Save storage location
                if (goldDetails.containsKey("storageLocation")) {
                    goldLoan.setStorageLocation(goldDetails.get("storageLocation").toString());
                }
                
                // Credit loan amount to user's account
                Account account = accountService.getAccountByNumber(goldLoan.getAccountNumber());
                if (account != null) {
                    accountService.creditBalance(goldLoan.getAccountNumber(), goldLoan.getLoanAmount());
                }
                
                // Set EMI start date
                goldLoan.setEmiStartDate(LocalDate.now());
                
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
}

