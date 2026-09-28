package com.neo.springapp.service;

import com.neo.springapp.model.*;
import com.neo.springapp.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for validating funds allocation business rules.
 * Ensures compliance with allocation policies, limits, and constraints.
 */
@Service
@SuppressWarnings("null")
public class AllocationValidatorService {

    @Autowired
    private FundsAllocationRepository allocationRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private AccountService accountService;

    /**
     * Validate if an allocation request can proceed.
     */
    public Map<String, Object> validateAllocationRequest(
            Double amount,
            Long branchManagerId,
            String productTypes,
            LocalDate validTill,
            Long hodAdminId) {

        Map<String, Object> result = new HashMap<>();
        List<String> validationErrors = new ArrayList<>();

        // Validate amount
        if (amount == null || amount <= 0) {
            validationErrors.add("Allocation amount must be greater than 0");
        }

        if (amount != null && amount > 50000000) { // 50 Cr max
            validationErrors.add("Allocation amount exceeds maximum limit of 50 Crores");
        }

        // Validate HOD admin
        Admin hodAdmin = adminRepository.findById(hodAdminId).orElse(null);
        if (hodAdmin == null) {
            validationErrors.add("HOD admin not found");
        } else if (!"HOD".equals(hodAdmin.getRole())) {
            validationErrors.add("User is not HOD admin");
        }

        // Validate manager
        Admin branchManager = adminRepository.findById(branchManagerId).orElse(null);
        if (branchManager == null) {
            validationErrors.add("Branch manager not found");
        } else if (!"MANAGER".equals(branchManager.getRole())) {
            validationErrors.add("User is not branch manager");
        }

        // Validate validity period
        if (validTill != null && validTill.isBefore(LocalDate.now())) {
            validationErrors.add("Valid till date must be in future");
        }

        if (validTill != null && validTill.isAfter(LocalDate.now().plusYears(1))) {
            validationErrors.add("Allocation validity cannot exceed 1 year");
        }

        // Validate product types
        if (productTypes == null || productTypes.trim().isEmpty()) {
            validationErrors.add("At least one product type must be selected");
        }

        // Check for duplicate active allocations
        if (branchManagerId != null) {
            List<FundsAllocation> activeAllocations = allocationRepository.findActiveAllocationsForManager(branchManagerId);
            if (!activeAllocations.isEmpty()) {
                // Allow multiple allocations but warn if too many
                if (activeAllocations.size() >= 5) {
                    validationErrors.add("Manager already has 5 active allocations. Consider consolidating");
                }
            }
        }

        result.put("isValid", validationErrors.isEmpty());
        if (!validationErrors.isEmpty()) {
            result.put("errors", validationErrors);
        }
        result.put("warningCount", validationErrors.size());

        return result;
    }

    /**
     * Validate debit transaction against allocation.
     */
    public Map<String, Object> validateDebitTransaction(
            Long allocationId,
            Double amount,
            String productType,
            String userAccountNumber) {

        Map<String, Object> result = new HashMap<>();
        List<String> validationErrors = new ArrayList<>();

        // Validate allocation exists
        FundsAllocation allocation = allocationRepository.findById(allocationId).orElse(null);
        if (allocation == null) {
            validationErrors.add("Allocation not found");
            result.put("isValid", false);
            result.put("errors", validationErrors);
            return result;
        }

        // Check allocation is active
        if (!"ACTIVE".equals(allocation.getStatus())) {
            validationErrors.add("Allocation is not active. Current status: " + allocation.getStatus());
        }

        // Check allocation is not expired
        if (allocation.getValidTill() != null && LocalDate.now().isAfter(allocation.getValidTill())) {
            validationErrors.add("Allocation has expired on " + allocation.getValidTill());
        }

        // Validate amount
        if (amount == null || amount <= 0) {
            validationErrors.add("Debit amount must be greater than 0");
        }

        // Check sufficient balance
        Double currentBalance = allocation.getCurrentBalance() != null ? allocation.getCurrentBalance() : 0.0;
        if (amount != null && currentBalance < amount) {
            validationErrors.add("Insufficient allocation balance. Available: " + currentBalance + ", Required: " + amount);
        }

        // Validate product type is in allocation
        if (productType != null && allocation.getProductTypes() != null &&
                !allocation.getProductTypes().contains(productType)) {
            validationErrors.add("Product type " + productType + " not allocated for this manager");
        }

        // Validate user account
        if (userAccountNumber == null || userAccountNumber.trim().isEmpty()) {
            validationErrors.add("User account number is required");
        }

        result.put("isValid", validationErrors.isEmpty());
        if (!validationErrors.isEmpty()) {
            result.put("errors", validationErrors);
        }
        result.put("allowedDebitAmount", currentBalance);
        result.put("requestedDebitAmount", amount);

        return result;
    }

