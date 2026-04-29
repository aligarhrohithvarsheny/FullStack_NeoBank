package com.neo.springapp.controller;

import com.neo.springapp.model.ScheduledPayment;
import com.neo.springapp.service.ScheduledPaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/scheduled-payments")
@CrossOrigin(origins = "*")
public class ScheduledPaymentController {

    @Autowired
    private ScheduledPaymentService scheduledPaymentService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> createScheduledPayment(@RequestBody ScheduledPayment payment) {
        Map<String, Object> response = new HashMap<>();
        try {
            ScheduledPayment created = scheduledPaymentService.createScheduledPayment(payment);
            response.put("success", true);
            response.put("message", "Scheduled payment created successfully");
            response.put("payment", created);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error creating scheduled payment: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<Map<String, Object>> getByAccountNumber(@PathVariable String accountNumber) {
        Map<String, Object> response = new HashMap<>();
        List<ScheduledPayment> payments = scheduledPaymentService.getByAccountNumber(accountNumber);
        response.put("success", true);
        response.put("payments", payments);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/account/{accountNumber}/active")
    public ResponseEntity<Map<String, Object>> getActiveByAccountNumber(@PathVariable String accountNumber) {
        Map<String, Object> response = new HashMap<>();
        List<ScheduledPayment> payments = scheduledPaymentService.getActiveByAccountNumber(accountNumber);
        response.put("success", true);
        response.put("payments", payments);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        Optional<ScheduledPayment> payment = scheduledPaymentService.getById(id);
        if (payment.isPresent()) {
            response.put("success", true);
            response.put("payment", payment.get());
            return ResponseEntity.ok(response);
        }
        response.put("success", false);
        response.put("message", "Scheduled payment not found");
        return ResponseEntity.badRequest().body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable Long id, @RequestBody ScheduledPayment payment) {
        Map<String, Object> response = new HashMap<>();
        payment.setId(id);
        ScheduledPayment updated = scheduledPaymentService.update(payment);
        if (updated != null) {
            response.put("success", true);
            response.put("message", "Scheduled payment updated successfully");
            response.put("payment", updated);
            return ResponseEntity.ok(response);
        }
        response.put("success", false);
        response.put("message", "Scheduled payment not found");
        return ResponseEntity.badRequest().body(response);
    }

    @PutMapping("/{id}/pause")
    public ResponseEntity<Map<String, Object>> pause(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        ScheduledPayment updated = scheduledPaymentService.pausePayment(id);
        if (updated != null) {
            response.put("success", true);
            response.put("message", "Payment paused");
            response.put("payment", updated);
            return ResponseEntity.ok(response);
        }
        response.put("success", false);
        response.put("message", "Scheduled payment not found");
        return ResponseEntity.badRequest().body(response);
    }

    @PutMapping("/{id}/resume")
    public ResponseEntity<Map<String, Object>> resume(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        ScheduledPayment updated = scheduledPaymentService.resumePayment(id);
        if (updated != null) {
            response.put("success", true);
            response.put("message", "Payment resumed");
            response.put("payment", updated);
            return ResponseEntity.ok(response);
        }
        response.put("success", false);
        response.put("message", "Scheduled payment not found");
        return ResponseEntity.badRequest().body(response);
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<Map<String, Object>> cancel(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        ScheduledPayment updated = scheduledPaymentService.cancelPayment(id);
        if (updated != null) {
            response.put("success", true);
            response.put("message", "Payment cancelled");
            response.put("payment", updated);
            return ResponseEntity.ok(response);
        }
        response.put("success", false);
        response.put("message", "Scheduled payment not found");
        return ResponseEntity.badRequest().body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        if (scheduledPaymentService.deletePayment(id)) {
            response.put("success", true);
            response.put("message", "Scheduled payment deleted");
            return ResponseEntity.ok(response);
        }
        response.put("success", false);
        response.put("message", "Scheduled payment not found");
        return ResponseEntity.badRequest().body(response);
    }

    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAll() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("payments", scheduledPaymentService.getAll());
        return ResponseEntity.ok(response);
    }
}
