package com.neo.springapp.controller;

import com.neo.springapp.model.Card;
import com.neo.springapp.service.CardService;
import com.neo.springapp.service.PasswordService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cards")
@CrossOrigin(origins = "http://localhost:4200")
public class CardController {

    private final CardService cardService;
    private final PasswordService passwordService;

    public CardController(CardService cardService, PasswordService passwordService) {
        this.cardService = cardService;
        this.passwordService = passwordService;
    }

    // Create new card
    @PostMapping
    public Card createCard(@RequestBody Card card) {
        return cardService.saveCard(card);
    }

    // Get all cards with pagination and sorting
    @GetMapping
    public Page<Card> getCards(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "issueDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return cardService.getAllCards(page, size, sortBy, sortDir);
    }

    // Get cards by account number
    @GetMapping("/account/{accountNumber}")
    public List<Card> getCardsByAccount(@PathVariable String accountNumber) {
        return cardService.getCardsByAccountNumber(accountNumber);
    }

    // Get cards by user email
    @GetMapping("/user/{userEmail}")
    public List<Card> getCardsByUser(@PathVariable String userEmail) {
        return cardService.getCardsByUserEmail(userEmail);
    }

    // Get cards by card type with pagination
    @GetMapping("/type/{cardType}")
    public Page<Card> getCardsByType(
            @PathVariable String cardType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return cardService.getCardsByType(cardType, page, size);
    }

    // Get cards by status with pagination
    @GetMapping("/status/{status}")
    public Page<Card> getCardsByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return cardService.getCardsByStatus(status, page, size);
    }

    // Get blocked cards with pagination
    @GetMapping("/blocked")
    public Page<Card> getBlockedCards(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return cardService.getBlockedCards(page, size);
    }

    // Get deactivated cards with pagination
    @GetMapping("/deactivated")
    public Page<Card> getDeactivatedCards(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return cardService.getDeactivatedCards(page, size);
    }

    // Get active cards with pagination
    @GetMapping("/active")
    public Page<Card> getActiveCards(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return cardService.getActiveCards(page, size);
    }

    // Get cards expiring soon
    @GetMapping("/expiring-soon")
    public List<Card> getCardsExpiringSoon(@RequestParam(defaultValue = "30") int daysFromNow) {
        return cardService.getCardsExpiringSoon(daysFromNow);
    }

    // Get card statistics
    @GetMapping("/statistics")
    public Object[] getCardStatistics() {
        return cardService.getCardStatistics();
    }

    // Block a card
    @PutMapping("/{cardId}/block")
    public Card blockCard(@PathVariable Long cardId) {
        return cardService.blockCard(cardId);
    }

    // Deactivate a card
    @PutMapping("/{cardId}/deactivate")
    public Card deactivateCard(@PathVariable Long cardId) {
        return cardService.deactivateCard(cardId);
    }

    // Set PIN for a card
    @PutMapping("/{cardId}/pin")
    public Card setCardPin(@PathVariable Long cardId, @RequestParam String pin) {
        // Encrypt PIN before setting
        String encryptedPin = passwordService.encryptPin(pin);
        return cardService.setCardPin(cardId, encryptedPin);
    }

    // Reset PIN for a card (forgot PIN functionality)
    @PutMapping("/{cardId}/reset-pin")
    public Card resetCardPin(@PathVariable Long cardId, @RequestParam String pin) {
        // Encrypt PIN before resetting
        String encryptedPin = passwordService.encryptPin(pin);
        return cardService.resetCardPin(cardId, encryptedPin);
    }

    // Verify PIN for a card
    @PostMapping("/{cardId}/verify-pin")
    public ResponseEntity<Map<String, Object>> verifyCardPin(@PathVariable Long cardId, @RequestParam String pin) {
        try {
            Card card = cardService.getCardById(cardId);
            if (card == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Card not found");
                return ResponseEntity.notFound().build();
            }
            
            if (!card.isPinSet()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "PIN not set for this card");
                return ResponseEntity.badRequest().body(response);
            }
            
            boolean isValid = passwordService.verifyPin(pin, card.getPin());
            Map<String, Object> response = new HashMap<>();
            response.put("success", isValid);
            response.put("message", isValid ? "PIN verified successfully" : "Invalid PIN");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "PIN verification failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // Replace a card (for admin approval of replacement requests)
    @PutMapping("/{cardId}/replace")
    public Card replaceCard(@PathVariable Long cardId, @RequestBody Card replacementData) {
        return cardService.replaceCard(cardId, replacementData);
    }

    // Generate new card
    @PostMapping("/generate")
    public Card generateNewCard(
            @RequestParam String cardType,
            @RequestParam String userName,
            @RequestParam String accountNumber,
            @RequestParam String userEmail) {
        return cardService.generateNewCard(cardType, userName, accountNumber, userEmail);
    }

    // Get card by ID
    @GetMapping("/{cardId}")
    public Card getCardById(@PathVariable Long cardId) {
        return cardService.getCardsByAccountNumber("").stream()
                .filter(card -> card.getId().equals(cardId))
                .findFirst()
                .orElse(null);
    }
}
