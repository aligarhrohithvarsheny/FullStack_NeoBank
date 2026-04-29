package com.neo.springapp.controller;

import com.neo.springapp.model.VirtualCard;
import com.neo.springapp.service.VirtualCardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/virtual-cards")
@CrossOrigin(origins = "*")
public class VirtualCardController {

    @Autowired
    private VirtualCardService virtualCardService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> createCard(@RequestBody Map<String, String> body) {
        Map<String, Object> response = new HashMap<>();
        try {
            String accountNumber = body.get("accountNumber");
            String cardholderName = body.get("cardholderName");
            VirtualCard card = virtualCardService.createVirtualCard(accountNumber, cardholderName);
            response.put("success", true);
            response.put("message", "Virtual card created successfully");
            response.put("card", card);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error creating virtual card: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<Map<String, Object>> getByAccountNumber(@PathVariable String accountNumber) {
        Map<String, Object> response = new HashMap<>();
        List<VirtualCard> cards = virtualCardService.getCardsByAccountNumber(accountNumber);
        response.put("success", true);
        response.put("cards", cards);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        Optional<VirtualCard> card = virtualCardService.getCardById(id);
        if (card.isPresent()) {
            response.put("success", true);
            response.put("card", card.get());
            return ResponseEntity.ok(response);
        }
        response.put("success", false);
        response.put("message", "Virtual card not found");
        return ResponseEntity.badRequest().body(response);
    }

    @PutMapping("/{id}/freeze")
    public ResponseEntity<Map<String, Object>> freezeCard(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        VirtualCard card = virtualCardService.freezeCard(id);
        if (card != null) {
            response.put("success", true);
            response.put("message", "Card frozen successfully");
            response.put("card", card);
            return ResponseEntity.ok(response);
        }
        response.put("success", false);
        response.put("message", "Card not found");
        return ResponseEntity.badRequest().body(response);
    }

    @PutMapping("/{id}/unfreeze")
    public ResponseEntity<Map<String, Object>> unfreezeCard(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        VirtualCard card = virtualCardService.unfreezeCard(id);
        if (card != null) {
            response.put("success", true);
            response.put("message", "Card unfrozen successfully");
            response.put("card", card);
            return ResponseEntity.ok(response);
        }
        response.put("success", false);
        response.put("message", "Card not found");
        return ResponseEntity.badRequest().body(response);
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<Map<String, Object>> cancelCard(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        VirtualCard card = virtualCardService.cancelCard(id);
        if (card != null) {
            response.put("success", true);
            response.put("message", "Card cancelled successfully");
            response.put("card", card);
            return ResponseEntity.ok(response);
        }
        response.put("success", false);
        response.put("message", "Card not found");
        return ResponseEntity.badRequest().body(response);
    }

    @PutMapping("/{id}/limits")
    public ResponseEntity<Map<String, Object>> updateLimits(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Map<String, Object> response = new HashMap<>();
        Double dailyLimit = body.get("dailyLimit") != null ? ((Number) body.get("dailyLimit")).doubleValue() : null;
        Double monthlyLimit = body.get("monthlyLimit") != null ? ((Number) body.get("monthlyLimit")).doubleValue() : null;
        VirtualCard card = virtualCardService.updateLimits(id, dailyLimit, monthlyLimit);
        if (card != null) {
            response.put("success", true);
            response.put("message", "Limits updated successfully");
            response.put("card", card);
            return ResponseEntity.ok(response);
        }
        response.put("success", false);
        response.put("message", "Card not found");
        return ResponseEntity.badRequest().body(response);
    }

    @PutMapping("/{id}/toggle-online")
    public ResponseEntity<Map<String, Object>> toggleOnline(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        Map<String, Object> response = new HashMap<>();
        VirtualCard card = virtualCardService.toggleOnlinePayments(id, body.getOrDefault("enabled", true));
        if (card != null) {
            response.put("success", true);
            response.put("card", card);
            return ResponseEntity.ok(response);
        }
        response.put("success", false);
        response.put("message", "Card not found");
        return ResponseEntity.badRequest().body(response);
    }

    @PutMapping("/{id}/toggle-international")
    public ResponseEntity<Map<String, Object>> toggleInternational(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        Map<String, Object> response = new HashMap<>();
        VirtualCard card = virtualCardService.toggleInternationalPayments(id, body.getOrDefault("enabled", false));
        if (card != null) {
            response.put("success", true);
            response.put("card", card);
            return ResponseEntity.ok(response);
        }
        response.put("success", false);
        response.put("message", "Card not found");
        return ResponseEntity.badRequest().body(response);
    }

    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAll() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("cards", virtualCardService.getAll());
        return ResponseEntity.ok(response);
    }
}
