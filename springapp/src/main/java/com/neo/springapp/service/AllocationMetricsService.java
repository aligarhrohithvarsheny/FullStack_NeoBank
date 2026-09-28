package com.neo.springapp.service;

import com.neo.springapp.model.AllocationRealTimeMetrics;
import com.neo.springapp.model.FundsAllocation;
import com.neo.springapp.repository.AllocationMetricsRepository;
import com.neo.springapp.repository.AllocationUtilizationRepository;
import com.neo.springapp.repository.FundsAllocationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for managing and retrieving real-time metrics for fund allocations.
 * Provides dashboard KPIs, trend analysis, and performance reports.
 */
@Service
@SuppressWarnings("null")
public class AllocationMetricsService {

    @Autowired
    private AllocationMetricsRepository metricsRepository;

    @Autowired
    private FundsAllocationRepository allocationRepository;

    @Autowired
    private AllocationUtilizationRepository utilizationRepository;

    /**
     * Get real-time metrics for a specific allocation.
     */
    public Map<String, Object> getMetricsSummary(Long allocationId) {
        Map<String, Object> result = new HashMap<>();

        FundsAllocation allocation = allocationRepository.findById(allocationId).orElse(null);
        if (allocation == null) {
            result.put("success", false);
            result.put("message", "Allocation not found");
            return result;
        }

        Optional<AllocationRealTimeMetrics> metricsOpt = metricsRepository.findByAllocationId(allocationId);
        if (metricsOpt.isEmpty()) {
            result.put("success", false);
            result.put("message", "Metrics not found for allocation");
            return result;
        }

        AllocationRealTimeMetrics metrics = metricsOpt.get();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("allocationId", allocation.getAllocationId());
        summary.put("managerName", allocation.getManagerName());
        summary.put("branchName", allocation.getBranchName());
        summary.put("productTypes", allocation.getProductTypes());
        summary.put("allocationStatus", allocation.getStatus());
        summary.put("validFrom", allocation.getValidFrom());
        summary.put("validTill", allocation.getValidTill());
        summary.put("daysRemaining", calculateDaysRemaining(allocation.getValidTill()));

        // Financial metrics
        summary.put("totalAllocated", metrics.getTotalAllocated());
        summary.put("totalDebited", metrics.getTotalDebited());
        summary.put("totalCredited", metrics.getTotalCredited());
        summary.put("currentBalance", metrics.getCurrentBalance());
        summary.put("utilizationPercentage", String.format("%.2f%%", metrics.getUtilizationPercentage() != null ? metrics.getUtilizationPercentage() : 0.0));

        // Transaction counts
        summary.put("debitCount", metrics.getDebitCount());
        summary.put("creditCount", metrics.getCreditCount());
        summary.put("totalTransactions", metrics.getTotalTransactions());

        // Product-wise breakdown
        Map<String, Object> productBreakdown = new LinkedHashMap<>();
        productBreakdown.put("goldLoan", Map.of(
            "debited", metrics.getGoldLoanDebited() != null ? metrics.getGoldLoanDebited() : 0.0,
            "percentage", calculatePercentage(metrics.getGoldLoanDebited(), metrics.getTotalDebited())
        ));
        productBreakdown.put("deposits", Map.of(
            "debited", metrics.getDepositsDebited() != null ? metrics.getDepositsDebited() : 0.0,
            "percentage", calculatePercentage(metrics.getDepositsDebited(), metrics.getTotalDebited())
        ));
        productBreakdown.put("withdrawals", Map.of(
            "debited", metrics.getWithdrawalsDebited() != null ? metrics.getWithdrawalsDebited() : 0.0,
            "percentage", calculatePercentage(metrics.getWithdrawalsDebited(), metrics.getTotalDebited())
        ));
        productBreakdown.put("loans", Map.of(
            "debited", metrics.getLoansDebited() != null ? metrics.getLoansDebited() : 0.0,
            "percentage", calculatePercentage(metrics.getLoansDebited(), metrics.getTotalDebited())
        ));
        productBreakdown.put("overdraft", Map.of(
            "debited", metrics.getOverdraftDebited() != null ? metrics.getOverdraftDebited() : 0.0,
            "percentage", calculatePercentage(metrics.getOverdraftDebited(), metrics.getTotalDebited())
        ));
        productBreakdown.put("salaryCredits", Map.of(
            "debited", metrics.getSalaryCreditDebited() != null ? metrics.getSalaryCreditDebited() : 0.0,
            "percentage", calculatePercentage(metrics.getSalaryCreditDebited(), metrics.getTotalDebited())
        ));
        summary.put("productBreakdown", productBreakdown);

        // Last updated
        summary.put("lastTransactionDate", metrics.getLastTransactionDate());
        summary.put("lastUpdated", metrics.getLastUpdated());

        result.put("success", true);
        result.put("metrics", summary);
        return result;
    }

