package com.neo.springapp.controller;

import com.neo.springapp.model.NewCardRequest;
import com.neo.springapp.service.NewCardRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/new-card-requests")
@CrossOrigin(origins = "*")
public class NewCardRequestController {

    @Autowired
    private NewCardRequestService newCardRequestService;

    // Basic CRUD operations
    @PostMapping("/create")
    public ResponseEntity<NewCardRequest> createRequest(@RequestBody NewCardRequest request) {
        try {
            NewCardRequest savedRequest = newCardRequestService.saveRequest(request);
            return ResponseEntity.ok(savedRequest);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<NewCardRequest> getRequestById(@PathVariable Long id) {
        Optional<NewCardRequest> request = newCardRequestService.getRequestById(id);
        return request.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/all")
    public ResponseEntity<Page<NewCardRequest>> getAllRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "requestDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Page<NewCardRequest> requests = newCardRequestService.getAllRequestsWithPagination(page, size, sortBy, sortDir);
        return ResponseEntity.ok(requests);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRequest(@PathVariable Long id) {
        try {
            newCardRequestService.deleteRequest(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // Status-based operations
    @GetMapping("/status/{status}")
    public ResponseEntity<List<NewCardRequest>> getRequestsByStatus(@PathVariable String status) {
        List<NewCardRequest> requests = newCardRequestService.getRequestsByStatus(status);
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/status/{status}/paginated")
    public ResponseEntity<Page<NewCardRequest>> getRequestsByStatusWithPagination(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<NewCardRequest> requests = newCardRequestService.getRequestsByStatusWithPagination(status, page, size);
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/pending")
    public ResponseEntity<List<NewCardRequest>> getPendingRequestsForReview() {
        List<NewCardRequest> requests = newCardRequestService.getPendingRequestsForReview();
        return ResponseEntity.ok(requests);
    }

    // Card type operations
    @GetMapping("/card-type/{cardType}")
    public ResponseEntity<List<NewCardRequest>> getRequestsByCardType(@PathVariable String cardType) {
        List<NewCardRequest> requests = newCardRequestService.getRequestsByCardType(cardType);
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/card-type/{cardType}/status/{status}")
    public ResponseEntity<List<NewCardRequest>> getRequestsByCardTypeAndStatus(
            @PathVariable String cardType, 
            @PathVariable String status) {
        List<NewCardRequest> requests = newCardRequestService.getRequestsByCardTypeAndStatus(cardType, status);
        return ResponseEntity.ok(requests);
    }

    // Admin operations
    @PutMapping("/approve/{id}")
    public ResponseEntity<NewCardRequest> approveRequest(@PathVariable Long id, @RequestParam String adminName) {
        NewCardRequest approvedRequest = newCardRequestService.approveRequest(id, adminName);
        return approvedRequest != null ? ResponseEntity.ok(approvedRequest) : ResponseEntity.notFound().build();
    }

    @PutMapping("/reject/{id}")
    public ResponseEntity<NewCardRequest> rejectRequest(@PathVariable Long id, @RequestParam String adminName) {
        NewCardRequest rejectedRequest = newCardRequestService.rejectRequest(id, adminName);
        return rejectedRequest != null ? ResponseEntity.ok(rejectedRequest) : ResponseEntity.notFound().build();
    }

    // Search operations
    @GetMapping("/search")
    public ResponseEntity<Page<NewCardRequest>> searchRequests(
            @RequestParam String searchTerm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<NewCardRequest> requests = newCardRequestService.searchRequests(searchTerm, page, size);
        return ResponseEntity.ok(requests);
    }

    // Statistics endpoints
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getRequestStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRequests", newCardRequestService.getTotalRequestsCount());
        stats.put("pendingRequests", newCardRequestService.getRequestsCountByStatus("Pending"));
        stats.put("approvedRequests", newCardRequestService.getRequestsCountByStatus("Approved"));
        stats.put("rejectedRequests", newCardRequestService.getRequestsCountByStatus("Rejected"));
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/recent")
    public ResponseEntity<List<NewCardRequest>> getRecentRequests(@RequestParam(defaultValue = "5") int limit) {
        List<NewCardRequest> requests = newCardRequestService.getRecentRequests(limit);
        return ResponseEntity.ok(requests);
    }
}
