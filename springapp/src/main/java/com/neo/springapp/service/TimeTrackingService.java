package com.neo.springapp.service;

import com.neo.springapp.model.EmployeeTimeTracking;
import com.neo.springapp.model.EmployeeTimeTracking.TimeTrackingStatus;
import com.neo.springapp.model.TimeManagementPolicy;
import com.neo.springapp.repository.EmployeeTimeTrackingRepository;
import com.neo.springapp.repository.TimeManagementPolicyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class TimeTrackingService {
    
    @Autowired
    private EmployeeTimeTrackingRepository timeTrackingRepository;
    
    @Autowired
    private TimeManagementPolicyRepository policyRepository;

    // ==================== CHECK IN / CHECK OUT ====================

    /**
     * Record employee check-in
     */
    public EmployeeTimeTracking recordCheckIn(String adminId, String adminName, String email, String idCardNumber) {
        LocalDate today = LocalDate.now();
        
        // Avoid duplicate check-ins on the same day
        Optional<EmployeeTimeTracking> existingRecord = timeTrackingRepository.findByAdminIdAndTrackingDate(adminId, today);
        if (existingRecord.isPresent() && existingRecord.get().getCheckInTime() != null) {
            log.warn("Employee {} already checked in today", adminId);
            return existingRecord.get();
        }
        
        EmployeeTimeTracking tracking = EmployeeTimeTracking.builder()
            .adminId(adminId)
            .adminName(adminName)
            .adminEmail(email)
            .idCardNumber(idCardNumber)
            .trackingDate(today)
            .checkInTime(LocalDateTime.now())
            .status(TimeTrackingStatus.ACTIVE)
            .totalWorkingHours(0.0)
            .overtimeHours(0.0)
            .build();
        
        return timeTrackingRepository.save(tracking);
    }

    /**
     * Record employee check-out and calculate working hours
     */
    public EmployeeTimeTracking recordCheckOut(String adminId) {
        LocalDate today = LocalDate.now();
        
        Optional<EmployeeTimeTracking> optionalRecord = timeTrackingRepository.findByAdminIdAndTrackingDate(adminId, today);
        if (optionalRecord.isEmpty()) {
            log.error("No check-in record found for employee {}", adminId);
            throw new RuntimeException("No check-in record found for today");
        }
        
        EmployeeTimeTracking tracking = optionalRecord.get();
        tracking.setCheckOutTime(LocalDateTime.now());
        tracking.setStatus(TimeTrackingStatus.CHECKED_OUT);
        
        // Calculate working hours
        double workingHours = calculateWorkingHours(tracking.getCheckInTime(), tracking.getCheckOutTime());
        tracking.setTotalWorkingHours(workingHours);
        
        // Calculate overtime
        double overtimeHours = calculateOvertimeHours(adminId, workingHours);
        tracking.setOvertimeHours(overtimeHours);
        
        return timeTrackingRepository.save(tracking);
    }

    // ==================== WORKING HOURS CALCULATION ====================

    /**
     * Calculate working hours between check-in and check-out
     */
    private double calculateWorkingHours(LocalDateTime checkIn, LocalDateTime checkOut) {
        if (checkIn == null || checkOut == null) {
            return 0.0;
        }
        
        Duration duration = Duration.between(checkIn, checkOut);
        // Convert duration to hours (including minutes as decimal)
        return duration.toMinutes() / 60.0;
    }

    /**
     * Calculate overtime hours based on policy
     */
    private double calculateOvertimeHours(String adminId, double totalHours) {
        Optional<TimeManagementPolicy> policy = policyRepository.findActiveByAdminId(adminId);
        if (policy.isEmpty()) {
            return 0.0;
        }
        
        int workingHoursPerDay = policy.get().getWorkingHoursPerDay();
        if (totalHours > workingHoursPerDay) {
            return totalHours - workingHoursPerDay;
        }
        
        return 0.0;
    }

    /**
     * Get employee's total working hours for a period
     */
    public Map<String, Object> getEmployeeWorkingHours(String adminId, LocalDate startDate, LocalDate endDate) {
        Double totalHours = timeTrackingRepository.getTotalWorkingHours(adminId, startDate, endDate);
        Double overtimeHours = timeTrackingRepository.getTotalOvertimeHours(adminId, startDate, endDate);
        
        long totalDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        double averageHours = totalDays > 0 ? (totalHours != null ? totalHours : 0.0) / totalDays : 0.0;
        
        Map<String, Object> result = new HashMap<>();
        result.put("totalHours", totalHours != null ? totalHours : 0.0);
        result.put("overtimeHours", overtimeHours != null ? overtimeHours : 0.0);
        result.put("averageHours", averageHours);
        result.put("totalDays", totalDays);
        
        return result;
    }

    /**
     * Get employee's weekly working hours
     */
    public Double getWeeklyWorkingHours(String adminId) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(6);
        return timeTrackingRepository.getTotalWorkingHours(adminId, startDate, endDate);
    }

    /**
     * Get employee's monthly working hours
     */
    public Double getMonthlyWorkingHours(String adminId) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.withDayOfMonth(1);
        return timeTrackingRepository.getTotalWorkingHours(adminId, startDate, endDate);
    }

    // ==================== TIME RECORDS MANAGEMENT ====================

    /**
     * Get employee's today's record
     */
    public EmployeeTimeTracking getTodayRecord(String adminId) {
        return timeTrackingRepository.findByAdminIdAndTrackingDate(adminId, LocalDate.now())
            .orElse(null);
    }

    /**
     * Get employee's records for a date range
     */
    public List<EmployeeTimeTracking> getEmployeeTimeRecords(String adminId, LocalDate startDate, LocalDate endDate) {
        return timeTrackingRepository.findByAdminIdAndDateRange(adminId, startDate, endDate);
    }

    /**
     * Get all time records with filters and pagination
     */
    public Page<EmployeeTimeTracking> getAllTimeRecords(TimeTrackingStatus status, String search, Pageable pageable) {
        return timeTrackingRepository.findAllWithFilters(status, search, pageable);
    }

    /**
     * Adjust time record (admin override)
     */
    public EmployeeTimeTracking adjustTimeRecord(Long recordId, LocalDateTime newCheckIn, LocalDateTime newCheckOut, String remarks) {
        EmployeeTimeTracking tracking = timeTrackingRepository.findById(recordId)
            .orElseThrow(() -> new RuntimeException("Time record not found"));
        
        if (newCheckIn != null) {
            tracking.setCheckInTime(newCheckIn);
        }
        if (newCheckOut != null) {
            tracking.setCheckOutTime(newCheckOut);
        }
        
        // Recalculate working hours
        if (tracking.getCheckInTime() != null && tracking.getCheckOutTime() != null) {
            double workingHours = calculateWorkingHours(tracking.getCheckInTime(), tracking.getCheckOutTime());
            tracking.setTotalWorkingHours(workingHours);
            double overtime = calculateOvertimeHours(tracking.getAdminId(), workingHours);
            tracking.setOvertimeHours(overtime);
        }
        
        if (remarks != null) {
            tracking.setRemarks(remarks);
        }
        
        log.info("Time record adjusted for {} - New working hours: {}", tracking.getAdminId(), tracking.getTotalWorkingHours());
        
        return timeTrackingRepository.save(tracking);
    }

    /**
     * Mark employee as absent
     */
    public EmployeeTimeTracking markAbsent(String adminId, String adminName, String email, String idCardNumber, LocalDate date, String reason) {
        Optional<EmployeeTimeTracking> existing = timeTrackingRepository.findByAdminIdAndTrackingDate(adminId, date);
        
        EmployeeTimeTracking tracking;
        if (existing.isPresent()) {
            tracking = existing.get();
        } else {
            tracking = new EmployeeTimeTracking();
            tracking.setAdminId(adminId);
            tracking.setAdminName(adminName);
            tracking.setAdminEmail(email);
            tracking.setIdCardNumber(idCardNumber);
            tracking.setTrackingDate(date);
        }
        
        tracking.setStatus(TimeTrackingStatus.ABSENT);
        tracking.setRemarks(reason != null ? reason : "Marked as absent");
        
        log.info("Employee {} marked as absent on {}", adminId, date);
        
        return timeTrackingRepository.save(tracking);
    }

    /**
     * Mark employee as on leave
     */
    public EmployeeTimeTracking markOnLeave(String adminId, String adminName, String email, String idCardNumber, LocalDate date, String reason) {
        Optional<EmployeeTimeTracking> existing = timeTrackingRepository.findByAdminIdAndTrackingDate(adminId, date);
        
        EmployeeTimeTracking tracking;
        if (existing.isPresent()) {
            tracking = existing.get();
        } else {
            tracking = new EmployeeTimeTracking();
            tracking.setAdminId(adminId);
            tracking.setAdminName(adminName);
            tracking.setAdminEmail(email);
            tracking.setIdCardNumber(idCardNumber);
            tracking.setTrackingDate(date);
        }
        
        tracking.setStatus(TimeTrackingStatus.ON_LEAVE);
        tracking.setRemarks(reason != null ? reason : "On leave");
        
        log.info("Employee {} marked as on leave on {}", adminId, date);
        
        return timeTrackingRepository.save(tracking);
    }

    // ==================== ATTENDANCE STATISTICS ====================

    /**
     * Get daily attendance statistics
     */
    public Map<String, Object> getDailyAttendanceStats(LocalDate date) {
        long totalEmployees = timeTrackingRepository.findByDate(date).stream()
            .map(EmployeeTimeTracking::getAdminId)
            .distinct()
            .count();
        
        long presentCount = timeTrackingRepository.countCheckedInForDate(date);
        long absentCount = timeTrackingRepository.countAbsentForDate(date);
        long onLeaveCount = timeTrackingRepository.countOnLeaveForDate(date);
        
        Double averageHours = timeTrackingRepository.getAverageWorkingHoursForDate(date);
        averageHours = averageHours != null ? averageHours : 0.0;
        
        long pendingCheckouts = timeTrackingRepository.findByDateAndStatus(date, TimeTrackingStatus.ACTIVE).size();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("date", date);
        stats.put("totalEmployees", totalEmployees);
        stats.put("presentCount", presentCount);
        stats.put("absentCount", absentCount);
        stats.put("onLeaveCount", onLeaveCount);
        stats.put("averageWorkingHours", averageHours);
        stats.put("overdueCheckouts", pendingCheckouts);
        
        return stats;
    }

    /**
     * Get overall time tracking statistics
     */
    public Map<String, Object> getTimeTrackingStats() {
        LocalDate today = LocalDate.now();
        List<EmployeeTimeTracking> todaysRecords = timeTrackingRepository.findByDate(today);
        
        long totalEmployees = todaysRecords.stream()
            .map(EmployeeTimeTracking::getAdminId)
            .distinct()
            .count();
        
        long checkedIn = todaysRecords.stream()
            .filter(r -> r.getCheckInTime() != null)
            .distinct()
            .count();
        
        long checkedOut = todaysRecords.stream()
            .filter(r -> TimeTrackingStatus.CHECKED_OUT.equals(r.getStatus()))
            .count();
        
        long onLeave = todaysRecords.stream()
            .filter(r -> TimeTrackingStatus.ON_LEAVE.equals(r.getStatus()))
            .count();
        
        Double averageHours = todaysRecords.stream()
            .mapToDouble(r -> r.getTotalWorkingHours() != null ? r.getTotalWorkingHours() : 0.0)
            .average()
            .orElse(0.0);
        
        long overdueCheckouts = todaysRecords.stream()
            .filter(r -> TimeTrackingStatus.ACTIVE.equals(r.getStatus()))
            .count();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalEmployees", totalEmployees);
        stats.put("checkedInToday", checkedIn);
        stats.put("checkedOutToday", checkedOut);
        stats.put("onLeaveToday", onLeave);
        stats.put("averageWorkingHours", averageHours);
        stats.put("overdueCheckPuts", overdueCheckouts);
        stats.put("timestamp", LocalDateTime.now());
        
        return stats;
    }

    /**
     * Get employee working hours summary for all employees
     */
    public List<Map<String, Object>> getAllEmployeeWorkingHoursSummary() {
        LocalDate endDate = LocalDate.now();
        LocalDate weekStart = endDate.minusDays(6);
        LocalDate monthStart = endDate.withDayOfMonth(1);
        
        // Get all unique employees from today's records
        List<EmployeeTimeTracking> allRecords = timeTrackingRepository.findByDate(endDate);
        
        return allRecords.stream()
            .map(EmployeeTimeTracking::getAdminId)
            .distinct()
            .map(adminId -> {
                Optional<EmployeeTimeTracking> firstRecord = allRecords.stream()
                    .filter(r -> r.getAdminId().equals(adminId))
                    .findFirst();
                
                if (firstRecord.isEmpty()) return null;
                
                EmployeeTimeTracking record = firstRecord.get();
                Double weeklyHours = timeTrackingRepository.getTotalWorkingHours(adminId, weekStart, endDate);
                Double monthlyHours = timeTrackingRepository.getTotalWorkingHours(adminId, monthStart, endDate);
                Double weeklyAverage = weeklyHours != null ? weeklyHours / 7 : 0.0;
                Double monthlyOvertime = timeTrackingRepository.getTotalOvertimeHours(adminId, monthStart, endDate);
                
                // Calculate attendance percentage
                long presentDays = ChronoUnit.DAYS.between(monthStart, endDate) + 1;
                long actualPresent = timeTrackingRepository.findByAdminIdAndDateRange(adminId, monthStart, endDate).stream()
                    .filter(r -> !TimeTrackingStatus.ABSENT.equals(r.getStatus()))
                    .count();
                double attendancePercent = presentDays > 0 ? (actualPresent * 100.0) / presentDays : 0.0;
                
                Map<String, Object> summary = new HashMap<>();
                summary.put("adminId", adminId);
                summary.put("adminName", record.getAdminName());
                summary.put("email", record.getAdminEmail());
                summary.put("idCardNumber", record.getIdCardNumber());
                summary.put("hoursThisWeek", weeklyHours != null ? weeklyHours : 0.0);
                summary.put("hoursThisMonth", monthlyHours != null ? monthlyHours : 0.0);
                summary.put("averageDailyHours", weeklyAverage);
                summary.put("overtimeHours", monthlyOvertime != null ? monthlyOvertime : 0.0);
                summary.put("attendancePercentage", attendancePercent);
                
                return summary;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
}
