package com.neo.springapp.service;

import com.neo.springapp.model.Account;
import com.neo.springapp.model.CustomerActivationCode;
import com.neo.springapp.model.User;
import com.neo.springapp.repository.AccountRepository;
import com.neo.springapp.repository.CurrentAccountRepository;
import com.neo.springapp.repository.CustomerActivationCodeRepository;
import com.neo.springapp.repository.SalaryAccountRepository;
import com.neo.springapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomerAccessServiceTest {
    private AccountRepository accountRepository;
    private CurrentAccountRepository currentAccountRepository;
    private CustomerActivationCodeRepository activationCodeRepository;
    private UserRepository userRepository;
    private CustomerAccessService service;

    @BeforeEach
    void setUp() {
        accountRepository = mock(AccountRepository.class);
        currentAccountRepository = mock(CurrentAccountRepository.class);
        activationCodeRepository = mock(CustomerActivationCodeRepository.class);
        SalaryAccountRepository salaryAccountRepository = mock(SalaryAccountRepository.class);
        userRepository = mock(UserRepository.class);
        service = new CustomerAccessService(
                accountRepository,
                currentAccountRepository,
                salaryAccountRepository,
                userRepository,
                activationCodeRepository,
                mock(CurrentAccountService.class),
                mock(SalaryAccountService.class),
                new PasswordService(),
                new UserSessionTokenService("test-secret-with-at-least-32-characters"));

        Account account = new Account();
        account.setCustomerId("123456789");
        account.setStatus("ACTIVE");
        account.setAadharNumber("123456789012");
        User user = new User();
        user.setEmail("customer@example.com");
        user.setStatus("APPROVED");
        user.setPasswordSet(false);
        user.setAccount(account);

        when(accountRepository.findByCustomerId("123456789")).thenReturn(account);
        when(userRepository.findByAccount_CustomerId("123456789")).thenReturn(Optional.of(user));
        when(currentAccountRepository.findByCustomerId("123456789")).thenReturn(Optional.empty());
        when(activationCodeRepository.findForUpdate("123456789")).thenReturn(Optional.empty());
        when(activationCodeRepository.save(any(CustomerActivationCode.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void issuesHashedCodeAndCountsIncorrectVerificationAttempts() {
        CustomerAccessService.IssuedCode issued =
                service.issueActivationCode("SAVINGS", "123456789", "admin@example.com");

        ArgumentCaptor<CustomerActivationCode> savedCode = ArgumentCaptor.forClass(CustomerActivationCode.class);
        verify(activationCodeRepository).save(savedCode.capture());
        CustomerActivationCode activation = savedCode.getValue();
        assertEquals(16, issued.activationCode().length());
        assertNotEquals(issued.activationCode(), activation.getCodeHash());
        when(activationCodeRepository.findForUpdate("123456789")).thenReturn(Optional.of(activation));
        assertThrows(IllegalArgumentException.class,
                () -> service.verifyActivationCode("123456789", "incorrect-code"));
        assertEquals(1, activation.getFailedAttempts());

        assertDoesNotThrow(() -> service.verifyActivationCode("123456789", issued.activationCode()));
    }
}
