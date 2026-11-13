package com.neo.springapp.controller;

import com.neo.springapp.model.Account;
import com.neo.springapp.model.CreditCardRequest;
import com.neo.springapp.model.User;
import com.neo.springapp.repository.UserRepository;
import com.neo.springapp.repository.SalaryAccountRepository;
import com.neo.springapp.service.CreditCardRequestService;
import com.neo.springapp.service.UserSessionTokenService.SessionPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CreditCardRequestControllerTest {
    private CreditCardRequestController controller;
    private CreditCardRequestService service;
    private UserRepository userRepository;
    private SalaryAccountRepository salaryAccountRepository;

    @BeforeEach
    void setUp() {
        service = mock(CreditCardRequestService.class);
        userRepository = mock(UserRepository.class);
        salaryAccountRepository = mock(SalaryAccountRepository.class);
        controller = new CreditCardRequestController(service, userRepository, salaryAccountRepository);
    }

    @Test
    void derivesApplicationOwnerFromAuthenticatedSession() {
        Account account = new Account();
        account.setName("Verified Applicant");
        account.setStatus("ACTIVE");
        User user = new User();
        user.setUsername("verified-user");
        user.setEmail("verified@example.com");
        user.setAccountNumber("ACC123");
        user.setStatus("APPROVED");
        user.setAccount(account);

        when(userRepository.findByAccountNumber("ACC123")).thenReturn(Optional.of(user));
        when(service.saveRequest(any(CreditCardRequest.class), eq(75000d)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = controller.create(
                new CreditCardRequestController.ApplicationSubmission("abcde1234f", 75000d),
                authenticated(new SessionPrincipal(5L, "ACC123", "USER")));

        assertEquals(201, response.getStatusCode().value());
        CreditCardRequest saved = (CreditCardRequest) response.getBody();
        assertEquals("ACC123", saved.getAccountNumber());
        assertEquals("Verified Applicant", saved.getUserName());
        assertEquals("verified@example.com", saved.getUserEmail());
        assertEquals("abcde1234f", saved.getPan());
    }

    @Test
    void refusesUnauthenticatedApplicationAndDoesNotSaveIt() {
        var response = controller.create(
                new CreditCardRequestController.ApplicationSubmission("ABCDE1234F", 75000d),
                null);

        assertEquals(401, response.getStatusCode().value());
        verifyNoInteractions(userRepository, salaryAccountRepository, service);
    }

    @Test
    void returnsOnlyApplicationsForTheAuthenticatedCustomer() {
        when(service.getRequestsByAccountNumber("ACC123")).thenReturn(List.of(new CreditCardRequest()));

        var response = controller.getMyApplications(
                authenticated(new SessionPrincipal(5L, "ACC123", "USER")));

        assertEquals(200, response.getStatusCode().value());
        verify(service).getRequestsByAccountNumber("ACC123");
    }

    @Test
    void requiresStaffAuthenticationToApproveWithAnAdminSetLimit() {
        var response = controller.approve(12L,
                new CreditCardRequestController.ApprovalRequest(50000d),
                authenticated(new SessionPrincipal(5L, "ACC123", "USER")));

        assertEquals(403, response.getStatusCode().value());
        verifyNoInteractions(service);
    }

    @Test
    void guestTrackingRequiresTheApplicationIdPanAndMobileNumber() {
        CreditCardRequest request = new CreditCardRequest();
        request.setApplicationId("CC2026ABCDEF1234");
        request.setStatus("Pending");
        request.setPan("ABCDE1234F");
        request.setMobileNumber("9876543210");
        when(service.trackGuestApplication("CC2026ABCDEF1234", "ABCDE1234F", "9876543210"))
                .thenReturn(request);

        var response = controller.trackGuest(new CreditCardRequestController.TrackingRequest(
                "CC2026ABCDEF1234", "ABCDE1234F", "9876543210"));

        assertEquals(200, response.getStatusCode().value());
        var status = (java.util.Map<?, ?>) response.getBody();
        assertEquals("Pending", status.get("status"));
        assertFalse(status.containsKey("pan"));
        assertFalse(status.containsKey("mobileNumber"));
    }

    @Test
    void guestTrackingShowsOnlyMaskedCardInformationBeforeOtpVerification() {
        CreditCardRequest request = new CreditCardRequest();
        request.setApplicationId("CC2026ABCDEF1234");
        request.setStatus("Approved");
        request.setGuestCardNumber("4111111111111111");
        request.setGuestCardExpiry("12/29");
        request.setGuestCardCvv("123");
        when(service.trackGuestApplication("CC2026ABCDEF1234", "ABCDE1234F", "9876543210"))
                .thenReturn(request);

        var response = controller.trackGuest(new CreditCardRequestController.TrackingRequest(
                "CC2026ABCDEF1234", "ABCDE1234F", "9876543210"));

        assertEquals(200, response.getStatusCode().value());
        var status = (java.util.Map<?, ?>) response.getBody();
        assertEquals(true, status.get("cardDetailsAvailable"));
        assertEquals("**** **** **** 1111", status.get("maskedCardNumber"));
        assertFalse(status.containsValue("4111111111111111"));
        assertFalse(status.containsValue("12/29"));
        assertFalse(status.containsValue("123"));
    }

    @Test
    void fullGuestCardDetailsResponseIsNotCacheable() {
        when(service.verifyGuestCardDetailsOtp("CC2026ABCDEF1234", "ABCDE1234F", "9876543210", "123456"))
                .thenReturn(java.util.Map.of(
                        "cardNumber", "4111111111111111",
                        "expiryDate", "12/29",
                        "cvv", "123",
                        "approvedLimit", 42000d));

        var response = controller.verifyGuestCardDetailsOtp(
                new CreditCardRequestController.CardDetailsOtpRequest(
                        "CC2026ABCDEF1234", "ABCDE1234F", "9876543210", "123456"));

        assertEquals(200, response.getStatusCode().value());
        assertEquals("no-store, no-cache, must-revalidate", response.getHeaders().getCacheControl());
    }

    @Test
    void nonStaffCannotViewApplicationDocuments() {
        var response = controller.getApplicationDocument(
                17L, "pan", authenticated(new SessionPrincipal(5L, "ACC123", "USER")));

        assertEquals(403, response.getStatusCode().value());
        verifyNoInteractions(service);
    }

    private UsernamePasswordAuthenticationToken authenticated(SessionPrincipal principal) {
        return new UsernamePasswordAuthenticationToken(principal, "token");
    }
}
