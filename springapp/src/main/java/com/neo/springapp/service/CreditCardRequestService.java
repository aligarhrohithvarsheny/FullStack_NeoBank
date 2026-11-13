package com.neo.springapp.service;

import com.neo.springapp.model.Card;
import com.neo.springapp.model.CreditCard;
import com.neo.springapp.model.CreditCardRequest;
import com.neo.springapp.repository.CardRepository;
import com.neo.springapp.repository.CreditCardRequestRepository;
import com.neo.springapp.repository.CreditCardRepository;
import com.neo.springapp.repository.SalaryAccountRepository;
import com.neo.springapp.model.SalaryAccount;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.neo.springapp.model.Account;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@SuppressWarnings("null")
public class CreditCardRequestService {

    private static final Logger logger = LoggerFactory.getLogger(CreditCardRequestService.class);
    private final CreditCardRequestRepository repo;
    private final CreditScorePredictorService predictor;
    private final CardRepository cardRepository;
    private final CreditCardRepository creditCardRepository;
    private final AccountService accountService;
    private final SalaryAccountRepository salaryAccountRepository;
    private final CreditCardApplicationDocumentService documentService;

    public CreditCardRequestService(CreditCardRequestRepository repo, CreditScorePredictorService predictor,
                                    CardRepository cardRepository, CreditCardRepository creditCardRepository,
                                    AccountService accountService, SalaryAccountRepository salaryAccountRepository,
                                    CreditCardApplicationDocumentService documentService) {
        this.repo = repo;
        this.predictor = predictor;
        this.cardRepository = cardRepository;
        this.creditCardRepository = creditCardRepository;
        this.accountService = accountService;
        this.salaryAccountRepository = salaryAccountRepository;
        this.documentService = documentService;
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
                .anyMatch(existing -> "Pending".equalsIgnoreCase(existing.getStatus())
                        || "Approved".equalsIgnoreCase(existing.getStatus()))) {
            throw new IllegalStateException("A credit card or open credit card application already exists for this account.");
        }

        request.setPan(pan);
        request.setAnnualIncome(income);
        request.setPredictedCibil(null);
        request.setPanBasedLimit(null);
        request.setSuggestedLimit(null);
        request.setCardType(null);
        request.setEligibility("Under Review");
        request.setInterestRate(null);
        request.setAnnualFee(null);
        request.setStatus("Pending");
        request.setRequestDate(LocalDateTime.now());
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
                .anyMatch(existing -> "Pending".equalsIgnoreCase(existing.getStatus())
                        || "Approved".equalsIgnoreCase(existing.getStatus()))) {
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
        request.setEligibility("Under Review");
        request.setStatus("Pending");
        request.setRequestDate(LocalDateTime.now());

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
    public CreditCardRequest approveRequest(Long id, String adminName, Double approvedLimit) {
        return repo.findById(id).map(req -> {
            if (!"Pending".equalsIgnoreCase(req.getStatus())) {
                throw new IllegalStateException("Only pending credit card applications can be approved.");
            }
            if (approvedLimit == null || !Double.isFinite(approvedLimit)
                    || approvedLimit <= 0 || approvedLimit > 1_000_000_000d) {
                throw new IllegalArgumentException("Enter a valid approved credit limit.");
            }
            if (req.getAccountNumber() == null || req.getAccountNumber().isBlank()) {
                req.setStatus("Approved");
                req.setEligibility("Approved");
                req.setProcessedBy(adminName);
                req.setProcessedDate(LocalDateTime.now());
                req.setApprovedLimit(approvedLimit);
                req.setDocumentsDeleteAfter(LocalDateTime.now().plusDays(90));
                return repo.save(req);
            }
            // Ensure target account is active before issuing credit card
            Account account = accountService.getAccountByNumber(req.getAccountNumber());
            SalaryAccount salaryAccount = account == null ? salaryAccountRepository.findByAccountNumber(req.getAccountNumber()) : null;
            if (account == null && salaryAccount == null) {
                throw new RuntimeException("Cannot approve credit card: account not found: " + req.getAccountNumber());
            }
            String accountStatus = account != null ? account.getStatus() : salaryAccount.getStatus();
            if (!"ACTIVE".equalsIgnoreCase(accountStatus)) {
                throw new RuntimeException("Cannot approve credit card: account is not active: " + req.getAccountNumber() + " | status=" + accountStatus);
            }
            if (!creditCardRepository.findByAccountNumber(req.getAccountNumber()).isEmpty()) {
                throw new IllegalStateException("A credit card is already linked to this account.");
            }
            req.setStatus("Approved");
            req.setEligibility("Approved");
            req.setProcessedBy(adminName);
            req.setProcessedDate(LocalDateTime.now());
            req.setApprovedLimit(approvedLimit);
            req.setDocumentsDeleteAfter(LocalDateTime.now().plusDays(90));
            
            // Generate credit card details
            String cardNumber = generateCardNumber();
            String cvv = String.format("%03d", (int)(Math.random()*1000));
            String expiryMonth = String.format("%02d", (int)(Math.random() * 12) + 1);
            String expiryYear = String.valueOf(LocalDateTime.now().getYear() + 3);
            String expiryDate = expiryMonth + "/" + expiryYear.substring(2);
            
            // Create CreditCard entity
            CreditCard creditCard = new CreditCard();
            creditCard.setCardNumber(cardNumber);
            creditCard.setCvv(cvv);
            creditCard.setExpiryDate(expiryDate);
            creditCard.setAccountNumber(req.getAccountNumber());
            creditCard.setUserName(req.getUserName());
            creditCard.setUserEmail(req.getUserEmail());
            creditCard.setAppliedDate(req.getRequestDate());
            creditCard.setApprovalDate(LocalDateTime.now());
            creditCard.setApprovedLimit(approvedLimit);
            creditCard.setCurrentBalance(0.0);
            creditCard.calculateAvailableLimit();
            creditCard.calculateUsageLimit();
            creditCard.setStatus("Active");
            creditCardRepository.save(creditCard);
            
            // Also create a regular Card entry for compatibility
            Card card = new Card();
            card.setCardNumber(cardNumber);
            card.setCardType("Credit");
            card.setUserName(req.getUserName());
            card.setAccountNumber(req.getAccountNumber());
            card.setUserEmail(req.getUserEmail());
            card.setCvv(cvv);
            card.setStatus("Active");
            cardRepository.save(card);
            
            // Store last 4
            req.setLast4(cardNumber.substring(cardNumber.length()-4));
            return repo.save(req);
        }).orElse(null);
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

    private String generateCardNumber() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 16; i++) sb.append((int)(Math.random()*10));
        return sb.toString();
    }

    public record GuestApplicationUpdate(String userName, String userEmail, String pan, Double annualIncome,
                                         String mobileNumber, String dateOfBirth, String aadhaarLastFour,
                                         String address, String employmentType, String employerName,
                                         String designation) {}
}