    /**
     * Get metrics for all allocations (HOD dashboard view).
     */
    public Map<String, Object> getAllMetricsReport(LocalDate fromDate, LocalDate toDate, Long branchManagerId) {
        Map<String, Object> result = new HashMap<>();

        List<FundsAllocation> allocations;
        if (branchManagerId != null) {
            allocations = allocationRepository.findActiveAllocationsForManager(branchManagerId);
        } else {
            allocations = allocationRepository.findAll();
        }

        List<Map<String, Object>> metricsReport = new ArrayList<>();
        Double totalAllocated = 0.0;
        Double totalDebited = 0.0;
        Double totalCredited = 0.0;
        Double totalAvailable = 0.0;

        for (FundsAllocation allocation : allocations) {
            Optional<AllocationRealTimeMetrics> metricsOpt = metricsRepository.findByAllocationId(allocation.getId());
            if (metricsOpt.isPresent()) {
                AllocationRealTimeMetrics metrics = metricsOpt.get();

                Map<String, Object> allocationReport = new LinkedHashMap<>();
                allocationReport.put("allocationId", allocation.getAllocationId());
                allocationReport.put("managerName", allocation.getManagerName());
                allocationReport.put("branchName", allocation.getBranchName());
                allocationReport.put("status", allocation.getStatus());
                allocationReport.put("totalAllocated", metrics.getTotalAllocated());
                allocationReport.put("totalDebited", metrics.getTotalDebited());
                allocationReport.put("totalCredited", metrics.getTotalCredited());
                allocationReport.put("currentBalance", metrics.getCurrentBalance());
                allocationReport.put("utilizationPercentage", String.format("%.2f%%", metrics.getUtilizationPercentage() != null ? metrics.getUtilizationPercentage() : 0.0));
                allocationReport.put("debitCount", metrics.getDebitCount());
                allocationReport.put("creditCount", metrics.getCreditCount());
                allocationReport.put("validTill", allocation.getValidTill());

                metricsReport.add(allocationReport);

                totalAllocated += metrics.getTotalAllocated() != null ? metrics.getTotalAllocated() : 0.0;
                totalDebited += metrics.getTotalDebited() != null ? metrics.getTotalDebited() : 0.0;
                totalCredited += metrics.getTotalCredited() != null ? metrics.getTotalCredited() : 0.0;
                totalAvailable += metrics.getCurrentBalance() != null ? metrics.getCurrentBalance() : 0.0;
            }
        }

        // Overall summary
        Map<String, Object> overallSummary = new LinkedHashMap<>();
        overallSummary.put("totalAllocations", allocations.size());
        overallSummary.put("totalAllocatedAmount", totalAllocated);
        overallSummary.put("totalDebitedAmount", totalDebited);
        overallSummary.put("totalCreditedAmount", totalCredited);
        overallSummary.put("totalAvailableBalance", totalAvailable);
        overallSummary.put("overallUtilizationPercentage", totalAllocated > 0 ? String.format("%.2f%%", (totalDebited / totalAllocated) * 100) : "0%");

        result.put("success", true);
        result.put("overallSummary", overallSummary);
        result.put("allocationMetrics", metricsReport);
        result.put("reportDate", LocalDate.now());
        return result;
    }

