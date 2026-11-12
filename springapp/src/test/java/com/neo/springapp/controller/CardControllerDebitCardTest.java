package com.neo.springapp.controller;

import com.neo.springapp.model.Card;
import com.neo.springapp.model.CardActionHistory;
import com.neo.springapp.model.User;
import com.neo.springapp.repository.UserRepository;
import com.neo.springapp.service.CardService;
import com.neo.springapp.service.PasswordService;
import com.neo.springapp.service.UserSessionTokenService.SessionPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class CardControllerDebitCardTest {
    private CardController controller;
    private CardService cardService;
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        cardService = mock(CardService.class);
        userRepository = mock(UserRepository.class);
        controller = new CardController(cardService, mock(PasswordService.class));
        ReflectionTestUtils.setField(controller, "userRepository", userRepository);
    }

    @Test
    void generatesCardForTheAuthenticatedUsersAccount() {
        User user = new User();
        user.setAccountNumber("ACC123");
        Card card = new Card();
        card.setId(8L);
        when(userRepository.findByAccountNumber("ACC123")).thenReturn(Optional.of(user));
        when(cardService.generateDebitCardForUser(user)).thenReturn(card);

        var response = controller.generateDebitCard(new SessionPrincipal(7L, "ACC123", "USER"));

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(card, response.getBody());
        verify(userRepository).findByAccountNumber("ACC123");
        verify(cardService).generateDebitCardForUser(user);
    }

    @Test
    void refusesCardGenerationForNonUserScopes() {
        var response = controller.generateDebitCard(new SessionPrincipal(1L, "ACC123", "ADMIN"));

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verifyNoInteractions(userRepository, cardService);
    }

    @Test
    void onlyReturnsHistoryForTheOwningAccount() {
        Card card = new Card();
        card.setAccountNumber("ACC123");
        when(cardService.getCardById(8L)).thenReturn(card);
        when(cardService.getCardActionHistory(8L)).thenReturn(List.of(new CardActionHistory()));

        var forbiddenResponse = controller.getUserCardActionHistory(
                8L, new SessionPrincipal(1L, "ACC999", "USER"));
        assertEquals(HttpStatus.FORBIDDEN, forbiddenResponse.getStatusCode());

        verify(cardService, never()).getCardActionHistory(8L);

        var ownerResponse = controller.getUserCardActionHistory(
                8L, new SessionPrincipal(7L, "ACC123", "USER"));
        assertEquals(HttpStatus.OK, ownerResponse.getStatusCode());
        verify(cardService).getCardActionHistory(8L);
    }
}
