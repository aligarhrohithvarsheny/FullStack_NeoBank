package com.neo.springapp.service;

import com.neo.springapp.model.Account;
import com.neo.springapp.model.EducationLoanSubsidyClaim;
import com.neo.springapp.model.Loan;
import com.neo.springapp.model.Transaction;
import com.neo.springapp.repository.EducationLoanSubsidyClaimRepository;
import com.neo.springapp.repository.LoanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

// PDFBox
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@SuppressWarnings("null")
public class EducationLoanSubsidyClaimService {

    @Autowired
    private EducationLoanSubsidyClaimRepository subsidyClaimRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private AccountService accountService;

    @Autowired
    private TransactionService transactionService;

    @Autowired(required = false)
    private EmailService emailService;

    /**
     * Calculate 3 years (36 months) of interest for education loan
     */
    public Double calculateThreeYearsInterest(Double principal, Double annualRate) {
        if (principal == null || principal <= 0 || annualRate == null || annualRate <= 0) {
            return 0.0;
        }
        
        double monthlyRate = annualRate / (12 * 100);
        double totalInterest = 0.0;
        double remainingPrincipal = principal;
        
        // Calculate interest for 36 months (3 years)
        for (int month = 1; month <= 36; month++) {
            double interestForMonth = remainingPrincipal * monthlyRate;
            totalInterest += interestForMonth;
            
            // Calculate EMI to reduce principal
            // Using standard EMI formula for remaining tenure
            int remainingMonths = 120; // Assuming 10 years tenure, adjust if needed
            double emi = 0.0;
            if (monthlyRate > 0) {
                emi = (principal * monthlyRate * Math.pow(1 + monthlyRate, remainingMonths)) / 
                      (Math.pow(1 + monthlyRate, remainingMonths) - 1);
            } else {
                emi = principal / remainingMonths;
            }
            
            // Principal paid this month
            double principalPaid = emi - interestForMonth;
            remainingPrincipal = Math.max(0, remainingPrincipal - principalPaid);
        }
        
        return Math.round(totalInterest * 100.0) / 100.0;
    }

    /**
     * Create a new subsidy claim request
     */
    @Transactional
    public EducationLoanSubsidyClaim createClaim(Long loanId, String childAadharNumber, String userNotes) {
        Loan loan = loanRepository.findById(loanId)
            .orElseThrow(() -> new RuntimeException("Loan not found"));
        
        // Verify it's an education loan
        if (!"Education Loan".equalsIgnoreCase(loan.getType())) {
            throw new RuntimeException("Subsidy is only available for Education Loans");
        }
        
        // Validate child Aadhar number
        if (childAadharNumber == null || childAadharNumber.trim().isEmpty()) {
            throw new RuntimeException("Child's Aadhar number is required");
        }
        
        if (childAadharNumber.length() != 12 || !childAadharNumber.matches("\\d{12}")) {
            throw new RuntimeException("Child's Aadhar number must be exactly 12 digits");
        }
        
        // Check if user has already made 3 claims for this loan
        List<EducationLoanSubsidyClaim> existingClaims = subsidyClaimRepository.findAllByLoanId(loanId);
        
        if (existingClaims.size() >= 3) {
            throw new RuntimeException("You have already made 3 subsidy claims for this loan. Maximum limit reached.");
        }
        
        // Calculate 3 years of interest
        Double subsidyAmount = calculateThreeYearsInterest(loan.getAmount(), loan.getInterestRate());
        
        EducationLoanSubsidyClaim claim = new EducationLoanSubsidyClaim();
        claim.setLoanId(loanId);
        claim.setLoanAccountNumber(loan.getLoanAccountNumber());
        claim.setLoanType(loan.getType());
        claim.setUserId(loan.getAccountNumber());
        claim.setUserName(loan.getUserName());
        claim.setUserEmail(loan.getUserEmail());
        claim.setAccountNumber(loan.getAccountNumber());
        claim.setLoanAmount(loan.getAmount());
        claim.setInterestRate(loan.getInterestRate());
        claim.setLoanTenure(loan.getTenure());
        claim.setCalculatedSubsidyAmount(subsidyAmount);
        claim.setApprovedSubsidyAmount(subsidyAmount); // Initially same as calculated
        claim.setChildAadharNumber(childAadharNumber.trim());
        claim.setUserNotes(userNotes);
        claim.setStatus("Pending");
        claim.setRequestDate(LocalDateTime.now());
        
        return subsidyClaimRepository.save(claim);
    }

