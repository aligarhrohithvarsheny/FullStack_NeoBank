package com.neo.springapp.service;

import com.neo.springapp.model.Account;
import com.neo.springapp.model.PgMerchant;
import com.neo.springapp.model.PgPaymentLink;
import com.neo.springapp.model.PgSettlementLedger;
import com.neo.springapp.model.PgTransaction;
import com.neo.springapp.model.SavingsUpiTransaction;
import com.neo.springapp.model.User;
import com.neo.springapp.repository.AccountRepository;
import com.neo.springapp.repository.CurrentAccountRepository;
import com.neo.springapp.repository.PgMerchantRepository;
import com.neo.springapp.repository.PgPaymentLinkRepository;
import com.neo.springapp.repository.PgSettlementLedgerRepository;
import com.neo.springapp.repository.PgTransactionRepository;
import com.neo.springapp.repository.SalaryAccountRepository;
import com.neo.springapp.repository.SavingsUpiTransactionRepository;
import com.neo.springapp.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

class SavingsUpiFailedAttemptTest {

    @Test
    void incorrectPinIsRecordedAsFailedAttemptWithoutPersistingPin() {
        UserRepository userRepository = mock(UserRepository.class);
        SavingsUpiTransactionRepository transactionRepository = mock(SavingsUpiTransactionRepository.class);
        SavingsUpiService service = new SavingsUpiService();
        ReflectionTestUtils.setField(service, "userRepository", userRepository);
        ReflectionTestUtils.setField(service, "savingsUpiTxnRepo", transactionRepository);

        User sender = new User("sender", "password", "sender@example.com");
        sender.setAccountNumber("ACC-100");
        sender.setUpiId("sender@neobank");
        sender.setUpiEnabled(true);
        sender.setTransactionPinSet(true);
        sender.setTransactionPin(new BCryptPasswordEncoder(10).encode("123456"));
        Account account = new Account();
        account.setName("Sender Name");
        sender.setAccount(account);
        when(userRepository.findByAccountNumber("ACC-100")).thenReturn(Optional.of(sender));

        Map<String, Object> response = service.sendMoney(
                "ACC-100", "recipient@neobank", new BigDecimal("125.50"), "wrong-pin", "Test payment",
                "203.0.113.8", "Mozilla/5.0 test browser");

        assertThat(response).containsEntry("success", false).containsEntry("error", "Incorrect UPI PIN");
        ArgumentCaptor<SavingsUpiTransaction> savedAttempt = ArgumentCaptor.forClass(SavingsUpiTransaction.class);
        verify(transactionRepository).save(savedAttempt.capture());
        assertThat(savedAttempt.getValue().getStatus()).isEqualTo("FAILED");
        assertThat(savedAttempt.getValue().getSenderAccount()).isEqualTo("ACC-100");
        assertThat(savedAttempt.getValue().getReceiverUpiId()).isEqualTo("recipient@neobank");
        assertThat(savedAttempt.getValue().getAmount()).isEqualByComparingTo("125.50");
        assertThat(savedAttempt.getValue().getRemark()).isEqualTo("Failed: Incorrect UPI PIN");
        assertThat(savedAttempt.getValue().getRemark()).doesNotContain("wrong-pin");
        assertThat(savedAttempt.getValue().getSourceIp()).isEqualTo("203.0.113.8");
        assertThat(savedAttempt.getValue().getUserAgent()).isEqualTo("Mozilla/5.0 test browser");
        assertThat(savedAttempt.getValue().getSenderDebited()).isFalse();
        assertThat(savedAttempt.getValue().getReceiverCredited()).isFalse();
    }

    @Test
    void attemptForUnknownSenderIsNotPersisted() {
        UserRepository userRepository = mock(UserRepository.class);
        SavingsUpiTransactionRepository transactionRepository = mock(SavingsUpiTransactionRepository.class);
        SavingsUpiService service = new SavingsUpiService();
        ReflectionTestUtils.setField(service, "userRepository", userRepository);
        ReflectionTestUtils.setField(service, "savingsUpiTxnRepo", transactionRepository);
        when(userRepository.findByAccountNumber("UNKNOWN")).thenReturn(Optional.empty());

        Map<String, Object> response = service.sendMoney(
                "UNKNOWN", "recipient@neobank", new BigDecimal("125.50"), "wrong-pin", "Test payment");

        assertThat(response).containsEntry("success", false).containsEntry("error", "Sender not found");
        verify(transactionRepository, org.mockito.Mockito.never()).save(any(SavingsUpiTransaction.class));
    }

