package com.neo.springapp.service;

import com.neo.springapp.model.Account;
import com.neo.springapp.model.CurrentAccount;
import com.neo.springapp.model.SalaryAccount;
import com.neo.springapp.model.User;
import com.neo.springapp.repository.CurrentAccountRepository;
import com.neo.springapp.repository.SalaryAccountRepository;
import com.neo.springapp.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

@Service
@SuppressWarnings("null")
public class AccountRecoveryService {
    private final UserRepository userRepository;
    private final CurrentAccountRepository currentAccountRepository;
    private final SalaryAccountRepository salaryAccountRepository;
    private final PasswordService passwordService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);

    public AccountRecoveryService(
            UserRepository userRepository,
            CurrentAccountRepository currentAccountRepository,
            SalaryAccountRepository salaryAccountRepository,
            PasswordService passwordService) {
        this.userRepository = userRepository;
        this.currentAccountRepository = currentAccountRepository;
        this.salaryAccountRepository = salaryAccountRepository;
        this.passwordService = passwordService;
    }

    @Transactional(readOnly = true)
    public Optional<String> verify(String accountType, String customerId, String accountNumber, String dob) {
        return findMatchingName(accountType, customerId, accountNumber, dob).map(AccountRecoveryService::maskName);
    }

    @Transactional
    public boolean resetPassword(String accountType, String customerId, String accountNumber, String dob, String newPassword) {
        String type = accountType.trim().toUpperCase(Locale.ROOT);
        if ("SAVINGS".equals(type)) {
            Optional<User> userMatch = userRepository.findByAccountNumber(accountNumber.trim())
                    .filter(user -> matches(user.getAccount(), customerId, accountNumber, dob));
            if (userMatch.isEmpty()) return false;

            User user = userMatch.get();
            user.setPassword(passwordService.encryptPassword(newPassword));
            user.setPasswordSet(true);
            user.setAccountLocked(false);
            user.setFailedLoginAttempts(0);
            user.setLastFailedLoginTime(null);
            userRepository.save(user);
            return true;
        }
        if ("CURRENT".equals(type)) {
            Optional<CurrentAccount> match = currentAccountRepository.findByAccountNumber(accountNumber.trim())
                    .filter(account -> matches(account.getCustomerId(), account.getAccountNumber(), account.getOwnerDob(),
                            customerId, accountNumber, dob));
            if (match.isEmpty()) return false;

            CurrentAccount account = match.get();
            account.setPassword(passwordEncoder.encode(newPassword));
            account.setPasswordSet(true);
            account.setAccountLocked(false);
            account.setFailedLoginAttempts(0);
            account.setLastFailedLoginTime(null);
            account.setLockReason(null);
            account.setLastUpdated(LocalDateTime.now());
            currentAccountRepository.save(account);
            return true;
        }
        if ("SALARY".equals(type)) {
            SalaryAccount match = salaryAccountRepository.findByAccountNumber(accountNumber.trim());
            if (match == null || !matches(match.getCustomerId(), match.getAccountNumber(), match.getDob(),
                    customerId, accountNumber, dob)) return false;

            match.setPassword(passwordEncoder.encode(newPassword));
            match.setPasswordSet(true);
            match.setAccountLocked(false);
            match.setFailedLoginAttempts(0);
            match.setLastFailedLoginTime(null);
            match.setLockReason(null);
            match.setUpdatedAt(LocalDateTime.now());
            salaryAccountRepository.save(match);
            return true;
        }
        return false;
    }

    private Optional<String> findMatchingName(String accountType, String customerId, String accountNumber, String dob) {
        if (accountType == null || customerId == null || accountNumber == null || dob == null) return Optional.empty();
        String type = accountType.trim().toUpperCase(Locale.ROOT);
        if ("SAVINGS".equals(type)) {
            return userRepository.findByAccountNumber(accountNumber.trim())
                    .filter(user -> matches(user.getAccount(), customerId, accountNumber, dob))
                    .map(User::getAccount)
                    .map(Account::getName);
        }
        if ("CURRENT".equals(type)) {
            return currentAccountRepository.findByAccountNumber(accountNumber.trim())
                    .filter(account -> matches(account.getCustomerId(), account.getAccountNumber(), account.getOwnerDob(),
                            customerId, accountNumber, dob))
                    .map(CurrentAccount::getOwnerName);
        }
        if ("SALARY".equals(type)) {
            SalaryAccount account = salaryAccountRepository.findByAccountNumber(accountNumber.trim());
            return account != null && matches(account.getCustomerId(), account.getAccountNumber(), account.getDob(),
                    customerId, accountNumber, dob) ? Optional.ofNullable(account.getEmployeeName()) : Optional.empty();
        }
        return Optional.empty();
    }

    private boolean matches(Account account, String customerId, String accountNumber, String dob) {
        return account != null && matches(account.getCustomerId(), account.getAccountNumber(), account.getDob(),
                customerId, accountNumber, dob);
    }

    private boolean matches(String storedCustomerId, String storedAccountNumber, String storedDob,
            String customerId, String accountNumber, String dob) {
        return storedCustomerId != null && storedAccountNumber != null && storedDob != null
                && storedCustomerId.trim().equalsIgnoreCase(customerId.trim())
                && storedAccountNumber.trim().equals(accountNumber.trim())
                && storedDob.trim().equals(dob.trim());
    }

    private static String maskName(String name) {
        if (name == null || name.isBlank()) return "Customer";
        return java.util.Arrays.stream(name.trim().split("\\s+"))
                .map(part -> part.substring(0, 1) + "*".repeat(Math.max(2, part.length() - 1)))
                .collect(java.util.stream.Collectors.joining(" "));
    }
}
