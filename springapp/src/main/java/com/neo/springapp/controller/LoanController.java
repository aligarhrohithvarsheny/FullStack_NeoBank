package com.neo.springapp.controller;

import com.neo.springapp.model.Loan;
import com.neo.springapp.model.Transaction;
import com.neo.springapp.model.User;
import com.neo.springapp.model.Account;
import com.neo.springapp.repository.LoanRepository;
import com.neo.springapp.service.LoanService;
import com.neo.springapp.service.AccountService;
import com.neo.springapp.service.TransactionService;
import com.neo.springapp.service.CibilService;
import com.neo.springapp.service.UserService;
import com.neo.springapp.service.PdfService;
import com.neo.springapp.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseEntity.BodyBuilder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/loans")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:3000"}) // ✅ allow Angular frontend
public class LoanController {

    private final LoanService loanService;
    
    @Autowired
    private LoanRepository loanRepository;
    
    @Autowired
    private AccountService accountService;
    
    @Autowired
    private TransactionService transactionService;
    
    @Autowired
    private CibilService cibilService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private PdfService pdfService;
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private com.neo.springapp.service.EmiService emiService;
    
    @Autowired
    private com.neo.springapp.service.LoanPredictionService loanPredictionService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    // Apply Loan
    @PostMapping
    public Loan applyLoan(@RequestBody Loan loan) {
        return loanService.applyLoan(loan);
    }

    // Get all loans
    @GetMapping
    public List<Loan> getAllLoans() {
        return loanService.getAllLoans();
    }

    // Get loans by account number (includes both applicant and child accounts for education loans)
    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<Map<String, Object>> getLoansByAccountNumber(@PathVariable String accountNumber) {
        Map<String, Object> response = new HashMap<>();
        List<Loan> loans = loanService.getLoansByAccountNumber(accountNumber);
        
        // Separate loans by role (applicant vs child)
        List<Loan> applicantLoans = loanRepository.findByAccountNumber(accountNumber);
        List<Loan> childLoans = loanRepository.findByChildAccountNumber(accountNumber);
        
        response.put("loans", loans);
        response.put("applicantLoans", applicantLoans);
        response.put("childLoans", childLoans);
        response.put("totalCount", loans.size());
        
        return ResponseEntity.ok(response);
    }

    // Get loan by ID
    @GetMapping("/{id}")
    public Loan getLoanById(@PathVariable Long id) {
        return loanService.getLoanById(id);
    }

