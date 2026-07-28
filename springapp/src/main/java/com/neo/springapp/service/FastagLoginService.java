package com.neo.springapp.service;

import com.neo.springapp.model.FastagUser;
import com.neo.springapp.repository.FastagUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class FastagLoginService {

    private static final int MAX_LOGIN_ATTEMPTS = 3;

    @Autowired
    private FastagUserRepository fastagUserRepository;

    @Autowired
    private PasswordService passwordService;

    /**
     * Login with Gmail + password (no OTP).
     */
    public LoginResult login(String gmailId, String password) {
        String normalizedEmail = normalizeGmail(gmailId);

        Optional<FastagUser> optUser = fastagUserRepository.findByGmailId(normalizedEmail);
        if (optUser.isEmpty()) {
            return LoginResult.notFound("No FASTag account found for this Gmail. Please set your password to register.");
        }

        FastagUser user = optUser.get();

        if (Boolean.TRUE.equals(user.getAccountLocked())) {
            return LoginResult.failed("Account is locked due to multiple failed login attempts. Please contact support.");
        }

        if (user.getPassword() == null || !Boolean.TRUE.equals(user.getPasswordSet())) {
            return LoginResult.requiresPasswordSetup(
                    "Password not set for this Gmail. Please set your password to continue.");
        }

        if (password == null || password.isBlank()) {
            return LoginResult.failed("Password is required.");
        }

        if (!passwordService.verifyPassword(password, user.getPassword())) {
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() != null ? user.getFailedLoginAttempts() + 1 : 1);
            if (user.getFailedLoginAttempts() >= MAX_LOGIN_ATTEMPTS) {
                user.setAccountLocked(true);
                fastagUserRepository.save(user);
                return LoginResult.failed("Account locked after 3 failed attempts.");
            }
            fastagUserRepository.save(user);
            int remaining = MAX_LOGIN_ATTEMPTS - user.getFailedLoginAttempts();
            return LoginResult.failed("Invalid password. " + remaining + " attempt(s) remaining.");
        }

        return completeLogin(user);
    }

    /**
     * Set or reset password for a FASTag user (creates account if new Gmail).
     */
    public LoginResult setPassword(String gmailId, String newPassword, String confirmPassword) {
        String normalizedEmail = normalizeGmail(gmailId);

        if (newPassword == null || confirmPassword == null) {
            return LoginResult.failed("New password and confirm password are required.");
        }
        if (!newPassword.equals(confirmPassword)) {
            return LoginResult.failed("Passwords do not match.");
        }
        if (newPassword.length() < 8) {
            return LoginResult.failed("Password must be at least 8 characters long.");
        }
        if (!newPassword.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$")) {
            return LoginResult.failed(
                    "Password must contain at least one uppercase letter, one lowercase letter, and one number.");
        }

        FastagUser user = fastagUserRepository.findByGmailId(normalizedEmail)
                .orElseGet(() -> {
                    FastagUser newUser = new FastagUser();
                    newUser.setGmailId(normalizedEmail);
                    newUser.setCreatedAt(LocalDateTime.now());
                    return newUser;
                });

        user.setPassword(passwordService.encryptPassword(newPassword));
        user.setPasswordSet(true);
        user.setFailedLoginAttempts(0);
        user.setAccountLocked(false);
        user.setIsVerified(true);
        user.setOtp(null);
        user.setOtpExpiry(null);
        user.setOtpAttempts(0);

        fastagUserRepository.save(user);
        return completeLogin(user);
    }

    public Optional<FastagUser> getUserByGmail(String gmailId) {
        return fastagUserRepository.findByGmailId(normalizeGmail(gmailId));
    }

    private LoginResult completeLogin(FastagUser user) {
        user.setFailedLoginAttempts(0);
        user.setAccountLocked(false);
        user.setIsVerified(true);
        user.setLastLoginAt(LocalDateTime.now());
        String sessionToken = UUID.randomUUID().toString();
        user.setSessionToken(sessionToken);
        fastagUserRepository.save(user);
        return LoginResult.success("Login successful!", user, sessionToken);
    }

    private static String normalizeGmail(String gmailId) {
        return gmailId != null ? gmailId.toLowerCase().trim() : "";
    }

    public static class LoginResult {
        public final boolean success;
        public final boolean requiresPasswordSetup;
        public final boolean accountNotFound;
        public final String message;
        public final FastagUser user;
        public final String sessionToken;

        private LoginResult(boolean success, boolean requiresPasswordSetup, boolean accountNotFound,
                            String message, FastagUser user, String sessionToken) {
            this.success = success;
            this.requiresPasswordSetup = requiresPasswordSetup;
            this.accountNotFound = accountNotFound;
            this.message = message;
            this.user = user;
            this.sessionToken = sessionToken;
        }

        static LoginResult success(String message, FastagUser user, String sessionToken) {
            return new LoginResult(true, false, false, message, user, sessionToken);
        }

        static LoginResult failed(String message) {
            return new LoginResult(false, false, false, message, null, null);
        }

        static LoginResult requiresPasswordSetup(String message) {
            return new LoginResult(false, true, false, message, null, null);
        }

        static LoginResult notFound(String message) {
            return new LoginResult(false, true, true, message, null, null);
        }
    }
}
