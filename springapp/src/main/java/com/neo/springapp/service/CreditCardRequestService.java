package com.neo.springapp.service;

import com.neo.springapp.model.Card;
import com.neo.springapp.model.CreditCard;
import com.neo.springapp.model.CreditCardRequest;
import com.neo.springapp.repository.CardRepository;
import com.neo.springapp.repository.CreditCardRequestRepository;
import com.neo.springapp.repository.CreditCardRepository;
import com.neo.springapp.repository.SalaryAccountRepository;
import com.neo.springapp.repository.VirtualCardRepository;
import com.neo.springapp.model.SalaryAccount;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import com.neo.springapp.model.Account;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@SuppressWarnings("null")
public class CreditCardRequestService {

    private static final Logger logger = LoggerFactory.getLogger(CreditCardRequestService.class);
    private static final SecureRandom CARD_RANDOM = new SecureRandom();
    private static final int MAX_CARD_DETAILS_OTP_ATTEMPTS = 5;
    private static final Duration CARD_DETAILS_OTP_RESEND_DELAY = Duration.ofSeconds(30);
    private static final Duration CARD_DETAILS_OTP_LOCK_DURATION = Duration.ofMinutes(15);
    private static final int MINIMUM_CIBIL_SCORE = 750;
    private static final int AGREEMENT_LINK_VALID_DAYS = 7;
    private static final int MAX_AGREEMENT_CAPTCHA_ATTEMPTS = 5;
    private static final String AGREEMENT_CAPTCHA_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final CreditCardRequestRepository repo;
    private final CreditScorePredictorService predictor;
    private final CardRepository cardRepository;
    private final CreditCardRepository creditCardRepository;
    private final VirtualCardRepository virtualCardRepository;
    private final AccountService accountService;
    private final SalaryAccountRepository salaryAccountRepository;
    private final CreditCardApplicationDocumentService documentService;
    private final OtpService otpService;
    private final EmailService emailService;
    private final String frontendUrl;

    public CreditCardRequestService(CreditCardRequestRepository repo, CreditScorePredictorService predictor,
                                    CardRepository cardRepository, CreditCardRepository creditCardRepository,
                                    VirtualCardRepository virtualCardRepository,
                                    AccountService accountService, SalaryAccountRepository salaryAccountRepository,
                                    CreditCardApplicationDocumentService documentService, OtpService otpService,
                                    EmailService emailService,
                                    @Value("${app.frontend-url:http://localhost:4200}") String frontendUrl) {
        this.repo = repo;
        this.predictor = predictor;
        this.cardRepository = cardRepository;
        this.creditCardRepository = creditCardRepository;
        this.virtualCardRepository = virtualCardRepository;
        this.accountService = accountService;
        this.salaryAccountRepository = salaryAccountRepository;
        this.documentService = documentService;
        this.otpService = otpService;
        this.emailService = emailService;
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
    }

    @Transactional
    public CreditCardRequest saveRequest(CreditCardRequest request, Double income) {
        String pan = request == null || request.getPan() == null
                ? "" : request.getPan().trim().toUpperCase(Locale.ROOT);
        if (!pan.matches("[A-Z]{5}[0-9]{4}[A-Z]")) {
            throw new IllegalArgumentException("Enter a valid 10-character PAN.");
        }
        if (income == null || !Double.isFinite(income) || income <= 0 || income > 1_000_000_000d) {
            throw new IllegalArgumentException("Enter a valid annual income.");
        }
        if (request.getAccountNumber() == null || request.getAccountNumber().isBlank()) {
            throw new IllegalArgumentException("A verified customer account is required.");
        }
        if (!creditCardRepository.findByAccountNumber(request.getAccountNumber()).isEmpty()
                || repo.findByAccountNumberOrderByRequestDateDesc(request.getAccountNumber()).stream()
                .anyMatch(existing -> isOpenApplicationStatus(existing.getStatus()))) {
            throw new IllegalStateException("A credit card or open credit card application already exists for this account.");
        }

        request.setPan(pan);
        request.setAnnualIncome(income);
        request.setRequestDate(LocalDateTime.now());
        evaluateCreditScore(request);
        return repo.save(request);
    }

