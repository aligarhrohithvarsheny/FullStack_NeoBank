package com.neo.springapp.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Random;

@Service
public class OtpService {
    
    // Store OTPs temporarily in memory (email -> OTP)
    // In production, consider using Redis or database with expiration
    private final Map<String, OtpData> otpStore = new ConcurrentHashMap<>();
    
    // OTP expiration time in milliseconds (5 minutes)
    private static final long OTP_EXPIRATION_TIME = 5 * 60 * 1000;
    
    private final Random random = new Random();
    
    /**
     * Generate a 6-digit OTP
     */
    public String generateOtp() {
        int otp = 100000 + random.nextInt(900000); // Generates 6-digit number
        return String.valueOf(otp);
    }
    
    /**
     * Store OTP for an email address
     */
    public void storeOtp(String email, String otp) {
        // Normalize email to lowercase for consistent storage
        String normalizedEmail = email != null ? email.toLowerCase().trim() : null;
        
        if (normalizedEmail == null || otp == null) {
            System.out.println("❌ Cannot store OTP: Email or OTP is null");
            return;
        }
        
        OtpData otpData = new OtpData(otp.trim(), System.currentTimeMillis());
        otpStore.put(normalizedEmail, otpData);
        
        System.out.println("✅ OTP stored for email: " + normalizedEmail + " (OTP: " + otp + ")");
        
        // Clean up expired OTPs periodically (simple approach)
        cleanupExpiredOtps();
    }
    
    /**
     * Verify OTP for an email address
     */
    public boolean verifyOtp(String email, String otp) {
        // Normalize email to lowercase for consistent lookup
        String normalizedEmail = email != null ? email.toLowerCase().trim() : null;
        
        // Trim OTP to remove any whitespace
        String trimmedOtp = otp != null ? otp.trim() : null;
        
        if (normalizedEmail == null || trimmedOtp == null) {
            System.out.println("❌ OTP verification failed: Email or OTP is null");
            return false;
        }
        
        OtpData otpData = otpStore.get(normalizedEmail);
        
        if (otpData == null) {
            System.out.println("❌ No OTP found for email: " + normalizedEmail);
            System.out.println("   Available emails in store: " + otpStore.keySet());
            return false;
        }
        
        // Check if OTP has expired
        long currentTime = System.currentTimeMillis();
        long timeElapsed = currentTime - otpData.getTimestamp();
        if (timeElapsed > OTP_EXPIRATION_TIME) {
            System.out.println("❌ OTP expired for email: " + normalizedEmail);
            System.out.println("   Time elapsed: " + (timeElapsed / 1000) + " seconds (max: " + (OTP_EXPIRATION_TIME / 1000) + " seconds)");
            otpStore.remove(normalizedEmail);
            return false;
        }
        
        // Verify OTP (compare trimmed values)
        String storedOtp = otpData.getOtp();
        boolean isValid = storedOtp.equals(trimmedOtp);
        
        // Debug logging
        System.out.println("🔍 OTP Verification Debug:");
        System.out.println("   Email: " + normalizedEmail);
        System.out.println("   Stored OTP: " + storedOtp + " (length: " + storedOtp.length() + ")");
        System.out.println("   Received OTP: " + trimmedOtp + " (length: " + trimmedOtp.length() + ")");
        System.out.println("   Match: " + isValid);
        System.out.println("   Time remaining: " + ((OTP_EXPIRATION_TIME - timeElapsed) / 1000) + " seconds");
        
        if (isValid) {
            // Remove OTP after successful verification
            otpStore.remove(normalizedEmail);
            System.out.println("✅ OTP verified successfully for email: " + normalizedEmail);
        } else {
            System.out.println("❌ Invalid OTP for email: " + normalizedEmail);
            System.out.println("   Expected: '" + storedOtp + "'");
            System.out.println("   Received: '" + trimmedOtp + "'");
        }
        
        return isValid;
    }
    
    /**
     * Get stored OTP for an email (for debugging)
     */
    public String getStoredOtp(String email) {
        OtpData otpData = otpStore.get(email);
        if (otpData != null && !isExpired(otpData)) {
            return otpData.getOtp();
        }
        return null;
    }
    
    /**
     * Check if OTP exists and is valid (not expired)
     */
    public boolean hasValidOtp(String email) {
        OtpData otpData = otpStore.get(email);
        if (otpData == null) {
            return false;
        }
        return !isExpired(otpData);
    }
    
    /**
     * Remove OTP for an email
     */
    public void removeOtp(String email) {
        otpStore.remove(email);
    }
    
    /**
     * Check if OTP data is expired
     */
    private boolean isExpired(OtpData otpData) {
        long currentTime = System.currentTimeMillis();
        return currentTime - otpData.getTimestamp() > OTP_EXPIRATION_TIME;
    }
    
    /**
     * Clean up expired OTPs
     */
    private void cleanupExpiredOtps() {
        long currentTime = System.currentTimeMillis();
        otpStore.entrySet().removeIf(entry -> 
            currentTime - entry.getValue().getTimestamp() > OTP_EXPIRATION_TIME
        );
    }
    
    /**
     * Inner class to store OTP data with timestamp
     */
    private static class OtpData {
        private final String otp;
        private final long timestamp;
        
        public OtpData(String otp, long timestamp) {
            this.otp = otp;
            this.timestamp = timestamp;
        }
        
        public String getOtp() {
            return otp;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
    }
}

