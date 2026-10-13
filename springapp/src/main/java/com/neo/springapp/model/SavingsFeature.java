package com.neo.springapp.model;

import java.util.List;
import java.util.Set;

public final class SavingsFeature {
    public static final List<String> IDS = List.of(
            "accounts", "ai-assistant", "atm-simulator", "bank-messages",
            "beneficiary-management", "bill-payment", "card", "cheque",
            "credit-card", "credit-card-bill", "demand-draft", "deposit-request",
            "ecs-mandate", "emi-monitoring", "family-banking", "fasttag",
            "fixed-deposits", "goldloan", "homeloan", "investments",
            "kycupdate", "loan", "ml-loan-prediction", "positive-pay",
            "scheduled-payments", "subsidy-claim", "support-tickets",
            "transaction", "transferfunds", "upi-pay", "virtual-card");

    public static final Set<String> ID_SET = Set.copyOf(IDS);

    private SavingsFeature() {}
}
