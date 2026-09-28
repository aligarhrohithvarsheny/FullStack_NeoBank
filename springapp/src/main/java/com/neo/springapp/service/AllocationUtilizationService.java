package com.neo.springapp.service;

import com.neo.springapp.model.*;
import com.neo.springapp.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for tracking real-time debit and credit transactions against allocations.
 * Each transaction is logged with full audit trail including user, product type, and amounts.
 */
@Service
@SuppressWarnings("null")
public class AllocationUtilizationService {

    @Autowired
    private AllocationUtilizationRepository utilizationRepository;

    @Autowired
    private FundsAllocationRepository allocationRepository;

    @Autowired
    private AllocationMetricsRepository metricsRepository;

    /**
     * Record a debit transaction against an allocation (fund usage).
     * Called when Gold Loan is disbursed, Withdrawal is made, etc.
     */
    @Transactional
    public Map<String, Object> debitAllocationFunds(
            Long allocationId,
            Double amount,
            String productType,
            String userAccountNumber,
            String userName,
            String linkedTransactionId,
            String description,
            Long performedByAdminId) {

        Map<String, Object> result = new HashMap<>();

        FundsAllocation allocation = allocationRepository.findById(allocationId).orElse(null);
        if (allocation == null) {
            result.put("success", false);
            result.put("message", "Allocation not found");
            return result;
        }

        // Validate allocation is active and not expired
        if (!"ACTIVE".equals(allocation.getStatus())) {
            result.put("success", false);
            result.put("message", "Allocation is not active");
            return result;
        }

        // Check if allocation is expired
        if (allocation.getValidTill() != null && LocalDateTime.now().toLocalDate().isAfter(allocation.getValidTill())) {
            result.put("success", false);
            result.put("message", "Allocation has expired");
            return result;
        }

        // Validate sufficient balance
        Double currentBalance = allocation.getCurrentBalance() != null ? allocation.getCurrentBalance() : 0.0;
        if (currentBalance < amount) {
            result.put("success", false);
            result.put("message", "Insufficient allocation balance. Available: " + currentBalance + ", Required: " + amount);
            return result;
        }

        // Create utilization record
        AllocationUtilization utilization = new AllocationUtilization();
        utilization.setAllocationId(allocationId);
        utilization.setTransactionType("DEBIT");
        utilization.setAmount(amount);
        utilization.setRemainingBalance(currentBalance - amount);
        utilization.setProductType(productType);
        utilization.setUserAccountNumber(userAccountNumber);
        utilization.setUserName(userName);
        utilization.setLinkedTransactionId(linkedTransactionId);
        utilization.setDescription(description);
        utilization.setPerformedByAdminId(performedByAdminId);
        utilization.setTransactionDate(LocalDateTime.now());
        utilization.setStatus("SUCCESS");

        AllocationUtilization saved = utilizationRepository.save(utilization);

        // Update allocation record
        allocation.setTotalDebited((allocation.getTotalDebited() != null ? allocation.getTotalDebited() : 0.0) + amount);
        allocation.setTotalUtilized((allocation.getTotalUtilized() != null ? allocation.getTotalUtilized() : 0.0) + amount);
        allocation.setCurrentBalance(allocation.getCurrentBalance() - amount);
        allocation.setUpdatedAt(LocalDateTime.now());
        allocationRepository.save(allocation);

        // Update metrics
        updateMetrics(allocationId, productType, "DEBIT", amount);

        result.put("success", true);
        result.put("message", "Funds debited from allocation successfully");
        result.put("utilizationId", saved.getUtilizationId());
        result.put("remainingBalance", saved.getRemainingBalance());
        result.put("utilization", buildUtilizationDTO(saved));
        return result;
    }