    /**
     * Get metrics for a specific manager's allocations.
     */
    public Map<String, Object> getManagerMetrics(Long managerId) {
        Map<String, Object> result = new HashMap<>();

        List<FundsAllocation> allocations = allocationRepository.findActiveAllocationsForManager(managerId);

        List<Map<String, Object>> managerAllocations = new ArrayList<>();
        Double totalAllocated = 0.0;
        Double totalUsed = 0.0;
        Double totalAvailable = 0.0;

        for (FundsAllocation allocation : allocations) {
            Optional<AllocationRealTimeMetrics> metricsOpt = metricsRepository.findByAllocationId(allocation.getId());
            if (metricsOpt.isPresent()) {
                AllocationRealTimeMetrics metrics = metricsOpt.get();

                Map<String, Object> alloc = new LinkedHashMap<>();
                alloc.put("allocationId", allocation.getAllocationId());
                alloc.put("allocatedAmount", metrics.getTotalAllocated());
                alloc.put("usedAmount", metrics.getTotalDebited());
                alloc.put("availableBalance", metrics.getCurrentBalance());
                alloc.put("utilizationPercentage", String.format("%.2f%%", metrics.getUtilizationPercentage() != null ? metrics.getUtilizationPercentage() : 0.0));
                alloc.put("validTill", allocation.getValidTill());
                alloc.put("daysRemaining", calculateDaysRemaining(allocation.getValidTill()));
                alloc.put("productTypes", allocation.getProductTypes());

                managerAllocations.add(alloc);

                totalAllocated += metrics.getTotalAllocated() != null ? metrics.getTotalAllocated() : 0.0;
                totalUsed += metrics.getTotalDebited() != null ? metrics.getTotalDebited() : 0.0;
                totalAvailable += metrics.getCurrentBalance() != null ? metrics.getCurrentBalance() : 0.0;
            }
        }

        result.put("success", true);
        result.put("totalAllocated", totalAllocated);
        result.put("totalUsed", totalUsed);
        result.put("totalAvailable", totalAvailable);
        result.put("utilizationPercentage", totalAllocated > 0 ? String.format("%.2f%%", (totalUsed / totalAllocated) * 100) : "0%");
        result.put("allocationCount", allocations.size());
        result.put("allocations", managerAllocations);
        return result;
    }

    /**
     * Get trending data for charts - daily utilization over time.
     */
    public Map<String, Object> getUtilizationTrend(Long allocationId, int daysBack) {
        Map<String, Object> result = new HashMap<>();

        LocalDate fromDate = LocalDate.now().minusDays(daysBack);
        LocalDate toDate = LocalDate.now();

        List<AllocationRealTimeMetrics> metrics = metricsRepository.findByMetricsDateBetween(fromDate, toDate);

        List<Map<String, Object>> trendData = new ArrayList<>();
        for (AllocationRealTimeMetrics m : metrics) {
            if (m.getAllocationId().equals(allocationId)) {
                Map<String, Object> point = new LinkedHashMap<>();
                point.put("date", m.getMetricsDate());
                point.put("totalAllocated", m.getTotalAllocated());
                point.put("totalDebited", m.getTotalDebited());
                point.put("currentBalance", m.getCurrentBalance());
                point.put("utilizationPercentage", m.getUtilizationPercentage());
                trendData.add(point);
            }
        }

        result.put("success", true);
        result.put("trendData", trendData);
        result.put("fromDate", fromDate);
        result.put("toDate", toDate);
        return result;
    }

    /**
     * Get top-performing allocations by utilization.
     */
    public Map<String, Object> getTopAllocations(int limit) {
        Map<String, Object> result = new HashMap<>();

        List<AllocationRealTimeMetrics> allMetrics = metricsRepository.findAll();
        List<AllocationRealTimeMetrics> sorted = allMetrics.stream()
            .filter(m -> m.getUtilizationPercentage() != null)
            .sorted((a, b) -> Double.compare(b.getUtilizationPercentage(), a.getUtilizationPercentage()))
            .limit(limit)
            .toList();

        List<Map<String, Object>> topAllocations = new ArrayList<>();
        for (AllocationRealTimeMetrics metrics : sorted) {
            FundsAllocation allocation = allocationRepository.findById(metrics.getAllocationId()).orElse(null);
            if (allocation != null) {
                Map<String, Object> top = new LinkedHashMap<>();
                top.put("allocationId", allocation.getAllocationId());
                top.put("managerName", allocation.getManagerName());
                top.put("totalAllocated", metrics.getTotalAllocated());
                top.put("totalDebited", metrics.getTotalDebited());
                top.put("utilizationPercentage", String.format("%.2f%%", metrics.getUtilizationPercentage()));
                topAllocations.add(top);
            }
        }

        result.put("success", true);
        result.put("topAllocations", topAllocations);
        return result;
    }

