package com.neo.springapp.service;

import com.neo.springapp.model.*;
import com.neo.springapp.repository.InsuranceApplicationRepository;
import com.neo.springapp.repository.InsuranceClaimRepository;
import com.neo.springapp.repository.InsurancePaymentRepository;
import com.neo.springapp.repository.InsurancePolicyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@SuppressWarnings("null")
public class InsuranceService {

    @Autowired
    private InsurancePolicyRepository policyRepository;

    @Autowired
    private InsuranceApplicationRepository applicationRepository;

    @Autowired
    private InsurancePaymentRepository paymentRepository;

    @Autowired
    private InsuranceClaimRepository claimRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private TransactionService transactionService;

    @Autowired(required = false)
    private EmailService emailService;

    // ===== Policies =====

    public List<InsurancePolicy> getActivePolicies() {
        return policyRepository.findByStatus("ACTIVE");
    }

    public Optional<InsurancePolicy> getPolicyById(Long id) {
        return policyRepository.findById(id);
    }

    public Optional<InsurancePolicy> getPolicyByNumber(String policyNumber) {
        return policyRepository.findByPolicyNumber(policyNumber);
    }

    public InsurancePolicy createPolicy(InsurancePolicy policy) {
        if (policy.getPolicyNumber() == null || policy.getPolicyNumber().isEmpty()) {
            policy.setPolicyNumber("POL" + System.currentTimeMillis());
        }
        policy.setCreatedAt(LocalDateTime.now());
        policy.setUpdatedAt(LocalDateTime.now());
        if (policy.getStatus() == null || policy.getStatus().isEmpty()) {
            policy.setStatus("ACTIVE");
        }
        return policyRepository.save(policy);
    }

    public InsurancePolicy updatePolicy(Long id, InsurancePolicy updated) {
        return policyRepository.findById(id)
                .map(existing -> {
                    existing.setName(updated.getName());
                    existing.setType(updated.getType());
                    existing.setCoverageAmount(updated.getCoverageAmount());
                    existing.setPremiumAmount(updated.getPremiumAmount());
                    existing.setPremiumType(updated.getPremiumType());
                    existing.setDurationMonths(updated.getDurationMonths());
                    existing.setDescription(updated.getDescription());
                    existing.setBenefits(updated.getBenefits());
                    existing.setEligibility(updated.getEligibility());
                    existing.setStatus(updated.getStatus());
                    existing.setUpdatedAt(LocalDateTime.now());
                    return policyRepository.save(existing);
                })
                .orElse(null);
    }

    public void deletePolicy(Long id) {
        policyRepository.deleteById(id);
    }

    // ===== Applications =====

    @Transactional
    public InsuranceApplication applyForPolicy(Long userId,
                                               Long policyId,
                                               String nomineeName,
                                               String nomineeRelation,
                                               String kycDocumentPath,
                                               String premiumType,
                                               Integer proposerAge,
                                               String healthConditions,
                                               String lifestyleHabits,
                                               Boolean hasExistingEmis) {
        User user = userService.getUserById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        InsurancePolicy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new RuntimeException("Policy not found"));

        if (user.getAccountNumber() == null || user.getAccountNumber().isEmpty()) {
            throw new RuntimeException("User does not have an approved bank account");
        }

        // Enforce: same policy cannot be applied/assigned twice for same account (unless rejected/expired)
        if (applicationRepository.existsNonRejectedByAccountAndPolicy(user.getAccountNumber(), policyId)) {
            throw new RuntimeException("This policy is already assigned/applied for this account.");
        }

        InsuranceApplication application = new InsuranceApplication();
        application.setPolicy(policy);
        application.setUserId(user.getId());
        application.setAccountNumber(user.getAccountNumber());
        application.setNomineeName(nomineeName);
        application.setNomineeRelation(nomineeRelation);
        application.setKycDocumentPath(kycDocumentPath);
        application.setPremiumType(premiumType);
        application.setProposerAge(proposerAge);
        application.setHealthConditions(healthConditions);
        application.setLifestyleHabits(lifestyleHabits);
        application.setHasExistingEmis(hasExistingEmis);
        application.setPaymentStatus("NOT_PAID");
        application.setCreatedByAdmin(false);

