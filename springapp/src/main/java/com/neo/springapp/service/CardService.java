package com.neo.springapp.service;

import com.neo.springapp.model.Card;
import com.neo.springapp.repository.CardRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CardService {

    private final CardRepository cardRepository;

    public CardService(CardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    // Save new card
    public Card saveCard(Card card) {
        return cardRepository.save(card);
    }

    // Get all cards with pagination and sorting
    public Page<Card> getAllCards(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return cardRepository.findAll(pageable);
    }

    // Get cards by account number
    public List<Card> getCardsByAccountNumber(String accountNumber) {
        return cardRepository.findByAccountNumber(accountNumber);
    }

    // Get cards by user email
    public List<Card> getCardsByUserEmail(String userEmail) {
        return cardRepository.findByUserEmail(userEmail);
    }

    // Get cards by card type with pagination
    public Page<Card> getCardsByType(String cardType, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return cardRepository.findByCardTypeOrderByIssueDateDesc(cardType, pageable);
    }

    // Get cards by status with pagination
    public Page<Card> getCardsByStatus(String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return cardRepository.findByStatusOrderByIssueDateDesc(status, pageable);
    }

    // Get blocked cards with pagination
    public Page<Card> getBlockedCards(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return cardRepository.findBlockedCards(pageable);
    }

    // Get deactivated cards with pagination
    public Page<Card> getDeactivatedCards(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return cardRepository.findDeactivatedCards(pageable);
    }

    // Get active cards with pagination
    public Page<Card> getActiveCards(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return cardRepository.findActiveCards(pageable);
    }

    // Get cards expiring soon
    public List<Card> getCardsExpiringSoon(int daysFromNow) {
        LocalDateTime expiryDate = LocalDateTime.now().plusDays(daysFromNow);
        return cardRepository.findCardsExpiringSoon(expiryDate);
    }

    // Get card statistics
    public Object[] getCardStatistics() {
        return cardRepository.getCardStatistics();
    }

    // Block a card
    public Card blockCard(Long cardId) {
        Card card = cardRepository.findById(cardId).orElse(null);
        if (card != null) {
            card.setBlocked(true);
            card.setStatus("Blocked");
            return cardRepository.save(card);
        }
        return null;
    }

    // Deactivate a card
    public Card deactivateCard(Long cardId) {
        Card card = cardRepository.findById(cardId).orElse(null);
        if (card != null) {
            card.setDeactivated(true);
            card.setStatus("Deactivated");
            return cardRepository.save(card);
        }
        return null;
    }

    // Set PIN for a card
    public Card setCardPin(Long cardId, String pin) {
        Card card = cardRepository.findById(cardId).orElse(null);
        if (card != null) {
            card.setPin(pin);
            card.setPinSet(true);
            return cardRepository.save(card);
        }
        return null;
    }

    // Generate new card
    public Card generateNewCard(String cardType, String userName, String accountNumber, String userEmail) {
        String cardNumber = generateCardNumber();
        Card card = new Card(cardNumber, cardType, userName, accountNumber, userEmail);
        return cardRepository.save(card);
    }

    // Generate random card number
    private String generateCardNumber() {
        StringBuilder cardNumber = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            cardNumber.append((int)(Math.random() * 10));
        }
        return cardNumber.toString();
    }
}
