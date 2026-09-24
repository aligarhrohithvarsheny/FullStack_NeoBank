package com.neo.springapp.service;

import com.neo.springapp.model.BranchCity;
import com.neo.springapp.model.TransferRecord;
import com.neo.springapp.repository.CibilReportRepository;
import com.neo.springapp.repository.CurrentAccountRepository;
import com.neo.springapp.repository.FasttagRepository;
import com.neo.springapp.repository.GoldLoanRepository;
import com.neo.springapp.repository.InsurancePaymentRepository;
import com.neo.springapp.repository.LoanRepository;
import com.neo.springapp.repository.TransferRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BranchCityMetricsService {

    private static final double CIBIL_CHARGE = 118.0;
    private static final double NEFT_CHARGE = 5.0;

    private final CurrentAccountRepository currentAccountRepository;
    private final LoanRepository loanRepository;
    private final GoldLoanRepository goldLoanRepository;
    private final InsurancePaymentRepository insurancePaymentRepository;
    private final FasttagRepository fasttagRepository;
    private final CibilReportRepository cibilReportRepository;
    private final TransferRepository transferRepository;

    public BranchCityMetricsService(CurrentAccountRepository currentAccountRepository,
                                    LoanRepository loanRepository,
                                    GoldLoanRepository goldLoanRepository,
                                    InsurancePaymentRepository insurancePaymentRepository,
                                    FasttagRepository fasttagRepository,
                                    CibilReportRepository cibilReportRepository,
                                    TransferRepository transferRepository) {
        this.currentAccountRepository = currentAccountRepository;
        this.loanRepository = loanRepository;
        this.goldLoanRepository = goldLoanRepository;
        this.insurancePaymentRepository = insurancePaymentRepository;
        this.fasttagRepository = fasttagRepository;
        this.cibilReportRepository = cibilReportRepository;
        this.transferRepository = transferRepository;
    }

    public Map<String, Map<String, Object>> metricsByCity(List<BranchCity> cities) {
        Map<String, Map<String, Object>> result = new HashMap<>();
        for (BranchCity city : cities) {
            result.put(city.getCity().toLowerCase(), emptyMetrics());
        }

        Map<String, String> accountCities = new HashMap<>();
        currentAccountRepository.findAll().forEach(account -> {
            if (account.getAccountNumber() != null && account.getCity() != null) {
                accountCities.put(account.getAccountNumber(), account.getCity().trim().toLowerCase());
            }
        });

        loanRepository.findAll().forEach(loan -> {
            Map<String, Object> metrics = forAccount(result, accountCities, loan.getAccountNumber());
            if (metrics == null) return;
            if ("Approved".equalsIgnoreCase(loan.getStatus())) {
                increment(metrics, "loansProvided");
                add(metrics, "loanAmount", loan.getAmount());
                add(metrics, "loss", loan.getAmount());
            }
        });

        goldLoanRepository.findAll().forEach(loan -> {
            Map<String, Object> metrics = forAccount(result, accountCities, loan.getAccountNumber());
            if (metrics == null) return;
            increment(metrics, "goldLoans");
            add(metrics, "goldLoanAmount", loan.getLoanAmount());
            add(metrics, "profit", loan.getProcessingCharges());
        });

        insurancePaymentRepository.findAll().forEach(payment -> {
            Map<String, Object> metrics = forAccount(result, accountCities, payment.getAccountNumber());
            if (metrics == null || !"SUCCESS".equalsIgnoreCase(payment.getStatus())) return;
            increment(metrics, "insurancePayments");
            add(metrics, "insuranceCollected", payment.getAmount());
            add(metrics, "profit", payment.getAmount());
        });

        fasttagRepository.findAll().forEach(fasttag -> {
            String city = fasttag.getCity() == null ? null : fasttag.getCity().trim().toLowerCase();
            Map<String, Object> metrics = result.get(city);
            if (metrics == null) return;
            increment(metrics, "fasttags");
            double collected = value(fasttag.getTagTotalAmount());
            if (collected == 0) collected = value(fasttag.getTagIssuanceFee()) + value(fasttag.getTagUploadAmount());
            add(metrics, "fastagCollected", collected);
            add(metrics, "profit", collected);
        });

        cibilReportRepository.findAll().forEach(report -> {
            String accountNumber = report.getCurrentAccountNumber() != null
                    ? report.getCurrentAccountNumber() : report.getSavingsAccountNumber();
            Map<String, Object> metrics = forAccount(result, accountCities, accountNumber);
            if (metrics == null) return;
            increment(metrics, "cibilReports");
            add(metrics, "cibilCharges", CIBIL_CHARGE);
            add(metrics, "profit", CIBIL_CHARGE);
        });

        transferRepository.findAll().forEach(transfer -> {
            if (transfer.getTransferType() != TransferRecord.TransferType.NEFT
                    || !"Completed".equalsIgnoreCase(transfer.getStatus())) return;
            Map<String, Object> metrics = forAccount(result, accountCities, transfer.getSenderAccountNumber());
            if (metrics == null) return;
            increment(metrics, "neftTransfers");
            add(metrics, "neftCharges", NEFT_CHARGE);
            add(metrics, "profit", NEFT_CHARGE);
        });

        result.values().forEach(metrics -> {
            double profit = value(metrics.get("profit"));
            double loss = value(metrics.get("loss"));
            metrics.put("profitLoss", profit - loss);
        });
        return result;
    }

    private Map<String, Object> forAccount(Map<String, Map<String, Object>> result,
                                           Map<String, String> accountCities, String accountNumber) {
        if (accountNumber == null) return null;
        String city = accountCities.get(accountNumber);
        return city == null ? null : result.get(city);
    }

    private Map<String, Object> emptyMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        for (String key : List.of("loansProvided", "goldLoans", "insurancePayments", "fasttags", "cibilReports", "neftTransfers")) {
            metrics.put(key, 0);
        }
        for (String key : List.of("loanAmount", "goldLoanAmount", "insuranceCollected", "fastagCollected", "cibilCharges", "neftCharges", "profit", "loss", "profitLoss")) {
            metrics.put(key, 0D);
        }
        return metrics;
    }

    private void increment(Map<String, Object> metrics, String key) {
        metrics.put(key, ((Number) metrics.get(key)).intValue() + 1);
    }

    private void add(Map<String, Object> metrics, String key, Double amount) {
        add(metrics, key, value(amount));
    }

    private void add(Map<String, Object> metrics, String key, double amount) {
        metrics.put(key, value(metrics.get(key)) + amount);
    }

    private double value(Object amount) {
        return amount instanceof Number ? ((Number) amount).doubleValue() : 0D;
    }
}