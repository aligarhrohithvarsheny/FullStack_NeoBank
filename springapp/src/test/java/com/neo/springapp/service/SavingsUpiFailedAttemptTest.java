package com.neo.springapp.service;

import com.neo.springapp.model.Account;
import com.neo.springapp.model.SavingsUpiTransaction;
import com.neo.springapp.model.User;
import com.neo.springapp.repository.SavingsUpiTransactionRepository;
import com.neo.springapp.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
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
                "ACC-100", "recipient@neobank", new BigDecimal("125.50"), "wrong-pin", "Test payment");

        assertThat(response).containsEntry("success", false).containsEntry("error", "Incorrect UPI PIN");
        ArgumentCaptor<SavingsUpiTransaction> savedAttempt = ArgumentCaptor.forClass(SavingsUpiTransaction.class);
        verify(transactionRepository).save(savedAttempt.capture());
        assertThat(savedAttempt.getValue().getStatus()).isEqualTo("FAILED");
        assertThat(savedAttempt.getValue().getSenderAccount()).isEqualTo("ACC-100");
        assertThat(savedAttempt.getValue().getReceiverUpiId()).isEqualTo("recipient@neobank");
        assertThat(savedAttempt.getValue().getAmount()).isEqualByComparingTo("125.50");
        assertThat(savedAttempt.getValue().getRemark()).isEqualTo("Failed: Incorrect UPI PIN");
        assertThat(savedAttempt.getValue().getRemark()).doesNotContain("wrong-pin");
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
}
