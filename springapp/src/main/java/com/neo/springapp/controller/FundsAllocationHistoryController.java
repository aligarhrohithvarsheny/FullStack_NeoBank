package com.neo.springapp.controller;

import com.neo.springapp.model.AllocationUtilization;
import com.neo.springapp.model.FundsAllocation;
import com.neo.springapp.model.FundsAllocationHistory;
import com.neo.springapp.repository.AllocationUtilizationRepository;
import com.neo.springapp.repository.FundsAllocationHistoryRepository;
import com.neo.springapp.repository.FundsAllocationRepository;
import com.neo.springapp.service.UserSessionTokenService.SessionPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/hod/allocations")
public class FundsAllocationHistoryController {
    private final FundsAllocationRepository allocationRepository;
    private final FundsAllocationHistoryRepository historyRepository;
    private final AllocationUtilizationRepository utilizationRepository;

    public FundsAllocationHistoryController(
            FundsAllocationRepository allocationRepository,
            FundsAllocationHistoryRepository historyRepository,
            AllocationUtilizationRepository utilizationRepository) {
        this.allocationRepository = allocationRepository;
        this.historyRepository = historyRepository;
        this.utilizationRepository = utilizationRepository;
    }

    @GetMapping("/{allocationId}/history")
    public ResponseEntity<Map<String, Object>> getAllocationHistory(
            @PathVariable Long allocationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            @AuthenticationPrincipal SessionPrincipal principal) {
        if (principal == null || !"HOD".equals(principal.scope())) {
            return error(HttpStatus.FORBIDDEN, "A HOD session is required.");
        }

        Optional<FundsAllocation> allocationResult = allocationRepository.findById(allocationId);
        if (allocationResult.isEmpty()) return error(HttpStatus.NOT_FOUND, "Allocation not found.");
        FundsAllocation allocation = allocationResult.get();
        if (!principal.userId().equals(allocation.getAllocatedByAdminId())) {
            return error(HttpStatus.FORBIDDEN, "You can only view history for allocations you manage.");
        }

        List<FundsAllocationHistory> allocationHistory = historyRepository
                .findByAllocationRecordIdOrderByChangedAtDesc(allocationId);
        Page<AllocationUtilization> transactionPage = utilizationRepository.findByAllocationIdOrderByTransactionDateDesc(
                allocationId,
                PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100)),
                        Sort.by(Sort.Direction.DESC, "transactionDate")));
        List<Map<String, Object>> transactions = transactionPage.getContent().stream()
                .map(this::toTransactionHistory).toList();

        Map<String, Object> response = new LinkedHashMap<>();
        Map<String, Object> allocationSummary = new LinkedHashMap<>();
        allocationSummary.put("id", allocation.getId());
        allocationSummary.put("allocationId", allocation.getAllocationId());
        allocationSummary.put("allocatedByName", allocation.getAllocatedByName());
        allocationSummary.put("allocatedAmount", allocation.getAllocatedAmount());
        allocationSummary.put("currentBalance", allocation.getCurrentBalance());
        allocationSummary.put("validTill", allocation.getValidTill());
        allocationSummary.put("description", allocation.getDescription());
        allocationSummary.put("status", allocation.getStatus());
        allocationSummary.put("createdAt", allocation.getCreatedAt());
        allocationSummary.put("updatedAt", allocation.getUpdatedAt());
        response.put("success", true);
        response.put("allocation", allocationSummary);
        response.put("allocationHistory", allocationHistory);
        response.put("transactions", transactions);
        response.put("transactionTotalElements", transactionPage.getTotalElements());
        response.put("transactionTotalPages", transactionPage.getTotalPages());
        response.put("transactionPage", transactionPage.getNumber());
        response.put("totalDebits", utilizationRepository.getDebitCount(allocationId));
        response.put("totalCredits", utilizationRepository.getCreditCount(allocationId));
        return ResponseEntity.ok(response);
    }

    private Map<String, Object> toTransactionHistory(AllocationUtilization transaction) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("id", transaction.getId());
        details.put("transactionId", transaction.getUtilizationId());
        details.put("type", transaction.getTransactionType());
        details.put("amount", transaction.getAmount());
        details.put("balanceAfter", transaction.getRemainingBalance());
        details.put("productType", transaction.getProductType());
        details.put("userAccountNumber", transaction.getUserAccountNumber());
        details.put("userName", transaction.getUserName());
        details.put("linkedTransactionId", transaction.getLinkedTransactionId());
        details.put("referenceNumber", transaction.getReferenceNumber());
        details.put("description", transaction.getDescription());
        details.put("remarks", transaction.getRemarks());
        details.put("performedByAdminId", transaction.getPerformedByAdminId());
        details.put("performedByName", transaction.getPerformedByName());
        details.put("status", transaction.getStatus());
        details.put("transactionDate", transaction.getTransactionDate());
        return details;
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("success", false, "message", message));
    }
}
