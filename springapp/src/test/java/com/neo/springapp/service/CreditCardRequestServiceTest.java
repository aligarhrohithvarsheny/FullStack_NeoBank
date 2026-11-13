package com.neo.springapp.service;

import com.neo.springapp.model.Account;
import com.neo.springapp.model.Card;
import com.neo.springapp.model.CreditCard;
import com.neo.springapp.model.CreditCardRequest;
import com.neo.springapp.repository.CardRepository;
import com.neo.springapp.repository.CreditCardRepository;
import com.neo.springapp.repository.CreditCardRequestRepository;
import com.neo.springapp.repository.SalaryAccountRepository;
import com.neo.springapp.service.CreditCardApplicationDocumentService;
import org.springframework.mock.web.MockMultipartFile;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CreditCardRequestServiceTest {

    @Test
    void storesApplicationDataWithoutAssigningOrPredictingALimit() {
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
        assertNull(saved.getSuggestedLimit());
        assertNull(saved.getPanBasedLimit());
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
        CreditCardRequest approved = service.approveRequest(17L, "admin-9", 42000d);

        assertEquals("Approved", approved.getStatus());
        assertEquals(42000d, approved.getApprovedLimit());
        var cardCaptor = org.mockito.ArgumentCaptor.forClass(CreditCard.class);
        verify(creditCardRepository).save(cardCaptor.capture());
        assertEquals(42000d, cardCaptor.getValue().getApprovedLimit());
        assertEquals(42000d, cardCaptor.getValue().getAvailableLimit());
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
    void guestApprovalRecordsTheLimitWithoutCreatingAnAccountOrCard() {
        CreditCardRequestRepository requestRepository = mock(CreditCardRequestRepository.class);
        CreditCardRepository creditCardRepository = mock(CreditCardRepository.class);
        CreditCardRequestService service = createService(requestRepository, creditCardRepository);
        CreditCardRequest request = new CreditCardRequest();
        request.setId(22L);
        request.setApplicationId("CC2026ABCDEF1234");
        request.setStatus("Pending");
        when(requestRepository.findById(22L)).thenReturn(Optional.of(request));
        when(requestRepository.save(any(CreditCardRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CreditCardRequest approved = service.approveRequest(22L, "admin-9", 42000d);

        assertEquals("Approved", approved.getStatus());
        assertEquals(42000d, approved.getApprovedLimit());
        assertNull(approved.getAccountNumber());
        assertNotNull(approved.getDocumentsDeleteAfter());
        verifyNoInteractions(creditCardRepository);
    }

    private CreditCardRequestService createService(CreditCardRequestRepository requestRepository,
                                                   CreditCardRepository creditCardRepository) {
        return createService(requestRepository, creditCardRepository, mock(CreditCardApplicationDocumentService.class));
    }

    private CreditCardRequestService createService(CreditCardRequestRepository requestRepository,
                                                   CreditCardRepository creditCardRepository,
                                                   CreditCardApplicationDocumentService documentService) {
        Account account = new Account();
        account.setStatus("ACTIVE");
        AccountService accountService = mock(AccountService.class);
        when(accountService.getAccountByNumber("ACC123")).thenReturn(account);
        return new CreditCardRequestService(requestRepository, mock(CreditScorePredictorService.class),
                mock(CardRepository.class), creditCardRepository, accountService,
                mock(SalaryAccountRepository.class), documentService);
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
