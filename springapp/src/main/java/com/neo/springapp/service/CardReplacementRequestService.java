package com.neo.springapp.service;

import com.neo.springapp.model.CardReplacementRequest;
import com.neo.springapp.model.Card;
import com.neo.springapp.repository.CardReplacementRequestRepository;
import com.neo.springapp.repository.CardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CardReplacementRequestService {

    @Autowired
    private CardReplacementRequestRepository cardReplacementRequestRepository;

    @Autowired
    private CardRepository cardRepository;

    // Basic CRUD operations
    public CardReplacementRequest saveRequest(CardReplacementRequest request) {
        return cardReplacementRequestRepository.save(request);
    }

    public Optional<CardReplacementRequest> getRequestById(Long id) {
        return cardReplacementRequestRepository.findById(id);
    }

    public List<CardReplacementRequest> getAllRequests() {
        return cardReplacementRequestRepository.findAll();
    }

    public Page<CardReplacementRequest> getAllRequestsWithPagination(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                   Sort.by(sortBy).descending() : 
                   Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return cardReplacementRequestRepository.findAll(pageable);
    }

    // Status-based operations
    public List<CardReplacementRequest> getRequestsByStatus(String status) {
        return cardReplacementRequestRepository.findByStatus(status);
    }

    public Page<CardReplacementRequest> getRequestsByStatusWithPagination(String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("requestDate").descending());
        return cardReplacementRequestRepository.findByStatus(status, pageable);
    }

    public List<CardReplacementRequest> getPendingRequestsForReview() {
        return cardReplacementRequestRepository.findPendingRequestsForReview();
    }

    // Search operations
    public Page<CardReplacementRequest> searchRequests(String searchTerm, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("requestDate").descending());
        return cardReplacementRequestRepository.searchRequests(searchTerm, pageable);
    }

    // Admin operations
    public CardReplacementRequest approveRequest(Long requestId, String adminName) {
        Optional<CardReplacementRequest> requestOpt = cardReplacementRequestRepository.findById(requestId);
        if (requestOpt.isPresent()) {
            CardReplacementRequest request = requestOpt.get();
            request.setStatus("Approved");
            request.setProcessedDate(LocalDateTime.now());
            request.setProcessedBy(adminName);
            
            // Generate new card details
            String newCardNumber = generateNewCardNumber();
            String newCvv = generateNewCVV();
            String newExpiryDate = generateNewExpiryDate();
            
            request.setNewCardNumber(newCardNumber);
            
            // Update the user's card in the database - find by current card number
            Card userCard = cardRepository.findByCardNumber(request.getCurrentCardNumber());
            if (userCard != null) {
                userCard.setCardNumber(newCardNumber);
                userCard.setCvv(newCvv);
                userCard.setExpiryDate(newExpiryDate);
                userCard.setStatus("Active");
                userCard.setPinSet(false);
                userCard.setBlocked(false);
                userCard.setDeactivated(false);
                cardRepository.save(userCard);
                
                System.out.println("✅ Card replaced for account: " + request.getAccountNumber() + 
                                 " | Old card: " + request.getCurrentCardNumber() +
                                 " | New card: " + newCardNumber);
            } else {
                System.out.println("⚠️ Card not found with number: " + request.getCurrentCardNumber());
            }
            
            return cardReplacementRequestRepository.save(request);
        }
        return null;
    }

    public CardReplacementRequest rejectRequest(Long requestId, String adminName) {
        Optional<CardReplacementRequest> requestOpt = cardReplacementRequestRepository.findById(requestId);
        if (requestOpt.isPresent()) {
            CardReplacementRequest request = requestOpt.get();
            request.setStatus("Rejected");
            request.setProcessedDate(LocalDateTime.now());
            request.setProcessedBy(adminName);
            return cardReplacementRequestRepository.save(request);
        }
        return null;
    }

    // Statistics operations
    public Long getTotalRequestsCount() {
        return cardReplacementRequestRepository.count();
    }

    public Long getRequestsCountByStatus(String status) {
        return cardReplacementRequestRepository.countByStatus(status);
    }

    public List<CardReplacementRequest> getRecentRequests(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by("requestDate").descending());
        return cardReplacementRequestRepository.findRecentRequests(pageable);
    }

    // Utility methods
    private String generateNewCardNumber() {
        return "4" + String.format("%015d", System.currentTimeMillis() % 1000000000000000L);
    }

    private String generateNewCVV() {
        return String.format("%03d", (int)(Math.random() * 1000));
    }

    private String generateNewExpiryDate() {
        int month = (int)(Math.random() * 12) + 1;
        int year = LocalDateTime.now().getYear() + (int)(Math.random() * 5) + 1;
        return String.format("%02d/%02d", month, year % 100);
    }

    public void deleteRequest(Long id) {
        cardReplacementRequestRepository.deleteById(id);
    }
}
