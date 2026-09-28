package com.neo.springapp.service;

import com.neo.springapp.model.*;
import com.neo.springapp.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for managing fund allocations from HOD to branch managers.
 * Handles creation, updating, cancellation, and status tracking of allocations.
 */
@Service
@SuppressWarnings("null")
public class FundsAllocationService {

    @Autowired
    private FundsAllocationRepository allocationRepository;

    @Autowired
    private AllocationUtilizationRepository utilizationRepository;

    @Autowired
    private AllocationMetricsRepository metricsRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private AccountService accountService;

    @Autowired
    private TransactionService transactionService;

    /**
     * Create new funds allocation by HOD for a branch manager.
     */
    @Transactional
    public Map<String, Object> allocateFunds(
            Double amount,
            Long branchManagerId,
            String allocationType,
            String productTypes,
            LocalDate validTill,
            String description,
            Long hodAdminId) {

        Map<String, Object> result = new HashMap<>();

        // Validate inputs
        if (amount == null || amount <= 0) {
            result.put("success", false);
            result.put("message", "Allocation amount must be greater than 0");
            return result;
        }

        Admin hodAdmin = adminRepository.findById(hodAdminId).orElse(null);
        if (hodAdmin == null) {
            result.put("success", false);
            result.put("message", "HOD admin not found");
            return result;
        }

        Admin branchManager = adminRepository.findById(branchManagerId).orElse(null);
        if (branchManager == null) {
            result.put("success", false);
            result.put("message", "Branch manager not found");
            return result;
        }

        String managerAccountNumber = branchManager.getSalaryAccountNumber();

        // Create allocation record
        FundsAllocation allocation = new FundsAllocation();
        allocation.setManagerId(branchManagerId);
        allocation.setManagerName(branchManager.getName());
        allocation.setBranchName(branchManager.getAssignedCity() != null && !branchManager.getAssignedCity().isBlank()
            ? branchManager.getAssignedCity().trim() + " Branch"
            : "Branch " + branchManager.getId());
        allocation.setManagerAccountNumber(managerAccountNumber);
        allocation.setAllocatedAmount(amount);
        allocation.setAllocationType(allocationType != null ? allocationType : "GENERAL");
        allocation.setProductTypes(productTypes != null ? productTypes : "GOLD_LOAN,DEPOSITS,WITHDRAWALS,LOANS,OVERDRAFT,SALARY_CREDITS");
        allocation.setAllocationDate(LocalDate.now());
        allocation.setValidFrom(LocalDate.now());
        allocation.setValidTill(validTill != null ? validTill : LocalDate.now().plusMonths(1));
        allocation.setDescription(description);
        allocation.setAllocatedByAdminId(hodAdminId);
        allocation.setAllocatedByName(hodAdmin.getName());
        allocation.setStatus("ACTIVE");
        allocation.setCurrentBalance(amount);
        allocation.setTotalUtilized(0.0);
        allocation.setTotalDebited(0.0);
        allocation.setTotalCredited(0.0);

        FundsAllocation saved = allocationRepository.save(allocation);

        // Create corresponding metrics record
        AllocationRealTimeMetrics metrics = new AllocationRealTimeMetrics();
        metrics.setAllocationId(saved.getId());
        metrics.setAllocationIdStr(saved.getAllocationId());
        metrics.setTotalAllocated(amount);
        metrics.setCurrentBalance(amount);
        metrics.setTotalDebited(0.0);
        metrics.setTotalCredited(0.0);
        metrics.setDebitCount(0L);
        metrics.setCreditCount(0L);
        metrics.setTotalTransactions(0L);
        metrics.setMetricsDate(LocalDate.now());
        metricsRepository.save(metrics);

        result.put("success", true);
        result.put("message", "Funds allocated successfully");
        result.put("allocationId", saved.getAllocationId());
        result.put("allocation", buildAllocationDTO(saved));
        return result;
    }

