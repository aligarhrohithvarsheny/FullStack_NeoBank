package com.neo.springapp.service;

import com.neo.springapp.model.Account;
import com.neo.springapp.model.Card;
import com.neo.springapp.model.CreditCard;
import com.neo.springapp.model.CreditCardNumberHistory;
import com.neo.springapp.model.CreditCardRequest;
import com.neo.springapp.repository.CardRepository;
import com.neo.springapp.repository.CreditCardRepository;
import com.neo.springapp.repository.CreditCardNumberHistoryRepository;
import com.neo.springapp.repository.CreditCardRequestRepository;
import com.neo.springapp.repository.SalaryAccountRepository;
import com.neo.springapp.repository.VirtualCardRepository;
import com.neo.springapp.service.CreditCardApplicationDocumentService;
import com.neo.springapp.service.EmailService;
import org.springframework.mock.web.MockMultipartFile;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CreditCardRequestServiceTest {

    @Test
    void storesApplicationDataWithSimulatedCreditScore() {
        CreditCardRequestRepository requestRepository = mock(CreditCardRequestRepository.class);
        CreditCardRepository creditCardRepository = mock(CreditCardRepository.class);
        CreditCardRequestService service = createService(requestRepository, creditCardRepository);
        CreditCardRequest request = application("ABCDE1234F");
        when(requestRepository.save(any(CreditCardRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CreditCardRequest saved = service.saveRequest(request, 84000d);

        assertEquals("Pending", saved.getStatus());
        assertEquals("Under Review", saved.getEligibility());
        assertEquals("ABCDE1234F", saved.getPan());
        assertEquals(84000d, saved.getAnnualIncome());
        assertNull(saved.getApprovedLimit());
        assertEquals(783, saved.getPredictedCibil());
        assertNotNull(saved.getSuggestedLimit());
        assertNotNull(saved.getPanBasedLimit());
    }

    @Test
    void rejectsScoresBelowSevenHundredFiftyWithAnAdminVisibleReason() {
        CreditCardRequestRepository requestRepository = mock(CreditCardRequestRepository.class);
        when(requestRepository.findByAccountNumberOrderByRequestDateDesc("ACC123")).thenReturn(List.of());
        when(requestRepository.save(any(CreditCardRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        CreditCardRequest request = application("ABCDE0000F");
        request.setAnnualIncome(84000d);

        CreditCardRequest saved = createService(requestRepository, mock(CreditCardRepository.class))
                .saveRequest(request, 84000d);

        assertEquals(749, saved.getPredictedCibil());
        assertEquals("Rejected", saved.getStatus());
        assertTrue(saved.getRejectionReason().contains("minimum score"));
        assertTrue(saved.getRejectionReason().contains("No credit-bureau lookup"));
        assertNotNull(saved.getDocumentsDeleteAfter());
    }

    @Test
    void acceptsTheExactSevenHundredFiftyScoreThresholdForAdminReview() {
        CreditCardRequestRepository requestRepository = mock(CreditCardRequestRepository.class);
        when(requestRepository.findByAccountNumberOrderByRequestDateDesc("ACC123")).thenReturn(List.of());
        when(requestRepository.save(any(CreditCardRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        CreditCardRequest request = application("ABCDE0001F");

        CreditCardRequest saved = createService(requestRepository, mock(CreditCardRepository.class))
                .saveRequest(request, 84000d);

        assertEquals(750, saved.getPredictedCibil());
        assertEquals("Pending", saved.getStatus());
        assertNull(saved.getRejectionReason());
    }

    @Test
    void rejectsAnotherOpenApplicationForTheSameAccount() {
        CreditCardRequestRepository requestRepository = mock(CreditCardRequestRepository.class);
        CreditCardRequest existing = application("ABCDE1234F");
        existing.setStatus("Pending");
        when(requestRepository.findByAccountNumberOrderByRequestDateDesc("ACC123"))
                .thenReturn(List.of(existing));
        CreditCardRequestService service = createService(requestRepository, mock(CreditCardRepository.class));

        assertThrows(IllegalStateException.class, () -> service.saveRequest(application("FGHIJ5678K"), 84000d));
        verify(requestRepository, never()).save(any(CreditCardRequest.class));
    }

    @Test
    void approvalUsesOnlyTheLimitEnteredByAdmin() {
        CreditCardRequestRepository requestRepository = mock(CreditCardRequestRepository.class);
        CreditCardRepository creditCardRepository = mock(CreditCardRepository.class);
        CreditCardRequestService service = createService(requestRepository, creditCardRepository);
        CreditCardRequest request = application("ABCDE1234F");
        request.setId(17L);
        request.setStatus("Pending");
        request.setSuggestedLimit(95000d);
        request.setPanBasedLimit(80000d);
        when(requestRepository.findById(17L)).thenReturn(Optional.of(request));
        when(requestRepository.save(any(CreditCardRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(creditCardRepository.findByAccountNumber("ACC123")).thenReturn(List.of());
        when(creditCardRepository.save(any(CreditCard.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        CreditCardRequestService.ApprovalResult result = service.approveRequest(17L, "admin-9", 42000d);

        assertEquals("Pending Signature", result.application().getStatus());
        assertEquals(42000d, result.application().getApprovedLimit());
        assertTrue(result.shareUrl().startsWith("https://bank.example/credit-card-esign/"));
        assertNotNull(result.expiresAt());
        verify(creditCardRepository, never()).save(any(CreditCard.class));
    }

    @Test
    void guestApplicationCanBeTrackedByApplicationIdPanAndMobile() {
        CreditCardRequestRepository requestRepository = mock(CreditCardRequestRepository.class);
        CreditCardRequestService service = createService(requestRepository, mock(CreditCardRepository.class));
        when(requestRepository.findByPanIgnoreCaseAndMobileNumberOrderByRequestDateDesc("ABCDE1234F", "9876543210"))
                .thenReturn(List.of());
        when(requestRepository.save(any(CreditCardRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        CreditCardRequest request = new CreditCardRequest();
        when(requestRepository.findByApplicationId(anyString())).thenAnswer(invocation ->
                request.getApplicationId().equals(invocation.getArgument(0)) ? request : null);
        request.setUserName("Guest applicant");
        request.setUserEmail("guest@example.com");
        request.setPan("abcde1234f");
        request.setAnnualIncome(84000d);

        CreditCardRequest saved = service.createGuestApplication(
                request, "1234", "1 Main Street", "9876543210", "1990-01-01",
                "SALARIED", "Example Company", "Analyst", true,
                new MockMultipartFile("panDocument", "pan.png", "image/png", new byte[]{1}),
                new MockMultipartFile("aadhaarDocument", "aadhaar.png", "image/png", new byte[]{1}),
                new MockMultipartFile("selfie", "selfie.jpg", "image/jpeg", new byte[]{1}));

        assertTrue(saved.getApplicationId().matches("CC[0-9]{4}[A-F0-9]{10}"));
        assertNull(saved.getAccountNumber());
        assertEquals("1234", saved.getAadhaarLastFour());
        assertTrue(saved.isDocumentsAvailable());
        assertSame(saved, service.trackGuestApplication(saved.getApplicationId(), "ABCDE1234F", "9876543210"));
        assertNull(service.trackGuestApplication(saved.getApplicationId(), "ABCDE1234F", "9876543211"));
    }

    @Test
    void cardIsGeneratedAtTheRequestedLimitOnlyAfterSignatureAndAdminAadhaarReview() {
        CreditCardRequestRepository requestRepository = mock(CreditCardRequestRepository.class);
        CreditCardRepository creditCardRepository = mock(CreditCardRepository.class);
        CreditCardNumberHistoryRepository historyRepository = mock(CreditCardNumberHistoryRepository.class);
        CreditCardRequestService service = createService(
                requestRepository, creditCardRepository, mock(CreditCardApplicationDocumentService.class),
                mock(OtpService.class), historyRepository);
        CreditCardRequest request = new CreditCardRequest();
        request.setId(22L);
        request.setApplicationId("CC2026ABCDEF1234");
        request.setUserName("Guest applicant");
        request.setUserEmail("guest@example.com");
        request.setPan("ABCDE1234F");
        request.setPredictedCibil(800);
        request.setStatus("Pending");
        when(requestRepository.findById(22L)).thenReturn(Optional.of(request));
        when(requestRepository.findByCardAgreementTokenHash(anyString())).thenAnswer(invocation ->
                request.getCardAgreementTokenHash().equals(invocation.getArgument(0)) ? request : null);
        when(requestRepository.save(any(CreditCardRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CreditCardRequestService.ApprovalResult approval = service.approveRequest(22L, "admin-9", 42000d);

        assertEquals("Pending Signature", approval.application().getStatus());
        assertEquals(42000d, approval.application().getApprovedLimit());
        assertNull(approval.application().getGuestCardNumber());
        String token = approval.shareUrl().substring(approval.shareUrl().lastIndexOf('/') + 1);
        request.setAgreementCaptchaChallengeId("captcha-1");
        request.setAgreementCaptchaAnswerHash(hash("ABC234"));
        request.setAgreementCaptchaExpiresAt(LocalDateTime.now().plusMinutes(5));

        CreditCardRequestService.AgreementSignResult signature = service.signCreditCardAgreement(
                token, "captcha-1", "abc234", "Guest applicant", true, "127.0.0.1");

        assertTrue(signature.signed());
        assertEquals("Pending Card Issuance", request.getStatus());
        assertEquals(42000d, request.getApprovedLimit());
        assertNull(request.getGuestCardNumber());
        assertNull(request.getGuestCardExpiry());
        assertNull(request.getGuestCardCvv());
        assertThrows(IllegalArgumentException.class, () -> service.generateApprovedCardNumber(
                22L, "admin-9", false, "Initial issuance"));

        CreditCardRequest issued = service.generateApprovedCardNumber(
                22L, "admin-9", true, "Initial approved issue");

        assertSame(request, issued);
        assertEquals("Approved", request.getStatus());
        assertEquals("Active", request.getIssuedCardStatus());
        assertTrue(request.getGuestCardNumber().matches("4[0-9]{15}"));
        assertTrue(hasValidLuhnChecksum(request.getGuestCardNumber()));
        assertTrue(request.getGuestCardExpiry().matches("(0[1-9]|1[0-2])/\\d{2}"));
        assertTrue(request.getGuestCardCvv().matches("[0-9]{3}"));
        assertNotNull(request.getCardAgreementSignatureHash());
        assertNull(request.getCardAgreementTokenHash());
        assertNotNull(request.getDocumentsDeleteAfter());
        verify(creditCardRepository, never()).save(any(CreditCard.class));
        verify(historyRepository).save(argThat(history ->
                "GENERATE".equals(history.getAction())
                        && "Initial approved issue".equals(history.getReason())
                        && history.getOldMaskedCardNumber() == null
                        && history.getNewMaskedCardNumber().endsWith(request.getLast4())));
    }

    @Test
    void cannotIssueCardBeforeTheAgreementHasBeenSigned() {
        CreditCardRequestRepository requestRepository = mock(CreditCardRequestRepository.class);
        CreditCardRequest request = new CreditCardRequest();
        request.setId(23L);
        request.setStatus("Pending Signature");
        when(requestRepository.findById(23L)).thenReturn(Optional.of(request));
        CreditCardRequestService service = createService(requestRepository, mock(CreditCardRepository.class));

        assertThrows(IllegalStateException.class, () -> service.generateApprovedCardNumber(
                23L, "admin-9", true, "Initial issue"));

        verify(requestRepository, never()).save(any(CreditCardRequest.class));
    }

    @Test
    void cardMustBeBlockedBeforeReissueAndHistoryRecordsOldAndNewMaskedNumbers() {
        CreditCardRequestRepository requestRepository = mock(CreditCardRequestRepository.class);
        CreditCardNumberHistoryRepository historyRepository = mock(CreditCardNumberHistoryRepository.class);
        CreditCardRequest request = approvedGuestRequest();
        request.setId(24L);
        request.setCardAgreementSignedAt(LocalDateTime.now());
        request.setCardAgreementSignatureHash("signed-evidence");
        when(requestRepository.findById(24L)).thenReturn(Optional.of(request));
        when(requestRepository.save(any(CreditCardRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(historyRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        CreditCardRequestService service = createService(
                requestRepository, mock(CreditCardRepository.class), mock(CreditCardApplicationDocumentService.class),
                mock(OtpService.class), historyRepository);

        assertThrows(IllegalStateException.class, () -> service.generateApprovedCardNumber(
                24L, "admin-9", true, "Replacement"));

        service.blockApprovedCard(24L, "admin-9", "Card reported lost");
        assertEquals("Blocked", request.getIssuedCardStatus());
        CreditCardRequest reissued = service.generateApprovedCardNumber(
                24L, "admin-9", true, "Lost card replacement");

        assertSame(request, reissued);
        assertEquals("Active", request.getIssuedCardStatus());
        assertNotEquals("4111111111111111", request.getGuestCardNumber());
        assertEquals("Approved", request.getStatus());
        ArgumentCaptor<CreditCardNumberHistory> historyCaptor =
                ArgumentCaptor.forClass(CreditCardNumberHistory.class);
        verify(historyRepository, times(2)).save(historyCaptor.capture());
        List<CreditCardNumberHistory> history = historyCaptor.getAllValues();
        assertEquals("BLOCK", history.get(0).getAction());
        assertEquals("**** **** **** 1111", history.get(0).getOldMaskedCardNumber());
        assertNull(history.get(0).getNewMaskedCardNumber());
        assertEquals("Card reported lost", history.get(0).getReason());
        assertEquals("REISSUE", history.get(1).getAction());
        assertEquals("**** **** **** 1111", history.get(1).getOldMaskedCardNumber());
        assertEquals("**** **** **** " + request.getLast4(), history.get(1).getNewMaskedCardNumber());
        assertEquals("Lost card replacement", history.get(1).getReason());
        assertNotNull(history.get(0).getGeneratedAt());
        assertNotNull(history.get(1).getGeneratedAt());
    }

    @Test
    void approvedGuestCardDetailsAreReturnedOnlyAfterEmailOtpVerification() {
        CreditCardRequestRepository requestRepository = mock(CreditCardRequestRepository.class);
        OtpService otpService = mock(OtpService.class);
        CreditCardRequest request = approvedGuestRequest();
        request.setCardDetailsOtpSentAt(LocalDateTime.now());
        when(requestRepository.findByApplicationId(request.getApplicationId())).thenReturn(request);
        when(requestRepository.save(any(CreditCardRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(otpService.verifyOtpByKey("CREDIT_CARD_GUEST:" + request.getApplicationId(), "123456"))
                .thenReturn(true);
        CreditCardRequestService service = createService(
                requestRepository, mock(CreditCardRepository.class), mock(CreditCardApplicationDocumentService.class), otpService);

        Map<String, Object> details = service.verifyGuestCardDetailsOtp(
                request.getApplicationId(), "ABCDE1234F", "9876543210", "123456");

        assertEquals(request.getGuestCardNumber(), details.get("cardNumber"));
        assertEquals(request.getGuestCardExpiry(), details.get("expiryDate"));
        assertEquals(request.getGuestCardCvv(), details.get("cvv"));
        assertEquals(42000d, details.get("approvedLimit"));
        assertNull(request.getCardDetailsOtpSentAt());
        verify(otpService).verifyOtpByKey("CREDIT_CARD_GUEST:" + request.getApplicationId(), "123456");
    }

    @Test
    void guestCardOtpIsSentToTheApplicationEmailWithoutReturningTheCode() {
        CreditCardRequestRepository requestRepository = mock(CreditCardRequestRepository.class);
        OtpService otpService = mock(OtpService.class);
        CreditCardRequest request = approvedGuestRequest();
        when(requestRepository.findByApplicationId(request.getApplicationId())).thenReturn(request);
        when(requestRepository.save(any(CreditCardRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(otpService.sendOtpForKey(
                "guest@example.com", "CREDIT_CARD_GUEST:" + request.getApplicationId(),
                "NeoBank credit card details verification")).thenReturn("123456");
        CreditCardRequestService service = createService(
                requestRepository, mock(CreditCardRepository.class), mock(CreditCardApplicationDocumentService.class), otpService);

        String maskedEmail = service.requestGuestCardDetailsOtp(
                request.getApplicationId(), "ABCDE1234F", "9876543210");

        assertEquals("g***@example.com", maskedEmail);
        assertFalse(maskedEmail.contains("123456"));
        assertNotNull(request.getCardDetailsOtpSentAt());
        verify(otpService).sendOtpForKey(
                "guest@example.com", "CREDIT_CARD_GUEST:" + request.getApplicationId(),
                "NeoBank credit card details verification");
    }

    @Test
    void failedGuestCardOtpIncrementsAttemptsAndDoesNotReturnCardDetails() {
        CreditCardRequestRepository requestRepository = mock(CreditCardRequestRepository.class);
        OtpService otpService = mock(OtpService.class);
        CreditCardRequest request = approvedGuestRequest();
        request.setCardDetailsOtpSentAt(LocalDateTime.now());
        when(requestRepository.findByApplicationId(request.getApplicationId())).thenReturn(request);
        when(requestRepository.save(any(CreditCardRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(otpService.verifyOtpByKey(anyString(), eq("000000"))).thenReturn(false);
        CreditCardRequestService service = createService(
                requestRepository, mock(CreditCardRepository.class), mock(CreditCardApplicationDocumentService.class), otpService);

        assertThrows(IllegalArgumentException.class, () -> service.verifyGuestCardDetailsOtp(
                request.getApplicationId(), "ABCDE1234F", "9876543210", "000000"));

        assertEquals(1, request.getCardDetailsOtpAttempts());
        verify(requestRepository).save(request);
    }

    @Test
    void cardDetailsOtpLocksAfterFiveFailedAttempts() {
        CreditCardRequestRepository requestRepository = mock(CreditCardRequestRepository.class);
        OtpService otpService = mock(OtpService.class);
        CreditCardRequest request = approvedGuestRequest();
        request.setCardDetailsOtpSentAt(LocalDateTime.now());
        request.setCardDetailsOtpAttempts(4);
        when(requestRepository.findByApplicationId(request.getApplicationId())).thenReturn(request);
        when(requestRepository.save(any(CreditCardRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(otpService.verifyOtpByKey(anyString(), eq("000000"))).thenReturn(false);
        CreditCardRequestService service = createService(
                requestRepository, mock(CreditCardRepository.class), mock(CreditCardApplicationDocumentService.class), otpService);

        assertThrows(IllegalArgumentException.class, () -> service.verifyGuestCardDetailsOtp(
                request.getApplicationId(), "ABCDE1234F", "9876543210", "000000"));

        assertEquals(5, request.getCardDetailsOtpAttempts());
        assertTrue(request.getCardDetailsOtpLockedUntil().isAfter(LocalDateTime.now()));
    }

    private CreditCardRequestService createService(CreditCardRequestRepository requestRepository,
                                                   CreditCardRepository creditCardRepository) {
        return createService(requestRepository, creditCardRepository,
                mock(CreditCardApplicationDocumentService.class), mock(OtpService.class));
    }

    private CreditCardRequestService createService(CreditCardRequestRepository requestRepository,
                                                   CreditCardRepository creditCardRepository,
                                                   CreditCardApplicationDocumentService documentService) {
        return createService(requestRepository, creditCardRepository, documentService, mock(OtpService.class));
    }

    private CreditCardRequestService createService(CreditCardRequestRepository requestRepository,
                                                   CreditCardRepository creditCardRepository,
                                                   CreditCardApplicationDocumentService documentService,
                                                   OtpService otpService) {
        return createService(requestRepository, creditCardRepository, documentService,
                otpService, mock(CreditCardNumberHistoryRepository.class));
    }

    private CreditCardRequestService createService(CreditCardRequestRepository requestRepository,
                                                   CreditCardRepository creditCardRepository,
                                                   CreditCardApplicationDocumentService documentService,
                                                   OtpService otpService,
                                                   CreditCardNumberHistoryRepository historyRepository) {
        Account account = new Account();
        account.setStatus("ACTIVE");
        AccountService accountService = mock(AccountService.class);
        when(accountService.getAccountByNumber("ACC123")).thenReturn(account);
        EmailService emailService = mock(EmailService.class);
        when(emailService.sendCreditCardAgreementLinkEmail(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(true);
        return new CreditCardRequestService(requestRepository, new CreditScorePredictorService(),
                mock(CardRepository.class), creditCardRepository, historyRepository, mock(VirtualCardRepository.class), accountService,
                mock(SalaryAccountRepository.class), documentService, otpService, emailService, "https://bank.example");
    }

    private CreditCardRequest approvedGuestRequest() {
        CreditCardRequest request = new CreditCardRequest();
        request.setApplicationId("CC2026ABCDEF1234");
        request.setUserEmail("guest@example.com");
        request.setStatus("Approved");
        request.setApprovedLimit(42000d);
        request.setGuestCardNumber("4111111111111111");
        request.setGuestCardExpiry("12/29");
        request.setGuestCardCvv("123");
        request.setLast4("1111");
        request.setIssuedCardStatus("Active");
        request.setTrackingCredentialHash(trackingCredentialHash(
                request.getApplicationId(), "ABCDE1234F", "9876543210"));
        return request;
    }

    private String trackingCredentialHash(String applicationId, String pan, String mobileNumber) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return java.util.HexFormat.of().formatHex(digest.digest(
                    (applicationId + "|" + pan + "|" + mobileNumber).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return java.util.HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }

    private boolean hasValidLuhnChecksum(String cardNumber) {
        int sum = 0;
        for (int index = 0; index < cardNumber.length(); index++) {
            int digit = cardNumber.charAt(index) - '0';
            if ((cardNumber.length() - 1 - index) % 2 == 1) {
                digit *= 2;
                if (digit > 9) digit -= 9;
            }
            sum += digit;
        }
        return sum % 10 == 0;
    }

    private CreditCardRequest application(String pan) {
        CreditCardRequest request = new CreditCardRequest();
        request.setAccountNumber("ACC123");
        request.setUserName("Applicant");
        request.setUserEmail("applicant@example.com");
        request.setPan(pan);
        return request;
    }
}
