package com.neo.springapp.controller;

import com.neo.springapp.model.SubscriptionPayment;
import com.neo.springapp.service.SubscriptionPaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/subscription-payments")
@CrossOrigin(origins = "*")
public class SubscriptionPaymentController {

    @Autowired
    private SubscriptionPaymentService subscriptionPaymentService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody SubscriptionPayment subscription) {
        Map<String, Object> response = new HashMap<>();
        try {
            SubscriptionPayment created = subscriptionPaymentService.createSubscription(subscription);
            response.put("success", true);
            response.put("message", "Subscription created successfully");
            response.put("subscription", created);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error creating subscription: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<Map<String, Object>> getByEmployeeId(@PathVariable String employeeId) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("subscriptions", subscriptionPaymentService.getByEmployeeId(employeeId));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<Map<String, Object>> getByAccount(@PathVariable String accountNumber) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("subscriptions", subscriptionPaymentService.getBySalaryAccountNumber(accountNumber));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        Optional<SubscriptionPayment> sub = subscriptionPaymentService.getById(id);
        if (sub.isPresent()) {
            response.put("success", true);
            response.put("subscription", sub.get());
            return ResponseEntity.ok(response);
        }
        response.put("success", false);
        response.put("message", "Subscription not found");
        return ResponseEntity.badRequest().body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable Long id, @RequestBody SubscriptionPayment subscription) {
        Map<String, Object> response = new HashMap<>();
        subscription.setId(id);
        SubscriptionPayment updated = subscriptionPaymentService.update(subscription);
        if (updated != null) {
            response.put("success", true);
            response.put("message", "Subscription updated");
            response.put("subscription", updated);
            return ResponseEntity.ok(response);
        }
        response.put("success", false);
        response.put("message", "Subscription not found");
        return ResponseEntity.badRequest().body(response);
    }

    @PutMapping("/{id}/pause")
    public ResponseEntity<Map<String, Object>> pause(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        SubscriptionPayment updated = subscriptionPaymentService.pauseSubscription(id);
        if (updated != null) {
            response.put("success", true);
            response.put("message", "Subscription paused");
            response.put("subscription", updated);
            return ResponseEntity.ok(response);
        }
        response.put("success", false);
        response.put("message", "Subscription not found");
        return ResponseEntity.badRequest().body(response);
    }

    @PutMapping("/{id}/resume")
    public ResponseEntity<Map<String, Object>> resume(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        SubscriptionPayment updated = subscriptionPaymentService.resumeSubscription(id);
        if (updated != null) {
            response.put("success", true);
            response.put("message", "Subscription resumed");
            response.put("subscription", updated);
            return ResponseEntity.ok(response);
        }
        response.put("success", false);
        response.put("message", "Subscription not found");
        return ResponseEntity.badRequest().body(response);
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<Map<String, Object>> cancel(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        SubscriptionPayment updated = subscriptionPaymentService.cancelSubscription(id);
        if (updated != null) {
            response.put("success", true);
            response.put("message", "Subscription cancelled");
            response.put("subscription", updated);
            return ResponseEntity.ok(response);
        }
        response.put("success", false);
        response.put("message", "Subscription not found");
        return ResponseEntity.badRequest().body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        if (subscriptionPaymentService.deleteSubscription(id)) {
            response.put("success", true);
            response.put("message", "Subscription deleted");
            return ResponseEntity.ok(response);
        }
        response.put("success", false);
        response.put("message", "Subscription not found");
        return ResponseEntity.badRequest().body(response);
    }

    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAll() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("subscriptions", subscriptionPaymentService.getAll());
        return ResponseEntity.ok(response);
    }
}
