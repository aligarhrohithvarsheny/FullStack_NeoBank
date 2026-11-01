package com.neo.springapp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neo.springapp.model.Loan;
import com.neo.springapp.model.LoanAgreement;
import com.neo.springapp.repository.GoldLoanRepository;
import com.neo.springapp.repository.HomeLoanRepository;
import com.neo.springapp.repository.LoanAgreementRepository;
import com.neo.springapp.repository.LoanRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LoanAgreementServiceTest {
    private LoanAgreementRepository agreementRepository;
    private LoanRepository loanRepository;
    private LoanAgreementService service;
    private final AtomicReference<LoanAgreement> storedAgreement = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        agreementRepository = mock(LoanAgreementRepository.class);
        loanRepository = mock(LoanRepository.class);
        HomeLoanRepository homeLoanRepository = mock(HomeLoanRepository.class);
        GoldLoanRepository goldLoanRepository = mock(GoldLoanRepository.class);
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        service = new LoanAgreementService(agreementRepository, loanRepository, homeLoanRepository,
                goldLoanRepository, new ObjectMapper(), messagingTemplate, "http://localhost:4200");

        when(agreementRepository.findAllByLoanTypeAndLoanIdOrderByIssuedAtDesc(any(), any()))
                .thenReturn(List.of());
        when(agreementRepository.save(any())).thenAnswer(invocation -> {
            LoanAgreement agreement = invocation.getArgument(0);
            if (agreement.getId() == null) agreement.setId(41L);
            storedAgreement.set(agreement);
            return agreement;
        });
    }

    @Test
    void issuesTenPageMinimumWatermarkedPdfAndCalculatesStampDuty() throws Exception {
        Loan loan = new Loan("Personal Loan", 100_000.0, 12, 12.0);
        loan.setId(8L);
        loan.setUserName("Example Customer");
        loan.setUserEmail("customer@example.test");
        loan.setAccountNumber("1234567890");
        loan.setPurpose("Education expenses");
        loan.setLoanAccountNumber("LOAN-8");
        when(loanRepository.findById(8L)).thenReturn(Optional.of(loan));

        Map<String, Object> response = service.issue("LOAN", 8L, Map.of());
        String shareUrl = response.get("shareUrl").toString();
        assertTrue(shareUrl.startsWith("http://localhost:4200/loan-esign/"));
        assertEquals(500.0, response.get("stampDuty"));

        try (var document = Loader.loadPDF(storedAgreement.get().getPdfBytes())) {
            assertTrue(document.getNumberOfPages() >= 10);
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("NEOBANK SAMPLE"));
            assertTrue(text.contains("DEMO DOCUMENT"));
            assertTrue(text.contains("DEMO STAMP"));
            assertTrue(text.contains("NOT OFFICIAL"));
            assertTrue(text.contains("DEFAULT, NOTICES AND RECOVERY"));
            assertTrue(text.contains("FULL REPAYMENT"));
        }
    }

    @Test
    void invalidCaptchaIsRejectedAndConsumesTheChallenge() throws Exception {
        Loan loan = new Loan("Personal Loan", 10_000.0, 6, 8.0);
        loan.setId(9L);
        loan.setUserName("Example Customer");
        loan.setAccountNumber("1234567890");
        loan.setLoanAccountNumber("LOAN-9");
        when(loanRepository.findById(9L)).thenReturn(Optional.of(loan));

        Map<String, Object> response = service.issue("PERSONAL_LOAN", 9L, Map.of());
        String token = response.get("shareUrl").toString().substring(response.get("shareUrl").toString().lastIndexOf('/') + 1);
        String tokenHash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        when(agreementRepository.findByShareTokenHash(tokenHash)).thenAnswer(invocation ->
                Optional.ofNullable(storedAgreement.get()));

        Map<String, Object> challenge = service.createCaptcha(token);
        boolean signed = service.sign(token, challenge.get("challengeId").toString(), "WRONG",
                "Example Customer", true, "127.0.0.1");

        assertFalse(signed);
        assertEquals(1, storedAgreement.get().getCaptchaAttempts());
        assertTrue(storedAgreement.get().getCaptchaAnswerHash() == null);
    }
}
