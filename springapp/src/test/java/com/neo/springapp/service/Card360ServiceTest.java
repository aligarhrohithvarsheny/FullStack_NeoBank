package com.neo.springapp.service;

import com.neo.springapp.model.Admin;
import com.neo.springapp.model.Card;
import com.neo.springapp.model.Card360Access;
import com.neo.springapp.model.Card360ClosureRequest;
import com.neo.springapp.model.CreditCard;
import com.neo.springapp.model.CreditCardTransaction;
import com.neo.springapp.model.CreditCardRequest;
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
    @Mock private Card360ClosureRequestRepository closureRequestRepository;
    @Mock private UserRepository userRepository;
    @Mock private CreditCardRequestRepository creditCardRequestRepository;
    @Mock private AdminService adminService;
    @Mock private CreditCardService creditCardService;

    private UserSessionTokenService tokenService;
    private Card360Service service;

    @BeforeEach
    void setUp() {
        tokenService = new UserSessionTokenService("test-secret-with-at-least-32-characters");
        service = new Card360Service(accessRepository, auditRepository, cardRepository, creditCardRepository,
                creditTransactionRepository, closureRequestRepository, userRepository, creditCardRequestRepository,
                adminService, tokenService, creditCardService);
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
    void fullCreditCardNumberIsReturnedOnlyFromOwnerScopedReveal() {
        Card360Access access = new Card360Access();
        access.setAccountNumber("ACC123");
        access.setEnabled(true);
        CreditCard card = new CreditCard();
        card.setId(29L);
        card.setAccountNumber("ACC123");
        card.setCardNumber("5555444433332222");
        when(accessRepository.findByAccountNumber("ACC123")).thenReturn(Optional.of(access));
        when(userRepository.findByAccountNumber("ACC123")).thenReturn(Optional.of(approvedUser()));
        when(creditCardRepository.findById(29L)).thenReturn(Optional.of(card));

        assertThat(service.revealCreditCardNumber("ACC123", 29L))
                .containsEntry("fullCardNumber", "5555444433332222");
        card.setAccountNumber("OTHER");
        assertThatThrownBy(() -> service.revealCreditCardNumber("ACC123", 29L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Card not found");
    }

    @Test
    void customerCanSubmitOnePendingClosureRequestForOwnedCreditCard() {
        Card360Access access = new Card360Access();
        access.setAccountNumber("ACC123");
        access.setEnabled(true);
        CreditCard card = new CreditCard();
        card.setId(29L);
        card.setAccountNumber("ACC123");
        card.setCardNumber("5555444433332222");
        card.setUserName("NeoBank Customer");
        when(accessRepository.findByAccountNumber("ACC123")).thenReturn(Optional.of(access));
        when(userRepository.findByAccountNumber("ACC123")).thenReturn(Optional.of(approvedUser()));
        when(creditCardRepository.findById(29L)).thenReturn(Optional.of(card));
        when(closureRequestRepository.findFirstByCreditCardIdAndStatusOrderByRequestedAtDesc(29L, "Pending"))
                .thenReturn(Optional.empty());
        org.mockito.Mockito.when(closureRequestRepository.save(
                org.mockito.ArgumentMatchers.any(Card360ClosureRequest.class))).thenAnswer(invocation -> {
                    Card360ClosureRequest request = invocation.getArgument(0);
                    request.setId(51L);
                    return request;
                });

        Map<String, Object> result = service.requestCreditCardClosure("ACC123", 29L, "No longer needed");

        assertThat(result.get("id")).isEqualTo(51L);
        assertThat(result.get("status")).isEqualTo("Pending");
        assertThat(result.get("maskedCardNumber")).isEqualTo("•••• •••• •••• 2222");
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
    void customerCanUpdateOnlySupportedControlsForOwnedCreditCard() {
        Card360Access access = new Card360Access();
        access.setAccountNumber("ACC123");
        access.setEnabled(true);
        CreditCard card = new CreditCard();
        card.setId(29L);
        card.setAccountNumber("ACC123");
        when(accessRepository.findByAccountNumber("ACC123")).thenReturn(Optional.of(access));
        when(userRepository.findByAccountNumber("ACC123")).thenReturn(Optional.of(approvedUser()));
        when(creditCardRepository.findById(29L)).thenReturn(Optional.of(card));

        Map<String, Boolean> result = service.updateCreditCardControls(
                "ACC123", 29L, Map.of("onlineTransactionsEnabled", false));

        assertThat(result).containsEntry("onlineTransactionsEnabled", false);
        assertThat(card.isOnlineTransactionsEnabled()).isFalse();
        org.mockito.Mockito.verify(creditCardRepository).save(card);
    }

    @Test
    void emergencyLockDisablesAllCreditCardChannelsAndBlocksCard() {
        Card360Access access = new Card360Access();
        access.setAccountNumber("ACC123");
        access.setEnabled(true);
        CreditCard card = new CreditCard();
        card.setId(29L);
        card.setAccountNumber("ACC123");
        when(accessRepository.findByAccountNumber("ACC123")).thenReturn(Optional.of(access));
        when(userRepository.findByAccountNumber("ACC123")).thenReturn(Optional.of(approvedUser()));
        when(creditCardRepository.findById(29L)).thenReturn(Optional.of(card));

        Map<String, Object> result = service.emergencyLockCreditCard("ACC123", 29L);

        assertThat(result.get("status")).isEqualTo("Blocked");
        assertThat(card.isBlocked()).isTrue();
        assertThat(card.isOnlineTransactionsEnabled()).isFalse();
        assertThat(card.isPosTransactionsEnabled()).isFalse();
        assertThat(card.isContactlessEnabled()).isFalse();
        assertThat(card.isAtmWithdrawalsEnabled()).isFalse();
        assertThat(card.isInternationalTransactionsEnabled()).isFalse();
    }

    @Test
    void emergencyLockCanBeCustomerUnlockedButDeactivatedLostCardCannot() {
        Card360Access access = new Card360Access();
        access.setAccountNumber("ACC123");
        access.setEnabled(true);
        CreditCard card = new CreditCard();
        card.setId(29L);
        card.setAccountNumber("ACC123");
        card.setBlocked(true);
        card.setDeactivated(true);
        com.neo.springapp.model.Card360Audit audit = new com.neo.springapp.model.Card360Audit();
        audit.setAction("EMERGENCY_CARD_LOCK");
        audit.setChangedBy("CUSTOMER:ACC123");
        when(accessRepository.findByAccountNumber("ACC123")).thenReturn(Optional.of(access));
        when(userRepository.findByAccountNumber("ACC123")).thenReturn(Optional.of(approvedUser()));
        when(auditRepository.findFirstByAccountNumberAndCardTypeAndCardIdOrderByCreatedAtDesc(
                "ACC123", "credit", 29L)).thenReturn(Optional.of(audit));
        when(creditCardRepository.findById(29L)).thenReturn(Optional.of(card));

        assertThatThrownBy(() -> service.updateCardStatus("ACC123", "credit", 29L, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Contact NeoBank to unblock a deactivated card");
    }

    @Test
    void card360PinChangeUsesExistingEncryptedPinServiceAndOwnerCheck() {
        Card360Access access = new Card360Access();
        access.setAccountNumber("ACC123");
        access.setEnabled(true);
        CreditCard card = new CreditCard();
        card.setId(29L);
        card.setAccountNumber("ACC123");
        when(accessRepository.findByAccountNumber("ACC123")).thenReturn(Optional.of(access));
        when(userRepository.findByAccountNumber("ACC123")).thenReturn(Optional.of(approvedUser()));
        when(creditCardRepository.findById(29L)).thenReturn(Optional.of(card));
        when(creditCardService.setPin(29L, "1234")).thenReturn(true);

        service.setCreditCardPin("ACC123", 29L, "1234");

        org.mockito.Mockito.verify(creditCardService).setPin(29L, "1234");
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

    @Test
    void externalCustomerCanSignInAndOnlySeesTheirMaskedIssuedCard() {
        CreditCardRequest guest = new CreditCardRequest();
        guest.setId(42L);
        guest.setApplicationId("CC2026ABCDEF1234");
        guest.setUserName("External Customer");
        guest.setUserEmail("guest@example.com");
        guest.setGuestCardNumber("4111111111111234");
        guest.setGuestCardExpiry("12/29");
        guest.setLast4("1234");
        guest.setStatus("Approved");
        guest.setIssuedCardStatus("Active");

        Card360Access access = new Card360Access();
        access.setAccountNumber("GUEST_CARD:42");
        access.setEnabled(true);
        when(cardRepository.findByCardNumber("4111111111111234")).thenReturn(null);
        when(creditCardRepository.findByCardNumber("4111111111111234")).thenReturn(Optional.empty());
        when(creditCardRequestRepository.findByGuestCardNumber("4111111111111234")).thenReturn(guest);
        when(accessRepository.findByAccountNumber("GUEST_CARD:42")).thenReturn(Optional.of(access));
        when(creditCardRequestRepository.findById(42L)).thenReturn(Optional.of(guest));

        Map<String, Object> login = service.login("4111 1111 1111 1234", "GUEST@example.com");
        UserSessionTokenService.SessionPrincipal principal =
                tokenService.verify((String) login.get("token"));
        assertThat(login.get("externalCustomer")).isEqualTo(true);
        assertThat(principal).isNotNull();
        assertThat(principal.accountNumber()).isEqualTo("GUEST_CARD:42");

        Map<?, ?> card = (Map<?, ?>) ((List<?>) service.getCustomerCards("GUEST_CARD:42").get("cards")).get(0);
        assertThat(card.get("maskedNumber")).isEqualTo("•••• •••• •••• 1234");
        assertThat(card.get("cardholderName")).isEqualTo("External Customer");
        assertThat(card.containsKey("cardNumber")).isFalse();
        assertThat(card.containsKey("cvv")).isFalse();
    }

    @Test
    void adminCanProvisionExternalCards360AccessByApplicationId() {
        Admin admin = new Admin();
        admin.setEmail("admin@neobank.test");
        admin.setRole("ADMIN");
        CreditCardRequest guest = new CreditCardRequest();
        guest.setId(42L);
        guest.setApplicationId("CC2026ABCDEF1234");
        guest.setUserName("External Customer");
        guest.setUserEmail("guest@example.com");
        guest.setGuestCardNumber("4111111111111234");
        guest.setGuestCardExpiry("12/29");
        guest.setLast4("1234");
        guest.setStatus("Approved");
        guest.setIssuedCardStatus("Active");
        when(adminService.login("admin@neobank.test", "admin-password")).thenReturn(admin);
        when(userRepository.findByAccountNumber("CC2026ABCDEF1234")).thenReturn(Optional.empty());
        when(creditCardRequestRepository.findByApplicationId("CC2026ABCDEF1234")).thenReturn(guest);
        when(accessRepository.findByAccountNumber("GUEST_CARD:42")).thenReturn(Optional.empty());

        Map<String, Object> response = service.generatePasscode(
                "CC2026ABCDEF1234", "admin@neobank.test", "admin-password");

        assertThat(response.get("externalCustomer")).isEqualTo(true);
        assertThat(response.get("email")).isEqualTo("guest@example.com");
        assertThat(savedAccess().getAccountNumber()).isEqualTo("GUEST_CARD:42");
        assertThat(savedAccess().isEnabled()).isTrue();
    }

    @Test
    void adminCanLookUpAnExternalCardByRegisteredEmailWithoutAadhaar() {
        Admin admin = new Admin();
        admin.setEmail("admin@neobank.test");
        admin.setRole("ADMIN");
        CreditCardRequest guest = new CreditCardRequest();
        guest.setId(42L);
        guest.setApplicationId("CC2026ABCDEF1234");
        guest.setUserName("External Customer");
        guest.setUserEmail("guest@example.com");
        guest.setGuestCardNumber("4111111111111234");
        guest.setGuestCardExpiry("12/29");
        guest.setLast4("1234");
        guest.setStatus("Approved");
        guest.setIssuedCardStatus("Active");
        when(adminService.login("admin@neobank.test", "admin-password")).thenReturn(admin);
        when(userRepository.findByAccountNumber("guest@example.com")).thenReturn(Optional.empty());
        when(creditCardRequestRepository.findByApplicationId("guest@example.com")).thenReturn(null);
        when(creditCardRequestRepository.findByUserEmailIgnoreCaseAndAccountNumberIsNullOrderByRequestDateDesc(
                "guest@example.com")).thenReturn(List.of(guest));
        when(accessRepository.findByAccountNumber("GUEST_CARD:42")).thenReturn(Optional.empty());

        Map<String, Object> result = service.getAdminCustomerDetails(
                "guest@example.com", "admin@neobank.test", "admin-password");
        Map<?, ?> card = (Map<?, ?>) ((List<?>) result.get("cards")).get(0);

        assertThat(result.get("customerType")).isEqualTo("EXTERNAL");
        assertThat(result.get("applicationId")).isEqualTo("CC2026ABCDEF1234");
        assertThat(card.get("maskedNumber")).isEqualTo("•••• •••• •••• 1234");
        assertThat(card.containsKey("cardNumber")).isFalse();
        assertThat(card.containsKey("aadhaarNumber")).isFalse();
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