    /**
     * Record a credit transaction against an allocation (fund return/refund).
     * Called when Loan is repaid early, Deposit is reversed, etc.
     */
    @Transactional
    public Map<String, Object> creditAllocationFunds(
            Long allocationId,
            Double amount,
            String productType,
            String userAccountNumber,
            String userName,
            String linkedTransactionId,
            String description,
            Long performedByAdminId) {

        Map<String, Object> result = new HashMap<>();

        FundsAllocation allocation = allocationRepository.findById(allocationId).orElse(null);
        if (allocation == null) {
            result.put("success", false);
            result.put("message", "Allocation not found");
            return result;
        }

        // Create utilization record
        AllocationUtilization utilization = new AllocationUtilization();
        utilization.setAllocationId(allocationId);
        utilization.setTransactionType("CREDIT");
        utilization.setAmount(amount);
        Double newBalance = (allocation.getCurrentBalance() != null ? allocation.getCurrentBalance() : 0.0) + amount;
        utilization.setRemainingBalance(newBalance);
        utilization.setProductType(productType);
        utilization.setUserAccountNumber(userAccountNumber);
        utilization.setUserName(userName);
        utilization.setLinkedTransactionId(linkedTransactionId);
        utilization.setDescription(description);
        utilization.setPerformedByAdminId(performedByAdminId);
        utilization.setTransactionDate(LocalDateTime.now());
        utilization.setStatus("SUCCESS");

        AllocationUtilization saved = utilizationRepository.save(utilization);

        // Update allocation record
        allocation.setTotalCredited((allocation.getTotalCredited() != null ? allocation.getTotalCredited() : 0.0) + amount);
        allocation.setCurrentBalance(newBalance);
        allocation.setUpdatedAt(LocalDateTime.now());
        allocationRepository.save(allocation);

        // Update metrics
        updateMetrics(allocationId, productType, "CREDIT", amount);

        result.put("success", true);
        result.put("message", "Funds credited to allocation successfully");
        result.put("utilizationId", saved.getUtilizationId());
        result.put("remainingBalance", saved.getRemainingBalance());
        result.put("utilization", buildUtilizationDTO(saved));
        return result;
    }

    /**
     * Get utilization history for an allocation.
     */
    public Map<String, Object> getUtilizationHistory(Long allocationId, int page, int size) {
        Map<String, Object> result = new HashMap<>();

        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<AllocationUtilization> utilizations = utilizationRepository.findByAllocationId(allocationId, pageable);

        List<Map<String, Object>> content = new ArrayList<>();
        for (AllocationUtilization u : utilizations.getContent()) {
            content.add(buildUtilizationDTO(u));
        }

        result.put("content", content);
        result.put("totalElements", utilizations.getTotalElements());
        result.put("totalPages", utilizations.getTotalPages());
        result.put("currentPage", page);
        result.put("pageSize", utilizations.getSize());
        return result;
    }

    /**
     * Get utilization history filtered by product type.
     */
    public Map<String, Object> getUtilizationByProduct(Long allocationId, String productType, int page, int size) {
        Map<String, Object> result = new HashMap<>();

        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<AllocationUtilization> utilizations = utilizationRepository.findByAllocationIdAndProductType(allocationId, productType, pageable);

        List<Map<String, Object>> content = new ArrayList<>();
        for (AllocationUtilization u : utilizations.getContent()) {
            content.add(buildUtilizationDTO(u));
        }

        result.put("content", content);
        result.put("totalElements", utilizations.getTotalElements());
        result.put("totalPages", utilizations.getTotalPages());
        result.put("currentPage", page);
        result.put("pageSize", utilizations.getSize());
        return result;
    }

    /**
     * Get debit transactions only.
     */
    public Map<String, Object> getDebitTransactions(Long allocationId, int page, int size) {
        Map<String, Object> result = new HashMap<>();

        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<AllocationUtilization> utilizations = utilizationRepository.findByAllocationIdAndTransactionType(allocationId, "DEBIT", pageable);

        List<Map<String, Object>> content = new ArrayList<>();
        for (AllocationUtilization u : utilizations.getContent()) {
            content.add(buildUtilizationDTO(u));
        }

        result.put("content", content);
        result.put("totalElements", utilizations.getTotalElements());
        result.put("totalPages", utilizations.getTotalPages());
        result.put("currentPage", page);
        result.put("pageSize", utilizations.getSize());
        return result;
    }

    /**
     * Get credit transactions only.
     */
    public Map<String, Object> getCreditTransactions(Long allocationId, int page, int size) {
        Map<String, Object> result = new HashMap<>();

        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<AllocationUtilization> utilizations = utilizationRepository.findByAllocationIdAndTransactionType(allocationId, "CREDIT", pageable);

        List<Map<String, Object>> content = new ArrayList<>();
        for (AllocationUtilization u : utilizations.getContent()) {
            content.add(buildUtilizationDTO(u));
        }

        result.put("content", content);
        result.put("totalElements", utilizations.getTotalElements());
        result.put("totalPages", utilizations.getTotalPages());
        result.put("currentPage", page);
        result.put("pageSize", utilizations.getSize());
        return result;
    }

