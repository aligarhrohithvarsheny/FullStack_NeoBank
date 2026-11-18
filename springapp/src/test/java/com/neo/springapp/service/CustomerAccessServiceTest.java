package com.neo.springapp.service;

import com.neo.springapp.model.Account;
import com.neo.springapp.model.CustomerActivationCode;
import com.neo.springapp.model.CustomerActivationCodeHistory;
import com.neo.springapp.model.SalaryAccount;
import com.neo.springapp.model.User;
import com.neo.springapp.repository.AccountRepository;
import com.neo.springapp.repository.CurrentAccountRepository;
import com.neo.springapp.repository.CustomerActivationCodeRepository;
import com.neo.springapp.repository.CustomerActivationCodeHistoryRepository;
import com.neo.springapp.repository.SalaryAccountRepository;
import com.neo.springapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomerAccessServiceTest {
    private AccountRepository accountRepository;
    private CurrentAccountRepository currentAccountRepository;
    private CustomerActivationCodeRepository activationCodeRepository;
    private CustomerActivationCodeHistoryRepository activationCodeHistoryRepository;
    private SalaryAccountRepository salaryAccountRepository;
    private UserRepository userRepository;
    private Account savingsAccount;
    private User savingsUser;
    private CustomerAccessService service;

    @BeforeEach
    void setUp() {
        accountRepository = mock(AccountRepository.class);
        currentAccountRepository = mock(CurrentAccountRepository.class);
        activationCodeRepository = mock(CustomerActivationCodeRepository.class);
        activationCodeHistoryRepository = mock(CustomerActivationCodeHistoryRepository.class);
        salaryAccountRepository = mock(SalaryAccountRepository.class);
        userRepository = mock(UserRepository.class);
        service = new CustomerAccessService(
                accountRepository,
                currentAccountRepository,
                salaryAccountRepository,
                userRepository,
                activationCodeRepository,
                activationCodeHistoryRepository,
                mock(CurrentAccountService.class),
                mock(SalaryAccountService.class),
                new PasswordService(),
                new UserSessionTokenService("test-secret-with-at-least-32-characters"));

        savingsAccount = new Account();
        savingsAccount.setCustomerId("123456789");
        savingsAccount.setStatus("ACTIVE");
        savingsAccount.setAadharNumber("123456789012");
        savingsUser = new User();
        savingsUser.setEmail("customer@example.com");
        savingsUser.setStatus("APPROVED");
        savingsUser.setPasswordSet(false);
        savingsUser.setAccount(savingsAccount);

        when(accountRepository.findByCustomerId("123456789")).thenReturn(savingsAccount);
        when(userRepository.findByAccount_CustomerId("123456789")).thenReturn(Optional.of(savingsUser));
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
        assertTrue(issued.expiresAt().isAfter(LocalDateTime.now().plusDays(6)));
        assertTrue(issued.expiresAt().isBefore(LocalDateTime.now().plusDays(7).plusSeconds(10)));
        assertNotEquals(issued.activationCode(), activation.getCodeHash());
        when(activationCodeRepository.findForUpdate("123456789")).thenReturn(Optional.of(activation));
        assertThrows(IllegalArgumentException.class,
                () -> service.verifyActivationCode("123456789", "incorrect-code"));
        assertEquals(1, activation.getFailedAttempts());

        assertDoesNotThrow(() -> service.verifyActivationCode("123456789", issued.activationCode()));
    }

    @Test
    void ignoresConvertedSavingsRecordWhenIssuingForActiveSalaryAccount() {
        Account convertedSavings = new Account();
        convertedSavings.setCustomerId("CUST12345");
        convertedSavings.setStatus("CONVERTED_TO_SALARY");
        convertedSavings.setAadharNumber("123456789012");
        User savingsUser = new User();
        savingsUser.setEmail("customer@example.com");
        savingsUser.setStatus("APPROVED");
        savingsUser.setAccount(convertedSavings);
        when(accountRepository.findByCustomerId("CUST12345")).thenReturn(convertedSavings);
        when(userRepository.findByAccount_CustomerId("CUST12345")).thenReturn(Optional.of(savingsUser));

        SalaryAccount activeSalary = new SalaryAccount();
        activeSalary.setCustomerId("CUST12345");
        activeSalary.setStatus("Active");
        activeSalary.setAadharNumber("123456789012");
        activeSalary.setEmail("customer@example.com");
        when(salaryAccountRepository.findByCustomerId("CUST12345")).thenReturn(activeSalary);
        when(activationCodeRepository.findForUpdate("CUST12345")).thenReturn(Optional.empty());

        CustomerAccessService.IssuedCode issued =
                service.issueActivationCode("SALARY", "CUST12345", "admin@example.com");

        assertEquals("SALARY", issued.accountType());
    }

    @Test
    void consumingCodeUpdatesHistoryAndAccessIsReportedAsAlreadyExists() {
        CustomerAccessService.IssuedCode issued =
                service.issueActivationCode("SAVINGS", "123456789", "admin@example.com");
        ArgumentCaptor<CustomerActivationCode> activationCaptor = ArgumentCaptor.forClass(CustomerActivationCode.class);
        verify(activationCodeRepository).save(activationCaptor.capture());
        CustomerActivationCode activation = activationCaptor.getValue();

        ArgumentCaptor<CustomerActivationCodeHistory> historyCaptor =
                ArgumentCaptor.forClass(CustomerActivationCodeHistory.class);
        verify(activationCodeHistoryRepository).save(historyCaptor.capture());
        CustomerActivationCodeHistory history = historyCaptor.getValue();
        when(activationCodeRepository.findForUpdate("123456789")).thenReturn(Optional.of(activation));
        when(activationCodeHistoryRepository.findFirstByCustomerIdAndAccountTypeAndStatusOrderByIssuedAtDesc(
                "123456789", "SAVINGS", "ACTIVE")).thenReturn(Optional.of(history));

        CustomerAccessService.RegistrationResult result = service.activateOnlineAccess(
                new CustomerAccessService.ActivationRequest(
                        "123456789", "customer@example.com", "123456789012",
                        issued.activationCode(), "LongPassword123"));

        assertTrue(result.success());
        assertEquals("USED", history.getStatus());
        assertNotNull(history.getUsedAt());
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.issueActivationCode("SAVINGS", "123456789", "admin@example.com"));
        assertTrue(exception.getMessage().contains("already exists"));
    }

    @Test
    void reissuingCodeMarksPreviousVoucherAsRegenerated() {
        CustomerActivationCode previousCode = new CustomerActivationCode();
        previousCode.setCustomerId("123456789");
        previousCode.setAccountType("SAVINGS");
        previousCode.setIssuedAt(LocalDateTime.now().minusHours(1));
        previousCode.setExpiresAt(LocalDateTime.now().plusDays(6));
        previousCode.setCodeHash(new PasswordService().encryptPassword("PREVIOUSCODE123"));
        CustomerActivationCodeHistory previousHistory = new CustomerActivationCodeHistory();
        previousHistory.setCustomerId("123456789");
        previousHistory.setAccountType("SAVINGS");
        previousHistory.setIssuedAt(previousCode.getIssuedAt());
        previousHistory.setExpiresAt(previousCode.getExpiresAt());
        previousHistory.setStatus("ACTIVE");
        when(activationCodeRepository.findForUpdate("123456789")).thenReturn(Optional.of(previousCode));
        when(activationCodeHistoryRepository.findFirstByCustomerIdAndStatusOrderByIssuedAtDesc(
                "123456789", "ACTIVE")).thenReturn(Optional.of(previousHistory));

        service.issueActivationCode("SAVINGS", "123456789", "admin@example.com");

        assertEquals("REGENERATED", previousHistory.getStatus());
        verify(activationCodeHistoryRepository).save(previousHistory);
    }

    @Test
    void historyRefreshMarksUnusedExpiredVoucher() {
        CustomerActivationCodeHistory expired = new CustomerActivationCodeHistory();
        expired.setCustomerId("123456789");
        expired.setAccountType("SAVINGS");
        expired.setIssuedBy("admin@example.com");
        expired.setIssuedAt(LocalDateTime.now().minusDays(8));
        expired.setExpiresAt(LocalDateTime.now().minusDays(1));
        expired.setStatus("ACTIVE");
        when(activationCodeHistoryRepository.findTop200ByOrderByIssuedAtDesc())
                .thenReturn(java.util.List.of(expired));

        java.util.List<CustomerAccessService.ActivationHistoryEntry> history =
                service.getActivationCodeHistory();

        assertEquals("EXPIRED", history.get(0).status());
        verify(activationCodeHistoryRepository).saveAll(java.util.List.of(expired));
    }

    @Test
    void successfulCustomerIdLoginRecordsFirstAndLastLoginTimes() {
        savingsUser.setId(7L);
        savingsUser.setAccountNumber("100200300");
        savingsUser.setPasswordSet(true);
        savingsUser.setPassword(new PasswordService().encryptPassword("LongPassword123"));
        CustomerActivationCodeHistory history = new CustomerActivationCodeHistory();
        history.setCustomerId("123456789");
        history.setAccountType("SAVINGS");
        history.setStatus("USED");
        when(activationCodeHistoryRepository.findFirstByCustomerIdAndAccountTypeAndStatusOrderByIssuedAtDesc(
                "123456789", "SAVINGS", "USED")).thenReturn(Optional.of(history));

        java.util.Map<String, Object> response = service.authenticate("123456789", "LongPassword123");

        assertEquals(true, response.get("success"));
        assertNotNull(history.getFirstLoginAt());
        assertNotNull(history.getLastLoginAt());
        assertEquals(history.getFirstLoginAt(), history.getLastLoginAt());
        verify(activationCodeHistoryRepository).save(history);
    }
}