    @Transactional
    public CreditCardRequest createGuestApplication(CreditCardRequest request, String aadhaarLastFour,
                                                    String address, String mobileNumber, String dateOfBirth,
                                                    String employmentType, String employerName, String designation,
                                                    boolean consent, MultipartFile panDocument,
                                                    MultipartFile aadhaarDocument, MultipartFile selfie) {
        validateGuestApplication(request, aadhaarLastFour, address, mobileNumber, dateOfBirth,
                employmentType, employerName, designation, consent);
        String normalizedPan = request.getPan().trim().toUpperCase(Locale.ROOT);
        String normalizedMobile = mobileNumber.trim();
        if (repo.findByPanIgnoreCaseAndMobileNumberOrderByRequestDateDesc(normalizedPan, normalizedMobile).stream()
                .anyMatch(existing -> isOpenApplicationStatus(existing.getStatus()))) {
            throw new IllegalStateException("An open application already exists for this PAN and mobile number.");
        }

        String applicationId = "CC" + LocalDate.now().getYear()
                + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase(Locale.ROOT);
        request.setApplicationId(applicationId);
        request.setAccountNumber(null);
        request.setPan(normalizedPan);
        request.setMobileNumber(normalizedMobile);
        request.setDateOfBirth(dateOfBirth);
        request.setAadhaarLastFour(aadhaarLastFour.trim());
        request.setAddress(address.trim());
        request.setEmploymentType(employmentType);
        request.setEmployerName(employerName.trim());
        request.setDesignation(designation.trim());
        request.setTrackingCredentialHash(trackingCredentialHash(applicationId, normalizedPan, normalizedMobile));
        request.setConsentGiven(true);
        request.setConsentedAt(LocalDateTime.now());
        request.setRequestDate(LocalDateTime.now());
        evaluateCreditScore(request);

        try {
            documentService.store(applicationId, panDocument, aadhaarDocument, selfie);
            request.setDocumentsAvailable(true);
            return repo.save(request);
        } catch (IOException | RuntimeException exception) {
            try {
                documentService.delete(applicationId);
            } catch (IOException cleanupException) {
                exception.addSuppressed(cleanupException);
            }
            if (exception instanceof RuntimeException runtimeException) throw runtimeException;
            throw new IllegalStateException("The application documents could not be saved.", exception);
        }
    }

    public CreditCardRequest trackGuestApplication(String applicationId, String pan, String mobileNumber) {
        if (applicationId == null || pan == null || mobileNumber == null) return null;
        CreditCardRequest request = repo.findByApplicationId(applicationId.trim().toUpperCase(Locale.ROOT));
        if (request == null || request.getTrackingCredentialHash() == null) return null;
        String submittedHash = trackingCredentialHash(
                request.getApplicationId(), pan.trim().toUpperCase(Locale.ROOT), mobileNumber.trim());
        return MessageDigest.isEqual(
                request.getTrackingCredentialHash().getBytes(StandardCharsets.UTF_8),
                submittedHash.getBytes(StandardCharsets.UTF_8)) ? request : null;
    }

    @Transactional
    public String requestGuestCardDetailsOtp(String applicationId, String pan, String mobileNumber) {
        CreditCardRequest request = requireApprovedGuestCard(applicationId, pan, mobileNumber);
        LocalDateTime now = LocalDateTime.now();
        if (request.getCardDetailsOtpLockedUntil() != null
                && request.getCardDetailsOtpLockedUntil().isAfter(now)) {
            throw new IllegalStateException("Card detail verification is temporarily locked. Try again later.");
        }
        if (request.getCardDetailsOtpLockedUntil() != null) {
            request.setCardDetailsOtpLockedUntil(null);
            request.setCardDetailsOtpAttempts(0);
        }
        if (request.getCardDetailsOtpSentAt() != null
                && Duration.between(request.getCardDetailsOtpSentAt(), now)
                .compareTo(CARD_DETAILS_OTP_RESEND_DELAY) < 0) {
            throw new IllegalStateException("Please wait before requesting another verification code.");
        }

        otpService.sendOtpForKey(request.getUserEmail(), guestCardOtpKey(request.getApplicationId()),
                "NeoBank credit card details verification");
        request.setCardDetailsOtpSentAt(now);
        repo.save(request);
        return maskEmail(request.getUserEmail());
    }

    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public Map<String, Object> verifyGuestCardDetailsOtp(
            String applicationId, String pan, String mobileNumber, String otp) {
        CreditCardRequest request = requireApprovedGuestCard(applicationId, pan, mobileNumber);
        LocalDateTime now = LocalDateTime.now();
        if (request.getCardDetailsOtpLockedUntil() != null
                && request.getCardDetailsOtpLockedUntil().isAfter(now)) {
            throw new IllegalStateException("Card detail verification is temporarily locked. Try again later.");
        }
        if (request.getCardDetailsOtpSentAt() == null) {
            throw new IllegalStateException("Request an email verification code before entering it.");
        }
        if (!otpService.verifyOtpByKey(guestCardOtpKey(request.getApplicationId()), otp)) {
            int attempts = request.getCardDetailsOtpAttempts() == null ? 0 : request.getCardDetailsOtpAttempts();
            attempts++;
            request.setCardDetailsOtpAttempts(attempts);
            if (attempts >= MAX_CARD_DETAILS_OTP_ATTEMPTS) {
                request.setCardDetailsOtpLockedUntil(now.plus(CARD_DETAILS_OTP_LOCK_DURATION));
            }
            repo.save(request);
            throw new IllegalArgumentException("The verification code is invalid or expired.");
        }

        request.setCardDetailsOtpAttempts(0);
        request.setCardDetailsOtpSentAt(null);
        request.setCardDetailsOtpLockedUntil(null);
        repo.save(request);
        return Map.of(
                "cardNumber", request.getGuestCardNumber(),
                "expiryDate", request.getGuestCardExpiry(),
                "cvv", request.getGuestCardCvv(),
                "approvedLimit", request.getApprovedLimit());
    }

