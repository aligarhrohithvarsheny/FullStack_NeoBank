package com.neo.springapp.service;

import com.neo.springapp.model.Admin;
import com.neo.springapp.repository.AdminRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.time.LocalDateTime;

@Service
public class AdminService {

    @Autowired
    private AdminRepository adminRepository;
    
    @Autowired
    private PasswordService passwordService;

    public Admin saveAdmin(Admin admin) {
        // Encrypt password before saving
        if (admin.getPassword() != null && !admin.getPassword().isEmpty()) {
            if (!passwordService.isEncrypted(admin.getPassword())) {
                System.out.println("🔐 Encrypting password for admin: " + (admin.getEmail() != null ? admin.getEmail() : "new admin"));
                try {
            admin.setPassword(passwordService.encryptPassword(admin.getPassword()));
                    System.out.println("   ✅ Password encrypted successfully");
                } catch (Exception e) {
                    System.err.println("   ❌ Error encrypting password: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                System.out.println("🔐 Password already encrypted for admin: " + (admin.getEmail() != null ? admin.getEmail() : "new admin"));
            }
        }
        // Set default role if not provided
        if (admin.getRole() == null || admin.getRole().isEmpty()) {
            admin.setRole("ADMIN");
        }
        // Set profileComplete to false for new admins if not explicitly set
        if (admin.getProfileComplete() == null) {
            admin.setProfileComplete(false);
        }
        return adminRepository.save(admin);
    }

    public Admin getAdminById(Long id) {
        return adminRepository.findById(id).orElse(null);
    }

    public Admin updateAdmin(Long id, Admin adminDetails) {
        Admin admin = adminRepository.findById(id).orElse(null);
        if (admin != null) {
            // Update only non-null fields
            if (adminDetails.getName() != null) {
                admin.setName(adminDetails.getName());
            }
            if (adminDetails.getEmail() != null) {
                admin.setEmail(adminDetails.getEmail());
            }
            if (adminDetails.getPassword() != null) {
                // Encrypt password before updating
                if (!passwordService.isEncrypted(adminDetails.getPassword())) {
                    admin.setPassword(passwordService.encryptPassword(adminDetails.getPassword()));
                } else {
                    admin.setPassword(adminDetails.getPassword());
                }
            }
            if (adminDetails.getRole() != null) {
                admin.setRole(adminDetails.getRole());
            }
            if (adminDetails.getPan() != null) {
                admin.setPan(adminDetails.getPan());
            }
            // Update new profile fields
            if (adminDetails.getEmployeeId() != null) {
                admin.setEmployeeId(adminDetails.getEmployeeId());
            }
            if (adminDetails.getAddress() != null) {
                admin.setAddress(adminDetails.getAddress());
            }
            if (adminDetails.getAadharNumber() != null) {
                admin.setAadharNumber(adminDetails.getAadharNumber());
            }
            if (adminDetails.getMobileNumber() != null) {
                admin.setMobileNumber(adminDetails.getMobileNumber());
            }
            if (adminDetails.getQualifications() != null) {
                admin.setQualifications(adminDetails.getQualifications());
            }
            if (adminDetails.getProfileComplete() != null) {
                admin.setProfileComplete(adminDetails.getProfileComplete());
            }
            return adminRepository.save(admin);
        }
        return null;
    }
    
    /**
     * Get admin by email (for profile retrieval)
     */
    public Admin getAdminByEmail(String email) {
        return adminRepository.findByEmail(email);
    }

    public Admin login(String email, String password) {
        Admin admin = adminRepository.findByEmail(email);
        if (admin != null) {
            // Initialize failed attempts if null (for existing admins)
            if (admin.getFailedLoginAttempts() == null) {
                admin.setFailedLoginAttempts(0);
            }
            
            // Check if account is locked
            if (admin.getAccountLocked() != null && admin.getAccountLocked()) {
                System.out.println("⚠️ Login attempt blocked - account already locked for: " + email);
                return null; // Account is locked
            }
            
            // Verify password - check if admin password exists
            String adminPassword = admin.getPassword();
            if (adminPassword == null || adminPassword.isEmpty()) {
                System.out.println("⚠️ Admin password is null or empty for: " + email);
                // Still increment failed attempts even if password is null
                int currentAttempts = admin.getFailedLoginAttempts() != null ? admin.getFailedLoginAttempts() : 0;
                int newAttempts = currentAttempts + 1;
                admin.setFailedLoginAttempts(newAttempts);
                admin.setLastFailedLoginTime(LocalDateTime.now());
                
                if (newAttempts >= 3) {
                    admin.setAccountLocked(true);
                    System.out.println("🔒 Admin account LOCKED due to 3 failed login attempts (null password): " + email);
                }
                adminRepository.save(admin);
                return null;
            }
            
            // Check if password is in correct format, if not, it might be an old format
            // For now, we'll try to verify it, but log a warning
            boolean isEncrypted = passwordService.isEncrypted(adminPassword);
            if (!isEncrypted) {
                System.out.println("⚠️ WARNING: Admin password for " + email + " is not in encrypted format!");
                System.out.println("   Password length: " + adminPassword.length());
                System.out.println("   Password contains colon: " + adminPassword.contains(":"));
                System.out.println("   This might be an old password format. Password verification will likely fail.");
                System.out.println("   Consider resetting the password through manager dashboard.");
            }
            
            // Verify password - add detailed logging
            System.out.println("🔐 Password verification - Email: " + email);
            System.out.println("   Input password length: " + (password != null ? password.length() : "null"));
            System.out.println("   Stored password length: " + (adminPassword != null ? adminPassword.length() : "null"));
            System.out.println("   Stored password format check - Is encrypted: " + isEncrypted);
            System.out.println("   Stored password contains colon: " + (adminPassword != null && adminPassword.contains(":")));
            
            boolean passwordValid = false;
            try {
                passwordValid = passwordService.verifyPassword(password, adminPassword);
                System.out.println("   Password verification result: " + passwordValid);
            } catch (Exception e) {
                System.out.println("   ❌ Exception during password verification: " + e.getMessage());
                e.printStackTrace();
                passwordValid = false;
            }
            
            if (passwordValid) {
                // Successful login - reset failed attempts
                admin.setFailedLoginAttempts(0);
                admin.setAccountLocked(false);
                admin.setLastFailedLoginTime(null);
                System.out.println("✅ Successful login for admin: " + email + " - Failed attempts reset");
                return adminRepository.save(admin);
            } else {
                // Failed login - increment attempts
                int currentAttempts = admin.getFailedLoginAttempts() != null ? admin.getFailedLoginAttempts() : 0;
                int newAttempts = currentAttempts + 1;
                admin.setFailedLoginAttempts(newAttempts);
                admin.setLastFailedLoginTime(LocalDateTime.now());
                
                System.out.println("❌ Failed login attempt " + newAttempts + "/3 for admin: " + email);
                
                // Lock account after 3 failed attempts
                if (newAttempts >= 3) {
                    admin.setAccountLocked(true);
                    System.out.println("🔒 Admin account LOCKED due to 3 failed login attempts: " + email);
                }
                
                Admin savedAdmin = adminRepository.save(admin);
                System.out.println("💾 Saved admin with failed attempts: " + savedAdmin.getFailedLoginAttempts() + ", locked: " + savedAdmin.getAccountLocked());
                return null;
            }
        }
        System.out.println("❌ Admin not found for email: " + email);
        return null;
    }
    
    /**
     * Unblock an admin account
     */
    public Admin unblockAdmin(Long adminId) {
        Admin admin = adminRepository.findById(adminId).orElse(null);
        if (admin != null) {
            admin.setAccountLocked(false);
            admin.setFailedLoginAttempts(0);
            admin.setLastFailedLoginTime(null);
            return adminRepository.save(admin);
        }
        return null;
    }
    
    /**
     * Reset admin password
     */
    public Admin resetAdminPassword(Long adminId, String newPassword) {
        Admin admin = adminRepository.findById(adminId).orElse(null);
        if (admin != null) {
            // Encrypt and set new password
            admin.setPassword(passwordService.encryptPassword(newPassword));
            // Also unblock account if it was locked
            admin.setAccountLocked(false);
            admin.setFailedLoginAttempts(0);
            admin.setLastFailedLoginTime(null);
            return adminRepository.save(admin);
        }
        return null;
    }
    
    /**
     * Get all blocked admins
     */
    public List<Admin> getBlockedAdmins() {
        return adminRepository.findBlockedAdmins();
    }
    
    /**
     * Get all admins (for checking if manager exists)
     */
    public List<Admin> getAllAdmins() {
        return adminRepository.findAll();
    }
}
