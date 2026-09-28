package com.neo.springapp.controller;

import com.neo.springapp.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * REST API controller for managing fund allocations.
 * HOD uses these endpoints to allocate, update, cancel, and monitor branch allocations.
 * Branch Managers use these to view their allocated funds.
 * 
 * Base URL: /api/hod/allocations or /api/manager/allocations
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"})
@SuppressWarnings("null")
public class FundsAllocationController {

    @Autowired
    private FundsAllocationService allocationService;

    @Autowired
    private AllocationValidatorService validatorService;

    @Autowired
    private AllocationMetricsService metricsService;

    /**
     * HOD allocates funds to a branch manager.
     * POST /api/hod/allocate-funds
     * 
     * Request Body:
     * {
     *   "amount": 1000000,
     *   "branchManagerId": 5,
     *   "allocationType": "GENERAL",
     *   "productTypes": "GOLD_LOAN,DEPOSITS,WITHDRAWALS,LOANS,OVERDRAFT,SALARY_CREDITS",
     *   "validTill": "2025-12-31",
     *   "description": "Q4 2025 allocation"
     * }
     */
    @PostMapping("/hod/allocate-funds")
    public ResponseEntity<?> allocateFunds(@RequestBody Map<String, Object> request) {
        try {
            Long hodAdminId = Long.parseLong(request.get("hodAdminId").toString());
            Double amount = Double.parseDouble(request.get("amount").toString());
            Long branchManagerId = Long.parseLong(request.get("branchManagerId").toString());
            String allocationType = (String) request.get("allocationType");
            String productTypes = (String) request.get("productTypes");
            String validTillStr = (String) request.get("validTill");
            LocalDate validTill = validTillStr != null ? LocalDate.parse(validTillStr) : null;
            String description = (String) request.get("description");

            // Validate request
            Map<String, Object> validation = validatorService.validateAllocationRequest(
                amount, branchManagerId, productTypes, validTill, hodAdminId
            );
            if (!((Boolean) validation.get("isValid"))) {
                return ResponseEntity.badRequest().body(validation);
            }

            // Process allocation
            Map<String, Object> result = allocationService.allocateFunds(
                amount, branchManagerId, allocationType, productTypes, validTill, description, hodAdminId
            );

            if ((Boolean) result.get("success")) {
                return ResponseEntity.status(HttpStatus.CREATED).body(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        } catch (Exception e) {
            Map<String, Object> error = new java.util.HashMap<>();
            error.put("success", false);
            error.put("message", "Error allocating funds: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Get all allocations with filters (HOD view).
     * GET /api/hod/allocations?status=ACTIVE&managerId=5&page=0&size=20
     */
    @GetMapping("/hod/allocations")
    public ResponseEntity<?> getAllAllocations(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long managerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        try {
            Map<String, Object> result = allocationService.getAllAllocations(status, managerId, fromDate, toDate, page, size);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new java.util.HashMap<>();
            error.put("success", false);
            error.put("message", "Error fetching allocations: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Get detailed information about a specific allocation.
     * GET /api/hod/allocations/{allocationId}
     */
    @GetMapping("/hod/allocations/{allocationId}")
    public ResponseEntity<?> getAllocationDetails(@PathVariable Long allocationId) {
        try {
            Map<String, Object> result = allocationService.getAllocationDetails(allocationId);
            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            Map<String, Object> error = new java.util.HashMap<>();
            error.put("success", false);
            error.put("message", "Error fetching allocation: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Update an allocation (amount, validity, description).
     * Only ACTIVE allocations can be updated.
     * PUT /api/hod/allocations/{allocationId}
     */
    @PutMapping("/hod/allocations/{allocationId}")
    public ResponseEntity<?> updateAllocation(
            @PathVariable Long allocationId,
            @RequestBody Map<String, Object> request) {

        try {
            Long hodAdminId = Long.parseLong(request.get("hodAdminId").toString());
            Double newAmount = request.containsKey("amount") ? Double.parseDouble(request.get("amount").toString()) : null;
            String validTillStr = (String) request.get("validTill");
            LocalDate newValidTill = validTillStr != null ? LocalDate.parse(validTillStr) : null;
            String description = (String) request.get("description");

            // Validate update
            Map<String, Object> validation = validatorService.validateUpdateRequest(allocationId, newAmount, newValidTill);
            if (!((Boolean) validation.get("isValid"))) {
                return ResponseEntity.badRequest().body(validation);
            }

            Map<String, Object> result = allocationService.updateAllocation(
                allocationId, newAmount, newValidTill, description, hodAdminId
            );

            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        } catch (Exception e) {
            Map<String, Object> error = new java.util.HashMap<>();
            error.put("success", false);
            error.put("message", "Error updating allocation: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Cancel an allocation.
     * DELETE /api/hod/allocations/{allocationId}/cancel
     */
    @DeleteMapping("/hod/allocations/{allocationId}/cancel")
    public ResponseEntity<?> cancelAllocation(
            @PathVariable Long allocationId,
            @RequestBody Map<String, Object> request) {

        try {
            Long hodAdminId = Long.parseLong(request.get("hodAdminId").toString());
            String reason = (String) request.get("reason");

            // Validate cancellation
            Map<String, Object> validation = validatorService.validateCancellationRequest(allocationId);
            if (!((Boolean) validation.get("isValid"))) {
                return ResponseEntity.badRequest().body(validation);
            }

            Map<String, Object> result = allocationService.cancelAllocation(allocationId, reason, hodAdminId);

            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        } catch (Exception e) {
            Map<String, Object> error = new java.util.HashMap<>();
            error.put("success", false);
            error.put("message", "Error cancelling allocation: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Pause an allocation temporarily.
     * POST /api/hod/allocations/{allocationId}/pause
     */
    @PostMapping("/hod/allocations/{allocationId}/pause")
    public ResponseEntity<?> pauseAllocation(
            @PathVariable Long allocationId,
            @RequestBody Map<String, Object> request) {

        try {
            String reason = (String) request.get("reason");

            Map<String, Object> result = allocationService.pauseAllocation(allocationId, reason);

            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        } catch (Exception e) {
            Map<String, Object> error = new java.util.HashMap<>();
            error.put("success", false);
            error.put("message", "Error pausing allocation: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Resume a paused allocation.
     * POST /api/hod/allocations/{allocationId}/resume
     */
    @PostMapping("/hod/allocations/{allocationId}/resume")
    public ResponseEntity<?> resumeAllocation(@PathVariable Long allocationId) {
        try {
            Map<String, Object> result = allocationService.resumeAllocation(allocationId);

            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        } catch (Exception e) {
            Map<String, Object> error = new java.util.HashMap<>();
            error.put("success", false);
            error.put("message", "Error resuming allocation: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Check allocation status for a branch.
     * GET /api/hod/branch-allocation-status/{managerId}
     */
    @GetMapping("/hod/branch-allocation-status/{managerId}")
    public ResponseEntity<?> checkBranchAllocationStatus(@PathVariable Long managerId) {
        try {
            Map<String, Object> result = allocationService.checkAllocationStatus(managerId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new java.util.HashMap<>();
            error.put("success", false);
            error.put("message", "Error checking status: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Get HOD allocation dashboard report with all metrics.
     * GET /api/hod/allocations-report?fromDate=2025-01-01&toDate=2025-12-31
     */
    @GetMapping("/hod/allocations-report")
    public ResponseEntity<?> getAllocationsReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Long managerId) {

        try {
            Map<String, Object> result = metricsService.getAllMetricsReport(fromDate, toDate, managerId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new java.util.HashMap<>();
            error.put("success", false);
            error.put("message", "Error generating report: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Get manager's allocation limits.
     * GET /api/manager/allocation-limits/{managerId}
     */
    @GetMapping("/manager/allocation-limits/{managerId}")
    public ResponseEntity<?> getManagerAllocationLimits(@PathVariable Long managerId) {
        try {
            Map<String, Object> result = validatorService.getManagerAllocationLimits(managerId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new java.util.HashMap<>();
            error.put("success", false);
            error.put("message", "Error fetching limits: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Validate product types.
     * POST /api/allocations/validate-products
     */
    @PostMapping("/allocations/validate-products")
    public ResponseEntity<?> validateProductTypes(@RequestBody Map<String, String> request) {
        try {
            String productTypes = request.get("productTypes");
            Map<String, Object> result = validatorService.validateProductTypes(productTypes);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new java.util.HashMap<>();
            error.put("success", false);
            error.put("message", "Error validating products: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
