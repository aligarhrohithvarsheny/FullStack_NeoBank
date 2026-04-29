package com.neo.springapp.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.nio.charset.StandardCharsets;

/**
 * Password Service using BCrypt for passwords and SHA-256 for PINs
 * 
 * - Uses BCryptPasswordEncoder for password encryption (production-ready)
 * - Uses SHA-256 with salt for PIN encryption (4-digit PINs)
 * - BCrypt format: $2a$10$... (starts with $2a$ or $2b$)
 */
@Service
public class PasswordService {
    
    private final BCryptPasswordEncoder bcryptEncoder;
    private final SecureRandom random = new SecureRandom();
    
    public PasswordService() {
        // Initialize BCryptPasswordEncoder with strength 10 (default is 10)
        this.bcryptEncoder = new BCryptPasswordEncoder(10);
    }
    
    /**
     * Encrypt a plain text password using BCrypt
     * @param plainPassword the plain text password to encrypt
     * @return the BCrypt encrypted password hash (format: $2a$10$...)
     */
    public String encryptPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        String encoded = bcryptEncoder.encode(plainPassword);
        System.out.println("🔐 Password encrypted using BCrypt (length: " + encoded.length() + ")");
        return encoded;
    }
    
    /**
     * Verify a plain text password against a BCrypt encrypted password
     * @param plainPassword the plain text password to verify
     * @param encryptedPassword the BCrypt encrypted password hash to verify against
     * @return true if the password matches, false otherwise
     */
    public boolean verifyPassword(String plainPassword, String encryptedPassword) {
        if (plainPassword == null || encryptedPassword == null) {
            System.out.println("PasswordService: verifyPassword - null password or encrypted password");
            return false;
        }
        
        try {
            // Check if it's BCrypt format (starts with $2a$, $2b$, or $2y$)
            if (isBCryptFormat(encryptedPassword)) {
                boolean matches = bcryptEncoder.matches(plainPassword, encryptedPassword);
                System.out.println("PasswordService: BCrypt verification result: " + matches);
                return matches;
            } else {
                // Legacy SHA-256 format (for migration purposes)
                System.out.println("PasswordService: Legacy SHA-256 format detected, attempting verification...");
                return verifyLegacyPassword(plainPassword, encryptedPassword);
            }
        } catch (Exception e) {
            System.out.println("PasswordService: verifyPassword - Exception occurred: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Encrypt a PIN using SHA-256 with salt (for card PINs)
     * PINs use SHA-256 because they are 4-digit numbers and need consistent format
     * @param plainPin the plain text PIN to encrypt
     * @return the encrypted PIN hash
     */
    public String encryptPin(String plainPin) {
        if (plainPin == null || plainPin.trim().isEmpty()) {
            throw new IllegalArgumentException("PIN cannot be null or empty");
        }
        // Validate PIN format (4 digits)
        if (!plainPin.matches("\\d{4}")) {
            throw new IllegalArgumentException("PIN must be exactly 4 digits");
        }
        return hashWithSalt(plainPin);
    }
    
    /**
     * Verify a plain text PIN against an encrypted PIN
     * @param plainPin the plain text PIN to verify
     * @param encryptedPin the encrypted PIN hash to verify against
     * @return true if the PIN matches, false otherwise
     */
    public boolean verifyPin(String plainPin, String encryptedPin) {
        if (plainPin == null || encryptedPin == null) {
            return false;
        }
        // Validate PIN format (4 digits)
        if (!plainPin.matches("\\d{4}")) {
            return false;
        }
        try {
            // Extract salt from encrypted PIN
            String[] parts = encryptedPin.split(":");
            if (parts.length != 2) {
                return false;
            }
            String salt = parts[0];
            
            // Hash the plain PIN with the extracted salt
            String hashedInput = hashWithSalt(plainPin, salt);
            return hashedInput.equals(encryptedPin);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Check if a password is already encrypted (BCrypt or legacy format)
     * @param password the password to check
     * @return true if the password appears to be encrypted, false otherwise
     */
    public boolean isEncrypted(String password) {
        if (password == null || password.trim().isEmpty()) {
            return false;
        }
        // BCrypt format: starts with $2a$, $2b$, or $2y$ and is 60 characters long
        if (isBCryptFormat(password)) {
            return true;
        }
        // Legacy SHA-256 format: contains colon separator and is longer than 20 chars
        return password.contains(":") && password.length() > 20;
    }
    
    /**
     * Check if password is in BCrypt format
     */
    private boolean isBCryptFormat(String password) {
        return password != null && 
               password.length() == 60 && 
               (password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$"));
    }
    
    /**
     * Verify legacy SHA-256 password (for migration purposes)
     */
    private boolean verifyLegacyPassword(String plainPassword, String encryptedPassword) {
        try {
            // Extract salt from encrypted password
            String[] parts = encryptedPassword.split(":");
            if (parts.length != 2) {
                return false;
            }
            String salt = parts[0];
            
            // Hash the plain password with the extracted salt
            String hashedInput = hashWithSalt(plainPassword, salt);
            boolean matches = hashedInput.equals(encryptedPassword);
            System.out.println("PasswordService: Legacy SHA-256 verification result: " + matches);
            return matches;
        } catch (Exception e) {
            System.out.println("PasswordService: verifyLegacyPassword - Exception: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Hash a PIN/password with a random salt (SHA-256 for PINs)
     * @param password the password to hash
     * @return the salted hash in format "salt:hash"
     */
    private String hashWithSalt(String password) {
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return hashWithSalt(password, Base64.getEncoder().encodeToString(salt));
    }
    
    /**
     * Hash a password/PIN with a specific salt (SHA-256)
     * @param password the password to hash
     * @param salt the salt to use
     * @return the salted hash in format "salt:hash"
     */
    private String hashWithSalt(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] saltBytes = Base64.getDecoder().decode(salt);
            md.update(saltBytes);
            // Use UTF-8 encoding explicitly to avoid charset issues
            byte[] hashedPassword = md.digest(password.getBytes(StandardCharsets.UTF_8));
            String hash = Base64.getEncoder().encodeToString(hashedPassword);
            return salt + ":" + hash;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        } catch (IllegalArgumentException e) {
            System.out.println("PasswordService: hashWithSalt - Invalid Base64 salt: " + e.getMessage());
            throw new RuntimeException("Invalid salt format", e);
        }
    }
}