    private CreditCardRequest requireApprovedGuestCard(String applicationId, String pan, String mobileNumber) {
        CreditCardRequest request = trackGuestApplication(applicationId, pan, mobileNumber);
        if (request == null) {
            throw new IllegalArgumentException("Application not found. Check the application ID, PAN, and mobile number.");
        }
        if (request.getAccountNumber() != null || !"Approved".equalsIgnoreCase(request.getStatus())
                || request.getGuestCardNumber() == null || request.getGuestCardExpiry() == null
                || request.getGuestCardCvv() == null) {
            throw new IllegalStateException("Card details are available after an eligible guest application is approved.");
        }
        return request;
    }

    private String guestCardOtpKey(String applicationId) {
        return "CREDIT_CARD_GUEST:" + applicationId;
    }

    private String maskEmail(String email) {
        int atIndex = email == null ? -1 : email.indexOf('@');
        if (atIndex <= 1) return "***";
        return email.charAt(0) + "***" + email.substring(atIndex);
    }

    @Transactional
    public CreditCardRequest updateGuestApplication(Long id, GuestApplicationUpdate update) {
        CreditCardRequest request = repo.findById(id).orElse(null);
        if (request == null) return null;
        if (!"Pending".equalsIgnoreCase(request.getStatus())) {
            throw new IllegalStateException("Only pending applications can be edited.");
        }
        CreditCardRequest candidate = new CreditCardRequest();
        candidate.setPan(update.pan());
        candidate.setUserName(update.userName());
        candidate.setUserEmail(update.userEmail());
        validateGuestApplicationFields(candidate, update.aadhaarLastFour(), update.address(), update.mobileNumber(),
                update.dateOfBirth(), update.employmentType(), update.employerName(), update.designation());
        if (update.annualIncome() == null || !Double.isFinite(update.annualIncome())
                || update.annualIncome() <= 0 || update.annualIncome() > 1_000_000_000d) {
            throw new IllegalArgumentException("Enter a valid annual income.");
        }
        request.setUserName(update.userName().trim());
        request.setUserEmail(update.userEmail().trim());
        request.setPan(update.pan().trim().toUpperCase(Locale.ROOT));
        request.setAnnualIncome(update.annualIncome());
        request.setMobileNumber(update.mobileNumber().trim());
        request.setDateOfBirth(update.dateOfBirth());
        request.setAadhaarLastFour(update.aadhaarLastFour().trim());
        request.setAddress(update.address().trim());
        request.setEmploymentType(update.employmentType());
        request.setEmployerName(update.employerName().trim());
        request.setDesignation(update.designation().trim());
        evaluateCreditScore(request);
        return repo.save(request);
    }

    public Resource getApplicationDocument(Long id, String documentType) {
        CreditCardRequest request = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Application not found."));
        if (!request.isDocumentsAvailable()) {
            throw new IllegalArgumentException("The application documents are no longer available.");
        }
        return documentService.load(request.getApplicationId(), documentType);
    }

    public String getApplicationDocumentContentType(Resource resource) {
        return documentService.contentType(resource);
    }

