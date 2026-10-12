package com.neo.springapp.dto;

import java.time.LocalDate;

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
        LocalDate effectiveFrom,
        String requestedBy) {
}