    /**
     * Validate update request.
     */
    public Map<String, Object> validateUpdateRequest(Long allocationId, Double newAmount, LocalDate newValidTill) {
        Map<String, Object> result = new HashMap<>();
        List<String> validationErrors = new ArrayList<>();

        FundsAllocation allocation = allocationRepository.findById(allocationId).orElse(null);
        if (allocation == null) {
            validationErrors.add("Allocation not found");
            result.put("isValid", false);
            result.put("errors", validationErrors);
            return result;
        }

        // Check allocation is active
        if (!"ACTIVE".equals(allocation.getStatus())) {
            validationErrors.add("Can only update ACTIVE allocations. Current status: " + allocation.getStatus());
        }

        // Validate new amount
        if (newAmount != null) {
            if (newAmount <= 0) {
                validationErrors.add("New amount must be greater than 0");
            }
            if (newAmount > 50000000) {
                validationErrors.add("Amount exceeds maximum limit");
            }
            // Amount can only increase or stay same, not decrease below already debited
            Double alreadyDebited = allocation.getTotalDebited() != null ? allocation.getTotalDebited() : 0.0;
            if (newAmount < alreadyDebited) {
                validationErrors.add("New amount cannot be less than already debited amount: " + alreadyDebited);
            }
        }

        // Validate new validity
        if (newValidTill != null) {
            if (newValidTill.isBefore(LocalDate.now())) {
                validationErrors.add("New valid till date must be in future");
            }
            if (newValidTill.isBefore(allocation.getValidFrom())) {
                validationErrors.add("New valid till date cannot be before valid from date");
            }
            if (newValidTill.isAfter(LocalDate.now().plusYears(1))) {
                validationErrors.add("Validity cannot exceed 1 year from today");
            }
        }

        result.put("isValid", validationErrors.isEmpty());
        if (!validationErrors.isEmpty()) {
            result.put("errors", validationErrors);
        }

        return result;
    }

    /**
     * Validate cancellation request.
     */
    public Map<String, Object> validateCancellationRequest(Long allocationId) {
        Map<String, Object> result = new HashMap<>();
        List<String> validationErrors = new ArrayList<>();

        FundsAllocation allocation = allocationRepository.findById(allocationId).orElse(null);
        if (allocation == null) {
            validationErrors.add("Allocation not found");
            result.put("isValid", false);
            result.put("errors", validationErrors);
            return result;
        }

        // Cannot cancel already completed allocations
        if ("COMPLETED".equals(allocation.getStatus())) {
            validationErrors.add("Cannot cancel already completed allocations");
        }

        // Cannot cancel if already cancelled
        if ("CANCELLED".equals(allocation.getStatus())) {
            validationErrors.add("Allocation is already cancelled");
        }

        result.put("isValid", validationErrors.isEmpty());
        if (!validationErrors.isEmpty()) {
            result.put("errors", validationErrors);
        }

        return result;
    }

    /**
     * Get manager's allocation summary to check utilization limits.
     */
    public Map<String, Object> getManagerAllocationLimits(Long managerId) {
        Map<String, Object> result = new HashMap<>();

        Double totalAllocated = allocationRepository.getTotalActiveAllocationForManager(managerId);
        totalAllocated = totalAllocated != null ? totalAllocated : 0.0;

        Double maxLimit = 100000000.0; // 10 Crores max
        Double remainingLimit = maxLimit - totalAllocated;
        Double utilizationPercentage = (totalAllocated / maxLimit) * 100;

        result.put("managerId", managerId);
        result.put("totalActiveAllocations", totalAllocated);
        result.put("maximumLimit", maxLimit);
        result.put("remainingLimit", remainingLimit);
        result.put("utilizationPercentage", String.format("%.2f%%", utilizationPercentage));
        result.put("canAllocateMore", remainingLimit > 0);

        return result;
    }