    public List<CreditCardRequest> getRequestsByStatus(String status) {
        return repo.findByStatus(status);
    }

    public List<CreditCardRequest> getRequestsByAccountNumber(String accountNumber) {
        return repo.findByAccountNumberOrderByRequestDateDesc(accountNumber);
    }

    @Transactional
    public ApprovalResult approveRequest(Long id, String adminName, Double approvedLimit) {
        CreditCardRequest request = repo.findById(id).orElse(null);
        if (request == null) return null;
        if (!"Pending".equalsIgnoreCase(request.getStatus())) {
            throw new IllegalStateException("Only pending credit card applications can be approved.");
        }
        if (request.getPredictedCibil() == null) evaluateCreditScore(request);
        if (request.getPredictedCibil() < MINIMUM_CIBIL_SCORE) {
            request.setStatus("Rejected");
            request.setEligibility("Rejected");
            request.setDocumentsDeleteAfter(LocalDateTime.now().plusDays(90));
            request = repo.save(request);
            return new ApprovalResult(request, null, null, false);
        }
        if (approvedLimit == null || !Double.isFinite(approvedLimit)
                || approvedLimit <= 0 || approvedLimit > 1_000_000_000d) {
            throw new IllegalArgumentException("Enter a valid approved credit limit.");
        }
        validateAccountForCardIssuance(request);

        LocalDateTime now = LocalDateTime.now();
        String token = newAgreementToken();
        request.setStatus("Pending Signature");
        request.setEligibility("Pending Signature");
        request.setProcessedBy(adminName);
        request.setProcessedDate(now);
        request.setApprovedLimit(approvedLimit);
        request.setDocumentsDeleteAfter(now.plusDays(90));
        request.setCardAgreementTokenHash(sha256(token));
        request.setCardAgreementExpiresAt(now.plusDays(AGREEMENT_LINK_VALID_DAYS));
        request.setAgreementCaptchaAttempts(0);
        request.setAgreementCaptchaChallengeId(null);
        request.setAgreementCaptchaAnswerHash(null);
        request.setAgreementCaptchaExpiresAt(null);
        repo.save(request);

        String shareUrl = frontendUrl + "/credit-card-esign/" + token;
        boolean emailSent = emailService.sendCreditCardAgreementLinkEmail(
                request.getUserEmail(), request.getUserName(), shareUrl,
                request.getCardAgreementExpiresAt().toString());
        return new ApprovalResult(request, shareUrl, request.getCardAgreementExpiresAt(), emailSent);
    }

