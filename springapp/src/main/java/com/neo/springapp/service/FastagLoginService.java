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

    private static final int MAX_OTP_ATTEMPTS = 3;
    private static final int OTP_VALIDITY_MINUTES = 5;

    @Autowired
    private FastagUserRepository fastagUserRepository;

    @Autowired
    private OtpService otpService;

    @Autowired
    private EmailService emailService;

    /**
     * Send OTP to given Gmail ID. Auto-creates FastagUser if new.
     */
    public FastagUser sendOtp(String gmailId) {
        String normalizedEmail = gmailId.toLowerCase().trim();

        // Find or create user
        FastagUser user = fastagUserRepository.findByGmailId(normalizedEmail)
                .orElseGet(() -> {
                    FastagUser newUser = new FastagUser();
                    newUser.setGmailId(normalizedEmail);
                    newUser.setCreatedAt(LocalDateTime.now());
                    return newUser;
                });

        // Generate and store OTP
        String otp = otpService.generateOtp();
        user.setOtp(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(OTP_VALIDITY_MINUTES));
        user.setOtpAttempts(0);

        fastagUserRepository.save(user);

        // Also store in OtpService (for consistency with existing pattern)
        otpService.storeOtp(normalizedEmail, otp);

        // Send OTP via email
        boolean sent = emailService.sendFastagOtpEmail(normalizedEmail, otp);
        if (!sent) {
            System.out.println("⚠️ FASTag OTP email sending reported failure, but OTP is stored. Check console for OTP.");
        }

        return user;
    }

    /**
     * Verify OTP for given Gmail ID. Returns session token on success.
     */
    public VerifyResult verifyOtp(String gmailId, String otp) {
        String normalizedEmail = gmailId.toLowerCase().trim();

        Optional<FastagUser> optUser = fastagUserRepository.findByGmailId(normalizedEmail);
        if (optUser.isEmpty()) {
            return new VerifyResult(false, "No OTP request found for this email.", null, null);
        }

        FastagUser user = optUser.get();

        // Check attempt limit
        if (user.getOtpAttempts() >= MAX_OTP_ATTEMPTS) {
            return new VerifyResult(false, "Maximum OTP attempts exceeded. Please request a new OTP.", null, null);
        }

        // Check expiry
        if (user.getOtpExpiry() == null || LocalDateTime.now().isAfter(user.getOtpExpiry())) {
            return new VerifyResult(false, "OTP has expired. Please request a new OTP.", null, null);
        }

        // Increment attempts
        user.setOtpAttempts(user.getOtpAttempts() + 1);

        // Verify OTP
        if (!otp.trim().equals(user.getOtp())) {
            fastagUserRepository.save(user);
            int remaining = MAX_OTP_ATTEMPTS - user.getOtpAttempts();
            return new VerifyResult(false, "Invalid OTP. " + remaining + " attempt(s) remaining.", null, null);
        }

        // OTP correct - mark verified, generate session token
        user.setIsVerified(true);
        user.setOtp(null);
        user.setOtpExpiry(null);
        user.setOtpAttempts(0);
        user.setLastLoginAt(LocalDateTime.now());
        String sessionToken = UUID.randomUUID().toString();
        user.setSessionToken(sessionToken);

        fastagUserRepository.save(user);

        return new VerifyResult(true, "Login successful!", user, sessionToken);
    }

    /**
     * Get user by Gmail ID
     */
    public Optional<FastagUser> getUserByGmail(String gmailId) {
        return fastagUserRepository.findByGmailId(gmailId.toLowerCase().trim());
    }

    /**
     * Result object for verify OTP
     */
    public static class VerifyResult {
        public final boolean success;
        public final String message;
        public final FastagUser user;
        public final String sessionToken;

        public VerifyResult(boolean success, String message, FastagUser user, String sessionToken) {
            this.success = success;
            this.message = message;
            this.user = user;
            this.sessionToken = sessionToken;
        }
    }
}
