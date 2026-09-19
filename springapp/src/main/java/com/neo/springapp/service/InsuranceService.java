package com.neo.springapp.service;

import com.neo.springapp.model.*;
import com.neo.springapp.repository.InsuranceApplicationRepository;
import com.neo.springapp.repository.InsuranceClaimRepository;
import com.neo.springapp.repository.InsurancePaymentRepository;
import com.neo.springapp.repository.InsurancePolicyRepository;
import com.neo.springapp.repository.CurrentAccountRepository;
import com.neo.springapp.repository.SalaryAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.itextpdf.html2pdf.HtmlConverter;

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
    private BranchAccountService branchAccountService;

    @Autowired
    private CurrentAccountRepository currentAccountRepository;

    @Autowired
    private SalaryAccountRepository salaryAccountRepository;

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

    @Transactional
    public InsuranceApplication assignPolicyToVerifiedAccount(String accountNumber, Long policyId,
                                                               String premiumType, String adminRemark,
                                                               String customerName, Map<String, Object> details) {
        Map<String, Object> verified = verifyLinkedAccount(accountNumber, customerName);
        if (!Boolean.TRUE.equals(verified.get("valid"))) throw new RuntimeException(String.valueOf(verified.get("message")));
        InsurancePolicy policy = policyRepository.findById(policyId).orElseThrow(() -> new RuntimeException("Policy not found"));
        if (applicationRepository.existsNonRejectedByAccountAndPolicy(accountNumber, policyId)) {
            throw new RuntimeException("This policy is already assigned/applied for this account.");
        }
        Optional<User> user = userService.getUserByAccountNumber(accountNumber);
        InsuranceApplication application = new InsuranceApplication();
        application.setPolicy(policy);
        application.setUserId(user.map(User::getId).orElse(0L));
        application.setAccountNumber(accountNumber.trim());
        application.setNomineeName("ADMIN_ASSIGNED");
        application.setNomineeRelation("");
        application.setKycDocumentPath("");
        application.setPremiumType(premiumType == null ? policy.getPremiumType() : premiumType);
        application.setCreatedByAdmin(true);
        application.setPaymentStatus("NOT_PAID");
        application.setAdminRemark(adminRemark);
        application.setLinkedAccountType(String.valueOf(verified.get("accountType")));
        application.setPremiumAmountCalculated(calculatePremium(policy, application));
        application.setStatus("PENDING_APPROVAL");
        InsuranceApplication saved = applicationRepository.save(application);
        return editApplication(saved.getId(), details);
    }

    public Map<String, Object> verifyLinkedAccount(String accountNumber, String expectedName) {
        Map<String, Object> response = new HashMap<>();
        if (accountNumber == null || accountNumber.isBlank()) throw new IllegalArgumentException("Account number is required");
        String number = accountNumber.trim();
        Account savings = accountService.getAccountByNumber(number);
        String name = null;
        String type = null;
        Double balance = null;
        if (savings != null) {
            name = savings.getName(); type = "SAVINGS"; balance = savings.getBalance();
        } else {
            var current = currentAccountRepository.findByAccountNumber(number).orElse(null);
            if (current != null) { name = current.getOwnerName(); type = "CURRENT"; balance = current.getBalance(); }
            else {
                SalaryAccount salary = salaryAccountRepository.findByAccountNumber(number);
                if (salary != null) { name = salary.getEmployeeName(); type = "SALARY"; balance = salary.getBalance(); }
            }
        }
        if (name == null) { response.put("valid", false); response.put("message", "Savings, current or salary account not found"); return response; }
        boolean nameMatches = expectedName == null || expectedName.isBlank() || name.equalsIgnoreCase(expectedName.trim());
        response.put("valid", nameMatches); response.put("nameMatches", nameMatches); response.put("accountNumber", number);
        response.put("accountHolderName", name); response.put("accountType", type); response.put("balance", balance == null ? 0.0 : balance);
        response.put("message", nameMatches ? "Account verified and name matched" : "Account holder name does not match");
        return response;
    }

    @Transactional
    public InsuranceApplication editApplication(Long id, Map<String, Object> updates) {
        InsuranceApplication app = applicationRepository.findById(id).orElseThrow(() -> new RuntimeException("Insurance application not found"));
        if (updates == null) return app;
        if (updates.get("vehicleNumber") != null) app.setVehicleNumber(String.valueOf(updates.get("vehicleNumber")));
        if (updates.get("makeModel") != null) app.setMakeModel(String.valueOf(updates.get("makeModel")));
        if (updates.get("chassisNumber") != null) app.setChassisNumber(String.valueOf(updates.get("chassisNumber")));
        if (updates.get("engineNumber") != null) app.setEngineNumber(String.valueOf(updates.get("engineNumber")));
        if (updates.get("registrationDate") != null) app.setRegistrationDate(String.valueOf(updates.get("registrationDate")));
        if (updates.get("vehicleDocumentPaths") != null) app.setVehicleDocumentPaths(String.valueOf(updates.get("vehicleDocumentPaths")));
        if (updates.get("nomineeName") != null) app.setNomineeName(String.valueOf(updates.get("nomineeName")));
        if (updates.get("nomineeRelation") != null) app.setNomineeRelation(String.valueOf(updates.get("nomineeRelation")));
        if (updates.get("premiumAmountCalculated") != null) app.setPremiumAmountCalculated(Double.valueOf(updates.get("premiumAmountCalculated").toString()));
        return applicationRepository.save(app);
    }

    @Transactional
    public InsuranceApplication renewApplication(Long id, String renewedBy) {
        InsuranceApplication app = applicationRepository.findById(id).orElseThrow(() -> new RuntimeException("Insurance application not found"));
        if (!"ACTIVE".equalsIgnoreCase(app.getStatus())) throw new RuntimeException("Only active insurance can be renewed");
        LocalDate start = LocalDate.now();
        app.setPolicyStartDate(start);
        app.setPolicyEndDate(start.plusMonths(app.getPolicy().getDurationMonths() == null ? 12 : app.getPolicy().getDurationMonths()));
        app.setRenewalCount((app.getRenewalCount() == null ? 0 : app.getRenewalCount()) + 1);
        app.setPaymentStatus("NOT_PAID");
        app.setStatus("APPROVED");
        app.setAdminRemark("Renewed by " + (renewedBy == null || renewedBy.isBlank() ? "Admin" : renewedBy));
        return applicationRepository.save(app);
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
        Map<String, Object> verified = verifyLinkedAccount(accountNumber, null);
        if (!Boolean.TRUE.equals(verified.get("valid"))) throw new RuntimeException("Account not found for premium payment");
        String accountType = String.valueOf(verified.get("accountType"));
        Double currentBalance = Double.valueOf(verified.get("balance").toString());
        if (currentBalance < amount) throw new RuntimeException("Insufficient balance for premium payment");
        Double newBalance = debitLinkedAccount(accountNumber, accountType, amount);

        // Create transaction record
        Transaction txn = new Transaction();
        txn.setMerchant(merchant != null ? merchant : "Insurance Premium");
        txn.setAmount(amount);
        txn.setType("Debit");
        txn.setDescription("Insurance premium for policy " + application.getPolicy().getPolicyNumber());
        txn.setBalance(newBalance);
        txn.setUserName(String.valueOf(verified.get("accountHolderName")));
        txn.setAccountNumber(accountNumber);
        transactionService.saveTransaction(txn);

        if (branchAccountService != null) {
            String branchNumber = branchAccountService.getDepositAccountNumber();
            Double branchBalance = accountService.creditBalance(branchNumber, amount);
            if (branchBalance != null) {
                Transaction branchTxn = new Transaction();
                branchTxn.setMerchant("Insurance Premium Income");
                branchTxn.setAmount(amount);
                branchTxn.setType("Credit");
                branchTxn.setDescription("Insurance premium income for policy " + application.getPolicy().getPolicyNumber() + " (from " + accountNumber + ")");
                branchTxn.setBalance(branchBalance);
                branchTxn.setAccountNumber(branchNumber);
                branchTxn.setSourceAccountNumber(accountNumber);
                transactionService.saveTransaction(branchTxn);
            }
        }

        // Create insurance payment record
        InsurancePayment payment = new InsurancePayment();
        payment.setApplication(application);
        payment.setUserId(application.getUserId());
        payment.setAccountNumber(accountNumber);
        payment.setAmount(amount);
        payment.setAccountType(accountType);
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
        application.setLinkedAccountType(accountType);
        application.setPaidAt(LocalDateTime.now());
        application.setPolicyStartDate(LocalDate.now());
        application.setPolicyEndDate(LocalDate.now().plusMonths(application.getPolicy().getDurationMonths() == null ? 12 : application.getPolicy().getDurationMonths()));
        // If admin already approved, activate now
        if ("APPROVED".equalsIgnoreCase(application.getStatus())) {
            application.setStatus("ACTIVE");
        }
        applicationRepository.save(application);

        return paymentRepository.save(payment);
    }

    private Double debitLinkedAccount(String accountNumber, String accountType, Double amount) {
        if ("SAVINGS".equals(accountType)) return accountService.debitBalance(accountNumber, amount);
        if ("CURRENT".equals(accountType)) {
            var account = currentAccountRepository.findByAccountNumber(accountNumber).orElseThrow(() -> new RuntimeException("Current account not found"));
            account.setBalance(account.getBalance() - amount); currentAccountRepository.save(account); return account.getBalance();
        }
        SalaryAccount account = salaryAccountRepository.findByAccountNumber(accountNumber);
        if (account == null) throw new RuntimeException("Salary account not found");
        account.setBalance(account.getBalance() - amount); salaryAccountRepository.save(account); return account.getBalance();
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

        String html = "<html><head><style>body{font-family:Arial;color:#14213d;padding:28px}h1{color:#0b7285;border-bottom:2px solid #0b7285;padding-bottom:10px}.grid{display:grid;grid-template-columns:1fr 1fr;gap:10px}.row{border-bottom:1px solid #ddd;padding:8px}.label{font-weight:bold;color:#52606d}.badge{background:#d3f9d8;color:#087f5b;padding:5px 10px}</style></head><body>"
                + "<h1>NeoBank Insurance Policy Certificate</h1><p><span class='badge'>" + safe(app.getStatus()) + "</span></p>"
                + "<div class='grid'>"
                + row("Policy Number", policy.getPolicyNumber()) + row("Policy Name", policy.getName())
                + row("Insurance Type", policy.getType()) + row("Coverage", "Rs. " + policy.getCoverageAmount())
                + row("Premium", "Rs. " + (app.getPremiumAmountCalculated() != null ? app.getPremiumAmountCalculated() : policy.getPremiumAmount()) + " / " + policy.getPremiumType())
                + row("Duration", policy.getDurationMonths() + " months") + row("Account Number", app.getAccountNumber())
                + row("Linked Account Type", app.getLinkedAccountType()) + row("Nominee", app.getNomineeName())
                + row("Vehicle Number", app.getVehicleNumber()) + row("Make / Model", app.getMakeModel())
                + row("Chassis Number", app.getChassisNumber()) + row("Engine Number", app.getEngineNumber())
                + row("Policy Start", app.getPolicyStartDate()) + row("Policy End", app.getPolicyEndDate())
                + "</div><p style='margin-top:28px'>Generated on: " + LocalDateTime.now() + "</p><p>This certificate is generated from NeoBank records after approval and successful premium payment.</p></body></html>";
        try {
            java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
            HtmlConverter.convertToPdf(html, output);
            app.setCertificateGeneratedAt(LocalDateTime.now());
            applicationRepository.save(app);
            return output.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Unable to generate insurance certificate PDF", e);
        }
    }

    private String row(String label, Object value) {
        return "<div class='row'><span class='label'>" + safe(label) + ": </span>" + safe(value) + "</div>";
    }

    private String safe(Object value) {
        return String.valueOf(value == null ? "-" : value).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}

