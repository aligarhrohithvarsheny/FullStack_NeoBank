package com.neo.springapp.config;

import com.neo.springapp.model.Account;
import com.neo.springapp.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Ensures NeoBank's internal account (NEOBANK000001) exists for receiving
 * bank charges, loan charges, debit card charges, and CIBIL charges.
 */
@Component
@Order(2)
public class NeoBankAccountInitializer implements ApplicationRunner {

    @Autowired
    private AccountRepository accountRepository;

    public static final String NEOBANK_ACCOUNT_NUMBER = "NEOBANK000001";
    private static final String NEOBANK_AADHAR = "AADHARNEOBANK00001";
    private static final String NEOBANK_PAN = "PANNEOBANK00001";
    private static final String NEOBANK_PHONE = "9800000000";

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (accountRepository.findByAccountNumber(NEOBANK_ACCOUNT_NUMBER) != null) {
            return;
        }
        try {
            Account bank = new Account();
            bank.setName("NeoBank Official");
            bank.setAccountNumber(NEOBANK_ACCOUNT_NUMBER);
            bank.setStatus("ACTIVE");
            bank.setAccountType("Current");
            bank.setBalance(0.0);
            bank.setAadharNumber(NEOBANK_AADHAR);
            bank.setPan(NEOBANK_PAN);
            bank.setPhone(NEOBANK_PHONE);
            bank.setDob("01-01-2000");
            bank.setAge(25);
            bank.setOccupation("Bank");
            bank.setAddress("NeoBank Head Office");
            accountRepository.save(bank);
            System.out.println("✅ NeoBank internal account created: " + NEOBANK_ACCOUNT_NUMBER);
        } catch (Exception e) {
            System.err.println("❌ Failed to create NeoBank account: " + e.getMessage());
        }
    }
}
