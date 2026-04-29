package com.neo.springapp.controller;

import com.neo.springapp.model.CreditCardRequest;
import com.neo.springapp.service.CreditCardRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/credit-card-requests")
public class CreditCardRequestController {

    @Autowired
    private CreditCardRequestService service;

    @PostMapping("/create")
    public ResponseEntity<CreditCardRequest> create(@RequestBody CreditCardRequest request, @RequestParam(required = false) Double income) {
        try {
            CreditCardRequest saved = service.saveRequest(request, income);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<CreditCardRequest>> byStatus(@PathVariable String status) {
        return ResponseEntity.ok(service.getRequestsByStatus(status));
    }

    @PutMapping("/approve/{id}")
    public ResponseEntity<CreditCardRequest> approve(@PathVariable Long id, @RequestParam String adminName) {
        CreditCardRequest approved = service.approveRequest(id, adminName);
        return approved != null ? ResponseEntity.ok(approved) : ResponseEntity.notFound().build();
    }

    @PutMapping("/reject/{id}")
    public ResponseEntity<CreditCardRequest> reject(@PathVariable Long id, @RequestParam String adminName) {
        CreditCardRequest rejected = service.rejectRequest(id, adminName);
        return rejected != null ? ResponseEntity.ok(rejected) : ResponseEntity.notFound().build();
    }

    @PostMapping("/preview")
    public ResponseEntity<?> preview(@RequestParam String pan, @RequestParam(required = false) Double income) {
        try {
            // Get comprehensive credit card prediction
            java.util.Map<String, Object> prediction = service.getCreditCardPrediction(pan, income != null ? income : 0.0);
            return ResponseEntity.ok(prediction);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
