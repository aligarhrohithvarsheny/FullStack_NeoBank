package com.neo.springapp.service;

import org.springframework.stereotype.Service;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class PasswordService {
    
    private final SecureRandom random = new SecureRandom();
    
    public PasswordService() {
        // Initialize with secure random
    }
    
    /**
     * Encrypt a plain text password using SHA-256 with salt
     * @param plainPassword the plain text password to encrypt
     * @return the encrypted password hash
     */
    public String encryptPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        return hashWithSalt(plainPassword);
    }
    
    /**
     * Verify a plain text password against an encrypted password
     * @param plainPassword the plain text password to verify
     * @param encryptedPassword the encrypted password hash to verify against
     * @return true if the password matches, false otherwise
     */
    public boolean verifyPassword(String plainPassword, String encryptedPassword) {
        if (plainPassword == null || encryptedPassword == null) {
            return false;
        }
        try {
            // Extract salt from encrypted password
            String[] parts = encryptedPassword.split(":");
            if (parts.length != 2) {
                return false;
            }
            String salt = parts[0];
            
            // Hash the plain password with the extracted salt
            String hashedInput = hashWithSalt(plainPassword, salt);
            return hashedInput.equals(encryptedPassword);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Encrypt a PIN using SHA-256 with salt (for card PINs)
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
     * Check if a password is already encrypted (contains salt separator)
     * @param password the password to check
     * @return true if the password appears to be encrypted, false otherwise
     */
    public boolean isEncrypted(String password) {
        if (password == null || password.trim().isEmpty()) {
            return false;
        }
        // Our encrypted passwords contain a colon separator between salt and hash
        return password.contains(":") && password.length() > 20;
    }
    
    /**
     * Hash a password with a random salt
     * @param password the password to hash
     * @return the salted hash in format "salt:hash"
     */
    private String hashWithSalt(String password) {
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return hashWithSalt(password, Base64.getEncoder().encodeToString(salt));
    }
    
    /**
     * Hash a password with a specific salt
     * @param password the password to hash
     * @param salt the salt to use
     * @return the salted hash in format "salt:hash"
     */
    private String hashWithSalt(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] saltBytes = Base64.getDecoder().decode(salt);
            md.update(saltBytes);
            byte[] hashedPassword = md.digest(password.getBytes());
            String hash = Base64.getEncoder().encodeToString(hashedPassword);
            return salt + ":" + hash;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
