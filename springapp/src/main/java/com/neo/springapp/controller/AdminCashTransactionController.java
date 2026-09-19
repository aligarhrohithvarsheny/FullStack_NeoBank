package com.neo.springapp.controller;

import com.neo.springapp.model.AdminCashTransaction;
import com.neo.springapp.service.AdminCashTransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin-cash-transactions")
public class AdminCashTransactionController {

    @Autowired
    private AdminCashTransactionService service;

    @GetMapping("/all")
    public ResponseEntity<List<AdminCashTransaction>> getAll(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(service.getAll(search));
    }

    @PostMapping("/record")
    public ResponseEntity<Map<String, Object>> record(@RequestBody Map<String, Object> body) {
        Map<String, Object> response = new HashMap<>();
        try {
            String accountType = (String) body.get("accountType");
            Long accountId = body.get("accountId") != null ? Long.valueOf(body.get("accountId").toString()) : null;
            String accountNumber = (String) body.get("accountNumber");
            String accountHolderName = (String) body.get("accountHolderName");
            String operationType = (String) body.get("operationType");
            Double amount = body.get("amount") != null ? Double.valueOf(body.get("amount").toString()) : null;
            String description = (String) body.get("description");
            Double balanceBefore = body.get("balanceBefore") != null ? Double.valueOf(body.get("balanceBefore").toString()) : null;
            Double balanceAfter = body.get("balanceAfter") != null ? Double.valueOf(body.get("balanceAfter").toString()) : null;
            String performedBy = (String) body.get("performedBy");

            if (accountType == null || accountNumber == null || amount == null) {
                response.put("success", false);
                response.put("message", "accountType, accountNumber and amount are required");
                return ResponseEntity.badRequest().body(response);
            }

            AdminCashTransaction saved = service.record(accountType, accountId, accountNumber, accountHolderName,
                    operationType, amount, description, balanceBefore, balanceAfter, performedBy);
            response.put("success", true);
            response.put("transaction", saved);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to save transaction history: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/{id}/revert")
    public ResponseEntity<Map<String, Object>> revert(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        try {
            String revertedBy = body != null ? (String) body.getOrDefault("revertedBy", "Admin") : "Admin";
            String reason = body != null ? (String) body.getOrDefault("reason", "Admin reverted transaction") : "Admin reverted transaction";
            return ResponseEntity.ok(service.revert(id, revertedBy, reason));
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