    /**
     * Get all claims (enriched with customerId from account for admin display)
     */
    public List<EducationLoanSubsidyClaim> getAllClaims() {
        List<EducationLoanSubsidyClaim> claims = subsidyClaimRepository.findAll();
        for (EducationLoanSubsidyClaim claim : claims) {
            if (claim.getAccountNumber() != null) {
                Account account = accountService.getAccountByNumber(claim.getAccountNumber());
                if (account != null && account.getCustomerId() != null) {
                    claim.setCustomerId(account.getCustomerId());
                }
            }
        }
        return claims;
    }

    /**
     * Get claims by account number
     */
    public List<EducationLoanSubsidyClaim> getClaimsByAccountNumber(String accountNumber) {
        return subsidyClaimRepository.findByAccountNumber(accountNumber);
    }

    /**
     * Get claim by ID
     */
    public EducationLoanSubsidyClaim getClaimById(Long id) {
        return subsidyClaimRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Claim not found"));
    }

    /**
     * Get pending claims
     */
    public List<EducationLoanSubsidyClaim> getPendingClaims() {
        return subsidyClaimRepository.findPendingClaims();
    }

    /**
     * Approve claim (admin can edit amount)
     */
    @Transactional
    public EducationLoanSubsidyClaim approveClaim(Long claimId, Double approvedAmount, String adminName, String adminNotes) {
        EducationLoanSubsidyClaim claim = getClaimById(claimId);
        
        if (!"Pending".equals(claim.getStatus())) {
            throw new RuntimeException("Only pending claims can be approved");
        }
        
        if (approvedAmount == null || approvedAmount <= 0) {
            throw new RuntimeException("Approved amount must be greater than 0");
        }
        
        claim.setStatus("Approved");
        claim.setApprovedSubsidyAmount(approvedAmount);
        claim.setProcessedDate(LocalDateTime.now());
        claim.setProcessedBy(adminName);
        claim.setAdminNotes(adminNotes);
        
        return subsidyClaimRepository.save(claim);
    }

    /**
     * Reject claim
     */
    @Transactional
    public EducationLoanSubsidyClaim rejectClaim(Long claimId, String adminName, String rejectionReason) {
        EducationLoanSubsidyClaim claim = getClaimById(claimId);
        
        if (!"Pending".equals(claim.getStatus())) {
            throw new RuntimeException("Only pending claims can be rejected");
        }
        
        claim.setStatus("Rejected");
        claim.setProcessedDate(LocalDateTime.now());
        claim.setProcessedBy(adminName);
        claim.setRejectionReason(rejectionReason);
        
        return subsidyClaimRepository.save(claim);
    }

    /**
     * Credit subsidy to user account
     */
    @Transactional
    public Map<String, Object> creditSubsidyToAccount(Long claimId, String adminName) {
        Map<String, Object> response = new HashMap<>();
        
        EducationLoanSubsidyClaim claim = getClaimById(claimId);
        
        if (!"Approved".equals(claim.getStatus())) {
            response.put("success", false);
            response.put("message", "Only approved claims can be credited");
            return response;
        }
        
        if ("Credited".equals(claim.getStatus())) {
            response.put("success", false);
            response.put("message", "Subsidy has already been credited");
            return response;
        }
        
        // Credit amount to user account
        Double newBalance = accountService.creditBalance(claim.getAccountNumber(), claim.getApprovedSubsidyAmount());
        
        if (newBalance == null) {
            response.put("success", false);
            response.put("message", "Failed to credit amount to account");
            return response;
        }
        
        // Create transaction record
        Transaction transaction = new Transaction();
        transaction.setAccountNumber(claim.getAccountNumber());
        transaction.setMerchant("Education Loan Subsidy");
        transaction.setAmount(claim.getApprovedSubsidyAmount());
        transaction.setType("Credit");
        transaction.setDescription("Education Loan Interest Subsidy - Loan: " + claim.getLoanAccountNumber() + 
            " | 3 Years Interest Subsidy");
        transaction.setDate(LocalDateTime.now());
        transaction.setStatus("Completed");
        transaction.setBalance(newBalance);
        
        Transaction savedTransaction = transactionService.saveTransaction(transaction);
        
        // Update claim
        claim.setStatus("Credited");
        claim.setCreditedDate(LocalDateTime.now());
        claim.setCreditedBy(adminName);
        claim.setTransactionId(savedTransaction.getId().toString());
        subsidyClaimRepository.save(claim);
        
        // Send email to user's registered address with subsidy credited details
        if (emailService != null && claim.getUserEmail() != null && !claim.getUserEmail().trim().isEmpty()) {
            try {
                emailService.sendEducationSubsidyCreditedEmail(
                    claim.getUserEmail(),
                    claim.getUserName(),
                    claim.getLoanAccountNumber(),
                    claim.getApprovedSubsidyAmount(),
                    claim.getAccountNumber()
                );
            } catch (Exception e) {
                System.err.println("Failed to send education subsidy credited email: " + e.getMessage());
            }
        }
        
        response.put("success", true);
        response.put("message", "Subsidy credited successfully");
        response.put("claim", claim);
        response.put("transaction", savedTransaction);
        response.put("newBalance", newBalance);
        
        return response;
    }