    /**
     * Get summary of utilization by product type for an allocation.
     */
    public Map<String, Object> getProductTypeSummary(Long allocationId) {
        Map<String, Object> result = new HashMap<>();

        List<AllocationUtilization> allUtilizations = utilizationRepository.findByAllocationIdOrderByTransactionDateDesc(allocationId);

        Map<String, Map<String, Object>> productSummary = new LinkedHashMap<>();
        for (AllocationUtilization u : allUtilizations) {
            String product = u.getProductType();
            productSummary.putIfAbsent(product, new HashMap<String, Object>() {{
                put("productType", product);
                put("totalDebited", 0.0);
                put("totalCredited", 0.0);
                put("debitCount", 0);
                put("creditCount", 0);
                put("netAmount", 0.0);
            }});

            Map<String, Object> summary = productSummary.get(product);
            if ("DEBIT".equals(u.getTransactionType())) {
                summary.put("totalDebited", (Double) summary.get("totalDebited") + u.getAmount());
                summary.put("debitCount", (Integer) summary.get("debitCount") + 1);
            } else {
                summary.put("totalCredited", (Double) summary.get("totalCredited") + u.getAmount());
                summary.put("creditCount", (Integer) summary.get("creditCount") + 1);
            }
            Double net = (Double) summary.get("totalDebited") - (Double) summary.get("totalCredited");
            summary.put("netAmount", net);
        }

        result.put("productBreakdown", productSummary.values());
        result.put("totalProducts", productSummary.size());
        return result;
    }

    // Helper methods

    private void updateMetrics(Long allocationId, String productType, String transactionType, Double amount) {
        Optional<AllocationRealTimeMetrics> metricsOpt = metricsRepository.findByAllocationId(allocationId);
        if (metricsOpt.isEmpty()) {
            return; // Metrics should have been created with allocation
        }

        AllocationRealTimeMetrics metrics = metricsOpt.get();

        if ("DEBIT".equals(transactionType)) {
            metrics.setTotalDebited((metrics.getTotalDebited() != null ? metrics.getTotalDebited() : 0.0) + amount);
            metrics.setDebitCount((metrics.getDebitCount() != null ? metrics.getDebitCount() : 0L) + 1);

            // Update product-specific debits
            switch (productType) {
                case "GOLD_LOAN":
                    metrics.setGoldLoanDebited((metrics.getGoldLoanDebited() != null ? metrics.getGoldLoanDebited() : 0.0) + amount);
                    break;
                case "DEPOSITS":
                    metrics.setDepositsDebited((metrics.getDepositsDebited() != null ? metrics.getDepositsDebited() : 0.0) + amount);
                    break;
                case "WITHDRAWALS":
                    metrics.setWithdrawalsDebited((metrics.getWithdrawalsDebited() != null ? metrics.getWithdrawalsDebited() : 0.0) + amount);
                    break;
                case "LOANS":
                    metrics.setLoansDebited((metrics.getLoansDebited() != null ? metrics.getLoansDebited() : 0.0) + amount);
                    break;
                case "OVERDRAFT":
                    metrics.setOverdraftDebited((metrics.getOverdraftDebited() != null ? metrics.getOverdraftDebited() : 0.0) + amount);
                    break;
                case "SALARY_CREDIT":
                    metrics.setSalaryCreditDebited((metrics.getSalaryCreditDebited() != null ? metrics.getSalaryCreditDebited() : 0.0) + amount);
                    break;
            }
        } else if ("CREDIT".equals(transactionType)) {
            metrics.setTotalCredited((metrics.getTotalCredited() != null ? metrics.getTotalCredited() != null ? metrics.getTotalCredited() : 0.0) + amount);
            metrics.setCreditCount((metrics.getCreditCount() != null ? metrics.getCreditCount() : 0L) + 1);
        }

        metrics.setTotalTransactions((metrics.getDebitCount() != null ? metrics.getDebitCount() : 0L) + (metrics.getCreditCount() != null ? metrics.getCreditCount() : 0L));
        metrics.setLastTransactionDate(LocalDateTime.now());

        metricsRepository.save(metrics);
    }

    private Map<String, Object> buildUtilizationDTO(AllocationUtilization utilization) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", utilization.getId());
        dto.put("utilizationId", utilization.getUtilizationId());
        dto.put("allocationId", utilization.getAllocationId());
        dto.put("transactionType", utilization.getTransactionType());
        dto.put("amount", utilization.getAmount());
        dto.put("remainingBalance", utilization.getRemainingBalance());
        dto.put("productType", utilization.getProductType());
        dto.put("userAccountNumber", utilization.getUserAccountNumber());
        dto.put("userName", utilization.getUserName());
        dto.put("linkedTransactionId", utilization.getLinkedTransactionId());
        dto.put("description", utilization.getDescription());
        dto.put("transactionDate", utilization.getTransactionDate());
        dto.put("status", utilization.getStatus());
        dto.put("referenceNumber", utilization.getReferenceNumber());
        return dto;
    }
}
