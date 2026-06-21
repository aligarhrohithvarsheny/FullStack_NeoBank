package com.neo.springapp.config;

import java.util.*;

public final class BankFormCatalog {

    public record FormDefinition(int id, String code, String name, String category, List<String> fields) {}

    private static final List<String> LOAN_FIELDS = List.of(
            "Loan Amount", "Income", "Employment Details", "Documents Upload", "Loan Tenure", "EMI Details"
    );

    private static final List<String> ACCOUNT_OPENING_FIELDS = List.of(
            "Name", "DOB", "Gender", "Mobile", "Email", "Aadhaar", "PAN", "Address",
            "City", "State", "Pincode", "Nominee Details", "Account Number (Auto-generated)"
    );

    private static final List<String> KYC_FIELDS = List.of(
            "Aadhaar Upload", "PAN Upload", "Photo Upload", "Signature Upload"
    );

    private static final List<FormDefinition> FORMS = buildForms();

    private BankFormCatalog() {}

    private static List<FormDefinition> buildForms() {
        List<FormDefinition> list = new ArrayList<>();
        list.add(new FormDefinition(1, "account-opening", "Account Opening Form", "Customer Forms", ACCOUNT_OPENING_FIELDS));
        list.add(new FormDefinition(2, "customer-login", "Customer Login Form", "Customer Forms",
                List.of("Account Number / Customer ID", "Password", "OTP", "Login Timestamp")));
        list.add(new FormDefinition(3, "customer-registration", "Customer Registration Form", "Customer Forms",
                List.of("Full Name", "Email", "Mobile", "Password", "Confirm Password", "Terms Acceptance")));
        list.add(new FormDefinition(4, "kyc-verification", "KYC Verification Form", "Customer Forms", KYC_FIELDS));
        list.add(new FormDefinition(5, "debit-card-request", "Debit Card Request Form", "Customer Forms",
                List.of("Account Number", "Card Type", "Delivery Address", "Preferred Name on Card")));
        list.add(new FormDefinition(6, "credit-card-application", "Credit Card Application Form", "Customer Forms",
                List.of("Account Number", "Annual Income", "Employment Type", "Credit Limit Request", "Documents Upload")));
        list.add(new FormDefinition(7, "cheque-book-request", "Cheque Book Request Form", "Customer Forms",
                List.of("Account Number", "Number of Leaves", "Delivery Mode", "Branch Code")));
        list.add(new FormDefinition(8, "net-banking-registration", "Net Banking Registration Form", "Customer Forms",
                List.of("Account Number", "Customer ID", "Login ID", "Security Questions", "Terms Acceptance")));
        list.add(new FormDefinition(9, "mobile-banking-registration", "Mobile Banking Registration Form", "Customer Forms",
                List.of("Account Number", "Registered Mobile", "MPIN Setup", "Device Binding")));
        list.add(new FormDefinition(10, "upi-registration", "UPI Registration Form", "Customer Forms",
                List.of("Account Number", "UPI ID", "Linked Mobile", "Default VPA")));

        list.add(new FormDefinition(11, "personal-loan", "Personal Loan Application", "Loan Application Forms", LOAN_FIELDS));
        list.add(new FormDefinition(12, "home-loan", "Home Loan Application", "Loan Application Forms", LOAN_FIELDS));
        list.add(new FormDefinition(13, "car-loan", "Car Loan Application", "Loan Application Forms", LOAN_FIELDS));
        list.add(new FormDefinition(14, "gold-loan", "Gold Loan Application", "Loan Application Forms", LOAN_FIELDS));
        list.add(new FormDefinition(15, "education-loan", "Education Loan Application", "Loan Application Forms", LOAN_FIELDS));
        list.add(new FormDefinition(16, "business-loan", "Business Loan Application", "Loan Application Forms", LOAN_FIELDS));

        list.add(new FormDefinition(17, "deposit", "Deposit Form", "Transaction Forms",
                List.of("Account Number", "Deposit Amount", "Deposit Mode", "Reference Number", "Remarks")));
        list.add(new FormDefinition(18, "withdrawal", "Withdrawal Form", "Transaction Forms",
                List.of("Account Number", "Withdrawal Amount", "Mode", "Purpose", "Signature")));
        list.add(new FormDefinition(19, "fund-transfer", "Fund Transfer Form", "Transaction Forms",
                List.of("From Account", "To Account", "Amount", "Purpose", "OTP Verification")));
        list.add(new FormDefinition(20, "neft-transfer", "NEFT Transfer Form", "Transaction Forms",
                List.of("Debit Account", "Beneficiary Account", "IFSC", "Amount", "NEFT Reference")));
        list.add(new FormDefinition(21, "rtgs-transfer", "RTGS Transfer Form", "Transaction Forms",
                List.of("Debit Account", "Beneficiary Account", "IFSC", "Amount", "RTGS Reference")));
        list.add(new FormDefinition(22, "imps-transfer", "IMPS Transfer Form", "Transaction Forms",
                List.of("Debit Account", "Beneficiary Mobile/MMID", "Amount", "IMPS Reference")));
        list.add(new FormDefinition(23, "upi-payment", "UPI Payment Form", "Transaction Forms",
                List.of("Payer VPA", "Payee VPA", "Amount", "UPI Reference", "Remarks")));
        list.add(new FormDefinition(24, "bill-payment", "Bill Payment Form", "Transaction Forms",
                List.of("Account Number", "Biller Category", "Consumer Number", "Amount", "Payment Reference")));
        list.add(new FormDefinition(25, "recharge", "Recharge Form", "Transaction Forms",
                List.of("Account Number", "Mobile/Operator", "Recharge Amount", "Plan Type", "Reference")));

        list.add(new FormDefinition(26, "card-block", "Card Block Form", "Card Services Forms",
                List.of("Card Number", "Account Number", "Reason for Block", "Temporary/Permanent", "OTP")));
        list.add(new FormDefinition(27, "card-unblock", "Card Unblock Form", "Card Services Forms",
                List.of("Card Number", "Account Number", "Reason", "OTP Verification")));
        list.add(new FormDefinition(28, "card-replacement", "Card Replacement Request Form", "Card Services Forms",
                List.of("Card Number", "Account Number", "Replacement Reason", "Delivery Address")));
        list.add(new FormDefinition(29, "pin-generation", "PIN Generation Form", "Card Services Forms",
                List.of("Card Number", "Account Number", "PIN Delivery Mode", "OTP Verification")));
        list.add(new FormDefinition(30, "card-upgrade", "Card Upgrade Request Form", "Card Services Forms",
                List.of("Current Card Type", "Requested Upgrade", "Income Proof", "Account Number")));

        list.add(new FormDefinition(31, "address-change", "Address Change Form", "Service Request Forms",
                List.of("Account Number", "Old Address", "New Address", "Proof of Address Upload")));
        list.add(new FormDefinition(32, "mobile-change", "Mobile Number Change Form", "Service Request Forms",
                List.of("Account Number", "Old Mobile", "New Mobile", "OTP Verification")));
        list.add(new FormDefinition(33, "email-change", "Email Change Form", "Service Request Forms",
                List.of("Account Number", "Old Email", "New Email", "Verification OTP")));
        list.add(new FormDefinition(34, "nominee-update", "Nominee Update Form", "Service Request Forms",
                List.of("Account Number", "Nominee Name", "Relationship", "Nominee DOB", "Share Percentage")));
        list.add(new FormDefinition(35, "account-closure", "Account Closure Form", "Service Request Forms",
                List.of("Account Number", "Closure Reason", "Settlement Account", "Signature")));
        list.add(new FormDefinition(36, "complaint-registration", "Complaint Registration Form", "Service Request Forms",
                List.of("Account Number", "Complaint Category", "Description", "Preferred Contact")));
        list.add(new FormDefinition(37, "dispute-resolution", "Dispute Resolution Form", "Service Request Forms",
                List.of("Account Number", "Transaction Reference", "Dispute Amount", "Description")));
        list.add(new FormDefinition(38, "refund-request", "Refund Request Form", "Service Request Forms",
                List.of("Account Number", "Original Transaction ID", "Refund Amount", "Reason")));
        list.add(new FormDefinition(39, "stop-cheque", "Stop Cheque Request Form", "Service Request Forms",
                List.of("Account Number", "Cheque Number(s)", "Reason", "Date Range")));

        list.add(new FormDefinition(40, "customer-approval", "Customer Approval Form", "Manager Forms",
                List.of("Customer Name", "Account Number", "KYC Status", "Approval Decision", "Manager Remarks")));
        list.add(new FormDefinition(41, "kyc-approval", "KYC Approval Form", "Manager Forms",
                List.of("Customer ID", "Document Type", "Verification Status", "Approver Name", "Remarks")));
        list.add(new FormDefinition(42, "loan-approval", "Loan Approval Form", "Manager Forms",
                List.of("Loan Application ID", "Loan Amount", "Risk Score", "Approval Decision", "Sanction Terms")));
        list.add(new FormDefinition(43, "salary-account-opening", "Salary Account Opening Form", "Manager Forms",
                List.of("Employee Name", "Employer Name", "Salary Account Number", "Corporate Code", "Documents")));
        list.add(new FormDefinition(44, "corporate-salary-upload", "Corporate Salary Upload Form", "Manager Forms",
                List.of("Corporate ID", "Salary Month", "Employee Count", "Total Amount", "Upload File")));
        list.add(new FormDefinition(45, "customer-account-management", "Customer Account Management Form", "Manager Forms",
                List.of("Account Number", "Action Type", "Status Change", "Manager ID", "Remarks")));

        list.add(new FormDefinition(46, "admin-login", "Admin Login Form", "Admin Forms",
                List.of("Admin ID", "Password", "OTP", "Login Timestamp")));
        list.add(new FormDefinition(47, "create-admin", "Create Admin Form", "Admin Forms",
                List.of("Admin Name", "Email", "Role", "Branch", "Permissions")));
        list.add(new FormDefinition(48, "employee-registration", "Employee Registration Form", "Admin Forms",
                List.of("Employee Name", "Employee ID", "Department", "Branch", "Join Date")));
        list.add(new FormDefinition(49, "branch-creation", "Branch Creation Form", "Admin Forms",
                List.of("Branch Name", "Branch Code", "Address", "IFSC", "Manager Name")));
        list.add(new FormDefinition(50, "user-management", "User Management Form", "Admin Forms",
                List.of("User ID", "Account Number", "Action", "Access Level", "Admin Remarks")));
        list.add(new FormDefinition(51, "loan-management", "Loan Management Form", "Admin Forms",
                List.of("Loan ID", "Account Number", "Loan Status", "Action", "Remarks")));
        list.add(new FormDefinition(52, "fraud-monitoring", "Fraud Monitoring Form", "Admin Forms",
                List.of("Alert ID", "Account Number", "Risk Level", "Investigation Status", "Action Taken")));
        list.add(new FormDefinition(53, "transaction-monitoring", "Transaction Monitoring Form", "Admin Forms",
                List.of("Transaction ID", "Account Number", "Amount", "Flag Reason", "Review Status")));
        list.add(new FormDefinition(54, "bank-charges-management", "Bank Charges Management Form", "Admin Forms",
                List.of("Charge Type", "Account Category", "Amount/Fee", "Effective Date", "Remarks")));

        list.add(new FormDefinition(55, "fraud-detection-review", "Fraud Detection Review Form", "AI/Advanced Forms",
                List.of("Alert ID", "Account Number", "AI Risk Score", "Review Decision", "Analyst Remarks")));
        list.add(new FormDefinition(56, "loan-prediction-assessment", "Loan Prediction Assessment Form", "AI/Advanced Forms",
                List.of("Application ID", "Predicted Default Risk", "Income Score", "Recommendation", "Override Reason")));
        list.add(new FormDefinition(57, "risk-analysis", "Risk Analysis Form", "AI/Advanced Forms",
                List.of("Account Number", "Risk Category", "Exposure Amount", "Mitigation Plan", "Review Date")));
        list.add(new FormDefinition(58, "suspicious-transaction-review", "Suspicious Transaction Review Form", "AI/Advanced Forms",
                List.of("Transaction ID", "Account Number", "Suspicion Reason", "AI Confidence", "Final Decision")));
        list.add(new FormDefinition(59, "account-freeze-unfreeze", "Account Freeze / Unfreeze Form", "AI/Advanced Forms",
                List.of("Account Number", "Action (Freeze/Unfreeze)", "Reason", "Authorized By", "Effective Date")));
        list.add(new FormDefinition(60, "emergency-e-lock", "Emergency E-Lock Form", "AI/Advanced Forms",
                List.of("Account Number", "Lock Scope", "Trigger Reason", "Customer Consent", "Release Conditions")));

        return Collections.unmodifiableList(list);
    }

    public static List<FormDefinition> all() {
        return FORMS;
    }

    public static Optional<FormDefinition> findByCode(String code) {
        if (code == null) return Optional.empty();
        return FORMS.stream().filter(f -> f.code().equalsIgnoreCase(code.trim())).findFirst();
    }

    public static List<String> categories() {
        LinkedHashSet<String> cats = new LinkedHashSet<>();
        for (FormDefinition f : FORMS) {
            cats.add(f.category());
        }
        return new ArrayList<>(cats);
    }
}
