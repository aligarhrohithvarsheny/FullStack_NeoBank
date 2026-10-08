package com.neo.springapp.service;

import com.neo.springapp.model.Account;
import com.neo.springapp.model.CreditCard;
import com.neo.springapp.model.GoldLoan;
import com.neo.springapp.model.HomeLoan;
import com.neo.springapp.model.Loan;
import com.neo.springapp.repository.AccountRepository;
import com.neo.springapp.repository.CreditCardRepository;
import com.neo.springapp.repository.GoldLoanRepository;
import com.neo.springapp.repository.HomeLoanRepository;
import com.neo.springapp.repository.LoanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Real-time credit risk analysis for home loans. A logistic model scores the applicant
 * from live data (existing loans, repayment behaviour, card utilisation, income, CIBIL).
 */
@Service
public class HomeLoanAnalysisService {

    @Autowired private AccountRepository accountRepository;
    @Autowired private LoanRepository loanRepository;
    @Autowired private GoldLoanRepository goldLoanRepository;
    @Autowired private CreditCardRepository creditCardRepository;
    @Autowired private HomeLoanRepository homeLoanRepository;

    public static double emi(double principal, double annualRate, int months) {
        if (months <= 0) return principal;
        double r = annualRate / 1200.0;
        if (r == 0) return principal / months;
        double f = Math.pow(1 + r, months);
        return principal * r * f / (f - 1);
    }

    private static double n(Double v) { return v == null ? 0.0 : v; }
    private static double round2(double v) { return Math.round(v * 100.0) / 100.0; }

    private boolean active(String status) {
        return status != null && (status.equalsIgnoreCase("Approved") || status.equalsIgnoreCase("Active"));
    }

