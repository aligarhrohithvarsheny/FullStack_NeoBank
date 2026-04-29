package com.neo.springapp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;

/**
 * Scheduled run for automatic bank charges:
 * - 1st of every month: monthly bank charges (Rs 496) and debit card charges (Rs 596 every 6 months).
 */
@Service
public class BankChargesSchedulerService {

    @Autowired
    private BankChargesService bankChargesService;

    @Scheduled(cron = "0 0 3 1 * ?")
    @Transactional
    public void processMonthlyAndDebitCardCharges() {
        try {
            System.out.println("🔄 Starting automatic bank charges at " + LocalDate.now());
            Map<String, Object> monthly = bankChargesService.processMonthlyBankCharges();
            System.out.println("   Monthly bank charges: " + monthly.get("message"));
            Map<String, Object> debitCard = bankChargesService.processDebitCardChargesEvery6Months();
            System.out.println("   Debit card (6m) charges: " + debitCard.get("message"));
            System.out.println("✅ Automatic bank charges completed.");
        } catch (Exception e) {
            System.err.println("❌ Error in automatic bank charges: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
