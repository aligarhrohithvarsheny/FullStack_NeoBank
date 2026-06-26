package com.neo.springapp.config;

import com.neo.springapp.repository.AccountRepository;
import com.neo.springapp.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Assigns mandatory Customer ID (9 digits: PAN 4 + DOB 5) to all existing accounts
 * that don't have one. Runs on application startup after DefaultManagerInitializer.
 */
@Component
@Order(2) // Run after DefaultManagerInitializer (Order 1)
public class CustomerIdMigrationRunner implements ApplicationRunner {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try {
            long pending = accountRepository.countAccountsWithoutCustomerId();
            if (pending > 0) {
                System.out.println("🔄 Assigning Customer IDs to " + pending + " existing account(s)...");
                int count = accountService.assignCustomerIdsToExistingAccounts();
                System.out.println("✅ Assigned Customer ID to " + count + " account(s)");
            }
        } catch (Exception e) {
            System.err.println("⚠️ Customer ID migration warning: " + e.getMessage());
            // Don't fail startup - admin can call POST /api/admins/accounts/assign-customer-ids manually
        }
    }
}
