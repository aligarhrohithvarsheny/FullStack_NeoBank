package com.neo.springapp.controller;

import com.neo.springapp.model.*;
import com.neo.springapp.service.AtmManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/atm")
@SuppressWarnings("null")
public class AdminAtmController {

    @Autowired
    private AtmManagementService atmService;

    // ─── Dashboard Stats ──────────────────────────────────

    @GetMapping("/dashboard-stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        try {
            Map<String, Object> stats = atmService.getDashboardStats();
            stats.put("success", true);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to load stats: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ─── ATM Machine Management ───────────────────────────

    @GetMapping("/machines")
    public ResponseEntity<Map<String, Object>> getAllAtms() {
        try {
            List<AtmMachine> atms = atmService.getAllAtms();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("atms", atms);
            response.put("total", atms.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/machines/{atmId}")
    public ResponseEntity<Map<String, Object>> getAtmById(@PathVariable String atmId) {
        try {
            AtmMachine atm = atmService.getAtmById(atmId)
                    .orElseThrow(() -> new RuntimeException("ATM not found: " + atmId));
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("atm", atm);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    @GetMapping("/machines/status/{status}")
    public ResponseEntity<Map<String, Object>> getAtmsByStatus(@PathVariable String status) {
        try {
            List<AtmMachine> atms = atmService.getAtmsByStatus(status);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("atms", atms);
            response.put("total", atms.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/machines")
    public ResponseEntity<Map<String, Object>> createAtm(@RequestBody AtmMachine atm) {
        try {
            AtmMachine created = atmService.createAtm(atm);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "ATM created successfully");
            response.put("atm", created);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @PutMapping("/machines/{atmId}")
    public ResponseEntity<Map<String, Object>> updateAtm(@PathVariable String atmId, @RequestBody AtmMachine updates) {
        try {
            AtmMachine updated = atmService.updateAtm(atmId, updates);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "ATM updated successfully");
            response.put("atm", updated);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    // ─── ATM Status Control ───────────────────────────────

    @PutMapping("/machines/{atmId}/status")
    public ResponseEntity<Map<String, Object>> changeAtmStatus(@PathVariable String atmId,
                                                                 @RequestBody Map<String, String> body) {
        try {
            String newStatus = body.get("status");
            String performedBy = body.get("performedBy");
            String reason = body.get("reason");
            AtmMachine atm = atmService.changeAtmStatus(atmId, newStatus, performedBy, reason);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "ATM status changed to " + newStatus);
            response.put("atm", atm);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @PostMapping("/machines/all-out-of-service")
    public ResponseEntity<Map<String, Object>> setAllOutOfService(@RequestBody Map<String, String> body) {
        try {
            atmService.setAllAtmsOutOfService(body.get("performedBy"), body.get("reason"));
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "All ATMs set to OUT_OF_SERVICE");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/machines/all-activate")
    public ResponseEntity<Map<String, Object>> setAllActive(@RequestBody Map<String, String> body) {
        try {
            atmService.setAllAtmsActive(body.get("performedBy"), body.get("reason"));
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "All ATMs activated");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ─── Cash Loading ─────────────────────────────────────

    @PostMapping("/machines/{atmId}/load-cash")
    public ResponseEntity<Map<String, Object>> loadCash(@PathVariable String atmId,
                                                         @RequestBody Map<String, Object> body) {
        try {
            Double amount = Double.valueOf(body.get("amount").toString());
            String loadedBy = (String) body.get("loadedBy");
            Integer notes500 = body.get("notes500") != null ? Integer.valueOf(body.get("notes500").toString()) : null;
            Integer notes200 = body.get("notes200") != null ? Integer.valueOf(body.get("notes200").toString()) : null;
            Integer notes100 = body.get("notes100") != null ? Integer.valueOf(body.get("notes100").toString()) : null;
            Integer notes2000 = body.get("notes2000") != null ? Integer.valueOf(body.get("notes2000").toString()) : null;
            String remarks = (String) body.get("remarks");

            Map<String, Object> result = atmService.loadCash(atmId, amount, loadedBy,
                    notes500, notes200, notes100, notes2000, remarks);
            result.put("message", "Cash loaded successfully into ATM " + atmId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @GetMapping("/machines/{atmId}/cash-history")
    public ResponseEntity<Map<String, Object>> getCashLoadHistory(@PathVariable String atmId,
                                                                    @RequestParam(defaultValue = "0") int page,
                                                                    @RequestParam(defaultValue = "20") int size) {
        try {
            Page<AtmCashLoad> cashLoads = atmService.getCashLoadHistory(atmId, page, size);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("cashLoads", cashLoads.getContent());
            response.put("totalPages", cashLoads.getTotalPages());
            response.put("totalElements", cashLoads.getTotalElements());
            response.put("currentPage", page);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ─── ATM Transactions ─────────────────────────────────

    @GetMapping("/transactions")
    public ResponseEntity<Map<String, Object>> getAllTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Page<AtmTransaction> txns = atmService.getAllTransactions(page, size);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("transactions", txns.getContent());
            response.put("totalPages", txns.getTotalPages());
            response.put("totalElements", txns.getTotalElements());
            response.put("currentPage", page);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/transactions/atm/{atmId}")
    public ResponseEntity<Map<String, Object>> getTransactionsByAtm(@PathVariable String atmId,
                                                                      @RequestParam(defaultValue = "0") int page,
                                                                      @RequestParam(defaultValue = "20") int size) {
        try {
            Page<AtmTransaction> txns = atmService.getTransactionsByAtm(atmId, page, size);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("transactions", txns.getContent());
            response.put("totalPages", txns.getTotalPages());
            response.put("totalElements", txns.getTotalElements());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/transactions/account/{accountNumber}")
    public ResponseEntity<Map<String, Object>> getTransactionsByAccount(@PathVariable String accountNumber,
                                                                          @RequestParam(defaultValue = "0") int page,
                                                                          @RequestParam(defaultValue = "20") int size) {
        try {
            Page<AtmTransaction> txns = atmService.getTransactionsByAccount(accountNumber, page, size);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("transactions", txns.getContent());
            response.put("totalPages", txns.getTotalPages());
            response.put("totalElements", txns.getTotalElements());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/transactions/status/{status}")
    public ResponseEntity<Map<String, Object>> getTransactionsByStatus(@PathVariable String status,
                                                                         @RequestParam(defaultValue = "0") int page,
                                                                         @RequestParam(defaultValue = "20") int size) {
        try {
            Page<AtmTransaction> txns = atmService.getTransactionsByStatus(status, page, size);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("transactions", txns.getContent());
            response.put("totalPages", txns.getTotalPages());
            response.put("totalElements", txns.getTotalElements());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/transactions/receipts")
    public ResponseEntity<Map<String, Object>> getTransactionsWithReceipts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Page<AtmTransaction> txns = atmService.getTransactionsWithReceipts(page, size);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("transactions", txns.getContent());
            response.put("totalPages", txns.getTotalPages());
            response.put("totalElements", txns.getTotalElements());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ─── ATM Withdrawal (user-facing, called from ATM simulator) ──

    @PostMapping("/withdraw")
    public ResponseEntity<Map<String, Object>> processWithdrawal(@RequestBody Map<String, Object> body) {
        try {
            String atmId = (String) body.get("atmId");
            String accountNumber = (String) body.get("accountNumber");
            String cardNumber = (String) body.get("cardNumber");
            String userName = (String) body.get("userName");
            String userEmail = (String) body.get("userEmail");
            Double amount = Double.valueOf(body.get("amount").toString());
            Double userBalanceBefore = body.get("userBalanceBefore") != null ?
                    Double.valueOf(body.get("userBalanceBefore").toString()) : 0.0;
            Double userBalanceAfter = body.get("userBalanceAfter") != null ?
                    Double.valueOf(body.get("userBalanceAfter").toString()) : 0.0;

            Map<String, Object> result = atmService.processWithdrawal(atmId, accountNumber, cardNumber,
                    userName, userEmail, amount, userBalanceBefore, userBalanceAfter);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "ATM withdrawal failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    // ─── ATM Status Check (user-facing) ───────────────────

    @GetMapping("/status/{atmId}")
    public ResponseEntity<Map<String, Object>> getAtmStatus(@PathVariable String atmId) {
        try {
            Map<String, Object> result = atmService.getAtmStatusForUser(atmId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/active-atms")
    public ResponseEntity<Map<String, Object>> getActiveAtms() {
        try {
            List<AtmMachine> atms = atmService.getActiveAtmsForUser();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("atms", atms);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ─── Incident Management ──────────────────────────────

    @GetMapping("/incidents")
    public ResponseEntity<Map<String, Object>> getAllIncidents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Page<AtmIncident> incidents = atmService.getAllIncidents(page, size);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("incidents", incidents.getContent());
            response.put("totalPages", incidents.getTotalPages());
            response.put("totalElements", incidents.getTotalElements());
            response.put("currentPage", page);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/incidents/status/{status}")
    public ResponseEntity<Map<String, Object>> getIncidentsByStatus(@PathVariable String status,
                                                                       @RequestParam(defaultValue = "0") int page,
                                                                       @RequestParam(defaultValue = "20") int size) {
        try {
            Page<AtmIncident> incidents = atmService.getIncidentsByStatus(status, page, size);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("incidents", incidents.getContent());
            response.put("totalPages", incidents.getTotalPages());
            response.put("totalElements", incidents.getTotalElements());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/incidents/type/{type}")
    public ResponseEntity<Map<String, Object>> getIncidentsByType(@PathVariable String type,
                                                                     @RequestParam(defaultValue = "0") int page,
                                                                     @RequestParam(defaultValue = "20") int size) {
        try {
            Page<AtmIncident> incidents = atmService.getIncidentsByType(type, page, size);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("incidents", incidents.getContent());
            response.put("totalPages", incidents.getTotalPages());
            response.put("totalElements", incidents.getTotalElements());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/incidents")
    public ResponseEntity<Map<String, Object>> createIncident(@RequestBody Map<String, Object> body) {
        try {
            String atmId = (String) body.get("atmId");
            String incidentType = (String) body.get("incidentType");
            String accountNumber = (String) body.get("accountNumber");
            String cardNumber = (String) body.get("cardNumber");
            String userName = (String) body.get("userName");
            String description = (String) body.get("description");
            Double amountInvolved = body.get("amountInvolved") != null ?
                    Double.valueOf(body.get("amountInvolved").toString()) : null;

            AtmIncident incident = atmService.createIncident(atmId, incidentType, accountNumber,
                    cardNumber, userName, description, amountInvolved);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Incident reported successfully");
            response.put("incident", incident);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @PutMapping("/incidents/{incidentRef}/resolve")
    public ResponseEntity<Map<String, Object>> resolveIncident(@PathVariable String incidentRef,
                                                                  @RequestBody Map<String, String> body) {
        try {
            AtmIncident incident = atmService.resolveIncident(incidentRef,
                    body.get("resolution"), body.get("resolvedBy"));
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Incident resolved successfully");
            response.put("incident", incident);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    // ─── Service Logs ─────────────────────────────────────

    @GetMapping("/service-logs")
    public ResponseEntity<Map<String, Object>> getAllServiceLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Page<AtmServiceLog> logs = atmService.getAllServiceLogs(page, size);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("logs", logs.getContent());
            response.put("totalPages", logs.getTotalPages());
            response.put("totalElements", logs.getTotalElements());
            response.put("currentPage", page);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/service-logs/{atmId}")
    public ResponseEntity<Map<String, Object>> getServiceLogsByAtm(@PathVariable String atmId,
                                                                      @RequestParam(defaultValue = "0") int page,
                                                                      @RequestParam(defaultValue = "20") int size) {
        try {
            Page<AtmServiceLog> logs = atmService.getServiceLog(atmId, page, size);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("logs", logs.getContent());
            response.put("totalPages", logs.getTotalPages());
            response.put("totalElements", logs.getTotalElements());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
