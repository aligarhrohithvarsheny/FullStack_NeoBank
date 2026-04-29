package com.neo.springapp.controller;

import com.neo.springapp.service.AdminSalaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/manager")
public class AdminSalaryController {

    @Autowired
    private AdminSalaryService adminSalaryService;

    @PostMapping("/attendance/verify")
    public ResponseEntity<Map<String, Object>> verifyAttendance(@RequestBody Map<String, String> body) {
        String idCardNumber = body.get("idCardNumber");
        String verifiedBy = body.getOrDefault("verifiedBy", "Manager");
        Map<String, Object> resp = adminSalaryService.verifyAttendanceByIdCard(idCardNumber, verifiedBy);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/attendance/summary")
    public ResponseEntity<Map<String, Object>> getAttendanceSummary(@RequestParam(required = false) String date) {
        LocalDate d = null;
        if (date != null && !date.isBlank()) {
            try { d = LocalDate.parse(date.trim()); } catch (DateTimeParseException ignored) {}
        }
        return ResponseEntity.ok(adminSalaryService.getAttendanceSummary(d));
    }

    @GetMapping("/attendance/admin/{adminId}")
    public ResponseEntity<Map<String, Object>> getAdminAttendanceForYear(
            @PathVariable Long adminId,
            @RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(adminSalaryService.getAdminAttendanceForYear(adminId, year));
    }

    @PostMapping("/salary/pay-daily")
    public ResponseEntity<Map<String, Object>> payDaily(@RequestBody Map<String, String> body) {
        LocalDate d = null;
        String date = body.get("date");
        if (date != null && !date.isBlank()) {
            try { d = LocalDate.parse(date.trim()); } catch (DateTimeParseException ignored) {}
        }
        String paidBy = body.getOrDefault("paidBy", "Manager");
        return ResponseEntity.ok(adminSalaryService.payDailySalary(d, paidBy));
    }

    @PostMapping("/salary/pay-selected")
    public ResponseEntity<Map<String, Object>> paySelected(@RequestBody Map<String, Object> body) {
        Object adminIdObj = body.get("adminId");
        Object datesObj = body.get("dates");
        String paidBy = (String) body.getOrDefault("paidBy", "Manager");
        Long adminId = null;
        if (adminIdObj instanceof Number) {
            adminId = ((Number) adminIdObj).longValue();
        } else if (adminIdObj instanceof String s && !s.isBlank()) {
            adminId = Long.parseLong(s);
        }
        java.util.List<java.time.LocalDate> dates = new java.util.ArrayList<>();
        if (datesObj instanceof java.util.List<?> list) {
            for (Object o : list) {
                if (o instanceof String s && !s.isBlank()) {
                    try { dates.add(java.time.LocalDate.parse(s.trim())); } catch (java.time.format.DateTimeParseException ignored) {}
                }
            }
        }
        return ResponseEntity.ok(adminSalaryService.paySelectedDays(adminId, dates, paidBy));
    }

    @GetMapping("/salary/config")
    public ResponseEntity<Map<String, Object>> getConfig() {
        Map<String, Object> cfg = new HashMap<>();
        cfg.put("dailySalaryRs", AdminSalaryService.DAILY_SALARY_RS);
        return ResponseEntity.ok(cfg);
    }
}