    @Test
    void successfulTransferRecordsBothSettlementSidesAndRequestMetadata() {
        UserRepository userRepository = mock(UserRepository.class);
        AccountRepository accountRepository = mock(AccountRepository.class);
        SavingsUpiTransactionRepository transactionRepository = mock(SavingsUpiTransactionRepository.class);
        SavingsUpiService service = new SavingsUpiService();
        ReflectionTestUtils.setField(service, "userRepository", userRepository);
        ReflectionTestUtils.setField(service, "accountRepository", accountRepository);
        ReflectionTestUtils.setField(service, "savingsUpiTxnRepo", transactionRepository);
        ReflectionTestUtils.setField(service, "salaryAccountRepository", mock(SalaryAccountRepository.class));
        ReflectionTestUtils.setField(service, "currentAccountRepository", mock(CurrentAccountRepository.class));
        ReflectionTestUtils.setField(service, "emergencyControlService", mock(EmergencyControlService.class));

        User sender = createEnabledUser("ACC-100", "sender@neobank", "Sender", 500.0);
        sender.setTransactionPin(new BCryptPasswordEncoder(10).encode("123456"));
        User receiver = createEnabledUser("ACC-200", "recipient@neobank", "Recipient", 25.0);
        when(userRepository.findByAccountNumber("ACC-100")).thenReturn(Optional.of(sender));
        when(userRepository.findByAccountNumber("ACC-200")).thenReturn(Optional.of(receiver));
        when(userRepository.findByUpiId("sender@neobank")).thenReturn(Optional.of(sender));
        when(userRepository.findByUpiId("recipient@neobank")).thenReturn(Optional.of(receiver));
        when(transactionRepository.findBySenderAccountOrderByCreatedAtDesc("ACC-100")).thenReturn(List.of());

        Map<String, Object> response = service.sendMoney(
                "ACC-100", "recipient@neobank", new BigDecimal("125.50"), "123456", "Test payment",
                "203.0.113.8", "Mozilla/5.0 test browser", 37.422, -122.084, 12.5);

        assertThat(response).containsEntry("success", true);
        assertThat(sender.getAccount().getBalance()).isEqualTo(374.5);
        assertThat(receiver.getAccount().getBalance()).isEqualTo(150.5);
        ArgumentCaptor<SavingsUpiTransaction> savedTransaction =
                ArgumentCaptor.forClass(SavingsUpiTransaction.class);
        verify(transactionRepository).save(savedTransaction.capture());
        assertThat(savedTransaction.getValue().getStatus()).isEqualTo("SUCCESS");
        assertThat(savedTransaction.getValue().getSenderDebited()).isTrue();
        assertThat(savedTransaction.getValue().getReceiverCredited()).isTrue();
        assertThat(savedTransaction.getValue().getSourceIp()).isEqualTo("203.0.113.8");
        assertThat(savedTransaction.getValue().getUserAgent()).isEqualTo("Mozilla/5.0 test browser");
        assertThat(savedTransaction.getValue().getLocationLatitude()).isEqualTo(37.422);
        assertThat(savedTransaction.getValue().getLocationLongitude()).isEqualTo(-122.084);
        assertThat(savedTransaction.getValue().getLocationAccuracyMeters()).isEqualTo(12.5);

        when(transactionRepository.findAllOrderByCreatedAtDesc()).thenReturn(List.of(savedTransaction.getValue()));
        Map<String, Object> adminDetails = service.adminListAllTransactions().get(0);
        assertThat(adminDetails).containsEntry("sourceIp", "203.0.113.8")
                .containsEntry("location", "37.42200, -122.08400")
                .containsEntry("senderDebited", true)
                .containsEntry("receiverCredited", true);
        @SuppressWarnings("unchecked")
        Map<String, Object> senderProfile = (Map<String, Object>) adminDetails.get("senderProfile");
        assertThat(senderProfile).containsEntry("phone", "555-0100");
        assertThat(savedTransaction.getValue().getSenderPhone()).isEqualTo("555-0100");
    }