    @Transactional
    public AgreementLinkResult renewCreditCardAgreementLink(Long id) {
        CreditCardRequest request = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Credit card application not found."));
        if (!"Pending Signature".equalsIgnoreCase(request.getStatus())) {
            throw new IllegalStateException("An e-sign link can only be renewed while signature is pending.");
        }
        String token = newAgreementToken();
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(AGREEMENT_LINK_VALID_DAYS);
        request.setCardAgreementTokenHash(sha256(token));
        request.setCardAgreementExpiresAt(expiresAt);
        request.setAgreementCaptchaAttempts(0);
        request.setAgreementCaptchaChallengeId(null);
        request.setAgreementCaptchaAnswerHash(null);
        request.setAgreementCaptchaExpiresAt(null);
        repo.save(request);
        String shareUrl = frontendUrl + "/credit-card-esign/" + token;
        boolean emailSent = emailService.sendCreditCardAgreementLinkEmail(
                request.getUserEmail(), request.getUserName(), shareUrl, expiresAt.toString());
        return new AgreementLinkResult(shareUrl, expiresAt, emailSent);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> creditCardAgreementDetails(String token) {
        CreditCardRequest request = requirePendingAgreement(token);
        Map<String, Object> details = new java.util.LinkedHashMap<>();
        details.put("applicationId", request.getApplicationId());
        details.put("applicantName", request.getUserName());
        details.put("applicantEmail", maskEmail(request.getUserEmail()));
        details.put("panLastFour", request.getPan() == null ? "" : request.getPan().substring(6));
        details.put("mobileLastFour", request.getMobileNumber() == null
                || request.getMobileNumber().length() < 4 ? "" : request.getMobileNumber().substring(
                request.getMobileNumber().length() - 4));
        details.put("address", request.getAddress());
        details.put("dateOfBirth", request.getDateOfBirth());
        details.put("employmentType", request.getEmploymentType());
        details.put("employerName", request.getEmployerName());
        details.put("designation", request.getDesignation());
        details.put("annualIncome", request.getAnnualIncome());
        details.put("predictedCibil", request.getPredictedCibil());
        details.put("scoreNotice", "Simulated estimate based on PAN pattern and declared income; not a bureau score.");
        details.put("cardType", request.getCardType());
        details.put("approvedLimit", request.getApprovedLimit());
        details.put("interestRate", request.getInterestRate());
        details.put("annualFee", request.getAnnualFee());
        details.put("status", request.getStatus());
        details.put("expiresAt", request.getCardAgreementExpiresAt());
        details.put("signatureNotice", "Typed-name e-signature for this demo; not a certified digital signature or binding bank contract.");
        return details;
    }

    @Transactional
    public Map<String, Object> createCreditCardAgreementCaptcha(String token) {
        CreditCardRequest request = requirePendingAgreement(token);
        int attempts = request.getAgreementCaptchaAttempts() == null ? 0 : request.getAgreementCaptchaAttempts();
        if (attempts >= MAX_AGREEMENT_CAPTCHA_ATTEMPTS) {
            throw new IllegalStateException("CAPTCHA attempt limit reached. Ask the bank to issue a fresh e-sign link.");
        }
        String answer = randomAgreementCaptcha();
        String challengeId = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(5);
        request.setAgreementCaptchaChallengeId(challengeId);
        request.setAgreementCaptchaAnswerHash(sha256(answer));
        request.setAgreementCaptchaExpiresAt(expiresAt);
        repo.save(request);
        return Map.of(
                "challengeId", challengeId,
                "image", Base64.getEncoder().encodeToString(agreementCaptchaSvg(answer)
                        .getBytes(StandardCharsets.UTF_8)),
                "expiresAt", expiresAt);
    }

    @Transactional
    public AgreementSignResult signCreditCardAgreement(String token, String challengeId, String captchaAnswer,
                                                       String signedName, boolean consent, String remoteAddress) {
        CreditCardRequest request = requirePendingAgreement(token);
        if (signedName == null || signedName.isBlank() || signedName.length() > 120
                || !signedName.trim().equalsIgnoreCase(request.getUserName().trim())) {
            throw new IllegalArgumentException("Enter your full name as shown in the application to sign.");
        }
        if (!consent) {
            throw new IllegalArgumentException("Confirm that you reviewed and accept the credit card terms.");
        }
        int attempts = request.getAgreementCaptchaAttempts() == null ? 0 : request.getAgreementCaptchaAttempts();
        if (attempts >= MAX_AGREEMENT_CAPTCHA_ATTEMPTS) {
            throw new IllegalStateException("CAPTCHA attempt limit reached. Ask the bank to issue a fresh e-sign link.");
        }
        boolean challengeValid = challengeId != null
                && challengeId.equals(request.getAgreementCaptchaChallengeId())
                && request.getAgreementCaptchaExpiresAt() != null
                && request.getAgreementCaptchaExpiresAt().isAfter(LocalDateTime.now());
        boolean answerValid = captchaAnswer != null
                && request.getAgreementCaptchaAnswerHash() != null
                && MessageDigest.isEqual(
                sha256(captchaAnswer.trim().toUpperCase(Locale.ROOT)).getBytes(StandardCharsets.US_ASCII),
                request.getAgreementCaptchaAnswerHash().getBytes(StandardCharsets.US_ASCII));
        if (!challengeValid || !answerValid) {
            request.setAgreementCaptchaAttempts(attempts + 1);
            request.setAgreementCaptchaChallengeId(null);
            request.setAgreementCaptchaAnswerHash(null);
            request.setAgreementCaptchaExpiresAt(null);
            repo.save(request);
            return new AgreementSignResult(false, false);
        }

        LocalDateTime signedAt = LocalDateTime.now();
        request.setCardAgreementSignedName(signedName.trim());
        request.setCardAgreementSignedAt(signedAt);
        request.setCardAgreementSignatureHash(sha256(String.join("|",
                request.getId().toString(), request.getApprovedLimit().toString(),
                signedName.trim(), signedAt.toString(), remoteAddress == null ? "" : remoteAddress)));
        request.setAgreementCaptchaChallengeId(null);
        request.setAgreementCaptchaAnswerHash(null);
        request.setAgreementCaptchaExpiresAt(null);
        request.setCardAgreementTokenHash(null);
        issueApprovedCard(request, signedAt);
        repo.save(request);
        return new AgreementSignResult(true,
                request.getAccountNumber() == null || request.getAccountNumber().isBlank());
    }

    private CreditCardRequest requirePendingAgreement(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("The credit card e-sign link is invalid or has expired.");
        }
        CreditCardRequest request = repo.findByCardAgreementTokenHash(sha256(token));
        if (request == null || !"Pending Signature".equalsIgnoreCase(request.getStatus())
                || request.getCardAgreementExpiresAt() == null
                || !request.getCardAgreementExpiresAt().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("The credit card e-sign link is invalid or has expired.");
        }
        return request;
    }

