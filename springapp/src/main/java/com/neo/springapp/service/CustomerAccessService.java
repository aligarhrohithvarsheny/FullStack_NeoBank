package com.neo.springapp.service;

import com.neo.springapp.model.Account;
import com.neo.springapp.model.CurrentAccount;
import com.neo.springapp.model.CustomerActivationCode;
import com.neo.springapp.model.SalaryAccount;
import com.neo.springapp.model.User;
import com.neo.springapp.repository.AccountRepository;
import com.neo.springapp.repository.CurrentAccountRepository;
import com.neo.springapp.repository.CustomerActivationCodeRepository;
import com.neo.springapp.repository.SalaryAccountRepository;
import com.neo.springapp.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class CustomerAccessService {
    private static final String ACTIVATION_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int MAX_ACTIVATION_ATTEMPTS = 5;
    private static final int ACTIVATION_CODE_LENGTH = 16;
    private static final Pattern AADHAAR_PATTERN = Pattern.compile("\\d{12}");
    private final SecureRandom secureRandom = new SecureRandom();

    private final AccountRepository accountRepository;
    private final CurrentAccountRepository currentAccountRepository;
    private final SalaryAccountRepository salaryAccountRepository;
    private final UserRepository userRepository;
    private final CustomerActivationCodeRepository activationCodeRepository;
    private final CurrentAccountService currentAccountService;
    private final SalaryAccountService salaryAccountService;
    private final PasswordService passwordService;
    private final UserSessionTokenService sessionTokenService;

    public CustomerAccessService(
            AccountRepository accountRepository,
            CurrentAccountRepository currentAccountRepository,
            SalaryAccountRepository salaryAccountRepository,
            UserRepository userRepository,
            CustomerActivationCodeRepository activationCodeRepository,
            CurrentAccountService currentAccountService,
            SalaryAccountService salaryAccountService,
            PasswordService passwordService,
            UserSessionTokenService sessionTokenService) {
        this.accountRepository = accountRepository;
        this.currentAccountRepository = currentAccountRepository;
        this.salaryAccountRepository = salaryAccountRepository;
        this.userRepository = userRepository;
        this.activationCodeRepository = activationCodeRepository;
        this.currentAccountService = currentAccountService;
        this.salaryAccountService = salaryAccountService;
        this.passwordService = passwordService;
        this.sessionTokenService = sessionTokenService;
    }

    @Transactional
    public IssuedCode issueActivationCode(String accountType, String customerId, String issuedBy) {
        AccountType type = AccountType.from(accountType);
        String normalizedCustomerId = normalizeCustomerId(customerId);
        List<CustomerRecord> matches = findMatches(normalizedCustomerId);
        if (matches.size() != 1 || matches.get(0).type != type) {
            throw new IllegalArgumentException("Customer record is unavailable or ambiguous. Verify the customer ID and account type.");
        }

        CustomerRecord customer = matches.get(0);
        if (!customer.isActive() || customer.isPasswordSet()
                || blank(customer.email()) || blank(customer.aadhaar())) {
            throw new IllegalArgumentException("This customer is not eligible for first-time online access. Review the account record.");
        }

        String code = generateActivationCode();
        LocalDateTime now = LocalDateTime.now();
        CustomerActivationCode activation = activationCodeRepository.findForUpdate(normalizedCustomerId)
                .orElseGet(CustomerActivationCode::new);
        activation.setCustomerId(normalizedCustomerId);
        activation.setAccountType(type.name());
        activation.setCodeHash(passwordService.encryptPassword(code));
        activation.setIssuedBy(issuedBy);
        activation.setIssuedAt(now);
        activation.setExpiresAt(now.plusHours(24));
        activation.setConsumedAt(null);
        activation.setFailedAttempts(0);
        activationCodeRepository.save(activation);
        return new IssuedCode(code, activation.getExpiresAt(), type.name());
    }

    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public void verifyActivationCode(String customerId, String code) {
        CustomerActivationCode activation = getValidActivation(normalizeCustomerId(customerId), code);
        if (activation == null) throw invalidRegistration();
    }

    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public void verifyRegistrationDetails(String customerId, String email, String aadhaar, String code) {
        CustomerActivationCode activation = getValidActivation(normalizeCustomerId(customerId), code);
        if (activation == null) throw invalidRegistration();
        CustomerRecord customer = findCustomer(activation);
        if (customer == null || !matchesIdentity(customer, email, aadhaar) || !customer.isActive()
                || customer.isPasswordSet()) {
            recordFailedAttempt(activation);
            throw invalidRegistration();
        }
    }

    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public RegistrationResult activateOnlineAccess(ActivationRequest request) {
        if (request == null || blank(request.customerId()) || blank(request.activationCode())
                || blank(request.email()) || blank(request.aadhaar()) || !validPassword(request.password())) {
            throw new IllegalArgumentException("Enter your customer ID, registered email, Aadhaar number, activation code, and a password of at least 10 characters.");
        }

        CustomerActivationCode activation = getValidActivation(
                normalizeCustomerId(request.customerId()), request.activationCode());
        if (activation == null) throw invalidRegistration();

        CustomerRecord customer = findCustomer(activation);
        if (customer == null || !matchesIdentity(customer, request.email(), request.aadhaar())
                || !customer.isActive() || customer.isPasswordSet()) {
            recordFailedAttempt(activation);
            throw invalidRegistration();
        }

        String passwordHash = passwordService.encryptPassword(request.password());
        customer.setPassword(passwordHash);
        customer.setPasswordSet(true);
        customer.resetLoginFailures();
        activation.setConsumedAt(LocalDateTime.now());
        activationCodeRepository.save(activation);
        customer.save();
        return new RegistrationResult(true, "Online access is ready. Sign in with your customer ID.", customer.type.name());
    }

    @Transactional
    public Map<String, Object> authenticate(String customerId, String password) {
        if (blank(customerId) || blank(password)) return invalidLogin();
        List<CustomerRecord> matches = findMatches(normalizeCustomerId(customerId));
        if (matches.size() != 1) return invalidLogin();
        CustomerRecord customer = matches.get(0);

        if (!customer.isActive() || !customer.isPasswordSet() || customer.password() == null) {
            return invalidLogin();
        }

        if (customer.type == AccountType.SAVINGS) {
            User user = (User) customer.entity;
            Account account = user.getAccount();
            if (!"APPROVED".equalsIgnoreCase(user.getStatus())
                    || account == null
                    || Boolean.FALSE.equals(account.getNetBankingEnabled())
                    || user.isAccountLocked()) {
                return invalidLogin();
            }
            if (!passwordService.verifyPassword(password, user.getPassword())) {
                user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
                user.setLastFailedLoginTime(LocalDateTime.now());
                if (user.getFailedLoginAttempts() >= 3) user.setAccountLocked(true);
                userRepository.save(user);
                return invalidLogin();
            }
            user.setFailedLoginAttempts(0);
            user.setLastFailedLoginTime(null);
            userRepository.save(user);
            Map<String, Object> response = success(AccountType.SAVINGS, user.getId(), user.getAccountNumber());
            response.put("user", savingsUserView(user, account));
            return response;
        }

        if (customer.type == AccountType.SALARY) {
            SalaryAccount salary = (SalaryAccount) customer.entity;
            Map<String, Object> result = salaryAccountService.authenticate(salary.getAccountNumber(), password);
            if (!Boolean.TRUE.equals(result.get("success"))) return invalidLogin();
            Map<String, Object> response = success(AccountType.SALARY, salary.getId(), salary.getAccountNumber());
            response.put("account", salaryAccountView(salary));
            return response;
        }

        CurrentAccount current = (CurrentAccount) customer.entity;
        Map<String, Object> result = currentAccountService.authenticate(current.getAccountNumber(), password);
        if (!Boolean.TRUE.equals(result.get("success"))) return invalidLogin();
        Map<String, Object> response = success(AccountType.CURRENT, current.getId(), current.getAccountNumber());
        response.put("account", result.get("account"));
        return response;
    }

    private Map<String, Object> success(AccountType type, Long id, String accountNumber) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("role", "USER");
        response.put("accountType", type.name());
        response.put("token", sessionTokenService.issue(id, accountNumber));
        response.put("message", "Login successful");
        return response;
    }

    private Map<String, Object> savingsUserView(User user, Account account) {
        Map<String, Object> accountView = new HashMap<>();
        accountView.put("name", account.getName());
        accountView.put("customerId", account.getCustomerId());
        accountView.put("accountType", account.getAccountType());
        Map<String, Object> userView = new HashMap<>();
        userView.put("id", user.getId());
        userView.put("username", user.getUsername());
        userView.put("email", user.getEmail());
        userView.put("accountNumber", user.getAccountNumber());
        userView.put("status", user.getStatus());
        userView.put("passwordSet", user.isPasswordSet());
        userView.put("account", accountView);
        return userView;
    }

    private Map<String, Object> salaryAccountView(SalaryAccount account) {
        Map<String, Object> view = new HashMap<>();
        view.put("id", account.getId());
        view.put("employeeName", account.getEmployeeName());
        view.put("accountNumber", account.getAccountNumber());
        view.put("customerId", account.getCustomerId());
        view.put("companyName", account.getCompanyName());
        view.put("designation", account.getDesignation());
        view.put("email", account.getEmail());
        view.put("status", account.getStatus());
        view.put("balance", account.getBalance());
        return view;
    }

    private Map<String, Object> invalidLogin() {
        return Map.of("success", false, "message", "Customer ID or password is incorrect, or online access is unavailable.");
    }

    private CustomerActivationCode getValidActivation(String customerId, String rawCode) {
        CustomerActivationCode activation = activationCodeRepository.findForUpdate(customerId).orElse(null);
        if (activation == null || activation.getConsumedAt() != null
                || activation.getExpiresAt().isBefore(LocalDateTime.now())
                || activation.getFailedAttempts() >= MAX_ACTIVATION_ATTEMPTS) {
            return null;
        }
        String code = rawCode == null ? "" : rawCode.trim().toUpperCase(Locale.ROOT);
        if (!passwordService.verifyPassword(code, activation.getCodeHash())) {
            recordFailedAttempt(activation);
            return null;
        }
        return activation;
    }

    private void recordFailedAttempt(CustomerActivationCode activation) {
        activation.setFailedAttempts(activation.getFailedAttempts() + 1);
        activationCodeRepository.save(activation);
    }

    private IllegalArgumentException invalidRegistration() {
        return new IllegalArgumentException("Registration could not be verified. Check your details or contact NeoBank support.");
    }

    private CustomerRecord findCustomer(CustomerActivationCode activation) {
        return findMatches(activation.getCustomerId()).stream()
                .filter(customer -> customer.type.name().equals(activation.getAccountType()))
                .findFirst().orElse(null);
    }

    private List<CustomerRecord> findMatches(String customerId) {
        List<CustomerRecord> matches = new ArrayList<>();
        Account account = accountRepository.findByCustomerId(customerId);
        if (account != null) {
            User user = userRepository.findByAccount_CustomerId(customerId).orElse(null);
            if (user != null) matches.add(CustomerRecord.savings(user, userRepository));
        }
        SalaryAccount salary = salaryAccountRepository.findByCustomerId(customerId);
        if (salary != null) matches.add(CustomerRecord.salary(salary, salaryAccountRepository));
        currentAccountRepository.findByCustomerId(customerId)
                .ifPresent(current -> matches.add(CustomerRecord.current(current, currentAccountRepository)));
        return matches;
    }

    private CustomerRecord findByType(String accountType, String customerId) {
        AccountType type = AccountType.from(accountType);
        return findMatches(customerId).stream().filter(customer -> customer.type == type).findFirst().orElse(null);
    }

    private boolean matchesIdentity(CustomerRecord customer, String email, String aadhaar) {
        String submittedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        String registeredEmail = customer.email() == null ? "" : customer.email().trim().toLowerCase(Locale.ROOT);
        String submittedAadhaar = aadhaarDigits(aadhaar);
        String registeredAadhaar = aadhaarDigits(customer.aadhaar());
        return !submittedEmail.isEmpty()
                && submittedEmail.equals(registeredEmail)
                && AADHAAR_PATTERN.matcher(submittedAadhaar).matches()
                && submittedAadhaar.equals(registeredAadhaar);
    }

    private String aadhaarDigits(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }

    private String normalizeCustomerId(String customerId) {
        return customerId == null ? "" : customerId.trim().toUpperCase(Locale.ROOT);
    }

    private String generateActivationCode() {
        StringBuilder code = new StringBuilder(ACTIVATION_CODE_LENGTH);
        for (int index = 0; index < ACTIVATION_CODE_LENGTH; index++) {
            code.append(ACTIVATION_CODE_ALPHABET.charAt(secureRandom.nextInt(ACTIVATION_CODE_ALPHABET.length())));
        }
        return code.toString();
    }

    private boolean validPassword(String password) {
        return password != null && password.length() >= 10 && password.length() <= 64
                && password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    public record IssuedCode(String activationCode, LocalDateTime expiresAt, String accountType) {}
    public record ActivationRequest(String customerId, String email, String aadhaar, String activationCode, String password) {}
    public record RegistrationResult(boolean success, String message, String accountType) {}

    private enum AccountType {
        SAVINGS, SALARY, CURRENT;

        static AccountType from(String value) {
            if (value == null) throw new IllegalArgumentException("Select a supported account type.");
            try {
                return valueOf(value.trim().toUpperCase(Locale.ROOT).replace("_ACCOUNT", ""));
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("Select Savings, Salary, or Current account access.");
            }
        }
    }

    private static class CustomerRecord {
        private final AccountType type;
        private final Object entity;
        private final AccountRepository accountRepository;
        private final CurrentAccountRepository currentAccountRepository;
        private final SalaryAccountRepository salaryAccountRepository;
        private final UserRepository userRepository;

        private CustomerRecord(AccountType type, Object entity, AccountRepository accountRepository,
                               CurrentAccountRepository currentAccountRepository,
                               SalaryAccountRepository salaryAccountRepository, UserRepository userRepository) {
            this.type = type;
            this.entity = entity;
            this.accountRepository = accountRepository;
            this.currentAccountRepository = currentAccountRepository;
            this.salaryAccountRepository = salaryAccountRepository;
            this.userRepository = userRepository;
        }

        static CustomerRecord savings(User user, UserRepository users) {
            return new CustomerRecord(AccountType.SAVINGS, user, null, null, null, users);
        }

        static CustomerRecord salary(SalaryAccount account, SalaryAccountRepository accounts) {
            return new CustomerRecord(AccountType.SALARY, account, null, null, accounts, null);
        }

        static CustomerRecord current(CurrentAccount account, CurrentAccountRepository accounts) {
            return new CustomerRecord(AccountType.CURRENT, account, null, accounts, null, null);
        }

        String email() {
            if (entity instanceof User user) return user.getEmail();
            if (entity instanceof SalaryAccount account) return account.getEmail();
            return ((CurrentAccount) entity).getEmail();
        }

        String aadhaar() {
            if (entity instanceof User user) return user.getAccount().getAadharNumber();
            if (entity instanceof SalaryAccount account) return account.getAadharNumber();
            return ((CurrentAccount) entity).getAadharNumber();
        }

        boolean isActive() {
            if (entity instanceof User user) {
                return "APPROVED".equalsIgnoreCase(user.getStatus())
                        && user.getAccount() != null
                        && "ACTIVE".equalsIgnoreCase(user.getAccount().getStatus());
            }
            if (entity instanceof SalaryAccount account) return "ACTIVE".equalsIgnoreCase(account.getStatus());
            String status = ((CurrentAccount) entity).getStatus();
            return "ACTIVE".equalsIgnoreCase(status) || "APPROVED".equalsIgnoreCase(status);
        }

        boolean isPasswordSet() {
            if (entity instanceof User user) return user.isPasswordSet();
            if (entity instanceof SalaryAccount account) return Boolean.TRUE.equals(account.getPasswordSet());
            return Boolean.TRUE.equals(((CurrentAccount) entity).getPasswordSet());
        }

        String password() {
            if (entity instanceof User user) return user.getPassword();
            if (entity instanceof SalaryAccount account) return account.getPassword();
            return ((CurrentAccount) entity).getPassword();
        }

        void setPassword(String password) {
            if (entity instanceof User user) user.setPassword(password);
            else if (entity instanceof SalaryAccount account) account.setPassword(password);
            else ((CurrentAccount) entity).setPassword(password);
        }

        void setPasswordSet(boolean passwordSet) {
            if (entity instanceof User user) user.setPasswordSet(passwordSet);
            else if (entity instanceof SalaryAccount account) account.setPasswordSet(passwordSet);
            else ((CurrentAccount) entity).setPasswordSet(passwordSet);
        }

        void resetLoginFailures() {
            if (entity instanceof User user) {
                user.setFailedLoginAttempts(0);
                user.setLastFailedLoginTime(null);
                user.setAccountLocked(false);
            } else if (entity instanceof SalaryAccount account) {
                account.setFailedLoginAttempts(0);
                account.setLastFailedLoginTime(null);
                account.setAccountLocked(false);
            } else {
                CurrentAccount account = (CurrentAccount) entity;
                account.setFailedLoginAttempts(0);
                account.setLastFailedLoginTime(null);
                account.setAccountLocked(false);
            }
        }

        void save() {
            if (entity instanceof User user) userRepository.save(user);
            else if (entity instanceof SalaryAccount account) salaryAccountRepository.save(account);
            else currentAccountRepository.save((CurrentAccount) entity);
        }
    }
}
