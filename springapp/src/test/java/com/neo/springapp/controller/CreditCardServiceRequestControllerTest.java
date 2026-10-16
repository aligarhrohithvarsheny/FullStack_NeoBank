package com.neo.springapp.controller;

import com.neo.springapp.model.CreditCard;
import com.neo.springapp.model.CreditCardSettings;
import com.neo.springapp.repository.CreditCardAuditLogRepository;
import com.neo.springapp.repository.CreditCardRepository;
import com.neo.springapp.repository.CreditCardServiceRequestHistoryRepository;
import com.neo.springapp.repository.CreditCardServiceRequestRepository;
import com.neo.springapp.repository.CreditCardSettingsRepository;
import com.neo.springapp.repository.CreditCardTransactionRepository;
import com.neo.springapp.service.UserSessionTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CreditCardServiceRequestControllerTest {

    private CreditCardRepository cardRepository;
    private CreditCardServiceRequestRepository requestRepository;
    private CreditCardServiceRequestHistoryRepository historyRepository;
    private CreditCardSettingsRepository settingsRepository;
    private CreditCardServiceRequestController controller;

    @BeforeEach
    void setUp() {
        cardRepository = mock(CreditCardRepository.class);
        requestRepository = mock(CreditCardServiceRequestRepository.class);
        historyRepository = mock(CreditCardServiceRequestHistoryRepository.class);
        settingsRepository = mock(CreditCardSettingsRepository.class);
        controller = new CreditCardServiceRequestController(
                cardRepository,
                requestRepository,
                historyRepository,
                settingsRepository,
                mock(CreditCardTransactionRepository.class),
                mock(CreditCardAuditLogRepository.class));
    }

    @Test
    void customerCannotCreateRequestForAnotherAccountCard() {
        CreditCard card = new CreditCard();
        card.setAccountNumber("OTHER");
        when(cardRepository.findById(10L)).thenReturn(Optional.of(card));

        var response = controller.createRequest(
                new CreditCardServiceRequestController.CreateRequest(10L, "Billing Issue", null, "Please review this charge", null, null),
                user("CUSTOMER"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(requestRepository, never()).save(any());
    }

    @Test
    void limitIncreaseMustStayWithinConfiguredCeiling() {
        CreditCard card = new CreditCard();
        card.setId(10L);
        card.setAccountNumber("CUSTOMER");
        card.setApprovedLimit(50_000.0);
        when(cardRepository.findById(10L)).thenReturn(Optional.of(card));
        CreditCardSettings settings = new CreditCardSettings();
        settings.setCreditLimitIncreaseCeiling(new BigDecimal("100000"));
        when(settingsRepository.findAll()).thenReturn(List.of(settings));

        var response = controller.createRequest(
                new CreditCardServiceRequestController.CreateRequest(
                        10L, "Credit Limit Increase", null, "Request a higher limit for planned costs", new BigDecimal("100001"), null),
                user("CUSTOMER"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(requestRepository, never()).save(any());
    }

    @Test
    void customerCanSubmitLimitRequestWithinConfiguredCeiling() {
        CreditCard card = new CreditCard();
        card.setId(10L);
        card.setAccountNumber("CUSTOMER");
        card.setApprovedLimit(50_000.0);
        when(cardRepository.findById(10L)).thenReturn(Optional.of(card));
        CreditCardSettings settings = new CreditCardSettings();
        settings.setCreditLimitIncreaseCeiling(new BigDecimal("100000"));
        when(settingsRepository.findAll()).thenReturn(List.of(settings));
        when(requestRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(historyRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = controller.createRequest(
                new CreditCardServiceRequestController.CreateRequest(
                        10L, "Credit Limit Increase", null, "Request a higher limit for planned costs", new BigDecimal("75000"), null),
                user("CUSTOMER"));

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(requestRepository).save(any());
        verify(historyRepository).save(any());
    }

    private Authentication user(String accountNumber) {
        return new UsernamePasswordAuthenticationToken(
                new UserSessionTokenService.SessionPrincipal(1L, accountNumber, "USER"),
                null);
    }
}
