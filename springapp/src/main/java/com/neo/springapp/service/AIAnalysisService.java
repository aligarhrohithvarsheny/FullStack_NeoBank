package com.neo.springapp.service;

import com.neo.springapp.model.Transaction;
import com.neo.springapp.model.Loan;
import com.neo.springapp.model.Account;
import com.neo.springapp.repository.TransactionRepository;
import com.neo.springapp.repository.LoanRepository;
import com.neo.springapp.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AIAnalysisService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private AccountRepository accountRepository;

    /**
     * Analyze user's spending patterns and provide AI insights
     */
    public Map<String, Object> analyzeSpending(String accountNumber) {
        Map<String, Object> analysis = new HashMap<>();
        
        // Get account details
        Account account = accountRepository.findByAccountNumber(accountNumber);
        if (account == null) {
            analysis.put("error", "Account not found");
            return analysis;
        }

        // Get transactions from last 3 months
        LocalDateTime threeMonthsAgo = LocalDateTime.now().minus(3, ChronoUnit.MONTHS);
        List<Transaction> transactions = transactionRepository.findByAccountNumberAndDateBetweenOrderByDateDesc(
            accountNumber, threeMonthsAgo, LocalDateTime.now(), 
            org.springframework.data.domain.PageRequest.of(0, 1000)
        ).getContent();

        // Calculate spending metrics
        Map<String, Object> spendingMetrics = calculateSpendingMetrics(transactions, account);
        analysis.put("spendingMetrics", spendingMetrics);

        // Generate spending insights
        List<Map<String, Object>> insights = generateSpendingInsights(transactions, account);
        analysis.put("insights", insights);

        // Generate loan suggestions
        List<Map<String, Object>> loanSuggestions = generateLoanSuggestions(transactions, account);
        analysis.put("loanSuggestions", loanSuggestions);

        // Generate charge/fee suggestions
        List<Map<String, Object>> chargeSuggestions = generateChargeSuggestions(transactions, account);
        analysis.put("chargeSuggestions", chargeSuggestions);

        // Spending categories
        Map<String, Double> categorySpending = categorizeSpending(transactions);
        analysis.put("categorySpending", categorySpending);

        // Monthly trends
        Map<String, Object> monthlyTrends = calculateMonthlyTrends(transactions);
        analysis.put("monthlyTrends", monthlyTrends);

        return analysis;
    }

    /**
     * Calculate spending metrics
     */
    private Map<String, Object> calculateSpendingMetrics(List<Transaction> transactions, Account account) {
        Map<String, Object> metrics = new HashMap<>();
        
        // Filter debit transactions
        List<Transaction> debitTransactions = transactions.stream()
            .filter(t -> t.getType() != null && 
                (t.getType().equalsIgnoreCase("Debit") || 
                 t.getType().equalsIgnoreCase("Withdraw") ||
                 t.getType().equalsIgnoreCase("Transfer")))
            .collect(Collectors.toList());

        // Calculate total spending
        double totalSpending = debitTransactions.stream()
            .mapToDouble(t -> t.getAmount() != null ? t.getAmount() : 0.0)
            .sum();

        // Calculate average monthly spending
        long months = Math.max(1, ChronoUnit.MONTHS.between(
            transactions.isEmpty() ? LocalDateTime.now() : 
            transactions.get(transactions.size() - 1).getDate(),
            LocalDateTime.now()
        ));
        double avgMonthlySpending = totalSpending / Math.max(1, months);

        // Calculate income (credits)
        double totalIncome = transactions.stream()
            .filter(t -> t.getType() != null && 
                (t.getType().equalsIgnoreCase("Credit") || 
                 t.getType().equalsIgnoreCase("Deposit")))
            .mapToDouble(t -> t.getAmount() != null ? t.getAmount() : 0.0)
            .sum();

        double avgMonthlyIncome = totalIncome / Math.max(1, months);

        // Savings rate
        double savingsRate = avgMonthlyIncome > 0 ? 
            ((avgMonthlyIncome - avgMonthlySpending) / avgMonthlyIncome) * 100 : 0;

        metrics.put("totalSpending", totalSpending);
        metrics.put("avgMonthlySpending", avgMonthlySpending);
        metrics.put("totalIncome", totalIncome);
        metrics.put("avgMonthlyIncome", avgMonthlyIncome);
        metrics.put("savingsRate", savingsRate);
        metrics.put("currentBalance", account.getBalance() != null ? account.getBalance() : 0.0);
        metrics.put("accountIncome", account.getIncome() != null ? account.getIncome() : 0.0);
        metrics.put("analysisPeriod", months + " months");

        return metrics;
    }

    /**
     * Generate spending insights
     */
    private List<Map<String, Object>> generateSpendingInsights(List<Transaction> transactions, Account account) {
        List<Map<String, Object>> insights = new ArrayList<>();

        // Calculate spending metrics
        Map<String, Object> metrics = calculateSpendingMetrics(transactions, account);
        double avgMonthlySpending = (Double) metrics.get("avgMonthlySpending");
        double avgMonthlyIncome = (Double) metrics.get("avgMonthlyIncome");
        double savingsRate = (Double) metrics.get("savingsRate");

        // Insight 1: Spending vs Income
        if (avgMonthlySpending > avgMonthlyIncome * 0.8) {
            Map<String, Object> insight = new HashMap<>();
            insight.put("type", "warning");
            insight.put("title", "High Spending Alert");
            insight.put("message", String.format(
                "Your monthly spending (₹%.2f) is %.1f%% of your income. Consider reducing expenses to improve savings.",
                avgMonthlySpending, (avgMonthlySpending / avgMonthlyIncome) * 100
            ));
            insight.put("icon", "⚠️");
            insights.add(insight);
        } else if (savingsRate > 20) {
            Map<String, Object> insight = new HashMap<>();
            insight.put("type", "success");
            insight.put("title", "Excellent Savings Rate");
            insight.put("message", String.format(
                "Great job! You're saving %.1f%% of your income. Keep up the good financial habits!",
                savingsRate
            ));
            insight.put("icon", "✅");
            insights.add(insight);
        }

        // Insight 2: Large transactions
        List<Transaction> largeTransactions = transactions.stream()
            .filter(t -> t.getAmount() != null && t.getAmount() > 10000 && 
                (t.getType().equalsIgnoreCase("Debit") || t.getType().equalsIgnoreCase("Withdraw")))
            .collect(Collectors.toList());

        if (!largeTransactions.isEmpty()) {
            Map<String, Object> insight = new HashMap<>();
            insight.put("type", "info");
            insight.put("title", "Large Transactions Detected");
            insight.put("message", String.format(
                "You have %d large transactions (>₹10,000) in the last 3 months. Review these for potential savings.",
                largeTransactions.size()
            ));
            insight.put("icon", "💡");
            insights.add(insight);
        }

        // Insight 3: Balance trend
        if (account.getBalance() != null && account.getBalance() < 1000) {
            Map<String, Object> insight = new HashMap<>();
            insight.put("type", "warning");
            insight.put("title", "Low Balance Alert");
            insight.put("message", "Your account balance is low. Consider maintaining a minimum balance to avoid charges.");
            insight.put("icon", "⚠️");
            insights.add(insight);
        }

        // Insight 4: Spending consistency
        if (transactions.size() > 10) {
            double spendingVariance = calculateSpendingVariance(transactions);
            if (spendingVariance > 0.5) {
                Map<String, Object> insight = new HashMap<>();
                insight.put("type", "info");
                insight.put("title", "Irregular Spending Pattern");
                insight.put("message", "Your spending pattern shows high variance. Creating a budget could help stabilize your finances.");
                insight.put("icon", "📊");
                insights.add(insight);
            }
        }

        return insights;
    }

    /**
     * Generate loan suggestions based on spending patterns
     */
    private List<Map<String, Object>> generateLoanSuggestions(List<Transaction> transactions, Account account) {
        List<Map<String, Object>> suggestions = new ArrayList<>();

        // Check existing loans
        List<Loan> existingLoans = loanRepository.findByAccountNumber(account.getAccountNumber());
        boolean hasActiveLoan = existingLoans.stream()
            .anyMatch(l -> l.getStatus() != null && l.getStatus().equalsIgnoreCase("Approved"));

        Map<String, Object> metrics = calculateSpendingMetrics(transactions, account);
        double avgMonthlySpending = (Double) metrics.get("avgMonthlySpending");
        double avgMonthlyIncome = (Double) metrics.get("avgMonthlyIncome");
        double accountIncome = account.getIncome() != null ? account.getIncome() : avgMonthlyIncome * 12;

        // Suggestion 1: Personal Loan for debt consolidation
        if (avgMonthlySpending > avgMonthlyIncome * 0.7 && !hasActiveLoan) {
            Map<String, Object> suggestion = new HashMap<>();
            suggestion.put("loanType", "Personal Loan");
            suggestion.put("recommendedAmount", Math.min(accountIncome * 0.5, 500000));
            suggestion.put("reason", "Your spending is high relative to income. A personal loan could help consolidate expenses.");
            suggestion.put("benefits", Arrays.asList("Lower interest rates", "Single EMI payment", "Better financial management"));
            suggestion.put("interestRate", 10.5);
            suggestion.put("tenure", 36);
            suggestion.put("priority", "high");
            suggestions.add(suggestion);
        }

        // Suggestion 2: Home Loan
        if (accountIncome > 500000 && !hasActiveLoan) {
            Map<String, Object> suggestion = new HashMap<>();
            suggestion.put("loanType", "Home Loan");
            suggestion.put("recommendedAmount", accountIncome * 5);
            suggestion.put("reason", "Based on your income, you may be eligible for a home loan with attractive rates.");
            suggestion.put("benefits", Arrays.asList("Tax benefits", "Long tenure options", "Low interest rates"));
            suggestion.put("interestRate", 8.5);
            suggestion.put("tenure", 240);
            suggestion.put("priority", "medium");
            suggestions.add(suggestion);
        }

        // Suggestion 3: Education Loan
        if (accountIncome > 300000 && !hasActiveLoan) {
            Map<String, Object> suggestion = new HashMap<>();
            suggestion.put("loanType", "Education Loan");
            suggestion.put("recommendedAmount", Math.min(accountIncome * 2, 1000000));
            suggestion.put("reason", "Education loans offer special benefits and lower rates for students and parents.");
            suggestion.put("benefits", Arrays.asList("Subsidy available", "Moratorium period", "Tax deductions"));
            suggestion.put("interestRate", 7.5);
            suggestion.put("tenure", 60);
            suggestion.put("priority", "medium");
            suggestions.add(suggestion);
        }

        // Suggestion 4: Vehicle Loan
        if (avgMonthlyIncome > 50000 && !hasActiveLoan) {
            Map<String, Object> suggestion = new HashMap<>();
            suggestion.put("loanType", "Vehicle Loan");
            suggestion.put("recommendedAmount", Math.min(accountIncome * 0.8, 1500000));
            suggestion.put("reason", "Vehicle loans with quick approval and competitive rates available.");
            suggestion.put("benefits", Arrays.asList("Quick approval", "Flexible tenure", "Low down payment"));
            suggestion.put("interestRate", 9.0);
            suggestion.put("tenure", 60);
            suggestion.put("priority", "low");
            suggestions.add(suggestion);
        }

        return suggestions;
    }

    /**
     * Generate charge/fee suggestions
     */
    private List<Map<String, Object>> generateChargeSuggestions(List<Transaction> transactions, Account account) {
        List<Map<String, Object>> suggestions = new ArrayList<>();

        // Check for minimum balance charges
        if (account.getBalance() != null && account.getBalance() < 5000) {
            Map<String, Object> suggestion = new HashMap<>();
            suggestion.put("chargeType", "Minimum Balance Charge");
            suggestion.put("amount", 500);
            suggestion.put("reason", "Your balance is below the minimum required balance of ₹5,000");
            suggestion.put("action", "Maintain minimum balance to avoid charges");
            suggestion.put("frequency", "Monthly");
            suggestions.add(suggestion);
        }

        // Check transaction frequency for charges
        long recentTransactions = transactions.stream()
            .filter(t -> t.getDate().isAfter(LocalDateTime.now().minus(1, ChronoUnit.MONTHS)))
            .count();

        if (recentTransactions > 50) {
            Map<String, Object> suggestion = new HashMap<>();
            suggestion.put("chargeType", "Transaction Limit Charge");
            suggestion.put("amount", 2.0 * (recentTransactions - 50));
            suggestion.put("reason", String.format("You have %d transactions this month. Free limit is 50 transactions.", recentTransactions));
            suggestion.put("action", "Consider consolidating transactions to reduce charges");
            suggestion.put("frequency", "Per excess transaction");
            suggestions.add(suggestion);
        }

        // ATM withdrawal charges
        long atmWithdrawals = transactions.stream()
            .filter(t -> t.getDescription() != null && 
                t.getDescription().toLowerCase().contains("atm"))
            .filter(t -> t.getDate().isAfter(LocalDateTime.now().minus(1, ChronoUnit.MONTHS)))
            .count();

        if (atmWithdrawals > 5) {
            Map<String, Object> suggestion = new HashMap<>();
            suggestion.put("chargeType", "ATM Withdrawal Charge");
            suggestion.put("amount", 20.0 * (atmWithdrawals - 5));
            suggestion.put("reason", String.format("You have %d ATM withdrawals this month. Free limit is 5.", atmWithdrawals));
            suggestion.put("action", "Use UPI or online transfers to avoid ATM charges");
            suggestion.put("frequency", "Per excess withdrawal");
            suggestions.add(suggestion);
        }

        return suggestions;
    }

    /**
     * Categorize spending by merchant/description
     */
    private Map<String, Double> categorizeSpending(List<Transaction> transactions) {
        Map<String, Double> categories = new HashMap<>();
        
        List<Transaction> debitTransactions = transactions.stream()
            .filter(t -> t.getType() != null && 
                (t.getType().equalsIgnoreCase("Debit") || 
                 t.getType().equalsIgnoreCase("Withdraw")))
            .collect(Collectors.toList());

        for (Transaction t : debitTransactions) {
            String category = categorizeTransaction(t);
            double amount = t.getAmount() != null ? t.getAmount() : 0.0;
            categories.put(category, categories.getOrDefault(category, 0.0) + amount);
        }

        return categories;
    }

    /**
     * Categorize a transaction based on merchant/description
     */
    private String categorizeTransaction(Transaction transaction) {
        String merchant = transaction.getMerchant() != null ? transaction.getMerchant().toLowerCase() : "";
        String description = transaction.getDescription() != null ? transaction.getDescription().toLowerCase() : "";

        if (merchant.contains("restaurant") || merchant.contains("food") || description.contains("food")) {
            return "Food & Dining";
        } else if (merchant.contains("fuel") || merchant.contains("petrol") || merchant.contains("gas")) {
            return "Transportation";
        } else if (merchant.contains("shopping") || merchant.contains("mall") || description.contains("purchase")) {
            return "Shopping";
        } else if (merchant.contains("medical") || merchant.contains("hospital") || description.contains("health")) {
            return "Healthcare";
        } else if (merchant.contains("entertainment") || merchant.contains("movie") || description.contains("entertainment")) {
            return "Entertainment";
        } else if (merchant.contains("utility") || description.contains("bill") || description.contains("electricity")) {
            return "Utilities";
        } else if (description.contains("transfer") || description.contains("payment")) {
            return "Transfers";
        } else {
            return "Other";
        }
    }

    /**
     * Calculate monthly spending trends
     */
    private Map<String, Object> calculateMonthlyTrends(List<Transaction> transactions) {
        Map<String, Object> trends = new HashMap<>();
        Map<String, Double> monthlySpending = new LinkedHashMap<>();

        // Group by month
        transactions.stream()
            .filter(t -> t.getType() != null && 
                (t.getType().equalsIgnoreCase("Debit") || t.getType().equalsIgnoreCase("Withdraw")))
            .forEach(t -> {
                String monthKey = t.getDate().getYear() + "-" + String.format("%02d", t.getDate().getMonthValue());
                double amount = t.getAmount() != null ? t.getAmount() : 0.0;
                monthlySpending.put(monthKey, monthlySpending.getOrDefault(monthKey, 0.0) + amount);
            });

        trends.put("monthlySpending", monthlySpending);
        
        // Calculate trend (increasing/decreasing)
        if (monthlySpending.size() >= 2) {
            List<Double> values = new ArrayList<>(monthlySpending.values());
            double lastMonth = values.get(values.size() - 1);
            double previousMonth = values.get(values.size() - 2);
            double changePercent = ((lastMonth - previousMonth) / previousMonth) * 100;
            trends.put("trend", changePercent > 0 ? "increasing" : "decreasing");
            trends.put("changePercent", changePercent);
        }

        return trends;
    }

    /**
     * Calculate spending variance
     */
    private double calculateSpendingVariance(List<Transaction> transactions) {
        List<Double> dailySpending = new ArrayList<>();
        Map<String, Double> dailyTotals = new HashMap<>();

        transactions.stream()
            .filter(t -> t.getType() != null && 
                (t.getType().equalsIgnoreCase("Debit") || t.getType().equalsIgnoreCase("Withdraw")))
            .forEach(t -> {
                String dateKey = t.getDate().toLocalDate().toString();
                double amount = t.getAmount() != null ? t.getAmount() : 0.0;
                dailyTotals.put(dateKey, dailyTotals.getOrDefault(dateKey, 0.0) + amount);
            });

        dailySpending.addAll(dailyTotals.values());
        
        if (dailySpending.isEmpty()) return 0.0;

        double mean = dailySpending.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = dailySpending.stream()
            .mapToDouble(x -> Math.pow(x - mean, 2))
            .average()
            .orElse(0.0);

        return variance / (mean * mean); // Coefficient of variation
    }
}