        // Fraud scoring (rule-based)
        double fraudScore = calculateFraudScore(user, policy, application);
        application.setFraudScore(fraudScore);
        if (fraudScore > 0.75) {
            application.setStatus("UNDER_REVIEW");
        } else {
            application.setStatus("PENDING_APPROVAL");
        }
        application.setAppliedAt(LocalDateTime.now());

        // Premium calculation (simple rule-based adjustments)
        application.setPremiumAmountCalculated(calculatePremium(policy, application));

        return applicationRepository.save(application);
    }

    @Transactional
    public InsuranceApplication assignPolicyToAccount(String accountNumber,
                                                      Long policyId,
                                                      String premiumType,
                                                      String adminRemark,
                                                      String customerName) {
        User user = userService.getUserByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("User not found for accountNumber"));
        InsurancePolicy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new RuntimeException("Policy not found"));

        // Enforce: policy can be assigned to this account only once (unless rejected/expired)
        if (applicationRepository.existsNonRejectedByAccountAndPolicy(accountNumber, policyId)) {
            throw new RuntimeException("This policy is already assigned/applied for this account.");
        }

        // Enforce: a specific policy (policyNumber) must be unique across customers
        if (applicationRepository.existsNonRejectedByPolicy(policyId)) {
            throw new RuntimeException("This policy has already been assigned to another customer and cannot be reassigned.");
        }

        // If customerName is provided, verify it matches account holder name
        if (customerName != null && !customerName.trim().isEmpty()) {
            String expectedName = null;
            try {
                if (user.getAccount() != null && user.getAccount().getName() != null) {
                    expectedName = user.getAccount().getName();
                } else if (user.getName() != null) {
                    expectedName = user.getName();
                }
            } catch (Exception ignored) {}

            if (expectedName == null || !customerName.trim().equalsIgnoreCase(expectedName.trim())) {
                throw new RuntimeException("Provided customer name does not match account holder name. Assignment denied.");
            }
        }

        InsuranceApplication application = new InsuranceApplication();
        application.setPolicy(policy);
        application.setUserId(user.getId());
        application.setAccountNumber(accountNumber);
        application.setNomineeName("ADMIN_ASSIGNED");
        application.setNomineeRelation("");
        application.setKycDocumentPath("");
        application.setPremiumType(premiumType != null ? premiumType : policy.getPremiumType());
        application.setCreatedByAdmin(true);
        application.setPaymentStatus("NOT_PAID");
        application.setAdminRemark(adminRemark);
        application.setAppliedAt(LocalDateTime.now());

        // Fraud scoring using known user info
        double fraudScore = calculateFraudScore(user, policy, application);
        application.setFraudScore(fraudScore);
        application.setStatus(fraudScore > 0.75 ? "UNDER_REVIEW" : "PENDING_APPROVAL");
        application.setPremiumAmountCalculated(calculatePremium(policy, application));

        return applicationRepository.save(application);
    }

    private double calculatePremium(InsurancePolicy policy, InsuranceApplication application) {
        double base = policy.getPremiumAmount() != null ? policy.getPremiumAmount() : 0.0;
        String cycle = application.getPremiumType() != null ? application.getPremiumType().toUpperCase() : "MONTHLY";
        int age = application.getProposerAge() != null ? application.getProposerAge() : 0;
        boolean hasDisease = application.getHealthConditions() != null && !application.getHealthConditions().trim().isEmpty();

        double add = 0.0;
        if ("YEARLY".equals(cycle)) {
            if (age >= 45) add += 5000;
            if (hasDisease) add += 3000;
            if (Boolean.TRUE.equals(application.getHasExistingEmis())) add += 2000;
        } else {
            if (age >= 45) add += 500;
            if (hasDisease) add += 300;
            if (Boolean.TRUE.equals(application.getHasExistingEmis())) add += 200;
        }
        return Math.max(0.0, base + add);
    }

    private double calculateFraudScore(User user, InsurancePolicy policy, InsuranceApplication application) {
        double score = 0.12; // base

        Double coverage = policy.getCoverageAmount() != null ? policy.getCoverageAmount() : 0.0;
        Double income = null;
        try {
            income = user.getIncome();
        } catch (Exception ignored) {}
        if (income == null) income = 0.0;

        Integer age = application.getProposerAge();
        if (age == null) {
            try {
                if (user.getAccount() != null && user.getAccount().getAge() > 0) {
                    age = user.getAccount().getAge();
                }
            } catch (Exception ignored) {}
        }

        if (income > 0 && coverage > income * 10) score += 0.45;
        if (age != null && age > 60 && coverage >= 500000) score += 0.35;
        if (Boolean.TRUE.equals(application.getHasExistingEmis())) score += 0.15;
        if (application.getHealthConditions() != null && !application.getHealthConditions().trim().isEmpty()) score += 0.18;

        if (score > 1.0) score = 1.0;
        if (score < 0.0) score = 0.0;
        return Math.round(score * 100.0) / 100.0;
    }

    public List<InsuranceApplication> getApplicationsForUser(Long userId) {
        return applicationRepository.findByUserId(userId);
    }

    public List<InsuranceApplication> getApplicationsForAccount(String accountNumber) {
        return applicationRepository.findByAccountNumber(accountNumber);
    }

    public List<InsuranceApplication> getPendingApplications() {
        return applicationRepository.findByStatus("PENDING_APPROVAL");
    }

    /**
     * Lookup an insurance policy by its policy number and return a linked active application if exists.
     */
    public Map<String, Object> lookupPolicyWithCustomer(String policyNumber) {
        Map<String, Object> result = new HashMap<>();
        InsurancePolicy policy = policyRepository.findByPolicyNumber(policyNumber).orElse(null);
        result.put("policy", policy);
        if (policy == null) return result;

        List<InsuranceApplication> apps = applicationRepository.findActiveByPolicyNumber(policyNumber);
        if (apps != null && !apps.isEmpty()) {
            InsuranceApplication app = apps.get(0);
            result.put("application", app);
            try {
                User user = userService.getUserById(app.getUserId()).orElse(null);
                if (user != null) {
                    result.put("userEmail", user.getEmail());
                    result.put("accountNumber", user.getAccountNumber());
                }
            } catch (Exception ignored) {}
        }
        return result;
    }

    public List<InsuranceApplication> getUnderReviewApplications() {
        return applicationRepository.findByStatus("UNDER_REVIEW");
    }

    public List<InsuranceApplication> getApprovedApplications() {
        return applicationRepository.findByStatus("APPROVED");
    }

    @Transactional
    public InsuranceApplication approveApplication(Long applicationId, String adminRemark) {
        InsuranceApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));
        // Approve does NOT mean active unless payment completed
        if ("COMPLETED".equalsIgnoreCase(application.getPaymentStatus())) {
            application.setStatus("ACTIVE");
        } else {
            application.setStatus("APPROVED");
        }
        application.setAdminRemark(adminRemark);
        application.setApprovedAt(LocalDateTime.now());
        return applicationRepository.save(application);
    }

    @Transactional
    public InsuranceApplication rejectApplication(Long applicationId, String adminRemark) {
        InsuranceApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));
        application.setStatus("REJECTED");
        application.setAdminRemark(adminRemark);
        return applicationRepository.save(application);
    }

    // ===== Premium Payments =====

    @Transactional
    public InsurancePayment payPremium(Long applicationId,
                                       Double amount,
                                       boolean autoDebitEnabled,
                                       String merchant) {
        InsuranceApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        // Allow payment even before approval (fraud + admin approval controls activation)
        String st = application.getStatus() != null ? application.getStatus().toUpperCase() : "";
        if ("REJECTED".equals(st) || "EXPIRED".equals(st)) {
            throw new RuntimeException("Cannot pay premium for rejected/expired applications");
        }

        String accountNumber = application.getAccountNumber();

        // Debit from account using existing transaction/balance logic
        Account account = accountService.getAccountByNumber(accountNumber);
        if (account == null) {
            throw new RuntimeException("Account not found for premium payment");
        }

        if (account.getBalance() == null || account.getBalance() < amount) {
            throw new RuntimeException("Insufficient balance for premium payment");
        }

        Double newBalance = accountService.debitBalance(accountNumber, amount);

        // Create transaction record
        Transaction txn = new Transaction();
        txn.setMerchant(merchant != null ? merchant : "Insurance Premium");
        txn.setAmount(amount);
        txn.setType("Debit");
        txn.setDescription("Insurance premium for policy " + application.getPolicy().getPolicyNumber());
        txn.setBalance(newBalance);
        txn.setUserName(account.getName());
        txn.setAccountNumber(accountNumber);
        transactionService.saveTransaction(txn);

        // Create insurance payment record
        InsurancePayment payment = new InsurancePayment();
        payment.setApplication(application);
        payment.setUserId(application.getUserId());
        payment.setAccountNumber(accountNumber);
        payment.setAmount(amount);
        payment.setStatus("SUCCESS");

        // Simple next due date calculation based on premiumType
        LocalDate today = LocalDate.now();
        if ("MONTHLY".equalsIgnoreCase(application.getPremiumType())) {
            payment.setPremiumPeriodFrom(today);
            payment.setPremiumPeriodTo(today.plusMonths(1));
            payment.setNextDueDate(today.plusMonths(1));
        } else if ("YEARLY".equalsIgnoreCase(application.getPremiumType())) {
            payment.setPremiumPeriodFrom(today);
            payment.setPremiumPeriodTo(today.plusYears(1));
            payment.setNextDueDate(today.plusYears(1));
        }

        // Auto-debit workflow: user requests, admin approves
        if (autoDebitEnabled) {
            application.setAutoDebitRequested(true);
        }
        boolean approved = Boolean.TRUE.equals(application.getAutoDebitApproved());
        payment.setAutoDebitEnabled(approved);

        // Mark payment completed on application
        application.setPaymentStatus("COMPLETED");
        // If admin already approved, activate now
        if ("APPROVED".equalsIgnoreCase(application.getStatus())) {
            application.setStatus("ACTIVE");
        }
        applicationRepository.save(application);

        return paymentRepository.save(payment);
    }

    @Transactional
    public InsuranceApplication approveAutoDebit(Long applicationId, String adminRemark) {
        InsuranceApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));
        application.setAutoDebitApproved(true);
        application.setAdminRemark(adminRemark);
        InsuranceApplication saved = applicationRepository.save(application);

        // Best-effort email notification (development logs if not configured)
        try {
            if (emailService != null) {
                User user = userService.getUserById(saved.getUserId()).orElse(null);
                if (user != null && user.getEmail() != null) {
                    emailService.sendOtpEmailWithReason(user.getEmail(), "AUTO_DEBIT_APPROVED", "Insurance Auto Debit Approved");
                }
            }
        } catch (Exception ignored) {}

        return saved;
    }

    public List<InsurancePayment> getPaymentsForUser(Long userId) {
        return paymentRepository.findByUserId(userId);
    }

    public List<InsurancePayment> getPaymentsForAccount(String accountNumber) {
        return paymentRepository.findByAccountNumber(accountNumber);
    }

    // ===== Claims =====

    @Transactional
    public InsuranceClaim createClaim(Long applicationId,
                                      Double claimAmount,
                                      String documentsPath) {
        InsuranceApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        if (!"ACTIVE".equalsIgnoreCase(application.getStatus())) {
            throw new RuntimeException("Only ACTIVE policies can raise claims");
        }

        InsuranceClaim claim = new InsuranceClaim();
        claim.setApplication(application);
        claim.setUserId(application.getUserId());
        claim.setAccountNumber(application.getAccountNumber());
        claim.setClaimAmount(claimAmount);
        claim.setDocumentsPath(documentsPath);
        claim.setStatus("PENDING");
        claim.setCreatedAt(LocalDateTime.now());

        return claimRepository.save(claim);
    }

    public List<InsuranceClaim> getClaimsForUser(Long userId) {
        return claimRepository.findByUserId(userId);
    }

    public List<InsuranceClaim> getClaimsForAccount(String accountNumber) {
        return claimRepository.findByAccountNumber(accountNumber);
    }

    public List<InsuranceClaim> getClaimsByPolicyNumber(String policyNumber) {
        return claimRepository.findByPolicyNumber(policyNumber);
    }

    public List<InsuranceClaim> getPendingClaims() {
        return claimRepository.findByStatus("PENDING");
    }

    @Transactional
    public InsuranceClaim approveClaim(Long claimId, String adminRemark) {
        InsuranceClaim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Claim not found"));

        if (!"PENDING".equalsIgnoreCase(claim.getStatus())) {
            throw new RuntimeException("Only PENDING claims can be approved");
        }

        // Basic approval – payout handled separately
        claim.setStatus("APPROVED");
        claim.setAdminRemark(adminRemark);
        claim.setApprovedAt(LocalDateTime.now());

        return claimRepository.save(claim);
    }

    @Transactional
    public InsuranceClaim rejectClaim(Long claimId, String adminRemark) {
        InsuranceClaim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Claim not found"));

        if (!"PENDING".equalsIgnoreCase(claim.getStatus())) {
            throw new RuntimeException("Only PENDING claims can be rejected");
        }

        claim.setStatus("REJECTED");
        claim.setAdminRemark(adminRemark);
        claim.setUpdatedAt(LocalDateTime.now());

        return claimRepository.save(claim);
    }

    @Transactional
    public InsuranceClaim payoutClaim(Long claimId,
                                      String adminAccountNumber,
                                      String description) {
        InsuranceClaim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Claim not found"));

        if (!"APPROVED".equalsIgnoreCase(claim.getStatus())) {
            throw new RuntimeException("Only APPROVED claims can be paid out");
        }

        String beneficiaryAccount = claim.getAccountNumber();
        Double amount = claim.getClaimAmount();

        // Credit amount to user account
        Account userAccount = accountService.getAccountByNumber(beneficiaryAccount);
        if (userAccount == null) {
            throw new RuntimeException("User account not found for payout");
        }

        Double newBalance = accountService.creditBalance(beneficiaryAccount, amount);

        // Record transaction
        Transaction txn = new Transaction();
        txn.setMerchant("Insurance Claim Payout");
        txn.setAmount(amount);
        txn.setType("Credit");
        txn.setDescription(description != null ? description : "Insurance claim payout " + claim.getClaimNumber());
        txn.setBalance(newBalance);
        txn.setUserName(userAccount.getName());
        txn.setAccountNumber(beneficiaryAccount);
        transactionService.saveTransaction(txn);

        claim.setStatus("PAID");
        claim.setPaidAt(LocalDateTime.now());
        claim.setUpdatedAt(LocalDateTime.now());
        claim.setPayoutTransactionId(txn.getTransactionId());

        return claimRepository.save(claim);
    }

    // ===== Simple dashboard stats =====

    public Map<String, Object> getAdminDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalPolicies", policyRepository.count());
        stats.put("totalApplications", applicationRepository.count());
        stats.put("pendingApplications", getPendingApplications().size());
        stats.put("pendingClaims", getPendingClaims().size());
        // Premium collected can be refined with date filters
        double totalPremium = paymentRepository.findAll().stream()
                .filter(p -> "SUCCESS".equalsIgnoreCase(p.getStatus()))
                .mapToDouble(InsurancePayment::getAmount)
                .sum();
        stats.put("totalPremiumCollected", totalPremium);
        // Simple analytics: high-risk claims count (rule-based)
        long highRiskClaims = claimRepository.findAll().stream()
                .filter(c -> "PENDING".equalsIgnoreCase(c.getStatus()) || "APPROVED".equalsIgnoreCase(c.getStatus()))
                .filter(this::isHighRiskClaim)
                .count();
        stats.put("highRiskClaims", highRiskClaims);
        return stats;
    }

    // ===== Advanced helpers (risk scoring, reminders, certificates) =====

    public boolean isHighRiskClaim(InsuranceClaim claim) {
        if (claim == null || claim.getApplication() == null || claim.getApplication().getPolicy() == null) {
            return false;
        }
        InsurancePolicy policy = claim.getApplication().getPolicy();
        double coverage = policy.getCoverageAmount() != null ? policy.getCoverageAmount() : 0.0;
        double amount = claim.getClaimAmount() != null ? claim.getClaimAmount() : 0.0;

        boolean largePortionOfCoverage = coverage > 0 && amount >= coverage * 0.8;
        boolean veryLargeAbsolute = amount >= 500000; // 5 lakh threshold

        return largePortionOfCoverage || veryLargeAbsolute;
    }

    public Map<String, Object> getClaimRiskScore(Long claimId) {
        InsuranceClaim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Claim not found"));

        Map<String, Object> score = new HashMap<>();
        boolean highRisk = isHighRiskClaim(claim);
        score.put("claimId", claimId);
        score.put("highRisk", highRisk);
        score.put("status", claim.getStatus());

        // Simple explanation
        String explanation = "Normal";
        if (highRisk) {
            explanation = "High claim amount relative to coverage or very large absolute amount.";
        }
        score.put("explanation", explanation);
        return score;
    }

    public List<InsurancePayment> getUpcomingRenewalsForAccount(String accountNumber, int daysAhead) {
        LocalDate today = LocalDate.now();
        LocalDate limit = today.plusDays(daysAhead);
        return paymentRepository.findByAccountNumber(accountNumber).stream()
                .filter(p -> p.getNextDueDate() != null)
                .filter(p -> !p.getNextDueDate().isBefore(today) && !p.getNextDueDate().isAfter(limit))
                .toList();
    }

    public byte[] generatePolicyCertificatePdf(Long applicationId) {
        InsuranceApplication app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));
        InsurancePolicy policy = app.getPolicy();

        StringBuilder content = new StringBuilder();
        content.append("NeoBank Insurance Policy Certificate\n\n");
        content.append("Policy Number: ").append(policy.getPolicyNumber()).append("\n");
        content.append("Policy Name: ").append(policy.getName()).append("\n");
        content.append("Type: ").append(policy.getType()).append("\n");
        content.append("Coverage Amount: ₹").append(policy.getCoverageAmount()).append("\n");
        content.append("Premium: ₹").append(policy.getPremiumAmount()).append(" / ")
                .append(policy.getPremiumType()).append("\n");
        content.append("Duration: ").append(policy.getDurationMonths()).append(" months\n");
        content.append("Nominee: ").append(app.getNomineeName()).append("\n");
        content.append("Account Number: ").append(app.getAccountNumber()).append("\n");
        content.append("Status: ").append(app.getStatus()).append("\n");
        content.append("\nGenerated on: ").append(LocalDateTime.now()).append("\n");

        // For simplicity we return a UTF-8 encoded text as "PDF" content.
        // In production, integrate a real PDF library (e.g., iText, OpenPDF).
        return content.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
}

