package com.neo.springapp.config;

import com.neo.springapp.model.Admin;
import com.neo.springapp.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Default Manager Initializer
 * 
 * Automatically creates a default manager account on application startup
 * if no manager account exists in the database.
 * 
 * This ensures the system always has at least one manager account for initial access.
 * Password encryption is handled automatically by AdminService.saveAdmin().
 */
@Component
public class DefaultManagerInitializer implements ApplicationRunner {

    @Autowired
    private AdminService adminService;

    private static final String DEFAULT_MANAGER_EMAIL = "manager@neobank.com";
    private static final String DEFAULT_MANAGER_PASSWORD = "manager123";
    private static final String DEFAULT_MANAGER_NAME = "Manager";
    private static final String DEFAULT_MANAGER_ROLE = "MANAGER";
    private static final String DEFAULT_MANAGER_EMPLOYEE_ID = "MGR001";

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try {
            // Check if any manager exists using AdminService
            List<Admin> allAdmins = adminService.getAllAdmins();
            boolean managerExists = allAdmins.stream()
                .anyMatch(admin -> DEFAULT_MANAGER_ROLE.equalsIgnoreCase(admin.getRole()));

            if (!managerExists) {
                System.out.println("========================================");
                System.out.println("🔄 Creating default manager account...");
                
                // Create default manager
                Admin manager = new Admin();
                manager.setName(DEFAULT_MANAGER_NAME);
                manager.setEmail(DEFAULT_MANAGER_EMAIL);
                manager.setPassword(DEFAULT_MANAGER_PASSWORD); // Will be encrypted by AdminService.saveAdmin()
                manager.setRole(DEFAULT_MANAGER_ROLE);
                manager.setEmployeeId(DEFAULT_MANAGER_EMPLOYEE_ID);
                manager.setProfileComplete(false);
                manager.setAccountLocked(false);
                manager.setFailedLoginAttempts(0);

                // Save manager using AdminService (handles password encryption automatically)
                Admin savedManager = adminService.saveAdmin(manager);

                System.out.println("✅ Default manager account created successfully!");
                System.out.println("   Email: " + DEFAULT_MANAGER_EMAIL);
                System.out.println("   Password: " + DEFAULT_MANAGER_PASSWORD);
                System.out.println("   Role: " + DEFAULT_MANAGER_ROLE);
                System.out.println("   ID: " + savedManager.getId());
                System.out.println("⚠️  IMPORTANT: Change the default password after first login!");
                System.out.println("========================================");
            } else {
                System.out.println("ℹ️  Manager account already exists - skipping default manager creation");
            }
        } catch (Exception e) {
            System.err.println("❌ Error creating default manager account: " + e.getMessage());
            e.printStackTrace();
            // Don't fail application startup if manager creation fails
            // The admin can create a manager manually via API endpoint /api/admins/create-default-manager
        }
    }
}