    @Test
    void paidPaymentLinksAppearInAdminUpiTransactionsWithSettlementDetails() {
        SavingsUpiService service = new SavingsUpiService();
        SavingsUpiTransactionRepository upiTransactions = mock(SavingsUpiTransactionRepository.class);
        PgPaymentLinkRepository paymentLinks = mock(PgPaymentLinkRepository.class);
        PgMerchantRepository merchants = mock(PgMerchantRepository.class);
        PgTransactionRepository pgTransactions = mock(PgTransactionRepository.class);
        PgSettlementLedgerRepository settlementLedger = mock(PgSettlementLedgerRepository.class);
        ReflectionTestUtils.setField(service, "savingsUpiTxnRepo", upiTransactions);
        ReflectionTestUtils.setField(service, "paymentLinkRepository", paymentLinks);
        ReflectionTestUtils.setField(service, "merchantRepository", merchants);
        ReflectionTestUtils.setField(service, "pgTransactionRepository", pgTransactions);
        ReflectionTestUtils.setField(service, "settlementLedgerRepository", settlementLedger);
        UserRepository userRepository = mock(UserRepository.class);
        CurrentAccountRepository currentAccounts = mock(CurrentAccountRepository.class);
        ReflectionTestUtils.setField(service, "userRepository", userRepository);
        ReflectionTestUtils.setField(service, "salaryAccountRepository", mock(SalaryAccountRepository.class));
        ReflectionTestUtils.setField(service, "currentAccountRepository", currentAccounts);
        when(userRepository.findByUpiId(any())).thenReturn(Optional.empty());
        when(userRepository.findByAccountNumber(any())).thenReturn(Optional.empty());
        when(currentAccounts.findByAccountNumber(any())).thenReturn(Optional.empty());
        when(currentAccounts.findByUpiId(any())).thenReturn(Optional.empty());
        when(upiTransactions.findAllOrderByCreatedAtDesc()).thenReturn(List.of());

        LocalDateTime paidAt = LocalDateTime.of(2026, 11, 4, 10, 30);
        PgPaymentLink link = new PgPaymentLink();
        link.setId(42L);
        link.setLinkId("LNK-42");
        link.setTxnRef("PLK-42");
        link.setOrderId("ORDER-42");
        link.setMerchantId("MERCHANT-7");
        link.setMerchantName("Neo Store");
        link.setMerchantUpiId("ACC-700@ezyvault");
        link.setRecipientUpiId("payer@neobank");
        link.setRecipientName("Payer Name");
        link.setPayerAccountNumber("ACC-100");
        link.setPayerName("Payer Name");
        link.setAmount(new BigDecimal("1250.00"));
        link.setDescription("Invoice 42");
        link.setStatus("PAID");
        link.setPaidAt(paidAt);
        when(paymentLinks.findByStatusOrderByPaidAtDesc("PAID")).thenReturn(List.of(link));

        PgMerchant merchant = new PgMerchant();
        merchant.setBusinessName("Neo Store");
        merchant.setLinkedAccountNumber("ACC-700");
        when(merchants.findByMerchantId("MERCHANT-7")).thenReturn(Optional.of(merchant));

        PgTransaction pgTransaction = new PgTransaction();
        pgTransaction.setTransactionId("PGTXN-42");
        pgTransaction.setFee(new BigDecimal("25.00"));
        pgTransaction.setTax(new BigDecimal("4.50"));
        pgTransaction.setNetAmount(new BigDecimal("1220.50"));
        when(pgTransactions.findByOrderId("ORDER-42")).thenReturn(Optional.of(pgTransaction));

        PgSettlementLedger ledger = new PgSettlementLedger();
        ledger.setTransactionId("PGTXN-42");
        ledger.setCreditStatus("CREDITED");
        ledger.setNetAmount(new BigDecimal("1220.50"));
        when(settlementLedger.findByTransactionId("PGTXN-42")).thenReturn(List.of(ledger));

        List<Map<String, Object>> result = service.adminListAllTransactions();

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).containsEntry("sourceType", "PAYMENT_LINK")
                .containsEntry("transactionRef", "PLK-42")
                .containsEntry("txnId", "PGTXN-42")
                .containsEntry("senderAccount", "ACC-100")
                .containsEntry("senderUpiId", "payer@neobank")
                .containsEntry("receiverAccount", "ACC-700")
                .containsEntry("receiverUpiId", "ACC-700@ezyvault")
                .containsEntry("paymentMethod", "UPI_PAYMENT_LINK")
                .containsEntry("senderDebited", true)
                .containsEntry("receiverCredited", true)
                .containsEntry("settlementStatus", "CREDITED")
                .containsEntry("platformFee", new BigDecimal("25.00"))
                .containsEntry("netSettlementAmount", new BigDecimal("1220.50"));
    }

    private User createEnabledUser(String accountNumber, String upiId, String name, double balance) {
        User user = new User(accountNumber, "password", accountNumber + "@example.com");
        user.setAccountNumber(accountNumber);
        user.setUpiId(upiId);
        user.setUpiEnabled(true);
        user.setTransactionPinSet(true);
        Account account = new Account();
        account.setName(name);
        account.setBalance(balance);
        account.setPhone("555-0100");
        user.setAccount(account);
        return user;
    }
}
