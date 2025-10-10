package com.neo.springapp.controller;

import com.neo.springapp.model.Card;
import com.neo.springapp.service.CardService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cards")
@CrossOrigin(origins = "http://localhost:4200")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
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
        return cardService.setCardPin(cardId, pin);
    }

    // Reset PIN for a card (forgot PIN functionality)
    @PutMapping("/{cardId}/reset-pin")
    public Card resetCardPin(@PathVariable Long cardId, @RequestParam String pin) {
        return cardService.resetCardPin(cardId, pin);
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
