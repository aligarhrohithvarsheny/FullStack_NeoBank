package com.neo.springapp.controller;

import com.neo.springapp.model.EmployeeTimeTracking;
import com.neo.springapp.model.EmployeeTimeTracking.TimeTrackingStatus;
import com.neo.springapp.model.TimeManagementPolicy;
import com.neo.springapp.service.TimeTrackingService;
import com.neo.springapp.service.TimeManagementPolicyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/time-tracking")
@Slf4j
@CrossOrigin(origins = "*")
public class TimeTrackingController {
    
    @Autowired
    private TimeTrackingService timeTrackingService;
    
    @Autowired
    private TimeManagementPolicyService policyService;

    // ==================== CHECK IN / CHECK OUT ====================

    /**
     * Record employee check-in
     */
    @PostMapping("/check-in")
    public ResponseEntity<Map<String, Object>> checkIn(@RequestBody Map<String, String> request) {
        try {
            String adminId = request.get("adminId");
            String adminName = request.get("adminName");
            String email = request.get("adminEmail");
            String idCardNumber = request.get("idCardNumber");
            
            if (adminId == null || adminName == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Missing required fields"));
            }
            
            EmployeeTimeTracking record = timeTrackingService.recordCheckIn(adminId, adminName, email, idCardNumber);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Check-in recorded successfully");
            response.put("data", record);
            response.put("checkInTime", record.getCheckInTime());
            
            log.info("Employee {} checked in", adminId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error during check-in", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Record employee check-out
     */
    @PostMapping("/check-out")
    public ResponseEntity<Map<String, Object>> checkOut(@RequestBody Map<String, String> request) {
        try {
            String adminId = request.get("adminId");
            
            if (adminId == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "adminId is required"));
            }
            
            EmployeeTimeTracking record = timeTrackingService.recordCheckOut(adminId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Check-out recorded successfully");
            response.put("data", record);
            response.put("workingHours", record.getTotalWorkingHours());
            response.put("overtimeHours", record.getOvertimeHours());
            
            log.info("Employee {} checked out - Working hours: {}", adminId, record.getTotalWorkingHours());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error during check-out", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ==================== EMPLOYEE TIME RECORDS ====================

    /**
     * Get employee's today's record
     */
    @GetMapping("/today-record/{adminId}")
    public ResponseEntity<Map<String, Object>> getTodayRecord(@PathVariable String adminId) {
        try {
            EmployeeTimeTracking record = timeTrackingService.getTodayRecord(adminId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", record);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching today's record", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Get employee time records for a date range
     */
    @GetMapping("/records/{adminId}")
    public ResponseEntity<Map<String, Object>> getTimeRecords(
            @PathVariable String adminId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
            LocalDate start = startDate != null ? LocalDate.parse(startDate, formatter) : LocalDate.now().minusDays(30);
            LocalDate end = endDate != null ? LocalDate.parse(endDate, formatter) : LocalDate.now();
            
            List<EmployeeTimeTracking> records = timeTrackingService.getEmployeeTimeRecords(adminId, start, end);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", records);
            response.put("total", records.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching time records", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Get all time records with filters and pagination
     */
    @GetMapping("/all-records")
    public ResponseEntity<Map<String, Object>> getAllTimeRecords(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        try {
            TimeTrackingStatus trackingStatus = null;
            if (status != null && !status.isEmpty() && !status.equals("ALL")) {
                trackingStatus = TimeTrackingStatus.valueOf(status.toUpperCase());
            }
            
            Pageable pageable = PageRequest.of(page, size);
            Page<EmployeeTimeTracking> records = timeTrackingService.getAllTimeRecords(trackingStatus, search, pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("content", records.getContent());
            response.put("currentPage", records.getNumber());
            response.put("totalElements", records.getTotalElements());
            response.put("totalPages", records.getTotalPages());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching all time records", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Adjust time record (admin override)
     */
    @PutMapping("/record/{recordId}/adjust")
    public ResponseEntity<Map<String, Object>> adjustTimeRecord(
            @PathVariable Long recordId,
            @RequestBody Map<String, String> adjustments) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_TIME;
            LocalDateTime newCheckIn = null;
            LocalDateTime newCheckOut = null;
            
            if (adjustments.get("checkInTime") != null) {
                LocalDate date = LocalDate.now();
                newCheckIn = LocalDateTime.of(date, java.time.LocalTime.parse(adjustments.get("checkInTime"), formatter));
            }
            if (adjustments.get("checkOutTime") != null) {
                LocalDate date = LocalDate.now();
                newCheckOut = LocalDateTime.of(date, java.time.LocalTime.parse(adjustments.get("checkOutTime"), formatter));
            }
            
            String remarks = adjustments.get("remarks");
            EmployeeTimeTracking record = timeTrackingService.adjustTimeRecord(recordId, newCheckIn, newCheckOut, remarks);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Time record adjusted successfully");
            response.put("data", record);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error adjusting time record", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ==================== TIME MANAGEMENT ACTIONS ====================

    /**
     * Mark employee as absent
     */
    @PostMapping("/mark-absent")
    public ResponseEntity<Map<String, Object>> markAbsent(@RequestBody Map<String, Object> request) {
        try {
            String adminId = (String) request.get("adminId");
            String adminName = (String) request.get("adminName");
            String email = (String) request.get("email");
            String idCardNumber = (String) request.get("idCardNumber");
            String dateStr = (String) request.get("date");
            String reason = (String) request.get("reason");
            
            LocalDate date = dateStr != null ? LocalDate.parse(dateStr) : LocalDate.now();
            
            EmployeeTimeTracking record = timeTrackingService.markAbsent(adminId, adminName, email, idCardNumber, date, reason);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Employee marked as absent");
            response.put("data", record);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error marking absent", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Mark employee as on leave
     */
    @PostMapping("/mark-leave")
    public ResponseEntity<Map<String, Object>> markOnLeave(@RequestBody Map<String, Object> request) {
        try {
            String adminId = (String) request.get("adminId");
            String adminName = (String) request.get("adminName");
            String email = (String) request.get("email");
            String idCardNumber = (String) request.get("idCardNumber");
            String dateStr = (String) request.get("date");
            String reason = (String) request.get("reason");
            
            LocalDate date = dateStr != null ? LocalDate.parse(dateStr) : LocalDate.now();
            
            EmployeeTimeTracking record = timeTrackingService.markOnLeave(adminId, adminName, email, idCardNumber, date, reason);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Employee marked as on leave");
            response.put("data", record);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error marking on leave", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ==================== STATISTICS ====================

    /**
     * Get overall time tracking statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getTimeTrackingStats() {
        try {
            Map<String, Object> stats = timeTrackingService.getTimeTrackingStats();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.putAll(stats);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching stats", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Get daily attendance statistics
     */
    @GetMapping("/daily-attendance/{date}")
    public ResponseEntity<Map<String, Object>> getDailyAttendanceStats(@PathVariable String date) {
        try {
            LocalDate attendanceDate = LocalDate.parse(date);
            Map<String, Object> stats = timeTrackingService.getDailyAttendanceStats(attendanceDate);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.putAll(stats);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching daily stats", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Get all employees' working hours
     */
    @GetMapping("/employees-hours")
    public ResponseEntity<Map<String, Object>> getAllEmployeesWorkingHours(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search) {
        try {
            List<Map<String, Object>> summaries = timeTrackingService.getAllEmployeeWorkingHoursSummary();
            
            // Apply search filter if provided
            if (search != null && !search.isEmpty()) {
                String searchLower = search.toLowerCase();
                summaries = summaries.stream()
                    .filter(s -> s.get("adminName").toString().toLowerCase().contains(searchLower) ||
                               s.get("email").toString().toLowerCase().contains(searchLower) ||
                               s.get("idCardNumber").toString().toLowerCase().contains(searchLower))
                    .toList();
            }
            
            // Apply pagination
            int fromIndex = Math.min(page * size, summaries.size());
            int toIndex = Math.min(fromIndex + size, summaries.size());
            List<Map<String, Object>> paged = summaries.subList(fromIndex, toIndex);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("content", paged);
            response.put("currentPage", page);
            response.put("totalElements", summaries.size());
            response.put("totalPages", (summaries.size() + size - 1) / size);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching employee hours", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ==================== TIME MANAGEMENT POLICIES ====================

    /**
     * Create time management policy
     */
    @PostMapping("/policy")
    public ResponseEntity<Map<String, Object>> createPolicy(@RequestBody TimeManagementPolicy policy) {
        try {
            if (!policyService.validatePolicy(policy)) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Invalid policy settings"));
            }
            
            TimeManagementPolicy created = policyService.createPolicy(policy);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Policy created successfully");
            response.put("data", created);
            
            log.info("Policy created: {}", created.getId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error creating policy", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Update time management policy
     */
    @PutMapping("/policy/{policyId}")
    public ResponseEntity<Map<String, Object>> updatePolicy(
            @PathVariable Long policyId,
            @RequestBody TimeManagementPolicy updates) {
        try {
            TimeManagementPolicy updated = policyService.updatePolicy(policyId, updates);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Policy updated successfully");
            response.put("data", updated);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating policy", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Get policy by ID
     */
    @GetMapping("/policy/{policyId}")
    public ResponseEntity<Map<String, Object>> getPolicy(@PathVariable Long policyId) {
        try {
            TimeManagementPolicy policy = policyService.getPolicyById(policyId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", policy);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching policy", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Get all policies
     */
    @GetMapping("/policies")
    public ResponseEntity<Map<String, Object>> getAllPolicies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<TimeManagementPolicy> policies = policyService.getAllPolicies(pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("content", policies.getContent());
            response.put("currentPage", policies.getNumber());
            response.put("totalElements", policies.getTotalElements());
            response.put("totalPages", policies.getTotalPages());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching policies", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Toggle policy status
     */
    @PatchMapping("/policy/{policyId}/status")
    public ResponseEntity<Map<String, Object>> togglePolicyStatus(
            @PathVariable Long policyId,
            @RequestBody(required = false) Map<String, Boolean> request) {
        try {
            TimeManagementPolicy policy = policyService.getPolicyById(policyId);
            
            if (request != null && request.containsKey("isActive")) {
                Boolean isActive = request.get("isActive");
                if (isActive) {
                    policy = policyService.activatePolicy(policyId);
                } else {
                    policy = policyService.deactivatePolicy(policyId);
                }
            } else {
                policy = policyService.togglePolicyStatus(policyId);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Policy status updated");
            response.put("data", policy);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error toggling policy status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Delete policy
     */
    @DeleteMapping("/policy/{policyId}")
    public ResponseEntity<Map<String, Object>> deletePolicy(@PathVariable Long policyId) {
        try {
            policyService.deletePolicy(policyId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Policy deleted successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error deleting policy", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