    private void validateAccountForCardIssuance(CreditCardRequest request) {
        if (request.getAccountNumber() == null || request.getAccountNumber().isBlank()) return;
        Account account = accountService.getAccountByNumber(request.getAccountNumber());
        SalaryAccount salaryAccount = account == null
                ? salaryAccountRepository.findByAccountNumber(request.getAccountNumber()) : null;
        if (account == null && salaryAccount == null) {
            throw new IllegalStateException("Cannot approve credit card: the customer account was not found.");
        }
        String accountStatus = account != null ? account.getStatus() : salaryAccount.getStatus();
        if (!"ACTIVE".equalsIgnoreCase(accountStatus)) {
            throw new IllegalStateException("Cannot approve credit card: the customer account is not active.");
        }
        if (!creditCardRepository.findByAccountNumber(request.getAccountNumber()).isEmpty()) {
            throw new IllegalStateException("A credit card is already linked to this account.");
        }
    }

    private void issueApprovedCard(CreditCardRequest request, LocalDateTime issuedAt) {
        String cardNumber = generateUniqueGuestCardNumber();
        String cvv = String.format(Locale.ROOT, "%03d", CARD_RANDOM.nextInt(1000));
        LocalDate expiry = issuedAt.toLocalDate().plusYears(3);
        String expiryDate = String.format(Locale.ROOT, "%02d/%02d",
                expiry.getMonthValue(), expiry.getYear() % 100);
        if (request.getAccountNumber() == null || request.getAccountNumber().isBlank()) {
            request.setGuestCardNumber(cardNumber);
            request.setGuestCardExpiry(expiryDate);
            request.setGuestCardCvv(cvv);
        } else {
            validateAccountForCardIssuance(request);
            CreditCard creditCard = new CreditCard();
            creditCard.setCardNumber(cardNumber);
            creditCard.setCvv(cvv);
            creditCard.setExpiryDate(expiryDate);
            creditCard.setAccountNumber(request.getAccountNumber());
            creditCard.setUserName(request.getUserName());
            creditCard.setUserEmail(request.getUserEmail());
            creditCard.setAppliedDate(request.getRequestDate());
            creditCard.setApprovalDate(issuedAt);
            creditCard.setApprovedLimit(request.getApprovedLimit());
            creditCard.setCurrentBalance(0.0);
            creditCard.calculateAvailableLimit();
            creditCard.calculateUsageLimit();
            creditCard.setStatus("Active");
            creditCardRepository.save(creditCard);

            Card card = new Card();
            card.setCardNumber(cardNumber);
            card.setCardType("Credit");
            card.setUserName(request.getUserName());
            card.setAccountNumber(request.getAccountNumber());
            card.setUserEmail(request.getUserEmail());
            card.setCvv(cvv);
            card.setStatus("Active");
            cardRepository.save(card);
        }
        request.setLast4(cardNumber.substring(12));
        request.setStatus("Approved");
        request.setEligibility("Approved");
        request.setProcessedDate(issuedAt);
    }

    private void evaluateCreditScore(CreditCardRequest request) {
        Map<String, Object> prediction = predictor.getCreditCardPrediction(
                request.getPan(), request.getAnnualIncome());
        int score = ((Number) prediction.get("predictedCibil")).intValue();
        request.setPredictedCibil(score);
        request.setPanBasedLimit(((Number) prediction.get("panBasedLimit")).doubleValue());
        request.setSuggestedLimit(((Number) prediction.get("suggestedLimit")).doubleValue());
        request.setCardType(String.valueOf(prediction.get("cardType")));
        request.setInterestRate(((Number) prediction.get("interestRate")).doubleValue());
        request.setAnnualFee(((Number) prediction.get("annualFee")).doubleValue());
        if (score < MINIMUM_CIBIL_SCORE) {
            request.setStatus("Rejected");
            request.setEligibility("Rejected");
            request.setRejectionReason("The simulated CIBIL estimate is " + score
                    + "; the minimum score for this application is " + MINIMUM_CIBIL_SCORE
                    + ". No credit-bureau lookup was performed.");
            request.setProcessedBy("Automated scoring (demo model)");
            request.setProcessedDate(LocalDateTime.now());
            request.setDocumentsDeleteAfter(LocalDateTime.now().plusDays(90));
        } else {
            request.setStatus("Pending");
            request.setEligibility("Under Review");
            request.setRejectionReason(null);
            request.setProcessedBy(null);
            request.setProcessedDate(null);
            request.setDocumentsDeleteAfter(null);
        }
    }

