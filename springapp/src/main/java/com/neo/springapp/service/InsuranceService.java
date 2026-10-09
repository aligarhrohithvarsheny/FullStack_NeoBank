package com.neo.springapp.service;

import com.neo.springapp.model.*;
import com.neo.springapp.repository.InsuranceApplicationRepository;
import com.neo.springapp.repository.InsuranceCredentialRepository;
import com.neo.springapp.repository.GuestInsuranceClaimRepository;
import com.neo.springapp.repository.InsuranceClaimRepository;
import com.neo.springapp.repository.GuestInsuranceApplicationRepository;
import com.neo.springapp.repository.InsurancePaymentRepository;
import com.neo.springapp.repository.InsurancePolicyRepository;
import com.neo.springapp.repository.CurrentAccountRepository;
import com.neo.springapp.repository.SalaryAccountRepository;
import com.neo.springapp.repository.GuestInsurancePaymentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.itextpdf.html2pdf.HtmlConverter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
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
    private InsuranceCredentialRepository insuranceCredentialRepository;

    @Autowired
    private GuestInsuranceApplicationRepository guestApplicationRepository;

    @Autowired
    private GuestInsuranceClaimRepository guestInsuranceClaimRepository;

    @Autowired
    private GuestInsurancePaymentRepository guestInsurancePaymentRepository;

    @Autowired
    private InsurancePaymentRepository paymentRepository;

    @Autowired
    private InsuranceClaimRepository claimRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordService passwordService;

    @Autowired
    private UserSessionTokenService userSessionTokenService;

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

    @Autowired
    private SavingsUpiService savingsUpiService;

    @Value("${insurance.upi-id:}")
    private String insuranceUpiId;

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

    @Transactional
    public Map<String, Object> createInsurancePassword(String insuranceNumber, String email, String password) {
        String normalizedNumber = insuranceNumber == null ? "" : insuranceNumber.trim();
        String normalizedEmail = UserService.normalizeEmail(email);
        validateInsurancePassword(password);
        Optional<GuestInsuranceApplication> guestApplication =
                guestApplicationRepository.findByApplicationNumberAndEmailIgnoreCase(normalizedNumber, normalizedEmail);
        if (guestApplication.isPresent()) {
            return createGuestInsurancePassword(normalizedNumber, normalizedEmail, password);
        }
        User user = findCustomerForInsurance(normalizedNumber, normalizedEmail);
        if (!"APPROVED".equalsIgnoreCase(user.getStatus())) {
            throw new RuntimeException("Your NeoBank customer account must be approved before creating an insurance password");
        }

        InsuranceCredential credential = insuranceCredentialRepository.findByUserId(user.getId())
                .orElseGet(InsuranceCredential::new);
        if (credential.getUserId() != null) {
            throw new RuntimeException("An insurance password already exists. Please sign in.");
        }
        credential.setUserId(user.getId());
        credential.setPasswordHash(passwordService.encryptPassword(password));
        credential.setFailedAttempts(0);
        credential.setLockedUntil(null);
        credential.setUpdatedAt(LocalDateTime.now());
        insuranceCredentialRepository.save(credential);
        return Map.of("success", true, "message", "Insurance password created. You can now sign in.");
    }

    @Transactional
    public Map<String, Object> authenticateCustomer(String insuranceNumber, String email, String password) {
        String normalizedNumber = insuranceNumber == null ? "" : insuranceNumber.trim();
        String normalizedEmail = UserService.normalizeEmail(email);
        if (normalizedNumber.isEmpty() || normalizedEmail == null || password == null || password.isBlank()) {
            throw new IllegalArgumentException("Insurance number, registered email, and password are required");
        }

        Optional<GuestInsuranceApplication> guestApplication =
                guestApplicationRepository.findByApplicationNumberAndEmailIgnoreCase(normalizedNumber, normalizedEmail);
        if (guestApplication.isPresent()) {
            return authenticateGuestCustomer(guestApplication.get(), password);
        }

        User user = findCustomerForInsurance(normalizedNumber, normalizedEmail);
        InsuranceCredential credential = insuranceCredentialRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Create an insurance password before signing in"));
        if (credential.getLockedUntil() != null && credential.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new RuntimeException("Insurance sign-in is temporarily locked. Try again later.");
        }
        if (!passwordService.verifyPassword(password, credential.getPasswordHash())) {
            int attempts = credential.getFailedAttempts() + 1;
            credential.setFailedAttempts(attempts);
            if (attempts >= 5) {
                credential.setFailedAttempts(0);
                credential.setLockedUntil(LocalDateTime.now().plusMinutes(15));
            }
            insuranceCredentialRepository.save(credential);
            throw new RuntimeException("Invalid insurance credentials");
        }
        credential.setFailedAttempts(0);
        credential.setLockedUntil(null);
        insuranceCredentialRepository.save(credential);

        Map<String, Object> userData = new HashMap<>();
        userData.put("id", user.getId());
        userData.put("username", user.getUsername());
        userData.put("email", user.getEmail());
        userData.put("accountNumber", user.getAccountNumber());
        userData.put("status", user.getStatus());
        userData.put("passwordSet", user.isPasswordSet());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("user", userData);
        response.put("token", userSessionTokenService.issue(user.getId(), user.getAccountNumber()));
        response.put("message", "Insurance login successful");
        return response;
    }

    @Transactional
    public Map<String, Object> createGuestInsurancePassword(String applicationNumber, String email, String password) {
        String normalizedNumber = applicationNumber == null ? "" : applicationNumber.trim();
        String normalizedEmail = UserService.normalizeEmail(email);
        validateInsurancePassword(password);
        GuestInsuranceApplication application = guestApplicationRepository
                .findByApplicationNumberAndEmailIgnoreCase(normalizedNumber, normalizedEmail)
                .orElseThrow(() -> new RuntimeException("Invalid insurance application details"));
        if (!"APPROVED".equalsIgnoreCase(application.getStatus())) {
            throw new RuntimeException("Your insurance application must be approved before creating a password");
        }
        if (application.getPasswordHash() != null && !application.getPasswordHash().isBlank()) {
            throw new RuntimeException("An insurance password already exists. Please sign in.");
        }
        application.setPasswordHash(passwordService.encryptPassword(password));
        application.setFailedAttempts(0);
        application.setLockedUntil(null);
        guestApplicationRepository.save(application);
        return Map.of("success", true, "message", "Insurance password created. You can now sign in.");
    }

    private Map<String, Object> authenticateGuestCustomer(GuestInsuranceApplication application, String password) {
        if (!"APPROVED".equalsIgnoreCase(application.getStatus())) {
            throw new RuntimeException("Your insurance application is still awaiting review");
        }
        if (application.getPasswordHash() == null || application.getPasswordHash().isBlank()) {
            throw new RuntimeException("Create an insurance password before signing in");
        }
        if (application.getLockedUntil() != null && application.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new RuntimeException("Insurance sign-in is temporarily locked. Try again later.");
        }
        if (!passwordService.verifyPassword(password, application.getPasswordHash())) {
            int attempts = application.getFailedAttempts() + 1;
            application.setFailedAttempts(attempts >= 5 ? 0 : attempts);
            if (attempts >= 5) application.setLockedUntil(LocalDateTime.now().plusMinutes(15));
            guestApplicationRepository.save(application);
            throw new RuntimeException("Invalid insurance credentials");
        }
        application.setFailedAttempts(0);
        application.setLockedUntil(null);
        guestApplicationRepository.save(application);

        Map<String, Object> guest = new HashMap<>();
        guest.put("id", application.getId());
        guest.put("applicationNumber", application.getApplicationNumber());
        guest.put("applicantName", application.getApplicantName());
        guest.put("email", application.getEmail());
        guest.put("status", application.getStatus());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("guest", true);
        response.put("user", guest);
        response.put("token", userSessionTokenService.issueInsuranceGuest(application.getId()));
        response.put("message", "Insurance login successful");
        return response;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getGuestApplicationForDashboard(Long id) {
        GuestInsuranceApplication application = guestApplicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Insurance application not found"));
        Map<String, Object> details = new HashMap<>();
        details.put("applicationNumber", application.getApplicationNumber());
        details.put("applicantName", application.getApplicantName());
        details.put("policyName", application.getPolicy().getName());
        details.put("policyType", application.getPolicy().getType());
        details.put("coverageAmount", application.getPolicy().getCoverageAmount());
        details.put("premiumAmount", application.getPolicy().getPremiumAmount());
        details.put("premiumType", application.getPolicy().getPremiumType());
        details.put("status", application.getStatus());
        details.put("createdAt", application.getCreatedAt());
        details.put("policyStartDate", application.getPolicyStartDate());
        details.put("nextPremiumDueDate", application.getNextPremiumDueDate());
        details.put("payments", guestInsurancePaymentRepository.findByApplicationIdOrderByPaidAtDesc(id).stream()
                .map(payment -> Map.of(
                        "amount", payment.getAmount(),
                        "transactionReference", payment.getTransactionReference(),
                        "payerAccountLastFour", payment.getPayerAccountLastFour(),
                        "status", payment.getStatus(),
                        "paidAt", payment.getPaidAt()))
                .toList());
        details.put("claims", guestInsuranceClaimRepository.findByApplicationIdOrderByCreatedAtDesc(id).stream()
                .map(claim -> Map.of(
                        "claimNumber", claim.getClaimNumber(),
                        "claimAmount", claim.getClaimAmount(),
                        "reason", claim.getReason(),
                        "status", claim.getStatus(),
                        "createdAt", claim.getCreatedAt(),
                        "adminRemark", claim.getAdminRemark() == null ? "" : claim.getAdminRemark()))
                .toList());
        return details;
    }

    @Transactional
    public Map<String, Object> payGuestInsurancePremium(Long applicationId, String payerAccountNumber, String pin) {
        GuestInsuranceApplication application = guestApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Insurance application not found"));
        if (!"APPROVED".equalsIgnoreCase(application.getStatus())
                && !"ACTIVE".equalsIgnoreCase(application.getStatus())) {
            throw new RuntimeException("Only an approved insurance application can receive premium payments");
        }
        if (application.getNextPremiumDueDate() != null
                && application.getNextPremiumDueDate().isAfter(LocalDate.now())) {
            throw new RuntimeException("The next premium is not due until "
                    + application.getNextPremiumDueDate());
        }
        String normalizedPayerAccount = payerAccountNumber == null ? "" : payerAccountNumber.trim();
        if (normalizedPayerAccount.isEmpty() || pin == null || !pin.matches("[0-9]{6}")) {
            throw new IllegalArgumentException("A NeoBank savings account number and 6-digit UPI PIN are required");
        }
        if (insuranceUpiId == null || insuranceUpiId.isBlank()) {
            throw new IllegalStateException("Insurance UPI recipient is not configured");
        }

        BigDecimal amount = BigDecimal.valueOf(application.getPolicy().getPremiumAmount())
                .setScale(2, java.math.RoundingMode.HALF_UP);
        Map<String, Object> transfer = savingsUpiService.sendMoney(
                normalizedPayerAccount,
                insuranceUpiId,
                amount,
                pin,
                "Insurance premium " + application.getApplicationNumber());
        if (!Boolean.TRUE.equals(transfer.get("success"))) {
            return Map.of("success", false, "message",
                    transfer.getOrDefault("error", "NeoBank UPI could not complete the transfer").toString());
        }

        Object transactionReferenceValue = transfer.get("transactionRef");
        if (transactionReferenceValue == null || transactionReferenceValue.toString().isBlank()) {
            throw new IllegalStateException("NeoBank UPI completed without returning a transaction reference");
        }
        String transactionReference = transactionReferenceValue.toString();
        GuestInsurancePayment payment = new GuestInsurancePayment();
        payment.setApplication(application);
        payment.setTransactionReference(transactionReference);
        payment.setPayerAccountLastFour(normalizedPayerAccount.substring(
                Math.max(0, normalizedPayerAccount.length() - 4)));
        payment.setAmount(amount);
        payment.setPaidAt(LocalDateTime.now());
        guestInsurancePaymentRepository.save(payment);

        LocalDate today = LocalDate.now();
        if (application.getPolicyStartDate() == null) {
            application.setPolicyStartDate(today);
        }
        application.setStatus("ACTIVE");
        String premiumType = application.getPolicy().getPremiumType();
        application.setNextPremiumDueDate("YEARLY".equalsIgnoreCase(premiumType)
                ? today.plusYears(1) : today.plusMonths(1));
        guestApplicationRepository.save(application);

        return Map.of(
                "success", true,
                "transactionReference", transactionReference,
                "message", "Premium paid successfully using NeoBank UPI");
    }

    @Transactional
    public GuestInsuranceClaim createGuestInsuranceClaim(
            Long applicationId, java.math.BigDecimal amount, String reason, String details) {
        GuestInsuranceApplication application = guestApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Insurance application not found"));
        if (!"ACTIVE".equalsIgnoreCase(application.getStatus())) {
            throw new RuntimeException("A premium-paid active policy is required to file a claim");
        }
        if (amount == null || amount.signum() <= 0
                || amount.compareTo(java.math.BigDecimal.valueOf(application.getPolicy().getCoverageAmount())) > 0) {
            throw new IllegalArgumentException("Claim amount must be positive and within the policy coverage");
        }
        String normalizedReason = reason == null ? "" : reason.trim();
        String normalizedDetails = details == null ? "" : details.trim();
        if (normalizedReason.isEmpty() || normalizedReason.length() > 80
                || normalizedDetails.length() < 10 || normalizedDetails.length() > 4000) {
            throw new IllegalArgumentException("Enter a claim reason and details between 10 and 4000 characters");
        }
        GuestInsuranceClaim claim = new GuestInsuranceClaim();
        claim.setApplication(application);
        claim.setClaimAmount(amount.setScale(2, java.math.RoundingMode.HALF_UP));
        claim.setReason(normalizedReason);
        claim.setDetails(normalizedDetails);
        claim.setClaimNumber("GCL" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
        claim.setStatus("PENDING_REVIEW");
        claim.setCreatedAt(LocalDateTime.now());
        return guestInsuranceClaimRepository.save(claim);
    }

    public List<GuestInsuranceClaim> getPendingGuestInsuranceClaims() {
        return guestInsuranceClaimRepository.findByStatusOrderByCreatedAtAsc("PENDING_REVIEW");
    }

    @Transactional
    public GuestInsuranceClaim reviewGuestInsuranceClaim(Long claimId, boolean approve, String remark) {
        GuestInsuranceClaim claim = guestInsuranceClaimRepository.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Guest insurance claim not found"));
        if (!"PENDING_REVIEW".equalsIgnoreCase(claim.getStatus())) {
            throw new RuntimeException("This claim has already been reviewed");
        }
        claim.setStatus(approve ? "APPROVED" : "REJECTED");
        claim.setAdminRemark(remark == null ? "" : remark.trim());
        claim.setReviewedAt(LocalDateTime.now());
        return guestInsuranceClaimRepository.save(claim);
    }

    @Transactional(readOnly = true)
    public byte[] generateGuestInsuranceCertificate(Long applicationId) {
        GuestInsuranceApplication application = guestApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Insurance application not found"));
        if (!"ACTIVE".equalsIgnoreCase(application.getStatus())) {
            throw new RuntimeException("A paid, active policy is required to download a certificate");
        }
        String html = """
                <html><body style="font-family: sans-serif; padding: 32px; color: #172033">
                  <h1 style="color: #075fbe">NeoBank Insurance Certificate</h1>
                  <p>This certificate confirms the active insurance coverage below.</p>
                  <table style="width: 100%%; border-collapse: collapse">
                    <tr><td>Certificate reference</td><td>%s</td></tr>
                    <tr><td>Applicant</td><td>%s</td></tr>
                    <tr><td>Insurance application</td><td>%s</td></tr>
                    <tr><td>Policy</td><td>%s (%s)</td></tr>
                    <tr><td>Coverage amount</td><td>INR %s</td></tr>
                    <tr><td>Premium</td><td>INR %s / %s</td></tr>
                    <tr><td>Start date</td><td>%s</td></tr>
                  </table>
                </body></html>
                """.formatted(
                escapeHtml("CERT-" + application.getApplicationNumber()),
                escapeHtml(application.getApplicantName()),
                escapeHtml(application.getApplicationNumber()),
                escapeHtml(application.getPolicy().getName()),
                escapeHtml(application.getPolicy().getType()),
                application.getPolicy().getCoverageAmount(),
                application.getPolicy().getPremiumAmount(),
                escapeHtml(application.getPolicy().getPremiumType()),
                application.getPolicyStartDate() == null ? "" : application.getPolicyStartDate());
        java.io.ByteArrayOutputStream pdf = new java.io.ByteArrayOutputStream();
        try {
            HtmlConverter.convertToPdf(html, pdf);
            return pdf.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to generate insurance certificate", exception);
        }
    }

    private String escapeHtml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    @Transactional
    public Map<String, Object> closeGuestInsuranceApplication(Long id) {
        GuestInsuranceApplication application = guestApplicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Insurance application not found"));
        if (!"APPROVED".equalsIgnoreCase(application.getStatus())
                && !"ACTIVE".equalsIgnoreCase(application.getStatus())) {
            throw new RuntimeException("Only approved or active insurance can be closed");
        }
        application.setStatus("CLOSED");
        guestApplicationRepository.save(application);
        return Map.of("success", true, "message", "Insurance application closed");
    }

    private User findCustomerForInsurance(String insuranceNumber, String normalizedEmail) {
        if (insuranceNumber == null || insuranceNumber.isBlank() || normalizedEmail == null) {
            throw new IllegalArgumentException("Insurance number and registered email are required");
        }
        User user = userService.findByEmail(normalizedEmail)
                .orElseThrow(() -> new RuntimeException("Invalid insurance credentials"));
        boolean numberMatches = applicationRepository.findByUserId(user.getId()).stream()
                .anyMatch(application -> insuranceNumber.equalsIgnoreCase(application.getApplicationNumber())
                        || (application.getPolicy() != null
                        && insuranceNumber.equalsIgnoreCase(application.getPolicy().getPolicyNumber())));
        if (!numberMatches) {
            throw new RuntimeException("Invalid insurance credentials");
        }
        return user;
    }

    private void validateInsurancePassword(String password) {
        if (password == null || password.length() < 8
                || !password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$")) {
            throw new IllegalArgumentException("Password must be at least 8 characters and include uppercase, lowercase, and a number");
        }
    }

    @Transactional
    public GuestInsuranceApplication applyAsGuest(String name, String email, String phone, Long policyId) {
        String normalizedName = name == null ? "" : name.trim();
        String normalizedEmail = UserService.normalizeEmail(email);
        String normalizedPhone = phone == null ? "" : phone.trim();
        if (normalizedName.length() < 2 || normalizedEmail == null
                || !normalizedEmail.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
                || !normalizedPhone.matches("[0-9+() -]{7,20}") || policyId == null) {
            throw new IllegalArgumentException("Enter a name, valid email, phone number, and select an insurance policy");
        }
        InsurancePolicy policy = policyRepository.findById(policyId)
                .filter(item -> "ACTIVE".equalsIgnoreCase(item.getStatus()))
                .orElseThrow(() -> new RuntimeException("Selected insurance policy is unavailable"));

        GuestInsuranceApplication application = new GuestInsuranceApplication();
        application.setApplicantName(normalizedName);
        application.setEmail(normalizedEmail);
        application.setPhone(normalizedPhone);
        application.setPolicy(policy);
        application.setApplicationNumber("GIA" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
        application.setStatus("PENDING_APPROVAL");
        application.setCreatedAt(LocalDateTime.now());
        return guestApplicationRepository.save(application);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> trackGuestApplications(String email) {
        String normalizedEmail = UserService.normalizeEmail(email);
        if (normalizedEmail == null || !normalizedEmail.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new IllegalArgumentException("Enter a valid email address");
        }
        return guestApplicationRepository.findByEmailOrderByCreatedAtDesc(normalizedEmail).stream()
                .map(application -> {
                    Map<String, Object> status = new HashMap<>();
                    status.put("applicationNumber", application.getApplicationNumber());
                    status.put("policyName", application.getPolicy().getName());
                    status.put("policyType", application.getPolicy().getType());
                    status.put("status", application.getStatus());
                    status.put("createdAt", application.getCreatedAt());
                    return status;
                })
                .toList();
    }

    public List<GuestInsuranceApplication> getPendingGuestApplications() {
        return guestApplicationRepository.findByStatusOrderByCreatedAtAsc("PENDING_APPROVAL");
    }

    @Transactional
    public GuestInsuranceApplication reviewGuestApplication(Long id, boolean approve, String remark) {
        GuestInsuranceApplication application = guestApplicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Insurance application not found"));
        if (!"PENDING_APPROVAL".equalsIgnoreCase(application.getStatus())) {
            throw new RuntimeException("This insurance application has already been reviewed");
        }
        application.setStatus(approve ? "APPROVED" : "REJECTED");
        application.setAdminRemark(remark);
        application.setReviewedAt(LocalDateTime.now());
        return guestApplicationRepository.save(application);
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
                    existing.setTermsAndConditions(updated.getTermsAndConditions());
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

    @Transactional
    public InsuranceApplication closePolicy(Long userId, Long applicationId) {
        InsuranceApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Insurance application not found"));
        if (!userId.equals(application.getUserId())) {
            throw new RuntimeException("Insurance application not found");
        }
        if (!"ACTIVE".equalsIgnoreCase(application.getStatus())
                && !"APPROVED".equalsIgnoreCase(application.getStatus())) {
            throw new RuntimeException("Only approved or active insurance can be closed");
        }
        application.setStatus("CLOSED");
        application.setPolicyEndDate(LocalDate.now());
        application.setAdminRemark("Closed by customer");
        return applicationRepository.save(application);
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

    public List<InsuranceApplication> getAllApplications() {
        return applicationRepository.findAll();
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
        if ("REJECTED".equals(st) || "EXPIRED".equals(st) || "CLOSED".equals(st)) {
            throw new RuntimeException("Cannot pay premium for rejected, expired, or closing applications");
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
