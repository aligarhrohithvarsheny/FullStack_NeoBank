package com.neo.springapp.service;

import com.neo.springapp.model.Admin;
import com.neo.springapp.model.Card;
import com.neo.springapp.model.Card360Access;
import com.neo.springapp.model.CreditCard;
import com.neo.springapp.model.CreditCardTransaction;
import com.neo.springapp.model.User;
import com.neo.springapp.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.lang.NonNull;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Card360ServiceTest {
    @Mock private Card360AccessRepository accessRepository;
    @Mock private Card360AuditRepository auditRepository;
    @Mock private CardRepository cardRepository;
    @Mock private CreditCardRepository creditCardRepository;
    @Mock private CreditCardTransactionRepository creditTransactionRepository;
    @Mock private UserRepository userRepository;
    @Mock private AdminService adminService;
    @Mock private CreditCardService creditCardService;

    private UserSessionTokenService tokenService;
    private Card360Service service;

    @BeforeEach
    void setUp() {
        tokenService = new UserSessionTokenService("test-secret-with-at-least-32-characters");
        service = new Card360Service(accessRepository, auditRepository, cardRepository, creditCardRepository,
                creditTransactionRepository, userRepository, adminService, tokenService,
                creditCardService);
    }

    @Test
    void generatedPasscodeIsHashedAndIssuesCards360ScopedSession() {
        Admin admin = new Admin();
        admin.setEmail("admin@neobank.test");
        admin.setRole("ADMIN");
        User user = approvedUser();

        when(adminService.login("admin@neobank.test", "admin-password")).thenReturn(admin);
        when(userRepository.findByAccountNumber("ACC123")).thenReturn(Optional.of(user));
        when(accessRepository.findByAccountNumber("ACC123")).thenReturn(Optional.empty());

        Map<String, Object> response = service.generatePasscode("ACC123", "admin@neobank.test", "admin-password");
        String passcode = (String) response.get("passcode");
        assertThat(passcode).isNotBlank().hasSizeGreaterThanOrEqualTo(24);
        Card360Access access = savedAccess();
        assertThat(access.isEnabled()).isTrue();
        assertThat(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder()
                .matches(passcode, access.getPasscodeHash())).isTrue();
        Card debitCard = debitCard();
        debitCard.setUserEmail("old-customer-email@neobank.test");
        when(cardRepository.findByCardNumber("4111111111111234")).thenReturn(debitCard);
        when(accessRepository.findByAccountNumber("ACC123")).thenReturn(Optional.of(access));

        Map<String, Object> loginResponse = service.login(
                "4111 1111-1111 1234", "  " + user.getEmail().toUpperCase() + "  ");
        UserSessionTokenService.SessionPrincipal principal =
                tokenService.verify((String) loginResponse.get("token"));

        assertThat(principal).isNotNull();
        assertThat(principal.scope()).isEqualTo("CARD360");
        assertThat(principal.accountNumber()).isEqualTo("ACC123");
    }

    @Test
    void customerCardResponseNeverIncludesCardSecurityCodes() {
        Card360Access access = new Card360Access();
        access.setAccountNumber("ACC123");
        access.setEnabled(true);
        when(accessRepository.findByAccountNumber("ACC123")).thenReturn(Optional.of(access));
        when(userRepository.findByAccountNumber("ACC123")).thenReturn(Optional.of(approvedUser()));
        when(cardRepository.findByAccountNumber("ACC123")).thenReturn(List.of(debitCard()));
        when(creditCardRepository.findByAccountNumber("ACC123")).thenReturn(List.of());
        when(auditRepository.findFirstByAccountNumberAndCardTypeAndCardIdOrderByCreatedAtDesc(
                "ACC123", "debit", 17L)).thenReturn(Optional.empty());

        Object cardsValue = service.getCustomerCards("ACC123").get("cards");
        assertThat(cardsValue).isInstanceOf(List.class);
        Map<?, ?> card = (Map<?, ?>) ((List<?>) cardsValue).get(0);

        assertThat(card.get("maskedNumber")).isEqualTo("•••• •••• •••• 1234");
        assertThat(card.containsKey("cardNumber")).isFalse();
        assertThat(card.containsKey("cvv")).isFalse();
        assertThat(card.containsKey("pin")).isFalse();
    }

    @Test
    void wrongEmailCannotSignInWithCardNumber() {
        when(cardRepository.findByCardNumber("4111111111111234")).thenReturn(debitCard());
        when(userRepository.findByAccountNumber("ACC123")).thenReturn(Optional.of(approvedUser()));
        assertThatThrownBy(() -> service.login("4111111111111234", "wrong@neobank.test"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unable to sign in. Check your details or contact the bank.");
    }

    @Test
    void cardTransactionsAreLimitedToTheOwnedCreditCard() {
        Card360Access access = new Card360Access();
        access.setAccountNumber("ACC123");
        access.setEnabled(true);
        CreditCard card = new CreditCard();
        card.setId(29L);
        card.setAccountNumber("ACC123");
        CreditCardTransaction tx = new CreditCardTransaction();
        tx.setCreditCardId(29L);
        tx.setAmount(25.0);
        tx.setDescription("Card purchase");
        when(accessRepository.findByAccountNumber("ACC123")).thenReturn(Optional.of(access));
        when(userRepository.findByAccountNumber("ACC123")).thenReturn(Optional.of(approvedUser()));
        when(creditCardRepository.findById(29L)).thenReturn(Optional.of(card));
        when(creditTransactionRepository.findByCreditCardId(29L)).thenReturn(List.of(tx));

        List<Map<String, Object>> result = service.getCardTransactions("ACC123", "credit", 29L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).get("description")).isEqualTo("Card purchase");
        org.mockito.Mockito.verify(creditTransactionRepository).findByCreditCardId(29L);
    }

    @Test
    void cardTransactionsRejectCardsOwnedByAnotherAccount() {
        Card360Access access = new Card360Access();
        access.setAccountNumber("ACC123");
        access.setEnabled(true);
        CreditCard card = new CreditCard();
        card.setId(29L);
        card.setAccountNumber("OTHER");
        when(accessRepository.findByAccountNumber("ACC123")).thenReturn(Optional.of(access));
        when(userRepository.findByAccountNumber("ACC123")).thenReturn(Optional.of(approvedUser()));
        when(creditCardRepository.findById(29L)).thenReturn(Optional.of(card));

        assertThatThrownBy(() -> service.getCardTransactions("ACC123", "credit", 29L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Card not found");
    }

    @Test
    void debitCardTransactionsDoNotIncludeAccountWideActivity() {
        Card360Access access = new Card360Access();
        access.setAccountNumber("ACC123");
        access.setEnabled(true);
        when(accessRepository.findByAccountNumber("ACC123")).thenReturn(Optional.of(access));
        when(userRepository.findByAccountNumber("ACC123")).thenReturn(Optional.of(approvedUser()));
        when(cardRepository.findById(17L)).thenReturn(Optional.of(debitCard()));

        assertThat(service.getCardTransactions("ACC123", "debit", 17L)).isEmpty();
    }

    @Test
    void billPaymentUsesPrimarySavingsAccountForApprovedCustomer() {
        Card360Access access = new Card360Access();
        access.setAccountNumber("ACC123");
        access.setEnabled(true);
        when(accessRepository.findByAccountNumber("ACC123")).thenReturn(Optional.of(access));
        when(userRepository.findByAccountNumber("ACC123")).thenReturn(Optional.of(approvedUser()));
        when(creditCardService.payCards360Bill("ACC123", 29L, 125.0))
                .thenReturn(Map.of("success", true, "paidAmount", 125.0, "remainingOutstanding", 0.0));

        Map<String, Object> result = service.payCreditCardBill("ACC123", 29L, 125.0);

        assertThat(result.get("success")).isEqualTo(true);
        org.mockito.Mockito.verify(creditCardService).payCards360Bill("ACC123", 29L, 125.0);
    }

    @Test
    void adminAccountLookupReturnsCustomerEmailAndMaskedCardsOnly() {
        Admin admin = new Admin();
        admin.setEmail("admin@neobank.test");
        admin.setRole("ADMIN");
        when(adminService.login("admin@neobank.test", "admin-password")).thenReturn(admin);
        when(userRepository.findByAccountNumber("ACC123")).thenReturn(Optional.of(approvedUser()));
        when(cardRepository.findByAccountNumber("ACC123")).thenReturn(List.of(debitCard()));
        when(creditCardRepository.findByAccountNumber("ACC123")).thenReturn(List.of());
        when(accessRepository.findByAccountNumber("ACC123")).thenReturn(Optional.empty());

        Map<String, Object> result = service.getAdminCustomerDetails("ACC123", "admin@neobank.test", "admin-password");
        Map<?, ?> card = (Map<?, ?>) ((List<?>) result.get("cards")).get(0);

        assertThat(result.get("email")).isEqualTo("customer@neobank.test");
        assertThat(result.get("enabled")).isEqualTo(false);
        assertThat(card.get("maskedNumber")).isEqualTo("•••• •••• •••• 1234");
        assertThat(card.containsKey("cardNumber")).isFalse();
        assertThat(card.containsKey("cvv")).isFalse();
        assertThat(card.containsKey("pin")).isFalse();
    }

    private User approvedUser() {
        User user = new User();
        user.setId(123L);
        user.setEmail("customer@neobank.test");
        user.setAccountNumber("ACC123");
        user.setStatus("APPROVED");
        user.setUsername("NeoBank Customer");
        return user;
    }

    @NonNull
    private Card360Access savedAccess() {
        for (org.mockito.invocation.Invocation invocation :
                org.mockito.Mockito.mockingDetails(accessRepository).getInvocations()) {
            if ("save".equals(invocation.getMethod().getName())) {
                Object[] arguments = invocation.getRawArguments();
                if (arguments.length > 0 && arguments[0] instanceof Card360Access access) {
                    return access;
                }
            }
        }
        throw new AssertionError("Expected the generated Cards360 access record to be saved");
    }

    private Card debitCard() {
        Card card = new Card();
        card.setId(17L);
        card.setAccountNumber("ACC123");
        card.setUserEmail("customer@neobank.test");
        card.setCardNumber("4111111111111234");
        card.setCvv("123");
        card.setPin("1234");
        card.setCardType("Visa Debit");
        card.setExpiryDate("12/29");
        card.setStatus("Active");
        return card;
    }
}