    private boolean isOpenApplicationStatus(String status) {
        return "Pending".equalsIgnoreCase(status)
                || "Pending Signature".equalsIgnoreCase(status)
                || "Approved".equalsIgnoreCase(status);
    }

    private String newAgreementToken() {
        byte[] bytes = new byte[32];
        CARD_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String randomAgreementCaptcha() {
        StringBuilder answer = new StringBuilder(6);
        for (int index = 0; index < 6; index++) {
            answer.append(AGREEMENT_CAPTCHA_ALPHABET.charAt(
                    CARD_RANDOM.nextInt(AGREEMENT_CAPTCHA_ALPHABET.length())));
        }
        return answer.toString();
    }

    private String agreementCaptchaSvg(String answer) {
        StringBuilder letters = new StringBuilder();
        for (int index = 0; index < answer.length(); index++) {
            int rotation = CARD_RANDOM.nextInt(17) - 8;
            letters.append("<text x=\"").append(16 + index * 29)
                    .append("\" y=\"43\" transform=\"rotate(").append(rotation).append(' ')
                    .append(16 + index * 29).append(" 43)\">")
                    .append(answer.charAt(index)).append("</text>");
        }
        return "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"205\" height=\"58\" viewBox=\"0 0 205 58\">"
                + "<rect width=\"205\" height=\"58\" rx=\"8\" fill=\"#eef3f8\"/>"
                + "<path d=\"M4 13 L200 48 M0 50 L199 8\" stroke=\"#9aa8b6\" stroke-width=\"2\"/>"
                + "<g fill=\"#17324d\" font-family=\"monospace\" font-size=\"25\" font-weight=\"700\">"
                + letters + "</g></svg>";
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return java.util.HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Credit card agreement signing is unavailable.", exception);
        }
    }

