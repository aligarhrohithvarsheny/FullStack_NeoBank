package com.neo.springapp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;

/**
 * Scheduled service for processing monthly FD interest credits
 * Runs on the 1st day of every month at 2:00 AM
 */
@Service
public class FDInterestSchedulerService {

    @Autowired
    private FixedDepositService fixedDepositService;

    /**
     * Process monthly interest credit for all active FDs
     * Scheduled to run on the 1st day of every month at 2:00 AM
     * Cron expression: "0 0 2 1 * ?" = second, minute, hour, day of month, month, day of week
     */
    @Scheduled(cron = "0 0 2 1 * ?")
    @Transactional
    public void processMonthlyFDInterest() {
        try {
            System.out.println("🔄 Starting monthly FD interest credit process at " + LocalDate.now());
            Map<String, Object> result = fixedDepositService.processAllMonthlyInterestCredits();
            
            if ((Boolean) result.get("success")) {
                System.out.println("✅ Monthly FD interest credit completed successfully");
                System.out.println("   Success: " + result.get("successCount"));
                System.out.println("   Failures: " + result.get("failureCount"));
            } else {
                System.err.println("❌ Monthly FD interest credit failed: " + result.get("message"));
            }
        } catch (Exception e) {
            System.err.println("❌ Error in scheduled monthly FD interest credit: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Alternative: Process monthly interest on the last day of every month at 11:00 PM
     * Uncomment this method and comment the above if you prefer end-of-month processing
     */
    /*
    @Scheduled(cron = "0 0 23 L * ?")
    @Transactional
    public void processMonthlyFDInterestEndOfMonth() {
        try {
            System.out.println("🔄 Starting monthly FD interest credit process (end of month) at " + LocalDate.now());
            Map<String, Object> result = fixedDepositService.processAllMonthlyInterestCredits();
            
            if ((Boolean) result.get("success")) {
                System.out.println("✅ Monthly FD interest credit completed successfully");
                System.out.println("   Success: " + result.get("successCount"));
                System.out.println("   Failures: " + result.get("failureCount"));
            } else {
                System.err.println("❌ Monthly FD interest credit failed: " + result.get("message"));
            }
        } catch (Exception e) {
            System.err.println("❌ Error in scheduled monthly FD interest credit: " + e.getMessage());
            e.printStackTrace();
        }
    }
    */
}

