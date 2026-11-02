package com.neo.springapp.service;

import com.neo.springapp.model.Account;
import com.neo.springapp.model.RecurringDeposit;
import com.neo.springapp.model.TermDepositPlan;
import com.neo.springapp.repository.RecurringDepositRepository;
import com.neo.springapp.repository.TermDepositPlanRepository;
import com.neo.springapp.model.DepositManagementHistory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class RecurringDepositService {

    private final RecurringDepositRepository repository;
    private final TermDepositPlanRepository planRepository;
    private final AccountService accountService;
    private final DepositManagementHistoryService historyService;

    public RecurringDepositService(RecurringDepositRepository repository,
                                   TermDepositPlanRepository planRepository,
                                   AccountService accountService,
                                   DepositManagementHistoryService historyService) {
        this.repository = repository;
        this.planRepository = planRepository;
        this.accountService = accountService;
        this.historyService = historyService;
    }

    @Transactional(readOnly = true)
    public List<RecurringDeposit> getForAccount(String accountNumber) {
        return repository.findByAccountNumberOrderByCreatedAtDesc(accountNumber);
    }

    @Transactional(readOnly = true)
    public List<RecurringDeposit> getAll() {
        return repository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public RecurringDeposit getById(Long id) {
        return repository.findById(id).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<DepositManagementHistory> getHistory(Long id) {
        return historyService.getHistory("RD", id);
    }

    @Transactional
    public RecurringDeposit create(String accountNumber, Long planId, Double monthlyInstallment,
                                   LocalDate startDate, String remarks, String actor) {
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalArgumentException("Linked account number is required.");
        }
        Account account = accountService.getAccountByNumber(accountNumber);
        if (account == null) {
            throw new IllegalArgumentException("Linked customer account was not found.");
        }
        TermDepositPlan plan = planRepository.findById(planId)
                .filter(p -> Boolean.TRUE.equals(p.getActive()))
                .filter(p -> "RD".equalsIgnoreCase(p.getProductType()))
                .orElseThrow(() -> new IllegalArgumentException("Select an active RD scheme."));
        validateInstallment(monthlyInstallment, plan);
        if (plan.getDurationMonths() == null || plan.getDurationMonths() <= 0) {
            throw new IllegalArgumentException("The RD scheme must have a valid duration in months.");
        }

        RecurringDeposit rd = new RecurringDeposit();
        rd.setAccountNumber(accountNumber.trim());
        rd.setCustomerName(account.getName());
        rd.setPlanId(plan.getId());
        rd.setSchemeName(plan.getSchemeName());
        rd.setMonthlyInstallment(monthlyInstallment);
        rd.setAnnualInterestRate(plan.getInterestRate());
        rd.setTenureMonths(plan.getDurationMonths());
        rd.setStartDate(startDate == null ? LocalDate.now() : startDate);
        rd.setMaturityDate(rd.getStartDate().plusMonths(rd.getTenureMonths()));
        rd.setRemarks(remarks);
        rd.setCreatedBy(actor);
        rd.setUpdatedBy(actor);
        calculateEstimate(rd);
        RecurringDeposit saved = repository.save(rd);
        historyService.record("RD", saved.getId(), saved.getAccountNumber(), "DETAILS_CREATED",
                actor, "Admin-created record; no funds moved.", details(saved));
        return saved;
    }

    @Transactional
    public RecurringDeposit update(Long id, Double monthlyInstallment, Integer tenureMonths,
                                   Double annualInterestRate, LocalDate startDate,
                                   LocalDate maturityDate, String remarks, String reason, String actor) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("An audit reason is required.");
        }
        RecurringDeposit rd = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("RD record not found."));
        String before = details(rd);
        if (monthlyInstallment != null) rd.setMonthlyInstallment(monthlyInstallment);
        if (tenureMonths != null) rd.setTenureMonths(tenureMonths);
        if (annualInterestRate != null) rd.setAnnualInterestRate(annualInterestRate);
        if (startDate != null) rd.setStartDate(startDate);
        if (maturityDate != null) rd.setMaturityDate(maturityDate);
        if (remarks != null) rd.setRemarks(remarks);
        if (rd.getMonthlyInstallment() == null || rd.getMonthlyInstallment() <= 0
                || rd.getTenureMonths() == null || rd.getTenureMonths() <= 0
                || rd.getAnnualInterestRate() == null || rd.getAnnualInterestRate() < 0
                || rd.getAnnualInterestRate() > 100) {
            throw new IllegalArgumentException("RD amount, duration, or interest rate is invalid.");
        }
        if (rd.getMaturityDate() == null || rd.getMaturityDate().isBefore(rd.getStartDate())) {
            throw new IllegalArgumentException("Maturity date must be on or after the start date.");
        }
        rd.setUpdatedBy(actor);
        calculateEstimate(rd);
        RecurringDeposit saved = repository.save(rd);
        historyService.record("RD", saved.getId(), saved.getAccountNumber(), "DETAILS_UPDATED",
                actor, reason, before + " -> " + details(saved));
        return saved;
    }

    private void validateInstallment(Double amount, TermDepositPlan plan) {
        if (amount == null || amount <= 0
                || (plan.getMinimumAmount() != null && amount < plan.getMinimumAmount())
                || (plan.getMaximumAmount() != null && amount > plan.getMaximumAmount())) {
            throw new IllegalArgumentException("Monthly installment is outside the selected scheme limits.");
        }
    }

    private void calculateEstimate(RecurringDeposit rd) {
        double monthlyRate = rd.getAnnualInterestRate() / 1200.0;
        double months = rd.getTenureMonths();
        double principal = rd.getMonthlyInstallment() * months;
        double maturity = monthlyRate == 0
                ? principal
                : rd.getMonthlyInstallment() * (Math.pow(1 + monthlyRate, months) - 1) / monthlyRate;
        maturity = Math.round(maturity * 100.0) / 100.0;
        rd.setEstimatedPrincipal(Math.round(principal * 100.0) / 100.0);
        rd.setEstimatedMaturityAmount(maturity);
        rd.setEstimatedInterest(Math.round((maturity - principal) * 100.0) / 100.0);
    }

    private String details(RecurringDeposit rd) {
        return "scheme=" + rd.getSchemeName() + ", monthlyInstallment=" + rd.getMonthlyInstallment()
                + ", tenureMonths=" + rd.getTenureMonths() + ", annualRate=" + rd.getAnnualInterestRate()
                + ", startDate=" + rd.getStartDate() + ", maturityDate=" + rd.getMaturityDate()
                + ", estimatedMaturity=" + rd.getEstimatedMaturityAmount();
    }
}
