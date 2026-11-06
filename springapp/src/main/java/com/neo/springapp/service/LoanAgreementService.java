package com.neo.springapp.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neo.springapp.model.GoldLoan;
import com.neo.springapp.model.HomeLoan;
import com.neo.springapp.model.Loan;
import com.neo.springapp.model.LoanAgreement;
import com.neo.springapp.repository.GoldLoanRepository;
import com.neo.springapp.repository.HomeLoanRepository;
import com.neo.springapp.repository.LoanAgreementRepository;
import com.neo.springapp.repository.LoanRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class LoanAgreementService {
    private static final double STAMP_DUTY_RATE = 0.005;
    private static final int SHARE_DAYS = 7;
    private static final int CAPTCHA_MAX_ATTEMPTS = 5;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final PDType1Font BODY_FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDType1Font BOLD_FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final List<String> CAPTCHA_ALPHABET = List.of(
            "A", "B", "C", "D", "E", "F", "G", "H", "J", "K", "M", "N", "P", "R", "T", "U", "V", "W", "X", "Y", "2", "3", "4", "5", "6", "7", "8", "9");

    private final LoanAgreementRepository agreementRepository;
    private final LoanRepository loanRepository;
    private final HomeLoanRepository homeLoanRepository;
    private final GoldLoanRepository goldLoanRepository;
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final EmailService emailService;
    private final String frontendUrl;

    public LoanAgreementService(
            LoanAgreementRepository agreementRepository,
            LoanRepository loanRepository,
            HomeLoanRepository homeLoanRepository,
            GoldLoanRepository goldLoanRepository,
            ObjectMapper objectMapper,
            SimpMessagingTemplate messagingTemplate,
            EmailService emailService,
            @Value("${app.frontend-url:http://localhost:4200}") String frontendUrl) {
        this.agreementRepository = agreementRepository;
        this.loanRepository = loanRepository;
        this.homeLoanRepository = homeLoanRepository;
        this.goldLoanRepository = goldLoanRepository;
        this.objectMapper = objectMapper;
        this.messagingTemplate = messagingTemplate;
        this.emailService = emailService;
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
    }

    @Transactional
    public Map<String, Object> issue(String loanType, Long loanId, Map<String, Object> approvalContext) throws IOException {
        String normalizedType = normalizeType(loanType);
        String token = newToken();
        LoanAgreement agreement = new LoanAgreement();
        agreement.setLoanType(normalizedType);
        agreement.setLoanId(loanId);
        agreement.setStatus("PENDING_SIGNATURE");
        agreement.setShareTokenHash(sha256(token));
        agreement.setIssuedAt(LocalDateTime.now());
        agreement.setExpiresAt(LocalDateTime.now().plusDays(SHARE_DAYS));
        agreement.setCaptchaAttempts(0);
        Map<String, Object> context = new HashMap<>(approvalContext == null ? Map.of() : approvalContext);
        populateLoanSnapshot(agreement, context);
        agreement.setApprovalContext(writeContext(context));

        for (LoanAgreement previous : agreementRepository
                .findAllByLoanTypeAndLoanIdOrderByIssuedAtDesc(normalizedType, loanId)) {
            if ("PENDING_SIGNATURE".equals(previous.getStatus())) {
                previous.setStatus("SUPERSEDED");
                previous.setCaptchaAnswerHash(null);
                previous.setCaptchaChallengeId(null);
                agreementRepository.save(previous);
            }
        }

        agreement.setPdfBytes(generatePdf(agreement));
        LoanAgreement saved = agreementRepository.save(agreement);
        publish(saved, "PENDING_SIGNATURE");
        Map<String, Object> response = issueResponse(saved, token);
        addEmailStatus(saved, response);
        return response;
    }

    @Transactional
    public Map<String, Object> emailShareLink(Long agreementId) {
        Map<String, Object> response = renewShareLink(agreementId);
        LoanAgreement agreement = getAgreement(agreementId);
        addEmailStatus(agreement, response);
        return response;
    }

    @Transactional
    public Map<String, Object> renewShareLink(Long agreementId) {
        LoanAgreement agreement = getAgreement(agreementId);
        if (!"PENDING_SIGNATURE".equals(agreement.getStatus())) {
            throw new IllegalStateException("A share link can only be renewed while signature is pending.");
        }
        String token = newToken();
        agreement.setShareTokenHash(sha256(token));
        agreement.setExpiresAt(LocalDateTime.now().plusDays(SHARE_DAYS));
        agreement.setCaptchaAnswerHash(null);
        agreement.setCaptchaChallengeId(null);
        agreement.setCaptchaExpiresAt(null);
        agreement.setCaptchaAttempts(0);
        agreementRepository.save(agreement);
        publish(agreement, "PENDING_SIGNATURE");
        return issueResponse(agreement, token);
    }

    @Transactional
    public Map<String, Object> createCaptcha(String token) throws IOException {
        LoanAgreement agreement = findByToken(token);
        ensureShareCanBeSigned(agreement);
        if (agreement.getCaptchaAttempts() != null && agreement.getCaptchaAttempts() >= CAPTCHA_MAX_ATTEMPTS) {
            throw new IllegalStateException("CAPTCHA attempt limit reached. Ask the bank to issue a fresh share link.");
        }
        String answer = randomCaptchaText();
        String challengeId = UUID.randomUUID().toString();
        agreement.setCaptchaChallengeId(challengeId);
        agreement.setCaptchaAnswerHash(sha256(answer));
        agreement.setCaptchaExpiresAt(LocalDateTime.now().plusMinutes(5));
        agreementRepository.save(agreement);
        Map<String, Object> result = new HashMap<>();
        result.put("challengeId", challengeId);
        result.put("image", Base64.getEncoder().encodeToString(renderCaptcha(answer)));
        result.put("expiresAt", agreement.getCaptchaExpiresAt());
        return result;
    }

    @Transactional
    public boolean sign(String token, String challengeId, String captchaAnswer, String signedName,
                        boolean consent, String remoteAddress) throws IOException {
        LoanAgreement agreement = findByToken(token);
        ensureShareCanBeSigned(agreement);
        if (signedName == null || signedName.isBlank() || signedName.length() > 120) {
            throw new IllegalArgumentException("Enter your full name (up to 120 characters) to sign.");
        }
        if (!consent) {
            throw new IllegalArgumentException("Confirm that you have reviewed the agreement before signing.");
        }
        if (agreement.getCaptchaAttempts() == null) agreement.setCaptchaAttempts(0);
        boolean challengeValid = challengeId != null
                && challengeId.equals(agreement.getCaptchaChallengeId())
                && agreement.getCaptchaExpiresAt() != null
                && agreement.getCaptchaExpiresAt().isAfter(LocalDateTime.now());
        boolean answerValid = captchaAnswer != null
                && agreement.getCaptchaAnswerHash() != null
                && MessageDigest.isEqual(
                        sha256(captchaAnswer.trim().toUpperCase()).getBytes(StandardCharsets.US_ASCII),
                        agreement.getCaptchaAnswerHash().getBytes(StandardCharsets.US_ASCII));
        if (!challengeValid || !answerValid) {
            agreement.setCaptchaAttempts(agreement.getCaptchaAttempts() + 1);
            agreement.setCaptchaChallengeId(null);
            agreement.setCaptchaAnswerHash(null);
            agreementRepository.save(agreement);
            return false;
        }

        agreement.setSignedName(signedName.trim());
        agreement.setSignedAt(LocalDateTime.now());
        agreement.setStatus("SIGNED");
        agreement.setSignedDocumentSha256(sha256(String.join("|", agreement.getId().toString(),
                agreement.getLoanType(), agreement.getLoanId().toString(), signedName.trim(),
                agreement.getSignedAt().toString(), remoteAddress == null ? "" : remoteAddress)));
        agreement.setCaptchaChallengeId(null);
        agreement.setCaptchaAnswerHash(null);
        agreement.setCaptchaExpiresAt(null);
        agreement.setPdfBytes(generatePdf(agreement));
        agreementRepository.save(agreement);
        publish(agreement, "SIGNED");
        return true;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> shareDetails(String token) {
        LoanAgreement agreement = findByToken(token);
        ensureShareIsAvailable(agreement);
        Map<String, Object> result = new HashMap<>();
        result.put("id", agreement.getId());
        result.put("loanType", agreement.getLoanType());
        result.put("productName", agreement.getProductName());
        result.put("loanAccountNumber", agreement.getLoanAccountNumber());
        result.put("customerName", agreement.getCustomerName());
        result.put("purpose", agreement.getPurpose());
        result.put("amount", agreement.getAmount());
        result.put("tenureMonths", agreement.getTenureMonths());
        result.put("annualInterestRate", agreement.getAnnualInterestRate());
        result.put("stampDuty", agreement.getStampDuty());
        result.put("status", agreement.getStatus());
        result.put("issuedAt", agreement.getIssuedAt());
        result.put("expiresAt", agreement.getExpiresAt());
        result.put("minimumPages", 10);
        result.put("BindingNotice", " DOCUMENT - A LEGAL OR EXECUTABLE BANK AGREEMENT.");
        return result;
    }

    @Transactional(readOnly = true)
    public byte[] sharePdf(String token) {
        LoanAgreement agreement = findByToken(token);
        ensureShareIsAvailable(agreement);
        return agreement.getPdfBytes();
    }

    @Transactional(readOnly = true)
    public byte[] adminPdf(Long agreementId) {
        LoanAgreement agreement = getAgreement(agreementId);
        if (!List.of("SIGNED", "APPROVED").contains(agreement.getStatus())) {
            throw new IllegalStateException("The customer must sign the agreement before staff can download it.");
        }
        return agreement.getPdfBytes();
    }

    @Transactional(readOnly = true)
    public LoanAgreement requireSigned(Long agreementId, String loanType, Long loanId) {
        LoanAgreement agreement = getAgreement(agreementId);
        if (!normalizeType(loanType).equals(agreement.getLoanType()) || !loanId.equals(agreement.getLoanId())) {
            throw new IllegalArgumentException("The signed agreement does not match this loan.");
        }
        if (!"SIGNED".equals(agreement.getStatus())) {
            throw new IllegalStateException("Customer signature is required before final approval.");
        }
        return agreement;
    }

    @Transactional
    public void markApproved(Long agreementId, String adminName) {
        LoanAgreement agreement = getAgreement(agreementId);
        if (!"SIGNED".equals(agreement.getStatus())) {
            throw new IllegalStateException("Customer signature is required before final approval.");
        }
        agreement.setApprovedBy(adminName);
        agreement.setApprovedAt(LocalDateTime.now());
        agreement.setStatus("APPROVED");
        agreementRepository.save(agreement);
        publish(agreement, "APPROVED");
    }

    @Transactional(readOnly = true)
    public Map<String, Object> approvalContext(Long agreementId) {
        LoanAgreement agreement = getAgreement(agreementId);
        try {
            return objectMapper.readValue(agreement.getApprovalContext(), new TypeReference<>() {});
        } catch (IOException e) {
            throw new IllegalStateException("Stored loan approval details could not be read.", e);
        }
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> adminSummaries() {
        return agreementRepository.findAllByOrderByIssuedAtDesc().stream().map(this::adminSummary).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> adminSummariesForAccount(String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalArgumentException("Account number is required.");
        }
        return agreementRepository.findAllByAccountNumberIgnoreCaseOrderByIssuedAtDesc(accountNumber.trim())
                .stream().map(this::adminSummary).toList();
    }

    private Map<String, Object> adminSummary(LoanAgreement agreement) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("id", agreement.getId());
        summary.put("loanType", agreement.getLoanType());
        summary.put("loanId", agreement.getLoanId());
        summary.put("loanAccountNumber", agreement.getLoanAccountNumber());
        summary.put("accountNumber", agreement.getAccountNumber());
        summary.put("productName", agreement.getProductName());
        summary.put("customerName", agreement.getCustomerName());
        summary.put("amount", agreement.getAmount());
        summary.put("tenureMonths", agreement.getTenureMonths());
        summary.put("annualInterestRate", agreement.getAnnualInterestRate());
        summary.put("status", agreement.getStatus());
        summary.put("issuedAt", agreement.getIssuedAt());
        summary.put("expiresAt", agreement.getExpiresAt());
        summary.put("signedAt", agreement.getSignedAt());
        summary.put("signedName", agreement.getSignedName());
        return summary;
    }

    private void populateLoanSnapshot(LoanAgreement agreement, Map<String, Object> context) {
        switch (agreement.getLoanType()) {
            case "LOAN" -> {
                Loan loan = loanRepository.findById(agreement.getLoanId())
                        .orElseThrow(() -> new IllegalArgumentException("Loan application not found."));
                agreement.setProductName(loan.getType());
                agreement.setLoanAccountNumber(loan.getLoanAccountNumber());
                agreement.setCustomerName(loan.getUserName());
                agreement.setCustomerEmail(loan.getUserEmail());
                agreement.setAccountNumber(loan.getAccountNumber());
                agreement.setPurpose(loan.getPurpose());
                agreement.setAmount(requirePositive(loan.getAmount(), "Loan amount"));
                agreement.setTenureMonths(requireTenure(loan.getTenure()));
                agreement.setAnnualInterestRate(nonNegative(loan.getInterestRate()));
                context.put("productDetails", loan.getType());
            }
            case "HOME_LOAN" -> {
                HomeLoan loan = homeLoanRepository.findById(agreement.getLoanId())
                        .orElseThrow(() -> new IllegalArgumentException("Home, car, or crop loan application not found."));
                agreement.setProductName(switch (loan.getLoanType() == null ? "HOME" : loan.getLoanType().toUpperCase()) {
                    case "CAR" -> "Car Loan";
                    case "FARMER" -> "Farmer Loan";
                    case "CROP" -> "Crop Loan";
                    default -> "Home Loan";
                });
                agreement.setLoanAccountNumber(loan.getLoanAccountNumber());
                agreement.setCustomerName(loan.getUserName());
                agreement.setCustomerEmail(loan.getUserEmail());
                agreement.setAccountNumber(loan.getAccountNumber());
                agreement.setPurpose(loan.getPurpose());
                agreement.setAmount(requirePositive(loan.getAmount(), "Loan amount"));
                agreement.setTenureMonths(requireTenure(loan.getTenure()));
                agreement.setAnnualInterestRate(nonNegative(loan.getInterestRate()));
                context.put("productDetails", productDetails(
                        "Property address", loan.getPropertyAddress(),
                        "Property value", loan.getPropertyValue(),
                        "Vehicle", loan.getVehicleMakeModel(),
                        "Vehicle year", loan.getVehicleYear(),
                        "Vehicle price", loan.getVehiclePrice(),
                        "Dealer", loan.getVehicleDealer(),
                        "Farm location", loan.getFarmLocation(),
                        "Crop", loan.getCropName(),
                        "Crop season", loan.getCropSeason(),
                        "Land area", loan.getLandArea(),
                        "Area unit", loan.getLandAreaUnit(),
                        "Cultivation estimate", loan.getCultivationCostEstimate()));
            }
            case "GOLD_LOAN" -> {
                GoldLoan loan = goldLoanRepository.findById(agreement.getLoanId())
                        .orElseThrow(() -> new IllegalArgumentException("Gold loan application not found."));
                agreement.setProductName("Gold Loan");
                agreement.setLoanAccountNumber(loan.getLoanAccountNumber());
                agreement.setCustomerName(loan.getUserName());
                agreement.setCustomerEmail(loan.getUserEmail());
                agreement.setAccountNumber(loan.getAccountNumber());
                agreement.setPurpose(loan.getPurpose());
                double requestedAmount = loan.getLoanAmount() == null ? 0 : loan.getLoanAmount();
                if (context.get("verifiedGoldGrams") != null
                        && loan.getGoldRatePerGram() != null) {
                    try {
                        requestedAmount = Double.parseDouble(context.get("verifiedGoldGrams").toString())
                                * loan.getGoldRatePerGram() * 0.75;
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Verified gold weight must be numeric.", e);
                    }
                }
                agreement.setAmount(requirePositive(requestedAmount, "Verified gold loan amount"));
                agreement.setTenureMonths(requireTenure(loan.getTenure()));
                agreement.setAnnualInterestRate(nonNegative(loan.getInterestRate() == null ? 9.0 : loan.getInterestRate()));
                context.put("productDetails", productDetails(
                        "Gold items", context.get("goldItems"),
                        "Description", context.get("goldDescription"),
                        "Verified purity", context.get("goldPurity"),
                        "Verified weight (grams)", context.get("verifiedGoldGrams"),
                        "Gold value (INR)", context.get("verifiedGoldGrams") != null
                                && loan.getGoldRatePerGram() != null
                                ? Double.parseDouble(context.get("verifiedGoldGrams").toString()) * loan.getGoldRatePerGram()
                                : loan.getVerifiedGoldValue() == null ? loan.getGoldValue() : loan.getVerifiedGoldValue()));
            }
            default -> throw new IllegalArgumentException("Unsupported loan product.");
        }
        agreement.setProductDetails(context.get("productDetails") == null
                ? null : context.get("productDetails").toString());
        agreement.setStampDuty(Math.round(agreement.getAmount() * STAMP_DUTY_RATE * 100.0) / 100.0);
    }

    private Map<String, Object> issueResponse(LoanAgreement agreement, String token) {
        Map<String, Object> result = new HashMap<>();
        result.put("agreementId", agreement.getId());
        result.put("status", agreement.getStatus());
        result.put("shareUrl", frontendUrl + "/loan-esign/" + token);
        result.put("expiresAt", agreement.getExpiresAt());
        result.put("stampDuty", agreement.getStampDuty());
        result.put("pdfPages", 10);
        result.put("message", "Agreement issued. Copy and share the link with the customer.");
        return result;
    }

    private void addEmailStatus(LoanAgreement agreement, Map<String, Object> response) {
        String shareUrl = response.get("shareUrl").toString();
        boolean emailSent = agreement.getCustomerEmail() != null
                && emailService.sendLoanAgreementLinkEmail(
                        agreement.getCustomerEmail(),
                        agreement.getCustomerName(),
                        agreement.getProductName(),
                        shareUrl,
                        agreement.getExpiresAt() == null ? null : agreement.getExpiresAt().toString());
        response.put("emailSent", emailSent);
        if (emailSent) {
            response.put("emailMessage", "Agreement link emailed to the customer's registered email address.");
        } else if (agreement.getCustomerEmail() == null || agreement.getCustomerEmail().isBlank()) {
            response.put("emailMessage", "No registered customer email is available. Copy and share the link manually.");
        } else {
            response.put("emailMessage", "Email delivery failed. The link was generated; copy and share it manually or retry.");
        }
    }

    private LoanAgreement getAgreement(Long id) {
        return agreementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Loan agreement not found."));
    }

    private LoanAgreement findByToken(String token) {
        if (token == null || token.length() < 32 || token.length() > 128) {
            throw new IllegalArgumentException("Invalid or expired share link.");
        }
        return agreementRepository.findByShareTokenHash(sha256(token))
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired share link."));
    }

    private void ensureShareCanBeSigned(LoanAgreement agreement) {
        ensureShareIsAvailable(agreement);
        if (!"PENDING_SIGNATURE".equals(agreement.getStatus())) {
            throw new IllegalStateException("This agreement is no longer available for signing.");
        }
    }

    private void ensureShareIsAvailable(LoanAgreement agreement) {
        if (!List.of("PENDING_SIGNATURE", "SIGNED", "APPROVED").contains(agreement.getStatus())) {
            throw new IllegalStateException("This agreement is no longer available.");
        }
        if (agreement.getExpiresAt() == null || agreement.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("This share link has expired. Ask the bank to issue a new link.");
        }
    }

    private String normalizeType(String type) {
        if (type == null) throw new IllegalArgumentException("Loan type is required.");
        return switch (type.trim().toUpperCase().replace('-', '_').replace(' ', '_')) {
            case "LOAN", "PERSONAL_LOAN", "CAR_LOAN", "CROP_LOAN" -> "LOAN";
            case "HOME", "HOME_LOAN", "CAR", "FARMER", "CROP" -> "HOME_LOAN";
            case "GOLD", "GOLD_LOAN" -> "GOLD_LOAN";
            default -> throw new IllegalArgumentException("Unsupported loan product.");
        };
    }

    private double requirePositive(Double value, String label) {
        if (value == null || !Double.isFinite(value) || value <= 0) {
            throw new IllegalArgumentException(label + " must be greater than zero.");
        }
        return value;
    }

    private int requireTenure(Integer value) {
        if (value == null || value < 1 || value > 600) {
            throw new IllegalArgumentException("Loan tenure must be between 1 and 600 months.");
        }
        return value;
    }

    private double nonNegative(Double value) {
        if (value == null) return 0;
        if (!Double.isFinite(value) || value < 0 || value > 100) {
            throw new IllegalArgumentException("Interest rate must be between 0 and 100 percent.");
        }
        return value;
    }

    private String writeContext(Map<String, Object> context) {
        try {
            return objectMapper.writeValueAsString(context == null ? Map.of() : context);
        } catch (IOException e) {
            throw new IllegalArgumentException("Loan approval details could not be stored.", e);
        }
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String randomCaptchaText() {
        StringBuilder text = new StringBuilder(6);
        for (int i = 0; i < 6; i++) text.append(CAPTCHA_ALPHABET.get(RANDOM.nextInt(CAPTCHA_ALPHABET.size())));
        return text.toString();
    }

    private byte[] renderCaptcha(String answer) throws IOException {
        BufferedImage image = new BufferedImage(220, 72, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(new Color(244, 249, 255));
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.setColor(new Color(29, 78, 118));
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 32));
        graphics.drawString(answer, 24, 48);
        for (int i = 0; i < 8; i++) {
            graphics.setColor(new Color(80, 120, 160, 100));
            graphics.drawLine(RANDOM.nextInt(220), RANDOM.nextInt(72), RANDOM.nextInt(220), RANDOM.nextInt(72));
        }
        graphics.dispose();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }

    private byte[] generatePdf(LoanAgreement agreement) throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            List<List<String>> fixedPages = fixedPages(agreement);
            int pageNumber = 0;
            for (List<String> pageLines : fixedPages) {
                pageNumber++;
                addPdfPage(document, agreement, pageLines, pageNumber, fixedPages.size());
            }
            List<List<String>> schedulePages = schedulePages(agreement);
            for (int i = 0; i < schedulePages.size(); i++) {
                pageNumber++;
                List<String> pageLines = new ArrayList<>();
                pageLines.add("FULL REPAYMENT / TENURE SCHEDULE (CONTINUED)");
                pageLines.addAll(schedulePages.get(i));
                addPdfPage(document, agreement, pageLines, pageNumber, fixedPages.size() + schedulePages.size());
            }
            document.save(output);
            return output.toByteArray();
        }
    }

    private List<List<String>> fixedPages(LoanAgreement agreement) {
        String product = safe(agreement.getProductName());
        String emi = String.format("%.2f", calculateEmi(agreement));
        String stamp = String.format("%.2f", agreement.getStampDuty());
        String amount = String.format("%.2f", agreement.getAmount());
        String rate = String.format("%.2f", agreement.getAnnualInterestRate());
        String tenure = String.valueOf(agreement.getTenureMonths());
        String name = safe(agreement.getCustomerName());
        String purpose = safe(agreement.getPurpose());
        List<List<String>> pages = new ArrayList<>();
        pages.add(List.of(
                " DOCUMENT - TERMS ONLY -  A LEGAL AGREEMENT",
                "NEOBANK | LOAN SANCTION AND E-SIGN PACKET",
                "Product: " + product,
                "Customer: " + name,
                "Loan account: " + safe(agreement.getLoanAccountNumber()),
                "Principal: INR " + amount,
                "Tenure: " + tenure + " months | Annual rate: " + rate + "%",
                "Stamp duty estimate: INR " + stamp + " (0.5% of principal; subject to local law)",
                "This sample is generated for workflow demonstration only. It is not a sanctioned contract,",
                "proof of stamp payment, legal advice, or an instruction to disburse funds.",
                "A bank-approved document and legally compliant electronic-signature provider are required",
                "before this document can be used as an enforceable financial instrument."
        ));
        pages.add(List.of(
                "1. PARTIES, LOAN IDENTIFICATION AND SUMMARY",
                "Bank: NeoBank ( branding only;  official seal is represented in this PDF).",
                "Borrower: " + name,
                "Account: " + mask(agreement.getAccountNumber()),
                "Product: " + product + " | Loan reference: " + safe(agreement.getLoanAccountNumber()),
                "Requested purpose: " + purpose,
                "Principal considered for this : INR " + amount + ".",
                "Any final sanction amount, fees, dates, rate, conditions, and borrower identity must be",
                "verified against the lender's approved offer before an actual contract is issued."
        ));
        pages.add(List.of(
                "2. SANCTIONED AMOUNT AND INTEREST",
                "Illustrative principal: INR " + amount + ".",
                "Illustrative annual nominal interest rate: " + rate + "%.",
                "Illustrative term: " + tenure + " monthly instalments.",
                "Illustrative monthly instalment (reducing-balance estimate): INR " + emi + ".",
                "The rate type, repricing rules, compounding basis, dates, payment allocation, taxes, and",
                "actual repayment obligations must be set by the approved product schedule.",
                "No amount in this  constitutes an offer or approval by the bank."
        ));
        pages.add(List.of(
                "3. REPAYMENT AND TENURE SCHEDULE",
                "A full illustrative amortization schedule follows this section in the appended pages.",
                "Instalments are estimated monthly using the principal, annual rate, and tenure displayed",
                "on page 2. First due date is illustrative and must be confirmed by the lender.",
                "Actual payment dates, rounding, grace periods, holidays, payment allocation, and final",
                "instalment may differ under the product's approved terms.",
                "Customers should use only the repayment schedule issued with the final bank agreement."
        ));
        pages.add(List.of(
                "4. STAMP DUTY, TAXES AND FEES",
                "Illustrative stamp-duty rate supplied by the requester: 0.5% of principal.",
                "Illustrative stamp-duty amount: INR " + stamp + ".",
                "This figure is not a legal determination, collection receipt, or evidence that stamp duty",
                "has been paid. Actual duty, instrument classification, exemptions, payer, and payment",
                "method depend on jurisdiction and must be verified by authorized bank/legal staff.",
                "Processing, documentation, insurance, valuation, registration, and other fees are not",
                "included unless expressly listed in a separately approved offer."
        ));
        pages.add(List.of(
                "5. PURPOSE, SECURITY AND COLLATERAL",
                "Stated purpose: " + purpose + ".",
                "Product-specific details: " + safe(agreement.getProductDetails()) + ".",
                "Security, guarantee, lien, mortgage, pledge, hypothecation, or collateral requirements",
                "must be described in the final product-specific agreement and supporting schedules.",
                "For a gold loan, the verified items, purity, weight, valuation, custody, and release process",
                "must be recorded by the bank. For property, vehicle, or crop lending, asset and title",
                "records must be independently verified before any security is accepted."
        ));
        pages.add(List.of(
                "6. BORROWER INFORMATION AND ONGOING OBLIGATIONS",
                "The borrower must provide accurate application and identity information, promptly report",
                "material changes, protect account credentials, and use proceeds only for the stated purpose.",
                "The lender must explain the final repayment amount, annual percentage cost, fees, and",
                "conditions before execution. No blank or incomplete material term should be signed.",
                "The bank must retain supporting records and provide a copy of the executed agreement."
        ));
        pages.add(List.of(
                "7. PAYMENTS, DELAYS AND CUSTOMER SUPPORT",
                "Payments should be made through lender-approved channels and credited against the",
                "borrower's account with a dated receipt. Late fees, penal interest, grace periods, and",
                "payment allocation must be specified in the final approved schedule and comply with law.",
                "Customers should contact the bank promptly if an instalment is disputed or unaffordable.",
                "No recovery agent should request payment to a personal account or use coercion."
        ));
        pages.add(List.of(
                "8. DEFAULT, NOTICES AND RECOVERY",
                "The executed contract must identify events of default, notice periods, cure rights, and",
                "the lawful recovery process. Before enforcement, the bank must issue required notices,",
                "consider applicable consumer protections, and keep an auditable record of communications.",
                "Recovery must be proportionate, respectful, and performed only by authorized personnel.",
                "Collateral may be enforced only under the signed terms and applicable law; this sample",
                "does not grant a right to seize, sell, or otherwise dispose of any customer property."
        ));
        pages.add(List.of(
                "9. PREPAYMENT, CLOSURE, COMPLAINTS AND PRIVACY",
                "Prepayment, foreclosure, release of security, and any associated charges must be stated",
                "in the final product terms and follow applicable law and regulatory directions.",
                "Borrowers may use the bank's published grievance process and escalate unresolved matters",
                "to the appropriate regulator or dispute-resolution body.",
                "Personal data should be used only for lawful lending, servicing, compliance, and audit",
                "purposes, with access limited and retention handled under the bank's approved policy."
        ));
        List<String> signaturePage = new ArrayList<>(List.of(
                "10. CUSTOMER REVIEW AND SIGNATURE RECORD",
                "Review every page, the complete repayment schedule, charges, and all product conditions.",
                "The customer must receive a final, bank-approved agreement before any real-world signing.",
                "Signature type: typed-name confirmation protected by a CAPTCHA.",
                "This application does  provide a qualified or regulated electronic-signature service."
        ));
        if (agreement.getSignedAt() != null) {
            signaturePage.add("Customer typed name: " + safe(agreement.getSignedName()));
            signaturePage.add("Signed at: " + agreement.getSignedAt());
            signaturePage.add("Signature audit fingerprint: " + safe(agreement.getSignedDocumentSha256()));
        } else {
            signaturePage.add("Customer signature: __________________________________________");
            signaturePage.add("Date: ____________________");
        }
        signaturePage.add("Bank officer: __________________________  Date: ____________________");
        signaturePage.add("All terms in this  are binding placeholders requiring legal approval.");
        pages.add(signaturePage);
        return pages;
    }

    private List<List<String>> schedulePages(LoanAgreement agreement) {
        List<List<String>> pages = new ArrayList<>();
        List<String> current = new ArrayList<>();
        current.add("No.  | Due date |  Opening principal |  Interest |  Principal |  Instalment |  Closing");
        current.add("--------------------------------------------------------------------------------");
        double opening = agreement.getAmount();
        double monthlyRate = agreement.getAnnualInterestRate() / 1200.0;
        double emi = calculateEmi(agreement);
        LocalDate dueDate = LocalDate.now().plusMonths(1);
        for (int i = 1; i <= agreement.getTenureMonths(); i++) {
            double interest = opening * monthlyRate;
            double principal = Math.min(opening, Math.max(0, emi - interest));
            double installment = i == agreement.getTenureMonths() ? principal + interest : emi;
            double closing = Math.max(0, opening - principal);
            current.add(String.format("%d | %s | %.2f | %.2f | %.2f | %.2f | %.2f",
                    i, dueDate.toString(), opening, interest, principal, installment, closing));
            opening = closing;
            dueDate = dueDate.plusMonths(1);
            if (current.size() >= 26) {
                pages.add(current);
                current = new ArrayList<>();
            }
        }
        if (!current.isEmpty() || pages.isEmpty()) pages.add(current);
        return pages;
    }

    private void addPdfPage(PDDocument document, LoanAgreement agreement, List<String> lines,
                            int pageNumber, int totalPages) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
            float width = page.getMediaBox().getWidth();
            stream.setNonStrokingColor(new Color(21, 94, 117));
            stream.beginText();
            stream.setFont(BOLD_FONT, 17);
            stream.newLineAtOffset(52, 790);
            stream.showText("NEOBANK");
            stream.endText();

            stream.setNonStrokingColor(new Color(153, 27, 27));
            stream.beginText();
            stream.setFont(BOLD_FONT, 8);
            stream.newLineAtOffset(52, 772);
            stream.showText(" WATERMARK - A LEGAL AGREEMENT");
            stream.endText();

            stream.setStrokingColor(new Color(153, 27, 27));
            stream.addRect(width - 150, 755, 98, 34);
            stream.stroke();
            stream.setNonStrokingColor(new Color(153, 27, 27));
            stream.beginText();
            stream.setFont(BOLD_FONT, 8);
            stream.newLineAtOffset(width - 141, 776);
            stream.showText("STAMP");
            stream.newLineAtOffset(0, -11);
            stream.showText("OFFICIAL");
            stream.endText();

            stream.setNonStrokingColor(new Color(230, 236, 240));
            stream.beginText();
            stream.setFont(BOLD_FONT, 30);
            stream.setTextMatrix(org.apache.pdfbox.util.Matrix.getRotateInstance(
                    Math.toRadians(32), width / 4, 385));
            stream.showText("NEOBANK ");
            stream.endText();

            float y = 735;
            for (String line : lines) {
                boolean heading = line.matches("^\\d+\\..*") || line.startsWith("DOCUMENT")
                        || line.startsWith("NEOBANK |") || line.startsWith("FULL REPAYMENT");
                for (String wrapped : wrap(line, 92)) {
                    stream.setNonStrokingColor(heading ? new Color(21, 94, 117) : Color.DARK_GRAY);
                    stream.beginText();
                    stream.setFont(heading ? BOLD_FONT : BODY_FONT, heading ? 11 : 9);
                    stream.newLineAtOffset(52, y);
                    stream.showText(ascii(wrapped));
                    stream.endText();
                    y -= heading ? 21 : 15;
                    heading = false;
                }
                y -= 3;
            }

            stream.setStrokingColor(new Color(21, 94, 117));
            stream.moveTo(52, 40);
            stream.lineTo(width - 52, 40);
            stream.stroke();
            stream.setNonStrokingColor(new Color(80, 90, 100));
            stream.beginText();
            stream.setFont(BODY_FONT, 8);
            stream.newLineAtOffset(52, 25);
            stream.showText("NEOBANK  |  only -  A Stamped or enforceable contract | Page "
                    + pageNumber + " of " + totalPages);
            stream.endText();
        }
    }

    private List<String> wrap(String text, int maxLength) {
        List<String> wrapped = new ArrayList<>();
        if (text == null || text.isBlank()) return List.of("");
        StringBuilder line = new StringBuilder();
        for (String word : text.split("\\s+")) {
            if (line.length() > 0 && line.length() + word.length() + 1 > maxLength) {
                wrapped.add(line.toString());
                line.setLength(0);
            }
            if (line.length() > 0) line.append(' ');
            line.append(word);
        }
        if (line.length() > 0) wrapped.add(line.toString());
        return wrapped;
    }

    private double calculateEmi(LoanAgreement agreement) {
        double amount = agreement.getAmount();
        double monthlyRate = agreement.getAnnualInterestRate() / 1200.0;
        int months = agreement.getTenureMonths();
        if (monthlyRate == 0) return amount / months;
        double factor = Math.pow(1 + monthlyRate, months);
        return amount * monthlyRate * factor / (factor - 1);
    }

    private String mask(String value) {
        if (value == null || value.length() < 4) return "Not provided";
        return "****" + value.substring(value.length() - 4);
    }

    private String productDetails(Object... values) {
        StringBuilder details = new StringBuilder();
        for (int i = 0; i + 1 < values.length; i += 2) {
            Object value = values[i + 1];
            if (value == null || value.toString().isBlank()) continue;
            if (details.length() > 0) details.append("; ");
            details.append(values[i]).append(": ").append(value);
        }
        return details.length() == 0 ? "Not provided" : details.toString();
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "Not provided" : ascii(value);
    }

    private String ascii(String value) {
        return value.replaceAll("[^\\x20-\\x7E]", "?");
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(64);
            for (byte b : digest) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available.", e);
        }
    }

    private void publish(LoanAgreement agreement, String status) {
        Map<String, Object> event = new HashMap<>();
        event.put("agreementId", agreement.getId());
        event.put("loanType", agreement.getLoanType());
        event.put("loanId", agreement.getLoanId());
        event.put("status", status);
        event.put("signedAt", agreement.getSignedAt());
        event.put("approvedAt", agreement.getApprovedAt());
        messagingTemplate.convertAndSend("/topic/loan-agreements", event);
    }
}
