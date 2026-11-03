package com.neo.springapp.service;

import com.neo.springapp.model.Account;
import com.neo.springapp.model.SavingsUpiTransaction;
import com.neo.springapp.model.User;
import com.neo.springapp.repository.AccountRepository;
import com.neo.springapp.repository.CurrentAccountRepository;
import com.neo.springapp.repository.SalaryAccountRepository;
import com.neo.springapp.repository.SavingsUpiTransactionRepository;
import com.neo.springapp.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
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
        when(userRepository.findByUpiId("recipient@neobank")).thenReturn(Optional.of(receiver));
        when(transactionRepository.findBySenderAccountOrderByCreatedAtDesc("ACC-100")).thenReturn(List.of());

        Map<String, Object> response = service.sendMoney(
                "ACC-100", "recipient@neobank", new BigDecimal("125.50"), "123456", "Test payment",
                "203.0.113.8", "Mozilla/5.0 test browser");

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
