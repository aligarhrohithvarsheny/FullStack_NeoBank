package com.neo.springapp.service;

import com.neo.springapp.model.Account;
import com.neo.springapp.model.Card;
import com.neo.springapp.model.CardActionHistory;
import com.neo.springapp.model.User;
import com.neo.springapp.repository.CardActionHistoryRepository;
import com.neo.springapp.repository.CardRepository;
import com.neo.springapp.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class CardServiceDebitCardGenerationTest {

    @Test
    void generatesUniqueDebitCardAndPersistsMaskedIssuanceHistory() {
        CardRepository cardRepository = mock(CardRepository.class);
        CardActionHistoryRepository historyRepository = mock(CardActionHistoryRepository.class);
        CardService service = createService(cardRepository, historyRepository);
        User user = approvedUser();

        when(cardRepository.findByAccountNumber("ACC123")).thenReturn(List.of(
                new Card("4111111111111111", "Credit", "Test User", "ACC123", "test@example.com")));
        when(cardRepository.findByCardNumber(anyString())).thenReturn(null);
        when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> {
            Card saved = invocation.getArgument(0);
            saved.setId(42L);
            return saved;
        });

        Card card = service.generateDebitCardForUser(user, "123456789012");

        assertEquals("Visa Debit", card.getCardType());
        assertEquals("ACC123", card.getAccountNumber());
        assertEquals("Active", card.getStatus());
        assertFalse(card.isPinSet());
        assertTrue(card.getCardNumber().matches("4\\d{15}"));
        assertTrue(isLuhnValid(card.getCardNumber()));
        assertTrue(card.getCvv().matches("\\d{3}"));
        assertTrue(card.getExpiryDate().matches("\\d{2}/\\d{2}"));

        ArgumentCaptor<CardActionHistory> history = ArgumentCaptor.forClass(CardActionHistory.class);
        verify(historyRepository).save(history.capture());
        assertEquals(42L, history.getValue().getCardId());
        assertEquals("GENERATE", history.getValue().getActionType());
        assertEquals("USER", history.getValue().getChangedBy());
        assertEquals(card.getMaskedCardNumber(), history.getValue().getNewValue());
        assertFalse(history.getValue().getNewValue().contains(card.getCardNumber()));
    }

    @Test
    void preventsIssuingASecondDebitCardForTheSameAccount() {
        CardRepository cardRepository = mock(CardRepository.class);
        CardActionHistoryRepository historyRepository = mock(CardActionHistoryRepository.class);
        CardService service = createService(cardRepository, historyRepository);
        when(cardRepository.findByAccountNumber("ACC123")).thenReturn(List.of(
                new Card("4111111111111111", "Visa Debit", "Test User", "ACC123", "test@example.com")));

        assertThrows(IllegalStateException.class,
                () -> service.generateDebitCardForUser(approvedUser(), "123456789012"));
        verify(cardRepository, never()).save(any(Card.class));
        verify(historyRepository, never()).save(any(CardActionHistory.class));
    }

    @Test
    void rejectsUnapprovedOrInactiveAccounts() {
        CardRepository cardRepository = mock(CardRepository.class);
        CardActionHistoryRepository historyRepository = mock(CardActionHistoryRepository.class);
        CardService service = createService(cardRepository, historyRepository);
        User user = approvedUser();
        user.setStatus("PENDING");

        assertThrows(IllegalStateException.class,
                () -> service.generateDebitCardForUser(user, "123456789012"));

        user.setStatus("APPROVED");
        user.getAccount().setStatus("CLOSED");
        assertThrows(IllegalStateException.class,
                () -> service.generateDebitCardForUser(user, "123456789012"));
        verify(cardRepository, never()).save(any(Card.class));
    }

    @Test
    void requiresTheRegisteredVerifiedAadhaarBeforeGeneratingCard() {
        CardRepository cardRepository = mock(CardRepository.class);
        CardService service = createService(cardRepository, mock(CardActionHistoryRepository.class));
        User user = approvedUser();

        assertThrows(IllegalArgumentException.class,
                () -> service.generateDebitCardForUser(user, "123456789013"));
        user.getAccount().setAadharVerified(false);
        assertThrows(IllegalStateException.class,
                () -> service.generateDebitCardForUser(user, "123456789012"));

        verify(cardRepository, never()).save(any(Card.class));
    }

    private CardService createService(CardRepository cardRepository,
                                      CardActionHistoryRepository historyRepository) {
        return new CardService(cardRepository, mock(AccountService.class), historyRepository,
                mock(TransactionRepository.class));
    }

    private User approvedUser() {
        Account account = new Account();
        account.setName("Test User");
        account.setStatus("ACTIVE");
        account.setAadharNumber("123456789012");
        account.setAadharVerified(true);
        account.setAadharVerificationStatus("VERIFIED");

        User user = new User();
        user.setStatus("APPROVED");
        user.setAccountNumber("ACC123");
        user.setEmail("test@example.com");
        user.setUsername("test-user");
        user.setAccount(account);
        return user;
    }

    private boolean isLuhnValid(String number) {
        int sum = 0;
        boolean doubleDigit = false;
        for (int index = number.length() - 1; index >= 0; index--) {
            int digit = number.charAt(index) - '0';
            if (doubleDigit) {
                digit *= 2;
                if (digit > 9) digit -= 9;
            }
            sum += digit;
            doubleDigit = !doubleDigit;
        }
        return sum % 10 == 0;
    }
}