    /**
     * Update approved amount (admin can edit before crediting)
     */
    @Transactional
    public EducationLoanSubsidyClaim updateApprovedAmount(Long claimId, Double newAmount, String adminName) {
        EducationLoanSubsidyClaim claim = getClaimById(claimId);
        
        if (!"Approved".equals(claim.getStatus()) && !"Pending".equals(claim.getStatus())) {
            throw new RuntimeException("Cannot update amount for claims that are not Pending or Approved");
        }
        
        if (newAmount == null || newAmount <= 0) {
            throw new RuntimeException("Amount must be greater than 0");
        }
        
        claim.setApprovedSubsidyAmount(newAmount);
        if ("Pending".equals(claim.getStatus())) {
            claim.setStatus("Approved");
            claim.setProcessedDate(LocalDateTime.now());
            claim.setProcessedBy(adminName);
        }
        
        return subsidyClaimRepository.save(claim);
    }

    /**
     * Upload and attach a document to a subsidy claim. Validates type/size, saves file,
     * extracts text for PDFs and updates claim with document path and extracted text.
     */
    @Transactional
    public Map<String, Object> uploadClaimDocument(Long claimId, MultipartFile file) throws IOException {
        Map<String, Object> response = new HashMap<>();

        EducationLoanSubsidyClaim claim = getClaimById(claimId);

        if (file == null || file.isEmpty()) {
            response.put("success", false);
            response.put("message", "No file provided");
            return response;
        }

        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        long maxSize = 5L * 1024L * 1024L; // 5 MB

        if (file.getSize() > maxSize) {
            response.put("success", false);
            response.put("message", "File size exceeds 5MB limit");
            return response;
        }

        if (!(contentType.contains("pdf") || contentType.contains("jpeg") || contentType.contains("jpg"))) {
            response.put("success", false);
            response.put("message", "Invalid file type. Allowed: PDF, JPG, JPEG");
            return response;
        }

        String uploadDir = "uploads/subsidy-claims/" + claimId;
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalFilename = file.getOriginalFilename() == null ? "document" : file.getOriginalFilename();
        String safeName = System.currentTimeMillis() + "_" + originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
        Path target = uploadPath.resolve(safeName);
        Files.copy(file.getInputStream(), target);

        // Attempt text extraction for PDFs
        String extracted = null;
        if (contentType.contains("pdf")) {
            try {
                byte[] data = Files.readAllBytes(target);
                try (PDDocument pdf = Loader.loadPDF(data)) {
                    PDFTextStripper stripper = new PDFTextStripper();
                    extracted = stripper.getText(pdf);
                }
            } catch (Exception e) {
                // non-fatal - continue
                extracted = null;
            }
        }

        String relativePath = uploadDir + "/" + safeName;
        claim.setDocumentPath(relativePath);
        if (extracted != null && !extracted.trim().isEmpty()) {
            claim.setExtractedText(extracted);
        }

        subsidyClaimRepository.save(claim);

        response.put("success", true);
        response.put("filePath", relativePath);
        response.put("extractedText", extracted);
        return response;
    }
}

