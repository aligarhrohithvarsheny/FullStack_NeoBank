package com.neo.springapp.controller;

import com.neo.springapp.repository.AccountRepository;
import com.neo.springapp.repository.AdminCashTransactionRepository;
import com.neo.springapp.repository.CurrentAccountRepository;
import com.neo.springapp.repository.KycRepository;
import com.neo.springapp.repository.SalaryAccountRepository;
import com.neo.springapp.repository.TransactionRepository;
import com.neo.springapp.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/executive-dashboard")
public class AdminExecutiveDashboardController {

    private final AccountRepository accountRepository;
    private final AdminCashTransactionRepository cashTransactionRepository;
    private final CurrentAccountRepository currentAccountRepository;
    private final KycRepository kycRepository;
    private final SalaryAccountRepository salaryAccountRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public AdminExecutiveDashboardController(
            AccountRepository accountRepository,
            AdminCashTransactionRepository cashTransactionRepository,
            CurrentAccountRepository currentAccountRepository,
            KycRepository kycRepository,
            SalaryAccountRepository salaryAccountRepository,
            TransactionRepository transactionRepository,
            UserRepository userRepository) {
        this.accountRepository = accountRepository;
        this.cashTransactionRepository = cashTransactionRepository;
        this.currentAccountRepository = currentAccountRepository;
        this.kycRepository = kycRepository;
        this.salaryAccountRepository = salaryAccountRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> summary() {
        LocalDate today = LocalDate.now();
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Object> totals = new LinkedHashMap<>();

        totals.put("customers", userRepository.count());
        totals.put("activeAccounts", value(accountRepository.countByStatus("ACTIVE"))
                + value(currentAccountRepository.countByStatus("ACTIVE"))
                + value(salaryAccountRepository.countByStatus("Active")));
        totals.put("newRegistrationsToday",
                userRepository.countRegistrationsBetween(today.atStartOfDay(), today.plusDays(1).atStartOfDay()));
        totals.put("newRegistrationsThisMonth",
                userRepository.countRegistrationsBetween(today.withDayOfMonth(1).atStartOfDay(),
                        today.plusMonths(1).withDayOfMonth(1).atStartOfDay()));
        totals.put("newRegistrationsThisYear",
                userRepository.countRegistrationsBetween(today.withDayOfYear(1).atStartOfDay(),
                        today.plusYears(1).withDayOfYear(1).atStartOfDay()));

        Map<String, Double> flows = new LinkedHashMap<>();
        flows.put("deposits", 0.0);
        flows.put("withdrawals", 0.0);
        flows.put("transfers", 0.0);
        for (Object[] row : transactionRepository.summarizeBankFlows()) {
            addFlow(flows, String.valueOf(row[0]), number(row[1]));
        }
        for (Object[] row : cashTransactionRepository.summarizeCompletedOperations()) {
            addCashFlow(flows, String.valueOf(row[0]), number(row[1]));
        }
        totals.putAll(flows);

        long pendingUsers = value(userRepository.countByStatus("PENDING"));
        long pendingKyc = value(kycRepository.countByStatus("Pending"));
        totals.put("pendingApprovals", pendingUsers + pendingKyc);
        totals.put("pendingRegistrations", pendingUsers);
        totals.put("pendingKyc", pendingKyc);
        totals.put("failedTransactions", transactionRepository.countFailedTransactions());

        result.put("totals", totals);
        result.put("daily", daily(today));
        result.put("monthly", monthly(today));
        result.put("yearly", yearly(today));
        return ResponseEntity.ok(result);
    }

    private List<Map<String, Object>> daily(LocalDate today) {
        LocalDate start = today.minusDays(6);
        Map<LocalDate, Map<String, Double>> byDay = new LinkedHashMap<>();
        for (int i = 0; i < 7; i++) {
            byDay.put(start.plusDays(i), emptyFlows());
        }
        for (Object[] row : transactionRepository.summarizeDailyBankFlows(
                start.atStartOfDay(), today.plusDays(1).atStartOfDay())) {
            LocalDate date = LocalDate.parse(String.valueOf(row[0]));
            Map<String, Double> flows = byDay.get(date);
            if (flows != null) addFlow(flows, String.valueOf(row[1]), number(row[2]));
        }
        for (Object[] row : cashTransactionRepository.summarizeDailyOperations(
                start.atStartOfDay(), today.plusDays(1).atStartOfDay())) {
            LocalDate date = LocalDate.parse(String.valueOf(row[0]));
            Map<String, Double> flows = byDay.get(date);
            if (flows != null) addCashFlow(flows, String.valueOf(row[1]), number(row[2]));
        }
        return chartRows(byDay);
    }

    private List<Map<String, Object>> monthly(LocalDate today) {
        LocalDate start = today.withDayOfMonth(1).minusMonths(11);
        Map<String, Map<String, Double>> byMonth = new LinkedHashMap<>();
        for (int i = 0; i < 12; i++) {
            LocalDate date = start.plusMonths(i);
            byMonth.put(String.format(Locale.ROOT, "%04d-%02d", date.getYear(), date.getMonthValue()), emptyFlows());
        }
        for (Object[] row : transactionRepository.summarizeMonthlyBankFlows(
                start.atStartOfDay(), today.plusMonths(1).withDayOfMonth(1).atStartOfDay())) {
            String month = String.format(Locale.ROOT, "%04d-%02d", integer(row[0]), integer(row[1]));
            Map<String, Double> flows = byMonth.get(month);
            if (flows != null) addFlow(flows, String.valueOf(row[2]), number(row[3]));
        }
        for (Object[] row : cashTransactionRepository.summarizeMonthlyOperations(
                start.atStartOfDay(), today.plusMonths(1).withDayOfMonth(1).atStartOfDay())) {
            String month = String.format(Locale.ROOT, "%04d-%02d", integer(row[0]), integer(row[1]));
            Map<String, Double> flows = byMonth.get(month);
            if (flows != null) addCashFlow(flows, String.valueOf(row[2]), number(row[3]));
        }
        return chartRows(byMonth);
    }

    private List<Map<String, Object>> yearly(LocalDate today) {
        int firstYear = today.getYear() - 4;
        Map<String, Map<String, Double>> byYear = new LinkedHashMap<>();
        for (int year = firstYear; year <= today.getYear(); year++) {
            byYear.put(String.valueOf(year), emptyFlows());
        }
        LocalDateTime start = LocalDate.of(firstYear, 1, 1).atStartOfDay();
        LocalDateTime end = LocalDate.of(today.getYear() + 1, 1, 1).atStartOfDay();
        for (Object[] row : transactionRepository.summarizeYearlyBankFlows(start, end)) {
            Map<String, Double> flows = byYear.get(String.valueOf(integer(row[0])));
            if (flows != null) addFlow(flows, String.valueOf(row[1]), number(row[2]));
        }
        for (Object[] row : cashTransactionRepository.summarizeYearlyOperations(start, end)) {
            Map<String, Double> flows = byYear.get(String.valueOf(integer(row[0])));
            if (flows != null) addCashFlow(flows, String.valueOf(row[1]), number(row[2]));
        }
        return chartRows(byYear);
    }

    private List<Map<String, Object>> chartRows(Map<?, Map<String, Double>> groups) {
        List<Map<String, Object>> rows = new ArrayList<>();
        groups.forEach((label, flows) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("label", String.valueOf(label));
            row.putAll(flows);
            rows.add(row);
        });
        return rows;
    }

