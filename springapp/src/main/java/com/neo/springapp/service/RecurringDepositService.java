package com.neo.springapp.service;

import com.neo.springapp.model.Account;
import com.neo.springapp.model.RecurringDeposit;
import com.neo.springapp.model.RecurringDepositContribution;
import com.neo.springapp.model.TermDepositPlan;
import com.neo.springapp.repository.RecurringDepositContributionRepository;
import com.neo.springapp.repository.RecurringDepositRepository;
import com.neo.springapp.repository.TermDepositPlanRepository;
import com.neo.springapp.model.DepositManagementHistory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class RecurringDepositService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int SHARE_DAYS = 7;
    private static final int CAPTCHA_MAX_ATTEMPTS = 5;

    private final RecurringDepositRepository repository;
    private final TermDepositPlanRepository planRepository;
    private final AccountService accountService;
    private final DepositManagementHistoryService historyService;

    @Autowired private RecurringDepositContributionRepository contributionRepository;
    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    public RecurringDepositService(RecurringDepositRepository repository,
                                   TermDepositPlanRepository planRepository,
                                   AccountService accountService,
                                   DepositManagementHistoryService historyService) {
        this.repository = repository;
        this.planRepository = planRepository;
        this.accountService = accountService;
        this.historyService = historyService;
    }

    @Transactional(readOnly = true)
    public List<RecurringDeposit> getForAccount(String accountNumber) {
        return repository.findByAccountNumberOrderByCreatedAtDesc(accountNumber);
    }

    @Transactional(readOnly = true)
    public List<RecurringDeposit> getAll() {
        return repository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public RecurringDeposit getById(Long id) {
        return repository.findById(id).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<DepositManagementHistory> getHistory(Long id) {
        return historyService.getHistory("RD", id);
    }

    @Transactional
    public RecurringDeposit create(String accountNumber, Long planId, Double monthlyInstallment,
                                   LocalDate startDate, String remarks, String actor) {
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalArgumentException("Linked account number is required.");
        }
        Account account = accountService.getAccountByNumber(accountNumber);
        if (account == null) {
            throw new IllegalArgumentException("Linked customer account was not found.");
        }
        TermDepositPlan plan = planRepository.findById(planId)
                .filter(p -> Boolean.TRUE.equals(p.getActive()))
                .filter(p -> "RD".equalsIgnoreCase(p.getProductType()))
                .orElseThrow(() -> new IllegalArgumentException("Select an active RD scheme."));
        validateInstallment(monthlyInstallment, plan);
        if (plan.getDurationMonths() == null || plan.getDurationMonths() <= 0) {
            throw new IllegalArgumentException("The RD scheme must have a valid duration in months.");
        }

        RecurringDeposit rd = new RecurringDeposit();
        rd.setAccountNumber(accountNumber.trim());
        rd.setCustomerName(account.getName());
        rd.setPlanId(plan.getId());
        rd.setSchemeName(plan.getSchemeName());
        rd.setPayoutInstruction(plan.getPayoutDate());
        rd.setMonthlyInstallment(monthlyInstallment);
        rd.setAnnualInterestRate(plan.getInterestRate());
        rd.setTenureMonths(plan.getDurationMonths());
        rd.setStartDate(startDate == null ? LocalDate.now() : startDate);
        rd.setMaturityDate(rd.getStartDate().plusMonths(rd.getTenureMonths()));
        rd.setRemarks(remarks);
        rd.setCreatedBy(actor);
        rd.setUpdatedBy(actor);
        calculateEstimate(rd);
        RecurringDeposit saved = repository.save(rd);
        saved.setRdAccountNumber("RD" + String.format(java.util.Locale.ROOT, "%08d", saved.getId()));
        saved = repository.save(saved);
        historyService.record("RD", saved.getId(), saved.getAccountNumber(), "DETAILS_CREATED",
                actor, "Admin-created record; no funds moved.", details(saved));
        return saved;
    }

    @Transactional
    public RecurringDeposit update(Long id, Double monthlyInstallment, Integer tenureMonths,
                                   Double annualInterestRate, LocalDate startDate,
                                   LocalDate maturityDate, String remarks, String reason, String actor) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("An audit reason is required.");
        }
        RecurringDeposit rd = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("RD record not found."));
        if (!"DETAILS_ONLY".equals(rd.getStatus())) {
            throw new IllegalStateException("RD terms cannot be edited after a customer review link is issued.");
        }
        String before = details(rd);
        if (monthlyInstallment != null) rd.setMonthlyInstallment(monthlyInstallment);
        if (tenureMonths != null) rd.setTenureMonths(tenureMonths);
        if (annualInterestRate != null) rd.setAnnualInterestRate(annualInterestRate);
        if (startDate != null) rd.setStartDate(startDate);
        if (maturityDate != null) rd.setMaturityDate(maturityDate);
        if (remarks != null) rd.setRemarks(remarks);
        if (rd.getMonthlyInstallment() == null || rd.getMonthlyInstallment() <= 0
                || rd.getTenureMonths() == null || rd.getTenureMonths() <= 0
                || rd.getAnnualInterestRate() == null || rd.getAnnualInterestRate() < 0
                || rd.getAnnualInterestRate() > 100) {
            throw new IllegalArgumentException("RD amount, duration, or interest rate is invalid.");
        }
        if (rd.getMaturityDate() == null || rd.getMaturityDate().isBefore(rd.getStartDate())) {
            throw new IllegalArgumentException("Maturity date must be on or after the start date.");
        }
        rd.setUpdatedBy(actor);
        calculateEstimate(rd);
        RecurringDeposit saved = repository.save(rd);
        historyService.record("RD", saved.getId(), saved.getAccountNumber(), "DETAILS_UPDATED",
                actor, reason, before + " -> " + details(saved));
        return saved;
    }

    private void validateInstallment(Double amount, TermDepositPlan plan) {
        if (amount == null || amount <= 0
                || (plan.getMinimumAmount() != null && amount < plan.getMinimumAmount())
                || (plan.getMaximumAmount() != null && amount > plan.getMaximumAmount())) {
            throw new IllegalArgumentException("Monthly installment is outside the selected scheme limits.");
        }
    }

    private void calculateEstimate(RecurringDeposit rd) {
        double monthlyRate = rd.getAnnualInterestRate() / 1200.0;
        double months = rd.getTenureMonths();
        double principal = rd.getMonthlyInstallment() * months;
        double maturity = monthlyRate == 0
                ? principal
                : rd.getMonthlyInstallment() * (Math.pow(1 + monthlyRate, months) - 1) / monthlyRate;
        maturity = Math.round(maturity * 100.0) / 100.0;
        rd.setEstimatedPrincipal(Math.round(principal * 100.0) / 100.0);
        rd.setEstimatedMaturityAmount(maturity);
        rd.setEstimatedInterest(Math.round((maturity - principal) * 100.0) / 100.0);
    }

    private String details(RecurringDeposit rd) {
        return "scheme=" + rd.getSchemeName() + ", monthlyInstallment=" + rd.getMonthlyInstallment()
                + ", tenureMonths=" + rd.getTenureMonths() + ", annualRate=" + rd.getAnnualInterestRate()
                + ", startDate=" + rd.getStartDate() + ", maturityDate=" + rd.getMaturityDate()
                + ", estimatedInterest=" + rd.getEstimatedInterest()
                + ", estimatedMaturity=" + rd.getEstimatedMaturityAmount()
                + ", payout=" + rd.getPayoutInstruction();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> customerViews(String accountNumber) {
        return repository.findByAccountNumberOrderByCreatedAtDesc(accountNumber).stream().map(rd -> {
            Map<String, Object> view = recordView(rd);
            view.put("linkedAccount", accountView(accountService.getAccountByNumber(rd.getAccountNumber())));
            return view;
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> adminViews() {
        return repository.findAllByOrderByCreatedAtDesc().stream().map(rd -> {
            Map<String, Object> view = recordView(rd);
            Account account = accountService.getAccountByNumber(rd.getAccountNumber());
            view.put("linkedAccount", accountView(account));
            return view;
        }).toList();
    }

    @Transactional
    public Map<String, Object> issueShareLink(Long id, String actor) {
        RecurringDeposit rd = requireDeposit(id);
        if (!"DETAILS_ONLY".equals(rd.getStatus()) && !"PENDING_SIGNATURE".equals(rd.getStatus())) {
            throw new IllegalStateException("A customer review link can only be issued before signature.");
        }
        if (rd.getRdAccountNumber() == null || rd.getRdAccountNumber().isBlank()) {
            rd.setRdAccountNumber("RD" + String.format(java.util.Locale.ROOT, "%08d", rd.getId()));
        }
        String token = newToken();
        rd.setShareTokenHash(sha256(token));
        rd.setShareExpiresAt(LocalDateTime.now().plusDays(SHARE_DAYS));
        rd.setCaptchaChallengeId(null);
        rd.setCaptchaAnswerHash(null);
        rd.setCaptchaExpiresAt(null);
        rd.setCaptchaAttempts(0);
        rd.setStatus("PENDING_SIGNATURE");
        rd.setUpdatedBy(actor);
        repository.save(rd);
        historyService.record("RD", rd.getId(), rd.getAccountNumber(), "REVIEW_LINK_ISSUED",
                actor, "Customer review and acknowledgement link issued.", details(rd));
        Map<String, Object> response = new HashMap<>();
        response.put("shareUrl", frontendUrl.replaceAll("/+$", "") + "/rd-review/" + token);
        response.put("status", rd.getStatus());
        response.put("expiresAt", rd.getShareExpiresAt());
        return response;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> shareDetails(String token) {
        RecurringDeposit rd = findByToken(token);
        ensureShareAvailable(rd);
        Map<String, Object> result = recordView(rd);
        result.remove("linkedAccount");
        result.put("accountNumber", maskAccountNumber(rd.getAccountNumber()));
        result.put("expiresAt", rd.getShareExpiresAt());
        result.put("termsNotice", "This is a product-terms review and customer acknowledgement, not a legal contract or a bank-issued e-signature. A separate customer-approved ECS mandate is required before any installment debit.");
        return result;
    }

    @Transactional
    public Map<String, Object> createCaptcha(String token) {
        RecurringDeposit rd = findByToken(token);
        ensureCanSign(rd);
        int left = RANDOM.nextInt(8) + 2;
        int right = RANDOM.nextInt(8) + 2;
        String challengeId = UUID.randomUUID().toString();
        rd.setCaptchaChallengeId(challengeId);
        rd.setCaptchaAnswerHash(sha256(Integer.toString(left + right)));
        rd.setCaptchaExpiresAt(LocalDateTime.now().plusMinutes(5));
        repository.save(rd);
        return Map.of("challengeId", challengeId, "question", left + " + " + right + " = ?");
    }

    @Transactional
    public Map<String, Object> sign(String token, String challengeId, String captchaAnswer,
                                    String signedName, boolean consent, String remoteAddress) {
        RecurringDeposit rd = findByToken(token);
        ensureCanSign(rd);
        if (signedName == null || signedName.isBlank() || signedName.trim().length() > 120) {
            throw new IllegalArgumentException("Enter your full name (up to 120 characters).");
        }
        if (rd.getCustomerName() == null || !rd.getCustomerName().equalsIgnoreCase(signedName.trim())) {
            throw new IllegalArgumentException("Enter the full name registered to this recurring-deposit account.");
        }
        if (!consent) throw new IllegalArgumentException("Confirm that you reviewed the recurring-deposit terms.");
        if (rd.getCaptchaAttempts() == null) rd.setCaptchaAttempts(0);
        boolean challengeValid = challengeId != null && challengeId.equals(rd.getCaptchaChallengeId())
                && rd.getCaptchaExpiresAt() != null && rd.getCaptchaExpiresAt().isAfter(LocalDateTime.now());
        boolean answerValid = captchaAnswer != null && rd.getCaptchaAnswerHash() != null
                && MessageDigest.isEqual(sha256(captchaAnswer.trim()).getBytes(StandardCharsets.US_ASCII),
                    rd.getCaptchaAnswerHash().getBytes(StandardCharsets.US_ASCII));
        if (!challengeValid || !answerValid) {
            rd.setCaptchaAttempts(rd.getCaptchaAttempts() + 1);
            rd.setCaptchaChallengeId(null);
            rd.setCaptchaAnswerHash(null);
            repository.save(rd);
            return Map.of("success", false, "message",
                    "CAPTCHA answer is incorrect or expired. Refresh and try again.");
        }
        String name = signedName.trim();
        LocalDateTime signedAt = LocalDateTime.now();
        rd.setSignedName(name);
        rd.setSignedAt(signedAt);
        rd.setSignatureIp(remoteAddress == null ? null : remoteAddress.substring(0, Math.min(remoteAddress.length(), 64)));
        rd.setSignatureHash(sha256(String.join("|", rd.getId().toString(), name, signedAt.toString(),
                rd.getMonthlyInstallment().toString(), rd.getTenureMonths().toString(),
                rd.getAnnualInterestRate().toString(), rd.getMaturityDate().toString())));
        rd.setStatus("SIGNED");
        clearCaptcha(rd);
        repository.save(rd);
        historyService.record("RD", rd.getId(), rd.getAccountNumber(), "CUSTOMER_ACKNOWLEDGED",
                name, "Customer acknowledged the recurring-deposit terms; this is not ECS authorization.", details(rd));
        return Map.of("success", true, "status", rd.getStatus(),
                "message", "Customer acknowledgement recorded. Staff approval is still required.");
    }

    @Transactional
    public RecurringDeposit approve(Long id, String actor) {
        RecurringDeposit rd = requireDeposit(id);
        if (!"SIGNED".equals(rd.getStatus())) {
            throw new IllegalStateException("Customer terms acknowledgement is required before admin approval.");
        }
        rd.setStatus("APPROVED");
        rd.setApprovedAt(LocalDateTime.now());
        rd.setApprovedBy(actor);
        rd.setUpdatedBy(actor);
        RecurringDeposit saved = repository.save(rd);
        historyService.record("RD", saved.getId(), saved.getAccountNumber(), "ADMIN_APPROVED",
                actor, "Approved after customer acknowledgement; ECS consent remains separate.", details(saved));
        return saved;
    }

    @Transactional(readOnly = true)
    public List<RecurringDepositContribution> contributions(Long id) {
        requireDeposit(id);
        return contributionRepository.findByRecurringDepositIdOrderByPostedDateDesc(id);
    }

    private Map<String, Object> recordView(RecurringDeposit rd) {
        Map<String, Object> view = new HashMap<>();
        view.put("id", rd.getId());
        view.put("rdAccountNumber", rd.getRdAccountNumber());
        view.put("accountNumber", rd.getAccountNumber());
        view.put("customerName", rd.getCustomerName());
        view.put("planId", rd.getPlanId());
        view.put("schemeName", rd.getSchemeName());
        view.put("monthlyInstallment", rd.getMonthlyInstallment());
        view.put("annualInterestRate", rd.getAnnualInterestRate());
        view.put("tenureMonths", rd.getTenureMonths());
        view.put("startDate", rd.getStartDate());
        view.put("maturityDate", rd.getMaturityDate());
        view.put("estimatedPrincipal", rd.getEstimatedPrincipal());
        view.put("estimatedInterest", rd.getEstimatedInterest());
        view.put("estimatedMaturityAmount", rd.getEstimatedMaturityAmount());
        view.put("payoutInstruction", rd.getPayoutInstruction());
        view.put("paidInstallments", rd.getPaidInstallments());
        view.put("paidPrincipal", rd.getPaidPrincipal());
        view.put("status", rd.getStatus());
        view.put("signedName", rd.getSignedName());
        view.put("signedAt", rd.getSignedAt());
        view.put("approvedBy", rd.getApprovedBy());
        view.put("approvedAt", rd.getApprovedAt());
        view.put("createdAt", rd.getCreatedAt());
        view.put("updatedAt", rd.getUpdatedAt());
        view.put("remarks", rd.getRemarks());
        return view;
    }

    private Map<String, Object> accountView(Account account) {
        if (account == null) return Map.of();
        Map<String, Object> view = new HashMap<>();
        view.put("accountNumber", account.getAccountNumber());
        view.put("customerName", account.getName());
        view.put("accountType", account.getAccountType());
        view.put("balance", account.getBalance());
        view.put("status", account.getStatus());
        return view;
    }

    private RecurringDeposit requireDeposit(Long id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("RD record not found."));
    }

    private RecurringDeposit findByToken(String token) {
        if (token == null || token.isBlank()) throw new IllegalArgumentException("The RD review link is invalid.");
        return repository.findByShareTokenHash(sha256(token))
                .orElseThrow(() -> new IllegalArgumentException("The RD review link is invalid or expired."));
    }

    private void ensureShareAvailable(RecurringDeposit rd) {
        if (!List.of("PENDING_SIGNATURE", "SIGNED").contains(rd.getStatus())
                || rd.getShareExpiresAt() == null || !rd.getShareExpiresAt().isAfter(LocalDateTime.now())) {
            throw new IllegalStateException("The RD review link is no longer available.");
        }
    }

    private void ensureCanSign(RecurringDeposit rd) {
        ensureShareAvailable(rd);
        if (!"PENDING_SIGNATURE".equals(rd.getStatus())) {
            throw new IllegalStateException("Customer acknowledgement has already been recorded.");
        }
        if (rd.getCaptchaAttempts() != null && rd.getCaptchaAttempts() >= CAPTCHA_MAX_ATTEMPTS) {
            throw new IllegalStateException("CAPTCHA attempt limit reached. Ask the bank to issue a fresh link.");
        }
    }

    private void clearCaptcha(RecurringDeposit rd) {
        rd.setCaptchaChallengeId(null);
        rd.setCaptchaAnswerHash(null);
        rd.setCaptchaExpiresAt(null);
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Could not securely process the RD review token.", e);
        }
    }

    private String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() <= 4) return "****";
        return "*".repeat(accountNumber.length() - 4) + accountNumber.substring(accountNumber.length() - 4);
    }
}
