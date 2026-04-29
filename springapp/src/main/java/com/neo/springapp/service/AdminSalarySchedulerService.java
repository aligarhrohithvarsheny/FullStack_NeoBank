package com.neo.springapp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Daily salary auto-deposit:
 * - Uses verified attendance (ID card verified)
 * - Transfers ₹850 from manager branch account -> admin salary account
 * - Skips already-paid records
 */
@Service
public class AdminSalarySchedulerService {

    @Autowired
    private AdminSalaryService adminSalaryService;

    /** Runs daily at 7:00 PM. */
    @Scheduled(cron = "0 0 19 * * ?")
    @Transactional
    public void autoPayDailySalary() {
        try {
            LocalDate today = LocalDate.now();
            adminSalaryService.payDailySalary(today, "AutoScheduler");
        } catch (Exception e) {
            System.err.println("❌ Salary auto-pay failed: " + e.getMessage());
        }
    }
}

