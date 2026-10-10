package com.neo.springapp.service;

import com.neo.springapp.model.Account;
import com.neo.springapp.model.CurrentAccount;
import com.neo.springapp.model.SalaryAccount;
import com.neo.springapp.model.User;
import com.neo.springapp.repository.CurrentAccountRepository;
import com.neo.springapp.repository.SalaryAccountRepository;
import com.neo.springapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccountRecoveryServiceTest {
    private UserRepository userRepository;
    private CurrentAccountRepository currentAccountRepository;
    private SalaryAccountRepository salaryAccountRepository;
    private AccountRecoveryService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        currentAccountRepository = mock(CurrentAccountRepository.class);
        salaryAccountRepository = mock(SalaryAccountRepository.class);
        service = new AccountRecoveryService(
                userRepository, currentAccountRepository, salaryAccountRepository, new PasswordService());
    }

    @Test
    void verifiesSavingsIdentityAndReturnsOnlyMaskedName() {
        Account account = new Account();
        account.setName("Rohith Varsheny");
        account.setCustomerId("CUST12345");
        account.setAccountNumber("100200300");
        account.setDob("1995-03-12");
        User user = new User();
        user.setAccount(account);
        user.setAccountLocked(true);
        user.setFailedLoginAttempts(3);
        when(userRepository.findByAccountNumber("100200300")).thenReturn(Optional.of(user));

        assertEquals(Optional.of("R***** V*******"),
                service.verify("Savings", "CUST12345", "100200300", "1995-03-12"));
        assertTrue(service.verify("Savings", "CUST12345", "100200300", "1995-03-13").isEmpty());
        assertTrue(service.resetPassword("Savings", "CUST12345", "100200300", "1995-03-12", "NewSecurePass123"));

        assertTrue(new PasswordService().verifyPassword("NewSecurePass123", user.getPassword()));
        assertFalse(user.isAccountLocked());
        assertEquals(0, user.getFailedLoginAttempts());
        verify(userRepository).save(user);
    }

    @Test
    void resetsCurrentAccountPasswordAndClearsLockout() {
        CurrentAccount account = new CurrentAccount();
        account.setCustomerId("CURRENT123");
        account.setAccountNumber("200300400");
        account.setOwnerDob("1987-10-04");
        account.setOwnerName("Sam Customer");
        account.setFailedLoginAttempts(3);
        account.setAccountLocked(true);
        account.setLockReason("Failed attempts");
        when(currentAccountRepository.findByAccountNumber("200300400")).thenReturn(Optional.of(account));

        assertTrue(service.resetPassword("Current", "CURRENT123", "200300400", "1987-10-04", "NewSecurePass123"));

        assertTrue(new BCryptPasswordEncoder(10).matches("NewSecurePass123", account.getPassword()));
        assertFalse(account.getAccountLocked());
        assertEquals(0, account.getFailedLoginAttempts());
        assertNull(account.getLockReason());
        verify(currentAccountRepository).save(account);
    }

    @Test
    void resetsSalaryAccountPasswordAndRejectsWrongDateOfBirth() {
        SalaryAccount account = new SalaryAccount();
        account.setCustomerId("SALARY123");
        account.setAccountNumber("300400500");
        account.setDob("1990-01-02");
        account.setEmployeeName("Jamie Example");
        account.setFailedLoginAttempts(3);
        account.setAccountLocked(true);
        when(salaryAccountRepository.findByAccountNumber("300400500")).thenReturn(account);

        assertFalse(service.resetPassword("Salary", "SALARY123", "300400500", "1990-01-03", "NewSecurePass123"));
        assertTrue(service.resetPassword("Salary", "SALARY123", "300400500", "1990-01-02", "NewSecurePass123"));

        assertTrue(new BCryptPasswordEncoder(10).matches("NewSecurePass123", account.getPassword()));
        assertFalse(account.getAccountLocked());
        assertEquals(0, account.getFailedLoginAttempts());
        verify(salaryAccountRepository).save(account);
    }
}