    private Map<String, Double> emptyFlows() {
        Map<String, Double> flows = new LinkedHashMap<>();
        flows.put("deposits", 0.0);
        flows.put("withdrawals", 0.0);
        flows.put("transfers", 0.0);
        return flows;
    }

    private void addFlow(Map<String, Double> flows, String type, double amount) {
        String normalizedType = type.toLowerCase(Locale.ROOT);
        if ("deposit".equals(normalizedType)) {
            flows.put("deposits", flows.getOrDefault("deposits", 0.0) + amount);
        } else if ("withdraw".equals(normalizedType) || "withdrawal".equals(normalizedType)) {
            flows.put("withdrawals", flows.getOrDefault("withdrawals", 0.0) + amount);
        } else if ("transfer".equals(normalizedType)) {
            flows.put("transfers", flows.getOrDefault("transfers", 0.0) + amount);
        }
    }

    private void addCashFlow(Map<String, Double> flows, String type, double amount) {
        if ("DEPOSIT".equalsIgnoreCase(type)) {
            flows.put("deposits", flows.getOrDefault("deposits", 0.0) + amount);
        } else if ("WITHDRAWAL".equalsIgnoreCase(type) || "WITHDRAW".equalsIgnoreCase(type)) {
            flows.put("withdrawals", flows.getOrDefault("withdrawals", 0.0) + amount);
        }
    }

    private double number(Object value) {
        return value instanceof Number number ? number.doubleValue() : 0.0;
    }

    private int integer(Object value) {
        return value instanceof Number number ? number.intValue() : Integer.parseInt(String.valueOf(value));
    }

    private long value(Long value) {
        return value == null ? 0 : value;
    }
}
