package com.neo.springapp.controller;

import com.neo.springapp.model.GoldLoan;
import com.neo.springapp.model.GoldRate;
import com.neo.springapp.model.Transaction;
import com.neo.springapp.model.Account;
import com.neo.springapp.model.SalaryAccount;
import com.neo.springapp.model.SalaryNormalTransaction;
import com.neo.springapp.repository.SalaryAccountRepository;
import com.neo.springapp.repository.SalaryNormalTransactionRepository;
import com.neo.springapp.service.GoldLoanService;
import com.neo.springapp.service.GoldRateService;
import com.neo.springapp.service.AccountService;
import com.neo.springapp.service.TransactionService;
import com.neo.springapp.service.PdfService;
import com.neo.springapp.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/gold-loans")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class GoldLoanController {

    @Autowired
    private GoldLoanService goldLoanService;
    
    @Autowired
    private GoldRateService goldRateService;
    
    @Autowired
    private AccountService accountService;
    
    @Autowired
    private TransactionService transactionService;
    
    @Autowired
    private PdfService pdfService;
    
    @Autowired
    private EmailService emailService;

    @Autowired(required = false)
    private com.neo.springapp.service.BankChargesService bankChargesService;

    @Autowired
    private SalaryAccountRepository salaryAccountRepository;

    @Autowired
    private SalaryNormalTransactionRepository salaryNormalTransactionRepository;

    // Send OTP to registered email for gold loan apply (reason in email)
    @PostMapping("/request-otp")
    public ResponseEntity<Map<String, Object>> requestApplyOtp(@RequestParam String accountNumber) {
        return ResponseEntity.ok(goldLoanService.requestApplyOtp(accountNumber));
    }

    // Apply for gold loan. Pass otp in body or as request param to verify before applying.
    @PostMapping
    public ResponseEntity<Map<String, Object>> applyGoldLoan(
            @RequestBody GoldLoan goldLoan,
            @RequestParam(required = false) String otp) {
        Map<String, Object> response = new HashMap<>();
        try {
            GoldLoan savedLoan = otp != null && !otp.trim().isEmpty()
                ? goldLoanService.applyGoldLoan(goldLoan, otp)
                : goldLoanService.applyGoldLoan(goldLoan);
            if (savedLoan != null && bankChargesService != null && savedLoan.getAccountNumber() != null) {
                try {
                    bankChargesService.applyCibilChargeAtLoanApply(savedLoan.getAccountNumber(), savedLoan.getUserName());
                } catch (Exception ex) {
                    System.err.println("CIBIL charge could not be applied: " + ex.getMessage());
                }
            }
            response.put("success", true);
            response.put("message", "Gold loan application submitted successfully");
            response.put("goldLoan", savedLoan);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage() != null ? e.getMessage() : "Failed to apply for gold loan");
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Get all gold loans
    @GetMapping
    public ResponseEntity<List<GoldLoan>> getAllGoldLoans() {
        List<GoldLoan> loans = goldLoanService.getAllGoldLoans();
        return ResponseEntity.ok(loans);
    }

    // Get gold loans by account number
    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<List<GoldLoan>> getGoldLoansByAccountNumber(@PathVariable String accountNumber) {
        List<GoldLoan> loans = goldLoanService.getGoldLoansByAccountNumber(accountNumber);
        return ResponseEntity.ok(loans);
    }

    // Get gold loan by ID
    @GetMapping("/{id}")
    public ResponseEntity<GoldLoan> getGoldLoanById(@PathVariable Long id) {
        GoldLoan loan = goldLoanService.getGoldLoanById(id);
        if (loan != null) {
            return ResponseEntity.ok(loan);
        }
        return ResponseEntity.notFound().build();
    }

    // Get gold loans by status
    @GetMapping("/status/{status}")
    public ResponseEntity<List<GoldLoan>> getGoldLoansByStatus(@PathVariable String status) {
        List<GoldLoan> loans = goldLoanService.getGoldLoansByStatus(status);
        return ResponseEntity.ok(loans);
    }

    // Approve or reject gold loan
    @PutMapping("/{id}/approve")
    public ResponseEntity<Map<String, Object>> approveGoldLoan(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false, defaultValue = "Admin") String approvedBy,
            @RequestBody(required = false) Map<String, Object> goldDetails) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            GoldLoan approvedLoan = goldLoanService.approveGoldLoan(id, status, approvedBy, goldDetails);
            
            if (approvedLoan != null) {
                // If approved, create transaction record
                if ("Approved".equals(status)) {
                    Transaction loanTransaction = new Transaction();
                    loanTransaction.setMerchant("Gold Loan Disbursement");
                    loanTransaction.setAmount(approvedLoan.getLoanAmount());
                    loanTransaction.setType("Loan Credit");
                    loanTransaction.setAccountNumber(approvedLoan.getAccountNumber());
                    loanTransaction.setDescription("Gold Loan Approved: " + approvedLoan.getLoanAccountNumber() + 
                        " | Gold: " + approvedLoan.getGoldGrams() + " grams | Loan ID: " + approvedLoan.getId());
                    loanTransaction.setDate(LocalDateTime.now());
                    loanTransaction.setStatus("Completed");
                    
                    // Get updated balance
                    Double currentBalance = accountService.getBalanceByAccountNumber(approvedLoan.getAccountNumber());
                    loanTransaction.setBalance(currentBalance != null ? currentBalance : approvedLoan.getCurrentBalance() + approvedLoan.getLoanAmount());
                    
                    transactionService.saveTransaction(loanTransaction);
                    
                    // Generate and send gold loan receipt PDF via email
                    if (approvedLoan.getUserEmail() != null && !approvedLoan.getUserEmail().trim().isEmpty()) {
                        try {
                            byte[] pdfBytes = pdfService.generateGoldLoanReceipt(approvedLoan);
                            boolean emailSent = emailService.sendGoldLoanReceiptEmail(
                                approvedLoan.getUserEmail(),
                                approvedLoan.getLoanAccountNumber(),
                                approvedLoan.getUserName(),
                                pdfBytes
                            );
                            response.put("emailSent", emailSent);
                            System.out.println("Gold loan receipt email sent: " + emailSent);
                        } catch (IOException e) {
                            System.err.println("Error generating gold loan receipt PDF: " + e.getMessage());
                            response.put("emailSent", false);
                            response.put("emailError", e.getMessage());
                        } catch (Exception emailException) {
                            System.err.println("Error sending gold loan receipt email: " + emailException.getMessage());
                            response.put("emailSent", false);
                            response.put("emailError", emailException.getMessage());
                        }
                    }
                    // Automatic loan processing charge Rs 1180 - debit from user, credit to NeoBank A/C
                    if (bankChargesService != null && approvedLoan.getAccountNumber() != null) {
                        try {
                            String userName = approvedLoan.getUserName() != null ? approvedLoan.getUserName() : "Customer";
                            boolean loanChargeApplied = bankChargesService.applyLoanChargeAtApproval(approvedLoan.getAccountNumber(), userName);
                            response.put("loanChargeApplied", loanChargeApplied);
                        } catch (Exception e) {
                            System.err.println("Loan charge could not be applied: " + e.getMessage());
                            response.put("loanChargeApplied", false);
                        }
                    }
                }
                
                response.put("success", true);
                response.put("message", "Gold loan " + status.toLowerCase() + " successfully");
                response.put("goldLoan", approvedLoan);
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Gold loan not found");
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to process gold loan: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // Accept terms and conditions by user
    @PutMapping("/{id}/accept-terms")
    public ResponseEntity<Map<String, Object>> acceptTerms(
            @PathVariable Long id,
            @RequestParam String acceptedBy) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            GoldLoan loan = goldLoanService.acceptTerms(id, acceptedBy);
            
            if (loan != null) {
                response.put("success", true);
                response.put("message", "Terms and conditions accepted successfully");
                response.put("goldLoan", loan);
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Gold loan not found");
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to accept terms: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // Calculate loan amount for given grams (helper endpoint)
    @GetMapping("/calculate")
    public ResponseEntity<Map<String, Object>> calculateLoanAmount(@RequestParam Double grams) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            GoldRate currentRate = goldRateService.getCurrentGoldRate();
            Double goldValue = grams * currentRate.getRatePerGram();
            Double loanAmount = goldValue * 0.75; // 75% of gold value
            
            response.put("success", true);
            response.put("grams", grams);
            response.put("goldRatePerGram", currentRate.getRatePerGram());
            response.put("goldValue", goldValue);
            response.put("loanAmount", loanAmount);
            response.put("loanPercentage", 75.0);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to calculate loan amount: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // Calculate foreclosure for gold loan
    @GetMapping("/foreclosure/{loanAccountNumber}")
    public ResponseEntity<Map<String, Object>> calculateForeclosure(@PathVariable String loanAccountNumber) {
        Map<String, Object> response = goldLoanService.calculateForeclosure(loanAccountNumber);
        if ((Boolean) response.get("success")) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Process foreclosure for gold loan
    @PostMapping("/foreclosure/{loanAccountNumber}")
    public ResponseEntity<Map<String, Object>> processForeclosure(
            @PathVariable String loanAccountNumber,
            @RequestParam(required = false, defaultValue = "Admin") String foreclosedBy,
            @RequestBody(required = false) Map<String, Object> foreclosureData) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            GoldLoan goldLoan = goldLoanService.getGoldLoanByLoanAccountNumber(loanAccountNumber);
            
            if (goldLoan == null) {
                response.put("success", false);
                response.put("message", "Gold loan not found");
                return ResponseEntity.notFound().build();
            }

            // Get foreclosure amount - use edited amount if provided, otherwise calculate
            Double foreclosureAmount;
            
            if (foreclosureData != null && foreclosureData.containsKey("foreclosureAmount")) {
                foreclosureAmount = Double.valueOf(foreclosureData.get("foreclosureAmount").toString());
            } else {
                // Calculate foreclosure to get amount
                Map<String, Object> calculation = goldLoanService.calculateForeclosure(loanAccountNumber);
                if (!(Boolean) calculation.get("success")) {
                    response.put("success", false);
                    response.put("message", calculation.get("message"));
                    return ResponseEntity.badRequest().body(response);
                }
                foreclosureAmount = (Double) calculation.get("totalForeclosureAmount");
            }

            // Check account balance - try regular accounts first, then salary accounts
            Account account = accountService.getAccountByNumber(goldLoan.getAccountNumber());
            boolean isSalaryAccount = false;
            SalaryAccount salaryAccount = null;
            Double currentBalance = null;

            if (account != null) {
                currentBalance = account.getBalance();
            } else {
                // Fallback: check salary_accounts table
                salaryAccount = salaryAccountRepository.findByAccountNumber(goldLoan.getAccountNumber());
                if (salaryAccount != null && salaryAccount.getBalance() != null) {
                    currentBalance = salaryAccount.getBalance();
                    isSalaryAccount = true;
                }
            }

            if (currentBalance == null) {
                response.put("success", false);
                response.put("message", "Account not found");
                return ResponseEntity.badRequest().body(response);
            }

            if (currentBalance < foreclosureAmount) {
                response.put("success", false);
                response.put("message", "Insufficient balance. Required: ₹" + foreclosureAmount + ", Available: ₹" + currentBalance);
                return ResponseEntity.badRequest().body(response);
            }

            // Debit foreclosure amount
            Double balanceBefore = currentBalance;
            Double newBalance;
            if (isSalaryAccount && salaryAccount != null) {
                Double updatedBal = salaryAccount.getBalance() - foreclosureAmount;
                salaryAccount.setBalance(updatedBal);
                salaryAccount.setUpdatedAt(LocalDateTime.now());
                salaryAccountRepository.save(salaryAccount);
                newBalance = updatedBal;
            } else {
                newBalance = accountService.debitBalance(goldLoan.getAccountNumber(), foreclosureAmount);
            }

            if (newBalance == null) {
                response.put("success", false);
                response.put("message", "Failed to debit foreclosure amount from account");
                return ResponseEntity.badRequest().body(response);
            }

            // Process foreclosure - use custom amount if provided
            GoldLoan foreclosedLoan;
            Double editedInterestRate = null;
            
            if (foreclosureData != null && foreclosureData.containsKey("foreclosureAmount")) {
                if (foreclosureData.containsKey("interestRate")) {
                    editedInterestRate = Double.valueOf(foreclosureData.get("interestRate").toString());
                    // Update loan interest rate if changed
                    goldLoanService.updateInterestRate(goldLoan.getId(), editedInterestRate);
                    goldLoan = goldLoanService.getGoldLoanById(goldLoan.getId()); // Refresh
                }
                foreclosedLoan = goldLoanService.processForeclosureWithAmount(loanAccountNumber, foreclosedBy, foreclosureAmount);
            } else {
                foreclosedLoan = goldLoanService.processForeclosure(loanAccountNumber, foreclosedBy);
            }
            
            if (foreclosedLoan != null) {
                // Create transaction record with updated balance
                Transaction foreclosureTransaction = new Transaction();
                foreclosureTransaction.setMerchant("Gold Loan Foreclosure");
                foreclosureTransaction.setAmount(foreclosureAmount);
                foreclosureTransaction.setType("Debit");
                foreclosureTransaction.setAccountNumber(goldLoan.getAccountNumber());
                foreclosureTransaction.setDescription("Gold Loan Foreclosure - " + loanAccountNumber + 
                    (editedInterestRate != null ? " | Interest Rate: " + editedInterestRate + "%" : ""));
                foreclosureTransaction.setDate(LocalDateTime.now());
                foreclosureTransaction.setStatus("Completed");
                foreclosureTransaction.setBalance(newBalance);
                
                transactionService.saveTransaction(foreclosureTransaction);

                // Also create salary normal transaction if it's a salary account
                if (isSalaryAccount && salaryAccount != null) {
                    SalaryNormalTransaction salTxn = new SalaryNormalTransaction();
                    salTxn.setSalaryAccountId(salaryAccount.getId());
                    salTxn.setAccountNumber(goldLoan.getAccountNumber());
                    salTxn.setType("Debit");
                    salTxn.setAmount(foreclosureAmount);
                    salTxn.setRemark("Gold Loan Foreclosure - " + loanAccountNumber);
                    salTxn.setPreviousBalance(balanceBefore);
                    salTxn.setNewBalance(newBalance);
                    salTxn.setStatus("Success");
                    salaryNormalTransactionRepository.save(salTxn);
                }

                response.put("success", true);
                response.put("message", "Foreclosure processed successfully");
                response.put("goldLoan", foreclosedLoan);
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Failed to process foreclosure");
                return ResponseEntity.internalServerError().body(response);
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to process foreclosure: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // Update interest rate for gold loan
    @PutMapping("/{id}/interest-rate")
    public ResponseEntity<Map<String, Object>> updateInterestRate(
            @PathVariable Long id,
            @RequestParam Double interestRate) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            GoldLoan updatedLoan = goldLoanService.updateInterestRate(id, interestRate);
            if (updatedLoan != null) {
                response.put("success", true);
                response.put("message", "Interest rate updated successfully");
                response.put("goldLoan", updatedLoan);
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Gold loan not found");
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to update interest rate: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // Get EMI schedule for gold loan
    @GetMapping("/emi-schedule/{loanAccountNumber}")
    public ResponseEntity<List<com.neo.springapp.model.EmiPayment>> getEmiSchedule(@PathVariable String loanAccountNumber) {
        List<com.neo.springapp.model.EmiPayment> schedule = goldLoanService.getEmiSchedule(loanAccountNumber);
        return ResponseEntity.ok(schedule);
    }
}

