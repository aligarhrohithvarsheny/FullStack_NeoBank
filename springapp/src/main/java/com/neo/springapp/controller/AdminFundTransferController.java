package com.neo.springapp.controller;

import com.neo.springapp.model.AdminFundTransfer;
import com.neo.springapp.service.AdminFundTransferService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin-fund-transfers")
public class AdminFundTransferController {

    @Autowired
    private AdminFundTransferService service;

    @GetMapping("/all")
    public ResponseEntity<List<AdminFundTransfer>> getAll(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(service.getAll(search));
    }

    @GetMapping("/verify-sender-cheque")
    public ResponseEntity<Map<String, Object>> verifySenderCheque(
            @RequestParam String accountNumber, @RequestParam String chequeNumber) {
        return ResponseEntity.ok(service.verifySenderCheque(accountNumber, chequeNumber));
    }

    @GetMapping("/verify-receiver")
    public ResponseEntity<Map<String, Object>> verifyReceiver(@RequestParam String accountNumber) {
        return ResponseEntity.ok(service.verifyReceiver(accountNumber));
    }

    @PostMapping("/process")
    public ResponseEntity<Map<String, Object>> process(@RequestBody Map<String, Object> body) {
        try {
            String senderAccountNumber = (String) body.get("senderAccountNumber");
            String senderChequeNumber = (String) body.get("senderChequeNumber");
            String receiverAccountNumber = (String) body.get("receiverAccountNumber");
            Double amount = body.get("amount") != null ? Double.valueOf(body.get("amount").toString()) : null;
            String description = (String) body.get("description");
            String performedBy = (String) body.get("performedBy");

            return ResponseEntity.ok(service.processTransfer(
                    senderAccountNumber, senderChequeNumber, receiverAccountNumber, amount, description, performedBy));
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PutMapping("/{id}/edit")
    public ResponseEntity<Map<String, Object>> edit(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            String description = (String) body.get("description");
            String editedBy = (String) body.get("editedBy");
            return ResponseEntity.ok(service.editTransfer(id, description, editedBy));
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/{id}/revert")
    public ResponseEntity<Map<String, Object>> revert(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        try {
            String revertedBy = body != null ? (String) body.getOrDefault("revertedBy", "Admin") : "Admin";
            String reason = body != null ? (String) body.getOrDefault("reason", "Admin reverted transfer") : "Admin reverted transfer";
            return ResponseEntity.ok(service.revertTransfer(id, revertedBy, reason));
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
