package com.neo.springapp.controller;

import com.neo.springapp.model.CardReplacementRequest;
import com.neo.springapp.service.CardReplacementRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/card-replacement-requests")
public class CardReplacementRequestController {

    @Autowired
    private CardReplacementRequestService cardReplacementRequestService;

    // Basic CRUD operations
    @PostMapping("/create")
    public ResponseEntity<CardReplacementRequest> createRequest(@RequestBody CardReplacementRequest request) {
        try {
            CardReplacementRequest savedRequest = cardReplacementRequestService.saveRequest(request);
            return ResponseEntity.ok(savedRequest);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<CardReplacementRequest> getRequestById(@PathVariable Long id) {
        Optional<CardReplacementRequest> request = cardReplacementRequestService.getRequestById(id);
        return request.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/all")
    public ResponseEntity<Page<CardReplacementRequest>> getAllRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "requestDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Page<CardReplacementRequest> requests = cardReplacementRequestService.getAllRequestsWithPagination(page, size, sortBy, sortDir);
        return ResponseEntity.ok(requests);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRequest(@PathVariable Long id) {
        try {
            cardReplacementRequestService.deleteRequest(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // Status-based operations
    @GetMapping("/status/{status}")
    public ResponseEntity<List<CardReplacementRequest>> getRequestsByStatus(@PathVariable String status) {
        List<CardReplacementRequest> requests = cardReplacementRequestService.getRequestsByStatus(status);
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/status/{status}/paginated")
    public ResponseEntity<Page<CardReplacementRequest>> getRequestsByStatusWithPagination(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<CardReplacementRequest> requests = cardReplacementRequestService.getRequestsByStatusWithPagination(status, page, size);
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/pending")
    public ResponseEntity<List<CardReplacementRequest>> getPendingRequestsForReview() {
        List<CardReplacementRequest> requests = cardReplacementRequestService.getPendingRequestsForReview();
        return ResponseEntity.ok(requests);
    }

    // Admin operations
    @PutMapping("/approve/{id}")
    public ResponseEntity<CardReplacementRequest> approveRequest(@PathVariable Long id, @RequestParam String adminName) {
        CardReplacementRequest approvedRequest = cardReplacementRequestService.approveRequest(id, adminName);
        return approvedRequest != null ? ResponseEntity.ok(approvedRequest) : ResponseEntity.notFound().build();
    }

    @PutMapping("/reject/{id}")
    public ResponseEntity<CardReplacementRequest> rejectRequest(@PathVariable Long id, @RequestParam String adminName) {
        CardReplacementRequest rejectedRequest = cardReplacementRequestService.rejectRequest(id, adminName);
        return rejectedRequest != null ? ResponseEntity.ok(rejectedRequest) : ResponseEntity.notFound().build();
    }

    // Search operations
    @GetMapping("/search")
    public ResponseEntity<Page<CardReplacementRequest>> searchRequests(
            @RequestParam String searchTerm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<CardReplacementRequest> requests = cardReplacementRequestService.searchRequests(searchTerm, page, size);
        return ResponseEntity.ok(requests);
    }

    // Statistics endpoints
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getRequestStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRequests", cardReplacementRequestService.getTotalRequestsCount());
        stats.put("pendingRequests", cardReplacementRequestService.getRequestsCountByStatus("Pending"));
        stats.put("approvedRequests", cardReplacementRequestService.getRequestsCountByStatus("Approved"));
        stats.put("rejectedRequests", cardReplacementRequestService.getRequestsCountByStatus("Rejected"));
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/recent")
    public ResponseEntity<List<CardReplacementRequest>> getRecentRequests(@RequestParam(defaultValue = "5") int limit) {
        List<CardReplacementRequest> requests = cardReplacementRequestService.getRecentRequests(limit);
        return ResponseEntity.ok(requests);
    }
}
