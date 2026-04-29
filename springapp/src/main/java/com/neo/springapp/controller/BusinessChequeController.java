package com.neo.springapp.controller;

import com.neo.springapp.service.BusinessChequeDrawService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Business Account Cheque Draw Controller
 * Handles cheque draw requests for current/business accounts
 * Mirrors the salary account cheque draw endpoints pattern
 */
@RestController
@RequestMapping("/api/business-cheques")
public class BusinessChequeController {

    @Autowired
    private BusinessChequeDrawService businessChequeDrawService;

    // ==================== USER ENDPOINTS ====================

    @PostMapping("/draw/apply")
    public ResponseEntity<Map<String, Object>> applyCheque(@RequestBody Map<String, Object> request) {
        try {
            Long currentAccountId = Long.parseLong(request.get("currentAccountId").toString());
            String serialNumber = (String) request.get("serialNumber");
            String chequeDate = (String) request.get("chequeDate");
            Double amount = Double.parseDouble(request.get("amount").toString());
            String payeeName = (String) request.get("payeeName");
            String remarks = (String) request.get("remarks");

            return ResponseEntity.ok(businessChequeDrawService.applyChequeDrawRequest(
                    currentAccountId, serialNumber, chequeDate, amount, payeeName, remarks
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", true, "message", e.getMessage())
            );
        }
    }

    @GetMapping("/draw/user/{currentAccountId}")
    public ResponseEntity<Map<String, Object>> getUserChequeDrawRequests(
            @PathVariable Long currentAccountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            return ResponseEntity.ok(businessChequeDrawService.getUserChequeDrawRequests(currentAccountId, page, size));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", true, "message", e.getMessage())
            );
        }
    }

    @GetMapping("/draw/{id}")
    public ResponseEntity<Map<String, Object>> getChequeDrawDetails(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(businessChequeDrawService.getChequeDrawDetails(id));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/draw/{id}/cancel")
    public ResponseEntity<Map<String, Object>> cancelChequeDrawRequest(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> request) {
        try {
            String reason = request != null ? (String) request.get("reason") : "";
            return ResponseEntity.ok(businessChequeDrawService.cancelChequeDrawRequest(id, reason));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", true, "message", e.getMessage())
            );
        }
    }

    @PostMapping("/draw/{id}/mark-downloaded")
    public ResponseEntity<Map<String, Object>> markChequeDownloaded(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(businessChequeDrawService.markChequeDownloaded(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("success", false, "message", e.getMessage())
            );
        }
    }

    @GetMapping("/draw/leaves/{currentAccountId}")
    public ResponseEntity<Map<String, Object>> getAvailableChequeLeaves(
            @PathVariable Long currentAccountId) {
        try {
            return ResponseEntity.ok(businessChequeDrawService.getAvailableChequeLeaves(currentAccountId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", true, "message", e.getMessage())
            );
        }
    }

    @PutMapping("/draw/{id}/edit")
    public ResponseEntity<Map<String, Object>> editPendingChequeDrawRequest(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        try {
            String payeeName = request != null ? (String) request.get("payeeName") : null;
            Double amount = request != null && request.get("amount") != null
                    ? Double.parseDouble(request.get("amount").toString()) : null;

            return ResponseEntity.ok(businessChequeDrawService.editPendingChequeDrawRequest(id, payeeName, amount));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", true, "message", e.getMessage())
            );
        }
    }

    // ==================== ADMIN ENDPOINTS ====================

    @GetMapping("/draw/admin/all")
    public ResponseEntity<Map<String, Object>> getAdminChequeDrawRequests(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            return ResponseEntity.ok(businessChequeDrawService.getAdminChequeDrawRequests(status, search, page, size));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", true, "message", e.getMessage())
            );
        }
    }

    @GetMapping("/draw/admin/{id}")
    public ResponseEntity<Map<String, Object>> getAdminChequeDrawDetails(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(businessChequeDrawService.getAdminChequeDrawDetails(id));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/draw/admin/verify-payee")
    public ResponseEntity<Map<String, Object>> verifyPayeeAccount(@RequestBody Map<String, Object> request) {
        try {
            String payeeAccountNumber = (String) request.get("payeeAccountNumber");
            String payeeName = (String) request.get("payeeName");

            if (payeeAccountNumber == null || payeeAccountNumber.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", true, "message", "Payee account number is required")
                );
            }
            if (payeeName == null || payeeName.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", true, "message", "Payee name is required")
                );
            }

            return ResponseEntity.ok(businessChequeDrawService.verifyPayeeAccount(payeeAccountNumber, payeeName));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", true, "message", e.getMessage())
            );
        }
    }

    @PostMapping("/draw/admin/{id}/approve")
    public ResponseEntity<Map<String, Object>> approveChequeDrawRequest(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> request,
            @RequestHeader(value = "X-Admin-Email", required = false) String adminEmail) {
        try {
            String remarks = request != null ? (String) request.get("remarks") : "";
            String payeeAccountNumber = request != null ? (String) request.get("payeeAccountNumber") : null;
            if (adminEmail == null) adminEmail = "admin@neobank.com";

            return ResponseEntity.ok(businessChequeDrawService.approveChequeDrawRequest(id, adminEmail, remarks, payeeAccountNumber));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", true, "message", e.getMessage())
            );
        }
    }

    @PostMapping("/draw/admin/{id}/reject")
    public ResponseEntity<Map<String, Object>> rejectChequeDrawRequest(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request,
            @RequestHeader(value = "X-Admin-Email", required = false) String adminEmail) {
        try {
            String rejectionReason = (String) request.get("rejectionReason");
            if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", true, "message", "Rejection reason is required")
                );
            }
            if (adminEmail == null) adminEmail = "admin@neobank.com";

            return ResponseEntity.ok(businessChequeDrawService.rejectChequeDrawRequest(id, adminEmail, rejectionReason));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", true, "message", e.getMessage())
            );
        }
    }

    @PostMapping("/draw/admin/{id}/picked-up")
    public ResponseEntity<Map<String, Object>> markChequePickedUp(
            @PathVariable Long id,
            @RequestHeader(value = "X-Admin-Email", required = false) String adminEmail) {
        try {
            if (adminEmail == null) adminEmail = "admin@neobank.com";
            return ResponseEntity.ok(businessChequeDrawService.markChequeDrawPickedUp(id, adminEmail));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", true, "message", e.getMessage())
            );
        }
    }

    @PostMapping("/draw/admin/{id}/clear")
    public ResponseEntity<Map<String, Object>> clearCheque(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> request,
            @RequestHeader(value = "X-Admin-Email", required = false) String adminEmail) {
        try {
            String clearedDate = request != null ? (String) request.get("clearedDate") : null;
            if (adminEmail == null) adminEmail = "admin@neobank.com";

            return ResponseEntity.ok(businessChequeDrawService.clearChequeDrawRequest(id, adminEmail, clearedDate));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", true, "message", e.getMessage())
            );
        }
    }

    @GetMapping("/draw/admin/stats")
    public ResponseEntity<Map<String, Object>> getChequeDrawStats() {
        try {
            return ResponseEntity.ok(businessChequeDrawService.getChequeDrawStats());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", true, "message", e.getMessage())
            );
        }
    }

    @GetMapping("/draw/admin/{id}/audit-log")
    public ResponseEntity<Map<String, Object>> getChequeDrawAuditLog(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(businessChequeDrawService.getChequeDrawAuditLog(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", true, "message", e.getMessage())
            );
        }
    }

    @GetMapping("/draw/admin/search")
    public ResponseEntity<Map<String, Object>> searchChequeDrawByNumber(@RequestParam String chequeNumber) {
        try {
            return ResponseEntity.ok(businessChequeDrawService.searchChequeDrawByNumber(chequeNumber));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", true, "message", e.getMessage())
            );
        }
    }

    @GetMapping("/draw/admin/export")
    public ResponseEntity<?> exportChequeDrawToCSV(@RequestParam(required = false) String status) {
        try {
            byte[] csvData = businessChequeDrawService.exportChequeDrawToCSV(status);
            return ResponseEntity.ok()
                    .header("Content-Type", "text/csv")
                    .header("Content-Disposition", "attachment; filename=\"business_cheques_export.csv\"")
                    .body(csvData);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", true, "message", e.getMessage())
            );
        }
    }
}
