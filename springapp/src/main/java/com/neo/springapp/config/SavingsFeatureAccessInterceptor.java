package com.neo.springapp.config;

import com.neo.springapp.service.SavingsFeatureAccessService;
import com.neo.springapp.service.UserSessionTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Map;

@Component
public class SavingsFeatureAccessInterceptor implements HandlerInterceptor {
    private static final Map<String, String> FEATURE_PATHS = Map.ofEntries(
            Map.entry("/api/accounts", "accounts"),
            Map.entry("/api/admin/atm", "atm-simulator"),
            Map.entry("/api/admin/positive-pay", "positive-pay"),
            Map.entry("/api/business-cheques", "cheque"),
            Map.entry("/api/card360", "card"),
            Map.entry("/api/credit-card-requests", "credit-card"),
            Map.entry("/api/customer-nominees", "accounts"),
            Map.entry("/api/demand-drafts", "demand-draft"),
            Map.entry("/api/deposit-requests", "deposit-request"),
            Map.entry("/api/education-loan-subsidy-claims", "subsidy-claim"),
            Map.entry("/api/emis", "emi-monitoring"),
            Map.entry("/api/fastag", "fasttag"),
            Map.entry("/api/fasttags", "fasttag"),
            Map.entry("/api/family", "family-banking"),
            Map.entry("/api/kyc", "kycupdate"),
            Map.entry("/api/net-banking-control", "accounts"),
            Map.entry("/api/passbook", "accounts"),
            Map.entry("/api/virtual-card", "virtual-card"),
            Map.entry("/api/virtual-cards", "virtual-card"),
            Map.entry("/api/transactions", "transaction"),
            Map.entry("/api/transfers", "transferfunds"),
            Map.entry("/api/cheques", "cheque"),
            Map.entry("/api/savings-cheques", "cheque"),
            Map.entry("/api/positive-pay", "positive-pay"),
            Map.entry("/api/cards", "card"),
            Map.entry("/api/credit-cards", "credit-card"),
            Map.entry("/api/bill-payments", "bill-payment"),
            Map.entry("/api/loans", "loan"),
            Map.entry("/api/gold-loans", "goldloan"),
            Map.entry("/api/home-loans", "homeloan"),
            Map.entry("/api/fixed-deposits", "fixed-deposits"),
            Map.entry("/api/term-deposit-plans", "fixed-deposits"),
            Map.entry("/api/ecs-mandates", "ecs-mandate"),
            Map.entry("/api/subsidy-claims", "subsidy-claim"),
            Map.entry("/api/investments", "investments"),
            Map.entry("/api/savings-upi", "upi-pay"),
            Map.entry("/api/upi", "upi-pay"),
            Map.entry("/api/scheduled-payments", "scheduled-payments"),
            Map.entry("/api/support-tickets", "support-tickets"),
            Map.entry("/api/bank-messages", "bank-messages"),
            Map.entry("/api/beneficiaries", "beneficiary-management"),
            Map.entry("/api/atm", "atm-simulator"),
            Map.entry("/api/family-banking", "family-banking"),
            Map.entry("/api/ai", "ai-assistant"));

    private final SavingsFeatureAccessService featureAccessService;

    public SavingsFeatureAccessInterceptor(SavingsFeatureAccessService featureAccessService) {
        this.featureAccessService = featureAccessService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !(authentication.getPrincipal() instanceof UserSessionTokenService.SessionPrincipal principal)
                || !"USER".equals(principal.scope())) {
            return true;
        }

        String path = request.getRequestURI();
        if (path.endsWith("/features")) return true;
        if (!featureAccessService.isSavingsAccount(principal.accountNumber())) return true;
        String feature = resolveFeature(path);
        if (feature == null || featureAccessService.isFeatureEnabled(principal.accountNumber(), feature)) {
            return true;
        }

        response.setStatus(HttpStatus.LOCKED.value());
        response.setContentType("application/json");
        response.getWriter().write("{\"success\":false,\"message\":\"This feature is disabled for your savings account.\"}");
        return false;
    }

    private String resolveFeature(String path) {
        if (path.startsWith("/api/loans/predict-approval")) return "ml-loan-prediction";
        if (path.startsWith("/api/credit-cards/")
                && (path.contains("/bill-pay/") || path.contains("/bills/"))) {
            return "credit-card-bill";
        }
        return FEATURE_PATHS.entrySet().stream()
                .filter(entry -> path.equals(entry.getKey()) || path.startsWith(entry.getKey() + "/"))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }
}