    /**
     * Get list of all allocations with filters.
     */
    public Map<String, Object> getAllAllocations(String status, Long branchManagerId, LocalDate fromDate, LocalDate toDate, int page, int size) {
        Map<String, Object> result = new HashMap<>();
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));

        Page<FundsAllocation> allocations;
        if (branchManagerId != null && status != null) {
            allocations = allocationRepository.findByManagerIdAndStatus(branchManagerId, status, pageable);
        } else if (branchManagerId != null) {
            allocations = allocationRepository.findByManagerId(branchManagerId, pageable);
        } else if (status != null) {
            allocations = allocationRepository.findByStatus(status, pageable);
        } else {
            allocations = allocationRepository.findAll(pageable);
        }

        List<Map<String, Object>> content = new ArrayList<>();
        for (FundsAllocation allocation : allocations.getContent()) {
            content.add(buildAllocationDTO(allocation));
        }

        result.put("content", content);
        result.put("totalElements", allocations.getTotalElements());
        result.put("totalPages", allocations.getTotalPages());
        result.put("currentPage", page);
        result.put("pageSize", allocations.getSize());
        return result;
    }

    /**
     * Get detailed allocation information with metrics.
     */
    public Map<String, Object> getAllocationDetails(Long allocationId) {
        Map<String, Object> result = new HashMap<>();

        FundsAllocation allocation = allocationRepository.findById(allocationId).orElse(null);
        if (allocation == null) {
            result.put("success", false);
            result.put("message", "Allocation not found");
            return result;
        }

        Optional<AllocationRealTimeMetrics> metricsOpt = metricsRepository.findByAllocationId(allocationId);

        result.put("success", true);
        result.put("allocation", buildAllocationDTO(allocation));
        if (metricsOpt.isPresent()) {
            result.put("metrics", buildMetricsDTO(metricsOpt.get()));
        }

        // Get recent transactions
        List<AllocationUtilization> recentTx = utilizationRepository.findByAllocationIdOrderByTransactionDateDesc(allocationId);
        List<Map<String, Object>> txList = new ArrayList<>();
        for (AllocationUtilization tx : recentTx.stream().limit(20).toList()) {
            txList.add(buildUtilizationDTO(tx));
        }
        result.put("recentTransactions", txList);

        return result;
    }

    /**
     * Update allocation (amount, validity, description).
     */
    @Transactional
    public Map<String, Object> updateAllocation(Long allocationId, Double newAmount, LocalDate newValidTill, String description, Long hodAdminId) {
        Map<String, Object> result = new HashMap<>();

        FundsAllocation allocation = allocationRepository.findById(allocationId).orElse(null);
        if (allocation == null) {
            result.put("success", false);
            result.put("message", "Allocation not found");
            return result;
        }

        if (!"ACTIVE".equals(allocation.getStatus())) {
            result.put("success", false);
            result.put("message", "Can only update ACTIVE allocations");
            return result;
        }

        if (newAmount != null && newAmount > 0) {
            allocation.setAllocatedAmount(newAmount);
            allocation.setCurrentBalance(newAmount - (allocation.getTotalDebited() != null ? allocation.getTotalDebited() : 0.0)
                    + (allocation.getTotalCredited() != null ? allocation.getTotalCredited() : 0.0));
        }

        if (newValidTill != null) {
            allocation.setValidTill(newValidTill);
        }

        if (description != null && !description.trim().isEmpty()) {
            allocation.setDescription(description);
        }

        allocation.setUpdatedAt(LocalDateTime.now());
        FundsAllocation updated = allocationRepository.save(allocation);

        // Update metrics
        Optional<AllocationRealTimeMetrics> metricsOpt = metricsRepository.findByAllocationId(allocationId);
        if (metricsOpt.isPresent() && newAmount != null) {
            AllocationRealTimeMetrics metrics = metricsOpt.get();
            metrics.setTotalAllocated(newAmount);
            metrics.setCurrentBalance(newAmount - (metrics.getTotalDebited() != null ? metrics.getTotalDebited() : 0.0)
                    + (metrics.getTotalCredited() != null ? metrics.getTotalCredited() : 0.0));
            metricsRepository.save(metrics);
        }

        result.put("success", true);
        result.put("message", "Allocation updated successfully");
        result.put("allocation", buildAllocationDTO(updated));
        return result;
    }

    /**
     * Cancel an allocation.
     */
    @Transactional
    public Map<String, Object> cancelAllocation(Long allocationId, String reason, Long hodAdminId) {
        Map<String, Object> result = new HashMap<>();

        FundsAllocation allocation = allocationRepository.findById(allocationId).orElse(null);
        if (allocation == null) {
            result.put("success", false);
            result.put("message", "Allocation not found");
            return result;
        }

        allocation.setStatus("CANCELLED");
        allocation.setCancelledAt(LocalDateTime.now());
        allocation.setCancelledReason(reason);
        allocation.setCancelledByAdminId(hodAdminId);
        allocation.setUpdatedAt(LocalDateTime.now());

        FundsAllocation updated = allocationRepository.save(allocation);

        result.put("success", true);
        result.put("message", "Allocation cancelled successfully");
        result.put("allocation", buildAllocationDTO(updated));
        return result;
    }

    /**
     * Pause an allocation temporarily.
     */
    @Transactional
    public Map<String, Object> pauseAllocation(Long allocationId, String reason) {
        Map<String, Object> result = new HashMap<>();

        FundsAllocation allocation = allocationRepository.findById(allocationId).orElse(null);
        if (allocation == null) {
            result.put("success", false);
            result.put("message", "Allocation not found");
            return result;
        }

        if (!"ACTIVE".equals(allocation.getStatus())) {
            result.put("success", false);
            result.put("message", "Can only pause ACTIVE allocations");
            return result;
        }

        allocation.setStatus("PAUSED");
        allocation.setNotes((allocation.getNotes() != null ? allocation.getNotes() + "; " : "") + "Paused: " + reason);
        allocation.setUpdatedAt(LocalDateTime.now());

        FundsAllocation updated = allocationRepository.save(allocation);

        result.put("success", true);
        result.put("message", "Allocation paused successfully");
        result.put("allocation", buildAllocationDTO(updated));
        return result;
    }

    /**
     * Resume a paused allocation.
     */
    @Transactional
    public Map<String, Object> resumeAllocation(Long allocationId) {
        Map<String, Object> result = new HashMap<>();

        FundsAllocation allocation = allocationRepository.findById(allocationId).orElse(null);
        if (allocation == null) {
            result.put("success", false);
            result.put("message", "Allocation not found");
            return result;
        }

        if (!"PAUSED".equals(allocation.getStatus())) {
            result.put("success", false);
            result.put("message", "Can only resume PAUSED allocations");
            return result;
        }

        allocation.setStatus("ACTIVE");
        allocation.setUpdatedAt(LocalDateTime.now());

        FundsAllocation updated = allocationRepository.save(allocation);

        result.put("success", true);
        result.put("message", "Allocation resumed successfully");
        result.put("allocation", buildAllocationDTO(updated));
        return result;
    }

    /**
     * Check allocation status for a branch manager.
     */
    public Map<String, Object> checkAllocationStatus(Long branchManagerId) {
        Map<String, Object> result = new HashMap<>();

        List<FundsAllocation> activeAllocations = allocationRepository.findActiveAllocationsForManager(branchManagerId);
        Double totalActive = allocationRepository.getTotalActiveAllocationForManager(branchManagerId);

        List<Map<String, Object>> allocations = new ArrayList<>();
        for (FundsAllocation allocation : activeAllocations) {
            allocations.add(buildAllocationDTO(allocation));
        }

        result.put("success", true);
        result.put("totalActiveAllocations", totalActive != null ? totalActive : 0.0);
        result.put("allocationCount", activeAllocations.size());
        result.put("allocations", allocations);
        return result;
    }

    /**
     * Mark allocation as completed when period ends or balance is zero.
     */
    @Transactional
    public void completeAllocationIfNeeded(Long allocationId) {
        FundsAllocation allocation = allocationRepository.findById(allocationId).orElse(null);
        if (allocation != null && "ACTIVE".equals(allocation.getStatus())) {
            if (allocation.getValidTill() != null && LocalDate.now().isAfter(allocation.getValidTill())) {
                allocation.setStatus("COMPLETED");
                allocation.setUpdatedAt(LocalDateTime.now());
                allocationRepository.save(allocation);
            }
        }
    }

    // Helper methods

    private Map<String, Object> buildAllocationDTO(FundsAllocation allocation) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", allocation.getId());
        dto.put("allocationId", allocation.getAllocationId());
        dto.put("managerId", allocation.getManagerId());
        dto.put("managerName", allocation.getManagerName());
        Admin manager = allocation.getManagerId() != null
            ? adminRepository.findById(allocation.getManagerId()).orElse(null)
            : null;
        dto.put("city", manager != null ? manager.getAssignedCity() : null);
        dto.put("state", null);
        dto.put("branchName", allocation.getBranchName());
        dto.put("managerAccountNumber", allocation.getManagerAccountNumber());
        dto.put("allocatedAmount", allocation.getAllocatedAmount());
        dto.put("allocationType", allocation.getAllocationType());
        dto.put("productTypes", allocation.getProductTypes());
        dto.put("allocationDate", allocation.getAllocationDate());
        dto.put("validFrom", allocation.getValidFrom());
        dto.put("validTill", allocation.getValidTill());
        dto.put("status", allocation.getStatus());
        dto.put("description", allocation.getDescription());
        dto.put("totalUtilized", allocation.getTotalUtilized() != null ? allocation.getTotalUtilized() : 0.0);
        dto.put("totalDebited", allocation.getTotalDebited() != null ? allocation.getTotalDebited() : 0.0);
        dto.put("totalCredited", allocation.getTotalCredited() != null ? allocation.getTotalCredited() : 0.0);
        dto.put("currentBalance", allocation.getCurrentBalance() != null ? allocation.getCurrentBalance() : 0.0);
        dto.put("allocationAccountId", allocation.getAllocationAccountId());
        dto.put("accountNumber", allocation.getLinkedAccountNumber());
        dto.put("ifscCode", allocation.getLinkedIfscCode());
        dto.put("accountHolderName", allocation.getLinkedAccountHolderName());
        dto.put("bankName", allocation.getLinkedBankName());
        dto.put("accountStatus", allocation.getAccountStatus());
        dto.put("accountVerificationStatus", allocation.getAccountVerificationStatus());
        dto.put("chequeVerificationStatus", allocation.getChequeVerificationStatus());
        dto.put("chargeManagementEnabled", allocation.getChargeManagementEnabled());
        dto.put("createdAt", allocation.getCreatedAt());
        dto.put("updatedAt", allocation.getUpdatedAt());
        return dto;
    }

    private Map<String, Object> buildMetricsDTO(AllocationRealTimeMetrics metrics) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", metrics.getId());
        dto.put("allocationId", metrics.getAllocationId());
        dto.put("totalAllocated", metrics.getTotalAllocated());
        dto.put("totalDebited", metrics.getTotalDebited());
        dto.put("totalCredited", metrics.getTotalCredited());
        dto.put("currentBalance", metrics.getCurrentBalance());
        dto.put("utilizationPercentage", metrics.getUtilizationPercentage());
        dto.put("debitCount", metrics.getDebitCount());
        dto.put("creditCount", metrics.getCreditCount());
        dto.put("totalTransactions", metrics.getTotalTransactions());
        dto.put("lastTransactionDate", metrics.getLastTransactionDate());
        dto.put("lastUpdated", metrics.getLastUpdated());
        return dto;
    }

    private Map<String, Object> buildUtilizationDTO(AllocationUtilization utilization) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", utilization.getId());
        dto.put("utilizationId", utilization.getUtilizationId());
        dto.put("transactionType", utilization.getTransactionType());
        dto.put("amount", utilization.getAmount());
        dto.put("remainingBalance", utilization.getRemainingBalance());
        dto.put("productType", utilization.getProductType());
        dto.put("userName", utilization.getUserName());
        dto.put("userAccountNumber", utilization.getUserAccountNumber());
        dto.put("description", utilization.getDescription());
        dto.put("transactionDate", utilization.getTransactionDate());
        dto.put("status", utilization.getStatus());
        return dto;
    }
}
