package com.neo.springapp.service;

import com.neo.springapp.model.Transaction;
import com.neo.springapp.repository.TransactionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI-style real-time transaction fraud detection: analyzes deviations from user's normal activity
 * (high-value transfers, new recipients, unusual patterns). Creates fraud alerts when suspicious.
 */
@Service
public class FraudDetectionService {

    private static final double HIGH_VALUE_RATIO = 0.5;  // >50% of balance is high-value
    private static final double HIGH_ABSOLUTE_THRESHOLD = 100_000; // 1 lakh
    private static final int RECENT_DAYS_FOR_BASELINE = 90;

    private final TransactionRepository transactionRepository;
    private final FraudAlertService fraudAlertService;
    private final AccountService accountService;

    public FraudDetectionService(TransactionRepository transactionRepository,
                                FraudAlertService fraudAlertService,
                                AccountService accountService) {
        this.transactionRepository = transactionRepository;
        this.fraudAlertService = fraudAlertService;
        this.accountService = accountService;
    }

    /**
     * Analyze a transfer for fraud indicators: high value vs balance, first-time recipient, rapid succession.
     * Returns true if one or more alerts were created (suspicious).
     */
    public boolean analyzeTransferAndAlertIfSuspicious(String senderAccountNumber, String senderName,
                                                       String recipientAccountNumber, String recipientName,
                                                       Double amount, String clientIp, String location, String deviceInfo) {
        Double balance = accountService.getBalanceByAccountNumber(senderAccountNumber);
        if (balance == null) balance = 0.0;

        LocalDateTime since = LocalDateTime.now().minusDays(RECENT_DAYS_FOR_BASELINE);
        List<Transaction> recentDebits = transactionRepository.findByAccountNumberAndDateBetweenOrderByDateDesc(
                senderAccountNumber, since, LocalDateTime.now(), PageRequest.of(0, 500))
                .getContent().stream()
                .filter(t -> "Debit".equalsIgnoreCase(t.getType()) || "Transfer".equalsIgnoreCase(t.getType()))
                .collect(Collectors.toList());

        boolean alerted = false;
        StringBuilder reasons = new StringBuilder();

        // 1) High-value: amount > 50% of balance or > 1 lakh
        if (balance > 0 && (amount >= balance * HIGH_VALUE_RATIO || amount >= HIGH_ABSOLUTE_THRESHOLD)) {
            reasons.append("High-value transfer: ").append(amount).append(" (balance: ").append(balance).append("). ");
            alerted = true;
        }

        // 2) First-time recipient (no prior transfer to this recipient from this account)
        boolean hasTransferredToRecipientBefore = recentDebits.stream()
                .anyMatch(t -> recipientAccountNumber != null && recipientAccountNumber.equals(t.getRecipientAccountNumber()));
        if (recipientAccountNumber != null && !hasTransferredToRecipientBefore) {
            reasons.append("First-time transfer to recipient ").append(recipientAccountNumber).append(". ");
            alerted = true;
        }

        // 3) Multiple large debits in last 24 hours (velocity)
        LocalDateTime dayAgo = LocalDateTime.now().minusHours(24);
        double sumLast24h = recentDebits.stream()
                .filter(t -> t.getDate() != null && t.getDate().isAfter(dayAgo))
                .mapToDouble(t -> t.getAmount() != null ? t.getAmount() : 0)
                .sum();
        if (sumLast24h + amount > balance * 0.8) {
            reasons.append("Unusual velocity: high debit volume in 24h. ");
            alerted = true;
        }

        if (alerted && reasons.length() > 0) {
            String detailsJson = String.format("{\"amount\":%.2f,\"balance\":%.2f,\"recipient\":\"%s\",\"reasons\":\"%s\"}",
                    amount, balance, recipientAccountNumber != null ? recipientAccountNumber : "",
                    reasons.toString().replace("\"", "'"));
            fraudAlertService.recordTransactionAnomaly(
                    senderAccountNumber,
                    senderName != null ? senderName : "Unknown",
                    "Suspicious transfer detected",
                    "Real-time AI analysis: " + reasons.toString(),
                    detailsJson,
                    clientIp, location, deviceInfo
            );
        }
        return alerted;
    }
}