    /**
     * Check if product type is allowed for allocation.
     */
    public Map<String, Object> validateProductTypes(String productTypes) {
        Map<String, Object> result = new HashMap<>();
        List<String> validatedTypes = new ArrayList<>();
        List<String> invalidTypes = new ArrayList<>();

        String[] allowedTypes = {
            "GOLD_LOAN", "DEPOSITS", "WITHDRAWALS", "LOANS", "OVERDRAFT", "SALARY_CREDIT"
        };

        if (productTypes == null || productTypes.trim().isEmpty()) {
            result.put("isValid", false);
            result.put("message", "No product types provided");
            return result;
        }

        String[] providedTypes = productTypes.split(",");
        for (String type : providedTypes) {
            type = type.trim();
            boolean found = false;
            for (String allowed : allowedTypes) {
                if (allowed.equals(type)) {
                    validatedTypes.add(type);
                    found = true;
                    break;
                }
            }
            if (!found) {
                invalidTypes.add(type);
            }
        }

        result.put("isValid", invalidTypes.isEmpty() && !validatedTypes.isEmpty());
        result.put("validTypes", validatedTypes);
        if (!invalidTypes.isEmpty()) {
            result.put("invalidTypes", invalidTypes);
        }
        result.put("allowedTypes", Arrays.asList(allowedTypes));

        return result;
    }

    /**
     * Validate branch manager can access allocation.
     */
    public Map<String, Object> validateAccessControl(Long allocationId, Long requestingUserId, String userRole) {
        Map<String, Object> result = new HashMap<>();

        FundsAllocation allocation = allocationRepository.findById(allocationId).orElse(null);
        if (allocation == null) {
            result.put("hasAccess", false);
            result.put("reason", "Allocation not found");
            return result;
        }

        // HOD can access all allocations
        if ("HOD".equals(userRole)) {
            result.put("hasAccess", true);
            result.put("accessLevel", "full");
            return result;
        }

        // Manager can only access their own allocations
        if ("MANAGER".equals(userRole)) {
            if (allocation.getManagerId().equals(requestingUserId)) {
                result.put("hasAccess", true);
                result.put("accessLevel", "view_only");
                return result;
            }
        }

        result.put("hasAccess", false);
        result.put("reason", "User does not have access to this allocation");
        return result;
    }

    /**
     * Pre-debit validation with detailed report.
     */
    public Map<String, Object> preDebitValidationReport(Long allocationId, Double amount) {
        Map<String, Object> report = new HashMap<>();

        FundsAllocation allocation = allocationRepository.findById(allocationId).orElse(null);
        if (allocation == null) {
            report.put("canProceed", false);
            report.put("reason", "Allocation not found");
            return report;
        }

        Double currentBalance = allocation.getCurrentBalance() != null ? allocation.getCurrentBalance() : 0.0;
        Double alreadyDebited = allocation.getTotalDebited() != null ? allocation.getTotalDebited() : 0.0;
        Double allocated = allocation.getAllocatedAmount() != null ? allocation.getAllocatedAmount() : 0.0;

        boolean isActive = "ACTIVE".equals(allocation.getStatus());
        boolean isNotExpired = allocation.getValidTill() == null || !LocalDate.now().isAfter(allocation.getValidTill());
        boolean hasSufficientBalance = currentBalance >= amount;

        report.put("canProceed", isActive && isNotExpired && hasSufficientBalance);
        report.put("allocationStatus", allocation.getStatus());
        report.put("isActive", isActive);
        report.put("isNotExpired", isNotExpired);
        report.put("daysRemaining", allocation.getValidTill() != null ?
            java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), allocation.getValidTill()) : -1);
        report.put("totalAllocated", allocated);
        report.put("alreadyDebited", alreadyDebited);
        report.put("currentBalance", currentBalance);
        report.put("requestedDebitAmount", amount);
        report.put("hasSufficientBalance", hasSufficientBalance);
        report.put("utilizationPercentage", allocated > 0 ? String.format("%.2f%%", (alreadyDebited / allocated) * 100) : "0%");

        if (!report.get("canProceed").equals(true)) {
            List<String> reasons = new ArrayList<>();
            if (!isActive) reasons.add("Allocation is " + allocation.getStatus());
            if (!isNotExpired) reasons.add("Allocation has expired");
            if (!hasSufficientBalance) reasons.add("Insufficient balance: " + currentBalance + " < " + amount);
            report.put("blockers", reasons);
        }

        return report;
    }
}
