package com.neo.springapp.service;

import com.neo.springapp.model.NewCardRequest;
import com.neo.springapp.repository.NewCardRequestRepository;
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
public class NewCardRequestService {

    @Autowired
    private NewCardRequestRepository newCardRequestRepository;

    // Basic CRUD operations
    public NewCardRequest saveRequest(NewCardRequest request) {
        return newCardRequestRepository.save(request);
    }

    public Optional<NewCardRequest> getRequestById(Long id) {
        return newCardRequestRepository.findById(id);
    }

    public List<NewCardRequest> getAllRequests() {
        return newCardRequestRepository.findAll();
    }

    public Page<NewCardRequest> getAllRequestsWithPagination(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                   Sort.by(sortBy).descending() : 
                   Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return newCardRequestRepository.findAll(pageable);
    }

    // Status-based operations
    public List<NewCardRequest> getRequestsByStatus(String status) {
        return newCardRequestRepository.findByStatus(status);
    }

    public Page<NewCardRequest> getRequestsByStatusWithPagination(String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("requestDate").descending());
        return newCardRequestRepository.findByStatus(status, pageable);
    }

    public List<NewCardRequest> getPendingRequestsForReview() {
        return newCardRequestRepository.findPendingRequestsForReview();
    }

    // Card type operations
    public List<NewCardRequest> getRequestsByCardType(String cardType) {
        return newCardRequestRepository.findByCardType(cardType);
    }

    public List<NewCardRequest> getRequestsByCardTypeAndStatus(String cardType, String status) {
        return newCardRequestRepository.findByCardTypeAndStatus(cardType, status);
    }

    // Search operations
    public Page<NewCardRequest> searchRequests(String searchTerm, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("requestDate").descending());
        return newCardRequestRepository.searchRequests(searchTerm, pageable);
    }

    // Admin operations
    public NewCardRequest approveRequest(Long requestId, String adminName) {
        Optional<NewCardRequest> requestOpt = newCardRequestRepository.findById(requestId);
        if (requestOpt.isPresent()) {
            NewCardRequest request = requestOpt.get();
            request.setStatus("Approved");
            request.setProcessedDate(LocalDateTime.now());
            request.setProcessedBy(adminName);
            request.setNewCardNumber(generateNewCardNumber(request.getCardType()));
            return newCardRequestRepository.save(request);
        }
        return null;
    }

    public NewCardRequest rejectRequest(Long requestId, String adminName) {
        Optional<NewCardRequest> requestOpt = newCardRequestRepository.findById(requestId);
        if (requestOpt.isPresent()) {
            NewCardRequest request = requestOpt.get();
            request.setStatus("Rejected");
            request.setProcessedDate(LocalDateTime.now());
            request.setProcessedBy(adminName);
            return newCardRequestRepository.save(request);
        }
        return null;
    }

    // Statistics operations
    public Long getTotalRequestsCount() {
        return newCardRequestRepository.count();
    }

    public Long getRequestsCountByStatus(String status) {
        return newCardRequestRepository.countByStatus(status);
    }

    public List<NewCardRequest> getRecentRequests(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by("requestDate").descending());
        return newCardRequestRepository.findRecentRequests(pageable);
    }

    // Utility methods
    private String generateNewCardNumber(String cardType) {
        String prefix = "4"; // Default Visa prefix
        if (cardType.toLowerCase().contains("mastercard")) {
            prefix = "5";
        } else if (cardType.toLowerCase().contains("amex")) {
            prefix = "3";
        }
        return prefix + String.format("%015d", System.currentTimeMillis() % 1000000000000000L);
    }

    public void deleteRequest(Long id) {
        newCardRequestRepository.deleteById(id);
    }
}