    /**
     * Get low-performing allocations by utilization.
     */
    public Map<String, Object> getLowAllocations(int limit) {
        Map<String, Object> result = new HashMap<>();

        List<AllocationRealTimeMetrics> allMetrics = metricsRepository.findAll();
        List<AllocationRealTimeMetrics> sorted = allMetrics.stream()
            .filter(m -> m.getUtilizationPercentage() != null)
            .sorted((a, b) -> Double.compare(a.getUtilizationPercentage(), b.getUtilizationPercentage()))
            .limit(limit)
            .toList();

        List<Map<String, Object>> lowAllocations = new ArrayList<>();
        for (AllocationRealTimeMetrics metrics : sorted) {
            FundsAllocation allocation = allocationRepository.findById(metrics.getAllocationId()).orElse(null);
            if (allocation != null) {
                Map<String, Object> low = new LinkedHashMap<>();
                low.put("allocationId", allocation.getAllocationId());
                low.put("managerName", allocation.getManagerName());
                low.put("totalAllocated", metrics.getTotalAllocated());
                low.put("totalDebited", metrics.getTotalDebited());
                low.put("utilizationPercentage", String.format("%.2f%%", metrics.getUtilizationPercentage()));
                lowAllocations.add(low);
            }
        }

        result.put("success", true);
        result.put("lowAllocations", lowAllocations);
        return result;
    }

    /**
     * Get expiring allocations - those valid for less than N days.
     */
    public Map<String, Object> getExpiringAllocations(int withinDays) {
        Map<String, Object> result = new HashMap<>();

        LocalDate checkDate = LocalDate.now().plusDays(withinDays);
        List<FundsAllocation> expiring = allocationRepository.findByValidTillLessThanAndStatusEquals(checkDate, "ACTIVE");

        List<Map<String, Object>> expiringList = new ArrayList<>();
        for (FundsAllocation allocation : expiring) {
            if (allocation.getValidTill() != null && allocation.getValidTill().isAfter(LocalDate.now())) {
                Optional<AllocationRealTimeMetrics> metricsOpt = metricsRepository.findByAllocationId(allocation.getId());
                Map<String, Object> exp = new LinkedHashMap<>();
                exp.put("allocationId", allocation.getAllocationId());
                exp.put("managerName", allocation.getManagerName());
                exp.put("validTill", allocation.getValidTill());
                exp.put("daysRemaining", calculateDaysRemaining(allocation.getValidTill()));
                exp.put("currentBalance", metricsOpt.map(m -> m.getCurrentBalance()).orElse(allocation.getCurrentBalance()));
                expiringList.add(exp);
            }
        }

        result.put("success", true);
        result.put("expiringAllocations", expiringList);
        result.put("count", expiringList.size());
        return result;
    }

    // Helper methods

    private long calculateDaysRemaining(LocalDate validTill) {
        if (validTill == null) return -1;
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), validTill);
    }

    private String calculatePercentage(Double part, Double total) {
        if (total == null || total == 0) return "0%";
        return String.format("%.2f%%", (part != null ? part : 0.0) / total * 100);
    }

    @Transactional
    public void recalculateMetrics(Long allocationId) {
        Optional<AllocationRealTimeMetrics> metricsOpt = metricsRepository.findByAllocationId(allocationId);
        if (metricsOpt.isEmpty()) return;

        AllocationRealTimeMetrics metrics = metricsOpt.get();
        Double totalDebited = utilizationRepository.getTotalDebited(allocationId);
        Double totalCredited = utilizationRepository.getTotalCredited(allocationId);
        Long debitCount = utilizationRepository.getDebitCount(allocationId);
        Long creditCount = utilizationRepository.getCreditCount(allocationId);

        metrics.setTotalDebited(totalDebited != null ? totalDebited : 0.0);
        metrics.setTotalCredited(totalCredited != null ? totalCredited : 0.0);
        metrics.setDebitCount(debitCount != null ? debitCount : 0L);
        metrics.setCreditCount(creditCount != null ? creditCount : 0L);
        metrics.setTotalTransactions((debitCount != null ? debitCount : 0L) + (creditCount != null ? creditCount : 0L));
        metrics.setLastUpdated(LocalDateTime.now());

        metricsRepository.save(metrics);
    }
}
