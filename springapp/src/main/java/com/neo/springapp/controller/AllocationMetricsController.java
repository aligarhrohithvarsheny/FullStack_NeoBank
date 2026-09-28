package com.neo.springapp.controller;

import com.neo.springapp.service.AllocationMetricsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * REST API controller for accessing allocation metrics and reporting.
 * Provides real-time dashboards, trends, and performance analytics.
 * Used by HOD Dashboard for monitoring and Manager Dashboard for utilization tracking.
 * 
 * Base URL: /api/manager/metrics or /api/hod/metrics
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"})
@SuppressWarnings("null")
public class AllocationMetricsController {

    @Autowired
    private AllocationMetricsService metricsService;

    /**
     * Get real-time metrics summary for a specific allocation.
     * Used by Manager Dashboard to display current KPIs.
     * 
     * GET /api/manager/allocations/{allocationId}/metrics
     * 
     * Returns:
     * - Total allocated amount
     * - Total debited/credited amounts
     * - Current balance
     * - Utilization percentage
     * - Transaction counts
     * - Product-wise breakdown (Gold Loan, Deposits, etc.)
     * - Days remaining in allocation validity
     */
    @GetMapping("/manager/allocations/{allocationId}/metrics")
    public ResponseEntity<?> getMetricsSummary(@PathVariable Long allocationId) {
        try {
            Map<String, Object> result = metricsService.getMetricsSummary(allocationId);
            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            Map<String, Object> error = new java.util.HashMap<>();
            error.put("success", false);
            error.put("message", "Error fetching metrics: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Get all manager's allocations summary.
     * Shows all active allocations with current status and utilization.
     * 
     * GET /api/manager/allocations/summary/{managerId}
     */
    @GetMapping("/manager/allocations/summary/{managerId}")
    public ResponseEntity<?> getManagerMetrics(@PathVariable Long managerId) {
        try {
            Map<String, Object> result = metricsService.getManagerMetrics(managerId);
            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            Map<String, Object> error = new java.util.HashMap<>();
            error.put("success", false);
            error.put("message", "Error fetching manager metrics: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Get utilization trend data for charts (7-day or 30-day).
     * Shows how allocation balance changed over time.
     * 
     * GET /api/manager/allocations/{allocationId}/trend?days=7
     * 
     * Query Parameters:
     * - days: 7, 30, 90 (default: 7)
     */
    @GetMapping("/manager/allocations/{allocationId}/trend")
    public ResponseEntity<?> getUtilizationTrend(
            @PathVariable Long allocationId,
            @RequestParam(defaultValue = "7") int days) {

        try {
            Map<String, Object> result = metricsService.getUtilizationTrend(allocationId, days);
            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            Map<String, Object> error = new java.util.HashMap<>();
            error.put("success", false);
            error.put("message", "Error fetching trend: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Get HOD dashboard report with all allocations and aggregated metrics.
     * Provides bird's eye view of all branches' fund allocation status.
     * 
     * GET /api/hod/metrics/allocations-report?fromDate=2025-01-01&toDate=2025-12-31&managerId=5
     * 
     * Query Parameters:
     * - fromDate: Start date (optional)
     * - toDate: End date (optional)
     * - managerId: Filter by specific manager (optional)
     */
    @GetMapping("/hod/metrics/allocations-report")
    public ResponseEntity<?> getAllMetricsReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Long managerId) {

        try {
            Map<String, Object> result = metricsService.getAllMetricsReport(fromDate, toDate, managerId);
            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            Map<String, Object> error = new java.util.HashMap<>();
            error.put("success", false);
            error.put("message", "Error fetching report: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Get top performing allocations by utilization percentage.
     * Helps identify which managers are effectively using their allocations.
     * 
     * GET /api/hod/metrics/top-allocations?limit=10
     */
    @GetMapping("/hod/metrics/top-allocations")
    public ResponseEntity<?> getTopAllocations(
            @RequestParam(defaultValue = "10") int limit) {

        try {
            Map<String, Object> result = metricsService.getTopAllocations(limit);
            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            Map<String, Object> error = new java.util.HashMap<>();
            error.put("success", false);
            error.put("message", "Error fetching top allocations: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Get low performing allocations by utilization percentage.
     * Helps identify underutilized allocations or managers not using funds effectively.
     * 
     * GET /api/hod/metrics/low-allocations?limit=10
     */
    @GetMapping("/hod/metrics/low-allocations")
    public ResponseEntity<?> getLowAllocations(
            @RequestParam(defaultValue = "10") int limit) {

        try {
            Map<String, Object> result = metricsService.getLowAllocations(limit);
            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            Map<String, Object> error = new java.util.HashMap<>();
            error.put("success", false);
            error.put("message", "Error fetching low allocations: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Get allocations expiring within N days.
     * Helps HOD to renew or extend allocations before they expire.
     * 
     * GET /api/hod/metrics/expiring-allocations?days=30
     * 
     * Query Parameters:
     * - days: Check for allocations expiring within this many days (default: 30)
     */
    @GetMapping("/hod/metrics/expiring-allocations")
    public ResponseEntity<?> getExpiringAllocations(
            @RequestParam(defaultValue = "30") int days) {

        try {
            Map<String, Object> result = metricsService.getExpiringAllocations(days);
            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            Map<String, Object> error = new java.util.HashMap<>();
            error.put("success", false);
            error.put("message", "Error fetching expiring allocations: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Get comprehensive dashboard for Manager.
     * Combines all metrics into one view for manager's allocation overview.
     * 
     * GET /api/manager/dashboard/{managerId}
     */
    @GetMapping("/manager/dashboard/{managerId}")
    public ResponseEntity<?> getManagerDashboard(@PathVariable Long managerId) {
        try {
            Map<String, Object> dashboard = new java.util.HashMap<>();
            
            // Get all metrics for this manager
            Map<String, Object> metrics = metricsService.getManagerMetrics(managerId);
            dashboard.put("metricsOverview", metrics);
            
            // Add allocation limits
            dashboard.put("success", true);
            return ResponseEntity.ok(dashboard);
        } catch (Exception e) {
            Map<String, Object> error = new java.util.HashMap<>();
            error.put("success", false);
            error.put("message", "Error loading dashboard: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Get comprehensive dashboard for HOD.
     * Shows all allocations, performance metrics, and alerts.
     * 
     * GET /api/hod/dashboard
     */
    @GetMapping("/hod/dashboard")
    public ResponseEntity<?> getHODDashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {

        try {
            Map<String, Object> dashboard = new java.util.HashMap<>();
            
            // Get overall report
            Map<String, Object> report = metricsService.getAllMetricsReport(fromDate, toDate, null);
            dashboard.put("overallReport", report);
            
            // Get alerts - expiring allocations
            Map<String, Object> expiring = metricsService.getExpiringAllocations(30);
            dashboard.put("expiringAllocations", expiring);
            
            // Get top and low performers
            Map<String, Object> topPerformers = metricsService.getTopAllocations(5);
            dashboard.put("topPerformers", topPerformers);
            
            Map<String, Object> lowPerformers = metricsService.getLowAllocations(5);
            dashboard.put("lowPerformers", lowPerformers);
            
            dashboard.put("success", true);
            return ResponseEntity.ok(dashboard);
        } catch (Exception e) {
            Map<String, Object> error = new java.util.HashMap<>();
            error.put("success", false);
            error.put("message", "Error loading dashboard: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Get metrics history for an allocation over a date range.
     * Used for detailed trend analysis and forecasting.
     * 
     * GET /api/manager/allocations/{allocationId}/metrics-history?fromDate=2025-01-01&toDate=2025-12-31
     */
    @GetMapping("/manager/allocations/{allocationId}/metrics-history")
    public ResponseEntity<?> getMetricsHistory(
            @PathVariable Long allocationId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {

        try {
            Map<String, Object> result = metricsService.getUtilizationTrend(allocationId, 
                (int) java.time.temporal.ChronoUnit.DAYS.between(fromDate, toDate));
            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            Map<String, Object> error = new java.util.HashMap<>();
            error.put("success", false);
            error.put("message", "Error fetching history: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