    // Update loan status (e.g. Admin Approval)
    @PutMapping("/{id}/approve")
    public ResponseEntity<Map<String, Object>> approveLoan(@PathVariable Long id, @RequestParam String status) {
        System.out.println("=== LOAN APPROVAL ===");
        System.out.println("Loan ID: " + id);
        System.out.println("Status: " + status);
        
        try {
        // status should be "Approved" or "Rejected"
            Loan approvedLoan = loanService.approveLoan(id, status);
            
            if (approvedLoan != null) {
                System.out.println("✅ Loan approved successfully: " + approvedLoan.getId());
                System.out.println("Account Number: " + approvedLoan.getAccountNumber());
                System.out.println("Amount: " + approvedLoan.getAmount());
                
                // If loan is approved, credit the amount to user's account
                if ("Approved".equals(status)) {
                    System.out.println("=== CREDITING LOAN AMOUNT TO USER ACCOUNT ===");
                    
                    // Credit the loan amount to user's account
                    accountService.creditBalance(approvedLoan.getAccountNumber(), approvedLoan.getAmount());
                    
                    // Create transaction record
                    Transaction loanTransaction = new Transaction();
                    loanTransaction.setMerchant("Loan Disbursement");
                    loanTransaction.setAmount(approvedLoan.getAmount());
                    loanTransaction.setType("Loan Credit");
                    loanTransaction.setAccountNumber(approvedLoan.getAccountNumber());
                    loanTransaction.setDescription("Loan Approved: " + approvedLoan.getLoanAccountNumber() + " - " + approvedLoan.getType() + " | Loan ID: " + approvedLoan.getId());
                    loanTransaction.setDate(LocalDateTime.now());
                    loanTransaction.setStatus("Completed");
                    
                    // Save transaction
                    Transaction savedTransaction = transactionService.saveTransaction(loanTransaction);
                    
                    System.out.println("✅ Loan amount credited to account: " + approvedLoan.getAccountNumber());
                    System.out.println("✅ Transaction created: " + savedTransaction.getId());
                    
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("loan", approvedLoan);
                    response.put("transaction", savedTransaction);
                    response.put("message", "Loan approved and amount credited successfully");
                    
                    // Generate EMI schedule for approved loan
                    try {
                        java.util.List<com.neo.springapp.model.EmiPayment> emiSchedule = emiService.generateEmiSchedule(approvedLoan);
                        System.out.println("✅ EMI schedule generated: " + emiSchedule.size() + " installments");
                        response.put("emiSchedule", emiSchedule);
                        response.put("totalEmis", emiSchedule.size());
                    } catch (Exception emiException) {
                        System.err.println("⚠️ Error generating EMI schedule: " + emiException.getMessage());
                        emiException.printStackTrace();
                    }
                    
                    // Generate and send personal loan receipt PDF via email
                    if (approvedLoan.getUserEmail() != null && !approvedLoan.getUserEmail().trim().isEmpty()) {
                        try {
                            byte[] pdfBytes = pdfService.generatePersonalLoanReceipt(approvedLoan);
                            boolean emailSent = emailService.sendPersonalLoanReceiptEmail(
                                approvedLoan.getUserEmail(),
                                approvedLoan.getLoanAccountNumber(),
                                approvedLoan.getUserName(),
                                pdfBytes
                            );
                            response.put("emailSent", emailSent);
                            System.out.println("Personal loan receipt email sent: " + emailSent);
                        } catch (IOException e) {
                            System.err.println("Error generating personal loan receipt PDF: " + e.getMessage());
                            response.put("emailSent", false);
                            response.put("emailError", e.getMessage());
                        } catch (Exception emailException) {
                            System.err.println("Error sending personal loan receipt email: " + emailException.getMessage());
                            response.put("emailSent", false);
                            response.put("emailError", emailException.getMessage());
                        }
                    }
                    
                    return ResponseEntity.ok(response);
                } else {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("loan", approvedLoan);
                    response.put("message", "Loan status updated successfully");
                    
                    return ResponseEntity.ok(response);
                }
            } else {
                System.out.println("❌ Loan approval failed for ID: " + id);
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Loan not found or approval failed");
                
                return ((BodyBuilder) ResponseEntity.notFound()).body(response);
            }
        } catch (Exception e) {
            System.out.println("❌ Loan approval error: " + e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Loan approval failed: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // Get user PAN by account number
    @GetMapping("/user-pan/{accountNumber}")
    public ResponseEntity<Map<String, Object>> getUserPan(@PathVariable String accountNumber) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Optional<User> userOptional = userService.getUserByAccountNumber(accountNumber);
            if (!userOptional.isPresent()) {
                response.put("success", false);
                response.put("message", "User not found");
                return ResponseEntity.notFound().build();
            }
            
            User user = userOptional.get();
            String pan = user.getPan();
            
            response.put("success", true);
            response.put("pan", pan);
            response.put("accountNumber", accountNumber);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error fetching PAN: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // Check CIBIL score and get loan eligibility
    @GetMapping("/check-cibil/{pan}")
    public ResponseEntity<Map<String, Object>> checkCibil(@PathVariable String pan) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (pan == null || pan.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "PAN number is required");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Get CIBIL information
            Map<String, Object> cibilInfo = cibilService.getCibilInfo(pan);
            
            response.put("success", true);
            response.putAll(cibilInfo);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error checking CIBIL: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // Check CIBIL by account number (auto-fetches PAN)
    @GetMapping("/check-cibil-by-account/{accountNumber}")
    public ResponseEntity<Map<String, Object>> checkCibilByAccount(@PathVariable String accountNumber) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Optional<User> userOptional = userService.getUserByAccountNumber(accountNumber);
            if (!userOptional.isPresent()) {
                response.put("success", false);
                response.put("message", "User not found");
                return ResponseEntity.notFound().build();
            }
            
            User user = userOptional.get();
            String pan = user.getPan();
            
            if (pan == null || pan.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "PAN number not found for this account. Please update your KYC details.");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Get CIBIL information
            Map<String, Object> cibilInfo = cibilService.getCibilInfo(pan);
            
            response.put("success", true);
            response.putAll(cibilInfo);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error checking CIBIL: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // Calculate foreclosure amount
    @GetMapping("/foreclosure/calculate/{loanAccountNumber}")
    public ResponseEntity<Map<String, Object>> calculateForeclosure(@PathVariable String loanAccountNumber) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Map<String, Object> calculation = loanService.calculateForeclosure(loanAccountNumber);
            
            if ((Boolean) calculation.get("success")) {
                return ResponseEntity.ok(calculation);
            } else {
                response.put("success", false);
                response.put("message", calculation.get("message"));
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error calculating foreclosure: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // Process foreclosure
    @PostMapping("/foreclose/{loanAccountNumber}")
    public ResponseEntity<Map<String, Object>> forecloseLoan(
            @PathVariable String loanAccountNumber,
            @RequestParam(required = false, defaultValue = "Admin") String foreclosedBy) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // First calculate foreclosure to get details
            Map<String, Object> calculation = loanService.calculateForeclosure(loanAccountNumber);
            
            if (!(Boolean) calculation.get("success")) {
                response.put("success", false);
                response.put("message", calculation.get("message"));
                return ResponseEntity.badRequest().body(response);
            }

            // Process foreclosure
            Loan foreclosedLoan = loanService.processForeclosure(loanAccountNumber, foreclosedBy);
            
            if (foreclosedLoan == null) {
                response.put("success", false);
                response.put("message", "Failed to process foreclosure");
                return ResponseEntity.internalServerError().body(response);
            }

            // Debit foreclosure amount from user's account
            Double foreclosureAmount = (Double) calculation.get("totalForeclosureAmount");
            Account account = accountService.getAccountByNumber(foreclosedLoan.getAccountNumber());
            
            if (account != null) {
                Double currentBalance = account.getBalance();
                if (currentBalance < foreclosureAmount) {
                    response.put("success", false);
                    response.put("message", "Insufficient balance. Required: ₹" + foreclosureAmount + ", Available: ₹" + currentBalance);
                    return ResponseEntity.badRequest().body(response);
                }
                
                // Debit foreclosure amount and get updated balance
                Double newBalance = accountService.debitBalance(foreclosedLoan.getAccountNumber(), foreclosureAmount);
                
                if (newBalance == null) {
                    response.put("success", false);
                    response.put("message", "Failed to debit foreclosure amount from account");
                    return ResponseEntity.badRequest().body(response);
                }
                
                // Refresh account to get updated balance
                account = accountService.getAccountByNumber(foreclosedLoan.getAccountNumber());
                
                // Create transaction record with updated balance
                Transaction foreclosureTransaction = new Transaction();
                foreclosureTransaction.setMerchant("Loan Foreclosure");
                foreclosureTransaction.setAmount(foreclosureAmount);
                foreclosureTransaction.setType("Debit");
                foreclosureTransaction.setAccountNumber(foreclosedLoan.getAccountNumber());
                foreclosureTransaction.setDescription("Loan Foreclosure - " + loanAccountNumber + " | Charges: ₹" + 
                    calculation.get("foreclosureCharges") + " + GST: ₹" + calculation.get("gst"));
                foreclosureTransaction.setDate(LocalDateTime.now());
                foreclosureTransaction.setStatus("Completed");
                foreclosureTransaction.setBalance(newBalance);
                
                transactionService.saveTransaction(foreclosureTransaction);
            }

            // Generate foreclosure PDF
            byte[] pdfBytes = null;
            try {
                pdfBytes = pdfService.generateForeclosureStatement(foreclosedLoan, calculation);
                response.put("pdfGenerated", true);
            } catch (Exception pdfException) {
                System.err.println("Error generating foreclosure PDF: " + pdfException.getMessage());
                response.put("pdfGenerated", false);
                response.put("pdfError", pdfException.getMessage());
            }

            // Send PDF to user's email if available
            if (pdfBytes != null && foreclosedLoan.getUserEmail() != null && !foreclosedLoan.getUserEmail().trim().isEmpty()) {
                try {
                    boolean emailSent = emailService.sendForeclosureEmail(
                        foreclosedLoan.getUserEmail(),
                        foreclosedLoan.getLoanAccountNumber(),
                        foreclosedLoan.getUserName(),
                        pdfBytes
                    );
                    response.put("emailSent", emailSent);
                } catch (Exception emailException) {
                    System.err.println("Error sending foreclosure email: " + emailException.getMessage());
                    response.put("emailSent", false);
                }
            }

            response.put("success", true);
            response.put("message", "Loan foreclosed successfully");
            response.put("loan", foreclosedLoan);
            response.put("foreclosureDetails", calculation);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error processing foreclosure: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // Download foreclosure PDF
    @GetMapping("/foreclosure/pdf/{loanAccountNumber}")
    public ResponseEntity<byte[]> downloadForeclosurePdf(@PathVariable String loanAccountNumber) {
        try {
            Loan loan = loanService.getLoanByLoanAccountNumber(loanAccountNumber);
            if (loan == null || !"Foreclosed".equals(loan.getStatus())) {
                return ResponseEntity.notFound().build();
            }

            // Recalculate foreclosure details for PDF
            Map<String, Object> foreclosureDetails = new HashMap<>();
            foreclosureDetails.put("principalPaid", loan.getPrincipalPaid());
            foreclosureDetails.put("interestPaid", loan.getInterestPaid());
            foreclosureDetails.put("remainingPrincipal", loan.getRemainingPrincipal());
            foreclosureDetails.put("remainingInterest", loan.getRemainingInterest());
            foreclosureDetails.put("foreclosureCharges", loan.getForeclosureCharges());
            foreclosureDetails.put("gst", loan.getForeclosureGst());
            foreclosureDetails.put("totalForeclosureAmount", loan.getForeclosureAmount());
            
            // Calculate additional details
            long monthsElapsed = 0;
            if (loan.getApprovalDate() != null) {
                monthsElapsed = java.time.temporal.ChronoUnit.MONTHS.between(
                    loan.getApprovalDate(), 
                    loan.getForeclosureDate() != null ? loan.getForeclosureDate() : java.time.LocalDateTime.now()
                );
            }
            foreclosureDetails.put("monthsElapsed", monthsElapsed);
            foreclosureDetails.put("remainingMonths", loan.getTenure() - monthsElapsed);
            
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
            foreclosureDetails.put("emi", emi);
            foreclosureDetails.put("totalPaid", emi * monthsElapsed);

            byte[] pdfBytes = pdfService.generateForeclosureStatement(loan, foreclosureDetails);
            
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", 
                "Foreclosure_Statement_" + loanAccountNumber + ".pdf");
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);
                    
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    // Upload Personal Loan Application Form
    @PostMapping("/upload-personal-loan-form")
    public ResponseEntity<Map<String, Object>> uploadPersonalLoanForm(
            @RequestParam("file") MultipartFile file,
            @RequestParam("loanAccountNumber") String loanAccountNumber,
            @RequestParam("accountNumber") String accountNumber) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (file.isEmpty()) {
                response.put("success", false);
                response.put("message", "File is empty");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Validate file size (5MB max)
            if (file.getSize() > 5 * 1024 * 1024) {
                response.put("success", false);
                response.put("message", "File size exceeds 5MB limit");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Validate file type
            String contentType = file.getContentType();
            if (contentType == null || 
                (!contentType.equals("application/pdf") && 
                 !contentType.equals("image/jpeg") && 
                 !contentType.equals("image/jpg") && 
                 !contentType.equals("image/png"))) {
                response.put("success", false);
                response.put("message", "Invalid file type. Only PDF, JPG, and PNG are allowed");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Create upload directory if it doesn't exist
            String uploadDir = "uploads/personal-loan-forms";
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String fileExtension = originalFilename != null && originalFilename.contains(".") 
                ? originalFilename.substring(originalFilename.lastIndexOf(".")) 
                : "";
            String fileName = "personal-loan-form-" + loanAccountNumber + "-" + System.currentTimeMillis() + fileExtension;
            Path filePath = uploadPath.resolve(fileName);
            
            // Save file
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            // Return relative path for storage in database
            String relativePath = uploadDir + "/" + fileName;
            
            response.put("success", true);
            response.put("message", "File uploaded successfully");
            response.put("filePath", relativePath);
            response.put("fileName", fileName);
            response.put("fileSize", file.getSize());
            
            return ResponseEntity.ok(response);
            
        } catch (IOException e) {
            response.put("success", false);
            response.put("message", "Error uploading file: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Unexpected error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // Download Personal Loan Application Form
    @GetMapping("/download-personal-loan-form/{loanId}")
    public ResponseEntity<?> downloadPersonalLoanForm(@PathVariable Long loanId) {
        try {
            Loan loan = loanService.getLoanById(loanId);
            if (loan == null || loan.getPersonalLoanFormPath() == null) {
                return ResponseEntity.notFound().build();
            }
            
            Path filePath = Paths.get(loan.getPersonalLoanFormPath());
            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }
            
            byte[] fileBytes = Files.readAllBytes(filePath);
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.parseMediaType(contentType));
            headers.setContentDispositionFormData("attachment", filePath.getFileName().toString());
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(fileBytes);
                    
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    // ML-based Loan Approval Prediction
    @PostMapping("/predict-approval")
    public ResponseEntity<Map<String, Object>> predictLoanApproval(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String pan = (String) request.get("pan");
            String loanType = (String) request.get("loanType");
            Double requestedAmount = ((Number) request.get("requestedAmount")).doubleValue();
            Integer tenure = ((Number) request.get("tenure")).intValue();
            String accountNumber = (String) request.get("accountNumber");
            
            if (pan == null || pan.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "PAN number is required");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (loanType == null || loanType.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Loan type is required");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (accountNumber == null || accountNumber.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Account number is required");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Get prediction
            Map<String, Object> predictionResult = loanPredictionService.predictLoanApproval(
                pan, loanType, requestedAmount, tenure, accountNumber
            );
            
            return ResponseEntity.ok(predictionResult);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error predicting loan approval: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    // Get user's prediction history
    @GetMapping("/predictions/user/{accountNumber}")
    public ResponseEntity<Map<String, Object>> getUserPredictions(@PathVariable String accountNumber) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            java.util.List<com.neo.springapp.model.LoanPrediction> predictions = 
                loanPredictionService.getUserPredictions(accountNumber);
            
            response.put("success", true);
            response.put("predictions", predictions);
            response.put("count", predictions.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error fetching predictions: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    // Get all predictions (for admin dashboard)
    @GetMapping("/predictions/all")
    public ResponseEntity<Map<String, Object>> getAllPredictions() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            java.util.List<com.neo.springapp.model.LoanPrediction> predictions = 
                loanPredictionService.getAllPredictions();
            
            response.put("success", true);
            response.put("predictions", predictions);
            response.put("count", predictions.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error fetching predictions: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(response);
        }
    }

}
