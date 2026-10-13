package com.neo.springapp.dto;

import java.time.LocalDate;
import java.util.List;

public record BankingProductConfigRequest(
        String productCode,
        String productName,
        String accountType,
        Double interestRate,
        Double minimumBalance,
        Double dailyTransactionLimit,
        Double monthlyTransactionLimit,
        Double serviceCharge,
        String eligibility,
        List<String> enabledFeatures,
        LocalDate effectiveFrom,
        String requestedBy) {
}