    public Map<String, Object> analyze(String accountNumber, Double amount, Integer tenure, Double rate, Double propertyValue) {
        double reqAmount = n(amount);
        int reqTenure = tenure == null || tenure <= 0 ? 120 : tenure;
        double reqRate = rate == null || rate <= 0 ? 8.5 : rate;

        Account account = accountRepository.findByAccountNumber(accountNumber);
        List<Map<String, Object>> existing = new ArrayList<>();
        double existingEmi = 0;
        int cibil = 0;

        for (Loan l : loanRepository.findByAccountNumber(accountNumber)) {
            if (l.getCibilScore() != null && l.getCibilScore() > cibil) cibil = l.getCibilScore();
            double amt = n(l.getAmount());
            double paid = amt > 0 ? Math.min(100, n(l.getPrincipalPaid()) / amt * 100) : 0;
            boolean act = active(l.getStatus());
            double e = act && l.getTenure() != null ? emi(amt, n(l.getInterestRate()), l.getTenure()) : 0;
            existingEmi += e;
            existing.add(row(l.getType() == null ? "Personal Loan" : l.getType(), l.getLoanAccountNumber(), amt,
                    l.getStatus(), paid, e));
        }
        for (GoldLoan g : goldLoanRepository.findByAccountNumber(accountNumber)) {
            double amt = n(g.getLoanAmount());
            double paid = amt > 0 ? Math.min(100, n(g.getPrincipalPaid()) / amt * 100) : 0;
            boolean act = active(g.getStatus());
            double e = act ? n(g.getCurrentEmi()) : 0;
            existingEmi += e;
            existing.add(row("Gold Loan", g.getLoanAccountNumber(), amt, g.getStatus(), paid, e));
        }
        double cardUtilSum = 0; int cardCount = 0;
        for (CreditCard c : creditCardRepository.findByAccountNumber(accountNumber)) {
            double limit = n(c.getApprovedLimit());
            double used = n(c.getCurrentBalance());
            double util = limit > 0 ? Math.min(100, used / limit * 100) : 0;
            cardUtilSum += util; cardCount++;
            existing.add(row("Credit Card", c.getCardNumber() == null ? "-" : "XXXX" + c.getCardNumber().substring(Math.max(0, c.getCardNumber().length() - 4)),
                    limit, c.getStatus(), 100 - util, 0));
        }
        for (HomeLoan h : homeLoanRepository.findByAccountNumberOrderByApplicationDateDesc(accountNumber)) {
            if (!"Approved".equals(h.getStatus()) && !"Closed".equals(h.getStatus())) continue;
            double amt = n(h.getAmount());
            double paid = amt > 0 ? Math.min(100, n(h.getPrincipalPaid()) / amt * 100) : 0;
            double e = "Approved".equals(h.getStatus()) ? n(h.getEmi()) : 0;
            existingEmi += e;
            existing.add(row("Home Loan", h.getLoanAccountNumber(), amt, h.getStatus(), paid, e));
        }

        double income = account == null ? 0 : n(account.getIncome());
        double monthlyIncome = income > 0 ? income / 12.0 : 0;
        double newEmi = emi(reqAmount, reqRate, reqTenure);
        double foir = monthlyIncome > 0 ? (existingEmi + newEmi) / monthlyIncome : 1.0;
        double avgCardUtil = cardCount > 0 ? cardUtilSum / cardCount : 0;
        double ltv = n(propertyValue) > 0 ? reqAmount / n(propertyValue) : 0;
        double balance = account == null ? 0 : n(account.getBalance());

        List<Double> paidPcts = new ArrayList<>();
        for (Map<String, Object> r : existing) {
            if (!"Credit Card".equals(r.get("type"))) paidPcts.add((Double) r.get("paidPercentage"));
        }
        double avgPaid = paidPcts.isEmpty() ? 50 : paidPcts.stream().mapToDouble(d -> d).average().orElse(50);
        int effectiveCibil = cibil > 0 ? cibil : 650;

        // Logistic regression with hand-calibrated coefficients
        double z = -2.2
                + (effectiveCibil - 650) / 60.0
                - 2.8 * Math.max(0, foir - 0.35)
                + (avgPaid - 50) / 80.0
                - avgCardUtil / 120.0
                - (ltv > 0.8 ? (ltv - 0.8) * 6 : 0)
                + (balance >= reqAmount * 0.1 ? 0.4 : -0.2)
                + 1.6 + (income > 0 ? 0.3 : -0.8);
        double probability = 1.0 / (1.0 + Math.exp(-z));
        String band = probability >= 0.75 ? "LOW" : probability >= 0.5 ? "MEDIUM" : "HIGH";

        List<String> factors = new ArrayList<>();
        factors.add("CIBIL " + effectiveCibil + (cibil > 0 ? "" : " (assumed - no history)"));
        factors.add("FOIR " + round2(foir * 100) + "% (limit 50%)");
        factors.add("Avg repayment progress of existing loans " + round2(avgPaid) + "%");
        if (cardCount > 0) factors.add("Credit card utilisation " + round2(avgCardUtil) + "%");
        if (ltv > 0) factors.add("Loan-to-value " + round2(ltv * 100) + "%");
        List<String> recommendations = new ArrayList<>();
        if (foir > 0.5) recommendations.add("EMI burden exceeds 50% of income - reduce amount or extend tenure.");
        if (ltv > 0.8) recommendations.add("LTV above 80% - ask for a larger down payment.");
        if (avgCardUtil > 60) recommendations.add("High credit card utilisation - pay down cards before approval.");
        if (income <= 0) recommendations.add("Income missing on profile - verify income documents.");
        if (recommendations.isEmpty()) recommendations.add("Profile is healthy for the requested terms.");

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("accountNumber", accountNumber);
        out.put("pan", account == null ? null : account.getPan());
        out.put("cibilScore", effectiveCibil);
        out.put("monthlyIncome", round2(monthlyIncome));
        out.put("existingMonthlyEmi", round2(existingEmi));
        out.put("proposedEmi", round2(newEmi));
        out.put("foirPercent", round2(foir * 100));
        out.put("avgPaidPercentage", round2(avgPaid));
        out.put("approvalProbability", round2(probability * 100));
        out.put("riskBand", band);
        out.put("model", "NeoBank-HomeLoan-Logistic-v1");
        out.put("existingLoans", existing);
        out.put("factors", factors);
        out.put("recommendations", recommendations);
        out.put("analyzedAt", java.time.LocalDateTime.now().toString());
        return out;
    }

    private Map<String, Object> row(String type, String ref, double amount, String status, double paidPct, double emi) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", type);
        m.put("reference", ref);
        m.put("amount", round2(amount));
        m.put("status", status);
        m.put("paidPercentage", round2(paidPct));
        m.put("emi", round2(emi));
        return m;
    }
}