    private String generateUniqueGuestCardNumber() {
        for (int attempt = 0; attempt < 20; attempt++) {
            StringBuilder number = new StringBuilder("4");
            while (number.length() < 15) {
                number.append(CARD_RANDOM.nextInt(10));
            }
            number.append(luhnCheckDigit(number.toString()));
            String candidate = number.toString();
            if (cardRepository.findByCardNumber(candidate) == null
                    && creditCardRepository.findByCardNumber(candidate).isEmpty()
                    && virtualCardRepository.findByCardNumber(candidate).isEmpty()
                    && !repo.existsByGuestCardNumber(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("A unique guest card number could not be generated.");
    }

    private int luhnCheckDigit(String digits) {
        int sum = 0;
        for (int index = digits.length() - 1; index >= 0; index--) {
            int digit = digits.charAt(index) - '0';
            if ((digits.length() - 1 - index) % 2 == 0) {
                digit *= 2;
                if (digit > 9) digit -= 9;
            }
            sum += digit;
        }
        return (10 - sum % 10) % 10;
    }

    public CreditCardRequest rejectRequest(Long id, String adminName) {
        return repo.findById(id).map(req -> {
            if (!"Pending".equalsIgnoreCase(req.getStatus())) {
                throw new IllegalStateException("Only pending credit card applications can be rejected.");
            }
            req.setStatus("Rejected");
            req.setEligibility("Rejected");
            req.setProcessedBy(adminName);
            req.setProcessedDate(LocalDateTime.now());
            req.setDocumentsDeleteAfter(LocalDateTime.now().plusDays(90));
            return repo.save(req);
        }).orElse(null);
    }

    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void purgeExpiredApplicationDocuments() {
        for (CreditCardRequest request :
                repo.findByDocumentsDeleteAfterBeforeAndDocumentsAvailableTrue(LocalDateTime.now())) {
            try {
                documentService.delete(request.getApplicationId());
                request.setDocumentsAvailable(false);
                repo.save(request);
            } catch (IOException exception) {
                logger.error("Could not purge credit card application documents for application {}",
                        request.getApplicationId(), exception);
            }
        }
    }

    private void validateGuestApplication(CreditCardRequest request, String aadhaarLastFour, String address,
                                          String mobileNumber, String dateOfBirth, String employmentType,
                                          String employerName, String designation, boolean consent) {
        validateGuestApplicationFields(request, aadhaarLastFour, address, mobileNumber, dateOfBirth,
                employmentType, employerName, designation);
        if (request.getAnnualIncome() == null || !Double.isFinite(request.getAnnualIncome())
                || request.getAnnualIncome() <= 0 || request.getAnnualIncome() > 1_000_000_000d) {
            throw new IllegalArgumentException("Enter a valid annual income.");
        }
        if (!consent) throw new IllegalArgumentException("Consent is required to process this application.");
    }

    private void validateGuestApplicationFields(CreditCardRequest request, String aadhaarLastFour, String address,
                                                String mobileNumber, String dateOfBirth, String employmentType,
                                                String employerName, String designation) {
        if (request.getUserName() == null || request.getUserName().isBlank() || request.getUserName().length() > 120) {
            throw new IllegalArgumentException("Enter the applicant's full name.");
        }
        if (request.getUserEmail() == null
                || !request.getUserEmail().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("Enter a valid email address.");
        }
        if (request.getPan() == null || !request.getPan().trim().matches("(?i)[A-Z]{5}[0-9]{4}[A-Z]")) {
            throw new IllegalArgumentException("Enter a valid 10-character PAN.");
        }
        if (mobileNumber == null || !mobileNumber.trim().matches("[6-9][0-9]{9}")) {
            throw new IllegalArgumentException("Enter a valid 10-digit Indian mobile number.");
        }
        if (aadhaarLastFour == null || !aadhaarLastFour.trim().matches("[0-9]{4}")) {
            throw new IllegalArgumentException("Enter the last four digits of Aadhaar.");
        }
        if (address == null || address.isBlank() || address.length() > 500) {
            throw new IllegalArgumentException("Enter a valid residential address.");
        }
        try {
            LocalDate birthDate = LocalDate.parse(dateOfBirth);
            if (birthDate.isAfter(LocalDate.now().minusYears(18)) || birthDate.isBefore(LocalDate.now().minusYears(120))) {
                throw new IllegalArgumentException("The applicant must be at least 18 years old.");
            }
        } catch (java.time.DateTimeException | NullPointerException exception) {
            throw new IllegalArgumentException("Enter a valid date of birth.", exception);
        }
        if (!"SALARIED".equals(employmentType) && !"SELF_EMPLOYED".equals(employmentType)) {
            throw new IllegalArgumentException("Choose a valid employment type.");
        }
        if (employerName == null || employerName.isBlank() || employerName.length() > 150) {
            throw new IllegalArgumentException("Enter a company or business name.");
        }
        if (designation == null || designation.isBlank() || designation.length() > 120) {
            throw new IllegalArgumentException("Enter a designation or occupation.");
        }
    }

    private String trackingCredentialHash(String applicationId, String pan, String mobileNumber) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return java.util.HexFormat.of().formatHex(digest.digest(
                    (applicationId + "|" + pan + "|" + mobileNumber).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Application tracking is unavailable.", exception);
        }
    }

    public int getPredictedCibil(String pan, Double income) {
        return predictor.predictCibilFromPanAndIncome(pan, income);
    }

    public double getSuggestedLimit(int predictedCibil, Double income) {
        return predictor.suggestLimitFromCibil(predictedCibil, income);
    }

    public java.util.Map<String, Object> getCreditCardPrediction(String pan, Double income) {
        return predictor.getCreditCardPrediction(pan, income);
    }

    public double getPanBasedLimit(String pan) {
        return predictor.getPanBasedLimit(pan);
    }

    public record ApprovalResult(CreditCardRequest application, String shareUrl,
                                 LocalDateTime expiresAt, boolean emailSent) {}
    public record AgreementLinkResult(String shareUrl, LocalDateTime expiresAt, boolean emailSent) {}
    public record AgreementSignResult(boolean signed, boolean guestCard) {}
    public record GuestApplicationUpdate(String userName, String userEmail, String pan, Double annualIncome,
                                         String mobileNumber, String dateOfBirth, String aadhaarLastFour,
                                         String address, String employmentType, String employerName,
                                         String designation) {}
}
