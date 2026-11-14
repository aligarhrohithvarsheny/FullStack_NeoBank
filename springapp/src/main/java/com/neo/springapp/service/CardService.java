package com.neo.springapp.service;

import com.neo.springapp.model.Card;
import com.neo.springapp.repository.CardRepository;
import com.neo.springapp.repository.CardActionHistoryRepository;
import com.neo.springapp.repository.TransactionRepository;
import com.neo.springapp.model.CardActionHistory;
import com.neo.springapp.model.Transaction;
import com.neo.springapp.model.Account;
import com.neo.springapp.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@SuppressWarnings("null")
public class CardService {
    private static final SecureRandom CARD_RANDOM = new SecureRandom();

    private final CardRepository cardRepository;
    private final AccountService accountService;
    private final CardActionHistoryRepository cardActionHistoryRepository;
    private final TransactionRepository transactionRepository;

    public CardService(CardRepository cardRepository, AccountService accountService,
                       CardActionHistoryRepository cardActionHistoryRepository,
                       TransactionRepository transactionRepository) {
        this.cardRepository = cardRepository;
        this.accountService = accountService;
        this.cardActionHistoryRepository = cardActionHistoryRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public Card generateDebitCardForUser(User user, String aadhaarNumber) {
        if (user == null || !"APPROVED".equalsIgnoreCase(user.getStatus())
                || user.getAccount() == null || user.getAccountNumber() == null
                || user.getAccountNumber().isBlank()) {
            throw new IllegalStateException("An approved, active account is required to generate a debit card.");
        }

        Account account = user.getAccount();
        if (!"ACTIVE".equalsIgnoreCase(account.getStatus())) {
            throw new IllegalStateException("Your account is not active and cannot receive a debit card.");
        }
        String normalizedAadhaar = aadhaarNumber == null ? "" : aadhaarNumber.trim();
        String registeredAadhaar = account.getAadharNumber() == null ? "" : account.getAadharNumber().trim();
        if (!normalizedAadhaar.matches("[0-9]{12}")
                || !MessageDigest.isEqual(
                normalizedAadhaar.getBytes(StandardCharsets.US_ASCII),
                registeredAadhaar.getBytes(StandardCharsets.US_ASCII))) {
            throw new IllegalArgumentException("The Aadhaar number does not match this account.");
        }
        if (!account.isAadharVerified()
                || !"VERIFIED".equalsIgnoreCase(account.getAadharVerificationStatus())) {
            throw new IllegalStateException("Complete Aadhaar verification before generating your debit card.");
        }

        boolean hasDebitCard = cardRepository.findByAccountNumber(user.getAccountNumber()).stream()
                .anyMatch(card -> isDebitCard(card.getCardType()));
        if (hasDebitCard) {
            throw new IllegalStateException("A debit card is already linked to this account.");
        }

        String cardHolder = account.getName() == null || account.getName().isBlank()
                ? user.getUsername() : account.getName();
        Card card = new Card(generateDebitCardNumber(), "Visa Debit", cardHolder,
                user.getAccountNumber(), user.getEmail());
        LocalDateTime expiry = LocalDateTime.now().plusYears(3);
        card.setExpiryDate(String.format(Locale.ROOT, "%02d/%02d",
                expiry.getMonthValue(), expiry.getYear() % 100));
        card.setExpiryDateTime(expiry);
        card.setStatus("Active");
        card.setPinSet(false);

        Card savedCard = cardRepository.save(card);
        recordAction(savedCard, "GENERATE", "NONE", savedCard.getMaskedCardNumber(), "USER");
        return savedCard;
    }

    private boolean isDebitCard(String cardType) {
        String type = cardType == null ? "" : cardType.toLowerCase(Locale.ROOT);
        return type.contains("debit") && !type.contains("credit");
    }

    private String generateDebitCardNumber() {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder firstFifteenDigits = new StringBuilder("4");
            while (firstFifteenDigits.length() < 15) {
                firstFifteenDigits.append(CARD_RANDOM.nextInt(10));
            }

            int sum = 0;
            boolean doubleDigit = true;
            for (int index = firstFifteenDigits.length() - 1; index >= 0; index--) {
                int digit = firstFifteenDigits.charAt(index) - '0';
                if (doubleDigit) {
                    digit *= 2;
                    if (digit > 9) digit -= 9;
                }
                sum += digit;
                doubleDigit = !doubleDigit;
            }
            String cardNumber = firstFifteenDigits.append((10 - sum % 10) % 10).toString();
            if (cardRepository.findByCardNumber(cardNumber) == null) return cardNumber;
        }
        throw new IllegalStateException("Could not generate a unique debit card number. Please try again.");
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

    // Get card by ID
    public Card getCardById(Long cardId) {
        return cardRepository.findById(cardId).orElse(null);
    }

    // Block a card
    public Card blockCard(Long cardId) {
        Card card = cardRepository.findById(cardId).orElse(null);
        if (card != null) {
            String oldStatus = card.getStatus();
            card.setBlocked(true);
            card.setStatus("Blocked");
            recordAction(card, "BLOCK", oldStatus, "Blocked", "ADMIN");
            return cardRepository.save(card);
        }
        return null;
    }

    public Card unblockCard(Long cardId) {
        Card card = cardRepository.findById(cardId).orElse(null);
        if (card != null) {
            card.setBlocked(false);
            card.setPinLocked(false);
            card.setStatus("Active");
            recordAction(card, "UNBLOCK", "Blocked", "Active", "ADMIN");
            return cardRepository.save(card);
        }
        return null;
    }

    public List<Transaction> getCardTransactions(Long cardId) {
        Card card = cardRepository.findById(cardId).orElse(null);
        if (card == null || card.getAccountNumber() == null) {
            return List.of();
        }
        Pageable pageable = PageRequest.of(0, 50);
        return transactionRepository.findByAccountNumberOrderByDateDesc(card.getAccountNumber(), pageable).getContent();
    }

    public List<CardActionHistory> getCardActionHistory(Long cardId) {
        return cardActionHistoryRepository.findByCardIdOrderByCreatedAtDesc(cardId);
    }

    private void recordAction(Card card, String actionType, String oldValue, String newValue, String changedBy) {
        CardActionHistory history = new CardActionHistory();
        history.setCardId(card.getId());
        history.setAccountNumber(card.getAccountNumber());
        history.setActionType(actionType);
        history.setOldValue(oldValue);
        history.setNewValue(newValue);
        history.setChangedBy(changedBy);
        history.setCreatedAt(LocalDateTime.now());
        cardActionHistoryRepository.save(history);
    }

    // Deactivate a card
    public Card deactivateCard(Long cardId) {
        Card card = cardRepository.findById(cardId).orElse(null);
        if (card != null) {
            String oldStatus = card.getStatus();
            card.setDeactivated(true);
            card.setStatus("Deactivated");
            recordAction(card, "DEACTIVATE", oldStatus, "Deactivated", "ADMIN");
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

    // Reset PIN for a card (forgot PIN functionality)
    public Card resetCardPin(Long cardId, String pin) {
        Card card = cardRepository.findById(cardId).orElse(null);
        if (card != null) {
            card.setPin(pin);
            card.setPinSet(true);
            // Reset any blocked/deactivated status when PIN is reset
            card.setBlocked(false);
            card.setDeactivated(false);
            card.setStatus("Active");
            return cardRepository.save(card);
        }
        return null;
    }

    // Replace a card (for admin approval of replacement requests)
    public Card replaceCard(Long cardId, Card replacementData) {
        Card card = cardRepository.findById(cardId).orElse(null);
        if (card != null) {
            String oldLast4 = card.getCardNumber() != null && card.getCardNumber().length() >= 4
                    ? card.getCardNumber().substring(card.getCardNumber().length() - 4) : "****";
            card.setCardNumber(replacementData.getCardNumber());
            card.setCvv(replacementData.getCvv());
            card.setExpiryDate(replacementData.getExpiryDate());
            card.setStatus(replacementData.getStatus() != null ? replacementData.getStatus() : "Active");
            card.setPinSet(replacementData.isPinSet());
            card.setBlocked(false);
            card.setDeactivated(false);
            card.setPinLocked(false);
            recordAction(card, "REPLACE", "****" + oldLast4, "****" + card.getCardNumber().substring(card.getCardNumber().length() - 4), "ADMIN");
            return cardRepository.save(card);
        }
        return null;
    }

    public Card replaceCardAuto(Long cardId) {
        Card card = cardRepository.findById(cardId).orElse(null);
        if (card == null) return null;
        Card replacement = new Card();
        replacement.setCardNumber(generateCardNumber());
        replacement.setCvv(String.format("%03d", (int)(Math.random() * 1000)));
        int month = 1 + (int)(Math.random() * 12);
        int year = LocalDateTime.now().getYear() + 3;
        replacement.setExpiryDate(String.format("%02d/%02d", month, year % 100));
        replacement.setStatus("Active");
        replacement.setPinSet(false);
        return replaceCard(cardId, replacement);
    }

    public Card addOnCard(Long cardId) {
        Card parent = cardRepository.findById(cardId).orElse(null);
        if (parent == null || parent.getAccountNumber() == null) return null;
        Card addOn = new Card(generateCardNumber(), "Add-on Debit", parent.getUserName(),
                parent.getAccountNumber(), parent.getUserEmail());
        Card saved = cardRepository.save(addOn);
        recordAction(parent, "ADD_ON", "NONE", "CARD_" + saved.getId(), "ADMIN");
        return saved;
    }

    public Card mergeCards(Long cardId, Long targetCardId) {
        Card source = cardRepository.findById(cardId).orElse(null);
        Card target = cardRepository.findById(targetCardId).orElse(null);
        if (source == null || target == null || source.getAccountNumber() == null
                || !source.getAccountNumber().equals(target.getAccountNumber())) return null;
        String oldStatus = source.getStatus();
        source.setDeactivated(true);
        source.setStatus("Merged");
        recordAction(source, "MERGE", oldStatus, "CARD_" + target.getId(), "ADMIN");
        return cardRepository.save(source);
    }

    // Generate new card
    public Card generateNewCard(String cardType, String userName, String accountNumber, String userEmail) {
        // Ensure account is active before issuing any card
        Account account = accountService.getAccountByNumber(accountNumber);
        if (account == null) {
            throw new RuntimeException("Cannot generate card: account not found: " + accountNumber);
        }
        if (!"ACTIVE".equalsIgnoreCase(account.getStatus())) {
            throw new RuntimeException("Cannot generate card: account is not active: " + accountNumber + " | status=" + account.getStatus());
        }

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
