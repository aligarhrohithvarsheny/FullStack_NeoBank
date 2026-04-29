package com.neo.springapp.service;

import com.neo.springapp.model.CurrentAccount;
import com.neo.springapp.model.BusinessTransaction;
import com.neo.springapp.model.CurrentAccountEditHistory;
import com.neo.springapp.model.CurrentAccountBeneficiary;
import com.neo.springapp.model.CurrentAccountChequeRequest;
import com.neo.springapp.model.CurrentAccountVendorPayment;
import com.neo.springapp.model.CurrentAccountInvoice;
import com.neo.springapp.model.CurrentAccountBusinessLoan;
import com.neo.springapp.model.CurrentAccountBusinessUser;
import com.neo.springapp.model.LinkedAccount;
import com.neo.springapp.model.Account;
import com.neo.springapp.model.SalaryAccount;
import com.neo.springapp.repository.CurrentAccountRepository;
import com.neo.springapp.repository.LinkedAccountRepository;
import com.neo.springapp.repository.AccountRepository;
import com.neo.springapp.repository.SalaryAccountRepository;
import com.neo.springapp.repository.BusinessTransactionRepository;
import com.neo.springapp.repository.CurrentAccountEditHistoryRepository;
import com.neo.springapp.repository.CurrentAccountBeneficiaryRepository;
import com.neo.springapp.repository.CurrentAccountChequeRequestRepository;
import com.neo.springapp.repository.CurrentAccountVendorPaymentRepository;
import com.neo.springapp.repository.CurrentAccountInvoiceRepository;
import com.neo.springapp.repository.CurrentAccountBusinessLoanRepository;
import com.neo.springapp.repository.CurrentAccountBusinessUserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@SuppressWarnings("null")
public class CurrentAccountService {

    private final CurrentAccountRepository accountRepository;
    private final BusinessTransactionRepository transactionRepository;
    private final CurrentAccountEditHistoryRepository editHistoryRepository;
    private final CurrentAccountBeneficiaryRepository beneficiaryRepository;
    private final CurrentAccountChequeRequestRepository chequeRequestRepository;
    private final CurrentAccountVendorPaymentRepository vendorPaymentRepository;
    private final CurrentAccountInvoiceRepository invoiceRepository;
    private final CurrentAccountBusinessLoanRepository businessLoanRepository;
    private final CurrentAccountBusinessUserRepository businessUserRepository;
    private final LinkedAccountRepository linkedAccountRepository;
    private final AccountRepository savingsAccountRepository;
    private final SalaryAccountRepository salaryAccountRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);

    public CurrentAccountService(CurrentAccountRepository accountRepository,
                                  BusinessTransactionRepository transactionRepository,
                                  CurrentAccountEditHistoryRepository editHistoryRepository,
                                  CurrentAccountBeneficiaryRepository beneficiaryRepository,
                                  CurrentAccountChequeRequestRepository chequeRequestRepository,
                                  CurrentAccountVendorPaymentRepository vendorPaymentRepository,
                                  CurrentAccountInvoiceRepository invoiceRepository,
                                  CurrentAccountBusinessLoanRepository businessLoanRepository,
                                  CurrentAccountBusinessUserRepository businessUserRepository,
                                  LinkedAccountRepository linkedAccountRepository,
                                  AccountRepository savingsAccountRepository,
                                  SalaryAccountRepository salaryAccountRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.editHistoryRepository = editHistoryRepository;
        this.beneficiaryRepository = beneficiaryRepository;
        this.chequeRequestRepository = chequeRequestRepository;
        this.vendorPaymentRepository = vendorPaymentRepository;
        this.invoiceRepository = invoiceRepository;
        this.businessLoanRepository = businessLoanRepository;
        this.businessUserRepository = businessUserRepository;
        this.linkedAccountRepository = linkedAccountRepository;
        this.savingsAccountRepository = savingsAccountRepository;
        this.salaryAccountRepository = salaryAccountRepository;
    }

    // ==================== Account CRUD ====================

    public CurrentAccount createAccount(CurrentAccount account) {
        return accountRepository.save(account);
    }

    public Optional<CurrentAccount> getAccountById(Long id) {
        return accountRepository.findById(id);
    }

    public Optional<CurrentAccount> getAccountByNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber);
    }

    public Optional<CurrentAccount> getAccountByCustomerId(String customerId) {
        return accountRepository.findByCustomerId(customerId);
    }

    public List<CurrentAccount> getAllAccounts() {
        return accountRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    public Page<CurrentAccount> getAllAccountsPaginated(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return accountRepository.findAll(pageable);
    }

    public List<CurrentAccount> getAccountsByStatus(String status) {
        return accountRepository.findByStatus(status);
    }

    public Page<CurrentAccount> searchAccounts(String searchTerm, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return accountRepository.searchAccounts(searchTerm, pageable);
    }

    // ==================== Account Operations ====================

    @Transactional
    public CurrentAccount approveAccount(Long id, String approvedBy) {
        CurrentAccount account = accountRepository.findById(id).orElse(null);
        if (account != null && "PENDING".equals(account.getStatus())) {
            account.setStatus("ACTIVE");
            account.setApprovedAt(LocalDateTime.now());
            account.setApprovedBy(approvedBy);
            return accountRepository.save(account);
        }
        return null;
    }

    @Transactional
    public CurrentAccount rejectAccount(Long id) {
        CurrentAccount account = accountRepository.findById(id).orElse(null);
        if (account != null && "PENDING".equals(account.getStatus())) {
            account.setStatus("REJECTED");
            return accountRepository.save(account);
        }
        return null;
    }

    @Transactional
    public CurrentAccount verifyKyc(Long id, String verifiedBy) {
        CurrentAccount account = accountRepository.findById(id).orElse(null);
        if (account != null) {
            account.setKycVerified(true);
            account.setKycVerifiedDate(LocalDateTime.now());
            account.setKycVerifiedBy(verifiedBy);
            return accountRepository.save(account);
        }
        return null;
    }

    @Transactional
    public CurrentAccount freezeAccount(Long id, String reason, String frozenBy) {
        CurrentAccount account = accountRepository.findById(id).orElse(null);
        if (account != null && !account.getAccountFrozen()) {
            account.setAccountFrozen(true);
            account.setFrozenReason(reason);
            account.setFrozenBy(frozenBy);
            account.setFrozenDate(LocalDateTime.now());
            account.setStatus("FROZEN");
            return accountRepository.save(account);
        }
        return null;
    }

    @Transactional
    public CurrentAccount unfreezeAccount(Long id) {
        CurrentAccount account = accountRepository.findById(id).orElse(null);
        if (account != null && account.getAccountFrozen()) {
            account.setAccountFrozen(false);
            account.setFrozenReason(null);
            account.setFrozenBy(null);
            account.setFrozenDate(null);
            account.setStatus("ACTIVE");
            return accountRepository.save(account);
        }
        return null;
    }

    @Transactional
    public CurrentAccount approveOverdraft(Long id, Double overdraftLimit) {
        CurrentAccount account = accountRepository.findById(id).orElse(null);
        if (account != null && "ACTIVE".equals(account.getStatus())) {
            account.setOverdraftEnabled(true);
            account.setOverdraftLimit(overdraftLimit);
            return accountRepository.save(account);
        }
        return null;
    }

    public CurrentAccount updateAccount(Long id, CurrentAccount details) {
        CurrentAccount account = accountRepository.findById(id).orElse(null);
        if (account != null) {
            if (details.getBusinessName() != null) account.setBusinessName(details.getBusinessName());
            if (details.getBusinessType() != null) account.setBusinessType(details.getBusinessType());
            if (details.getBusinessRegistrationNumber() != null) account.setBusinessRegistrationNumber(details.getBusinessRegistrationNumber());
            if (details.getGstNumber() != null) account.setGstNumber(details.getGstNumber());
            if (details.getOwnerName() != null) account.setOwnerName(details.getOwnerName());
            if (details.getMobile() != null) account.setMobile(details.getMobile());
            if (details.getEmail() != null) account.setEmail(details.getEmail());
            if (details.getShopAddress() != null) account.setShopAddress(details.getShopAddress());
            if (details.getCity() != null) account.setCity(details.getCity());
            if (details.getState() != null) account.setState(details.getState());
            if (details.getPincode() != null) account.setPincode(details.getPincode());
            return accountRepository.save(account);
        }
        return null;
    }

    // ==================== Validation ====================

    public boolean isGstUnique(String gstNumber) {
        return !accountRepository.existsByGstNumber(gstNumber);
    }

    public boolean isPanUnique(String panNumber) {
        return !accountRepository.existsByPanNumber(panNumber);
    }

    public boolean isMobileUnique(String mobile) {
        return !accountRepository.existsByMobile(mobile);
    }

    public boolean isEmailUnique(String email) {
        return !accountRepository.existsByEmail(email);
    }

    // ==================== Statistics ====================

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalAccounts", accountRepository.count());
        stats.put("pendingAccounts", accountRepository.countByStatus("PENDING"));
        stats.put("activeAccounts", accountRepository.countByStatus("ACTIVE"));
        stats.put("frozenAccounts", accountRepository.countByStatus("FROZEN"));
        stats.put("totalActiveBalance", accountRepository.getTotalActiveBalance());
        stats.put("overdrawnAccounts", accountRepository.findOverdrawnAccounts().size());
        return stats;
    }

    // ==================== Transactions ====================

    @Transactional
    public BusinessTransaction processTransaction(String accountNumber, String txnType, Double amount, String description) {
        CurrentAccount account = accountRepository.findByAccountNumber(accountNumber).orElse(null);
        if (account == null || account.getAccountFrozen() || !"ACTIVE".equals(account.getStatus())) {
            return null;
        }

        Double currentBalance = account.getBalance();

        if ("Debit".equals(txnType)) {
            Double effectiveLimit = account.getOverdraftEnabled() ?
                    currentBalance + account.getOverdraftLimit() : currentBalance;
            if (amount > effectiveLimit) {
                return null; // Insufficient funds
            }
            account.setBalance(currentBalance - amount);
        } else if ("Credit".equals(txnType)) {
            account.setBalance(currentBalance + amount);
        }

        accountRepository.save(account);

        BusinessTransaction txn = new BusinessTransaction();
        txn.setAccountNumber(accountNumber);
        txn.setTxnType(txnType);
        txn.setAmount(amount);
        txn.setDescription(description);
        txn.setBalance(account.getBalance());
        txn.setStatus("Completed");

        return transactionRepository.save(txn);
    }

    // ==================== Verify Recipient Account ====================
    public Map<String, Object> verifyRecipientAccount(String accountNumber) {
        Map<String, Object> result = new HashMap<>();

        // Check current accounts
        Optional<CurrentAccount> currentOpt = accountRepository.findByAccountNumber(accountNumber);
        if (currentOpt.isPresent()) {
            CurrentAccount ca = currentOpt.get();
            result.put("found", true);
            result.put("name", ca.getOwnerName());
            result.put("accountType", "Current Account");
            result.put("bankName", "NEO BANK");
            result.put("ifscCode", ca.getIfscCode() != null ? ca.getIfscCode() : "NEOB0001234");
            result.put("businessName", ca.getBusinessName());
            return result;
        }

        // Check savings accounts
        Account acc = savingsAccountRepository.findByAccountNumber(accountNumber);
        if (acc != null) {
            result.put("found", true);
            result.put("name", acc.getName());
            result.put("accountType", "Savings Account");
            result.put("bankName", "NEO BANK");
            result.put("ifscCode", "NEOB0001234");
            return result;
        }

        // Check salary accounts
        SalaryAccount sal = salaryAccountRepository.findByAccountNumber(accountNumber);
        if (sal != null) {
            result.put("found", true);
            result.put("name", sal.getEmployeeName());
            result.put("accountType", "Salary Account");
            result.put("bankName", "NEO BANK");
            result.put("ifscCode", sal.getIfscCode() != null ? sal.getIfscCode() : "NEOB0001234");
            return result;
        }

        result.put("found", false);
        result.put("message", "Account not found in NEO BANK. You can still transfer to external accounts.");
        return result;
    }

    @Transactional
    public BusinessTransaction processTransfer(String fromAccount, String toAccount, String recipientName,
                                                Double amount, String transferType, String description) {
        CurrentAccount sender = accountRepository.findByAccountNumber(fromAccount).orElse(null);
        if (sender == null || sender.getAccountFrozen() || !"ACTIVE".equals(sender.getStatus())) {
            return null;
        }

        // Calculate transfer charges
        double charge = 0.0;
        if ("RTGS".equals(transferType)) charge = 10.0;
        else if ("NEFT".equals(transferType)) charge = 5.0;
        double totalDebit = amount + charge;

        Double effectiveLimit = sender.getOverdraftEnabled() ?
                sender.getBalance() + sender.getOverdraftLimit() : sender.getBalance();
        if (totalDebit > effectiveLimit) {
            return null;
        }

        sender.setBalance(sender.getBalance() - totalDebit);
        accountRepository.save(sender);

        // Credit to recipient if they have a current account
        Optional<CurrentAccount> recipient = accountRepository.findByAccountNumber(toAccount);
        if (recipient.isPresent()) {
            CurrentAccount recipientAccount = recipient.get();
            recipientAccount.setBalance(recipientAccount.getBalance() + amount);
            accountRepository.save(recipientAccount);
        }

        BusinessTransaction txn = new BusinessTransaction();
        txn.setAccountNumber(fromAccount);
        txn.setTxnType("Transfer");
        txn.setAmount(amount);
        txn.setDescription(description != null ? description : transferType + " Transfer to " + recipientName);
        txn.setBalance(sender.getBalance());
        txn.setRecipientAccount(toAccount);
        txn.setRecipientName(recipientName);
        txn.setTransferType(transferType);
        txn.setChargeAmount(charge);
        txn.setStatus("Completed");

        return transactionRepository.save(txn);
    }

    public List<BusinessTransaction> getTransactions(String accountNumber) {
        return transactionRepository.findByAccountNumber(accountNumber);
    }

    public Page<BusinessTransaction> getTransactionsPaginated(String accountNumber, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return transactionRepository.findByAccountNumberOrderByDateDesc(accountNumber, pageable);
    }

    public List<BusinessTransaction> getTransactionsByDateRange(String accountNumber,
                                                                 LocalDateTime startDate, LocalDateTime endDate) {
        return transactionRepository.findByAccountNumberAndDateRange(accountNumber, startDate, endDate);
    }

    public Map<String, Object> getTransactionSummary(String accountNumber) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalCredits", transactionRepository.getTotalCredits(accountNumber));
        summary.put("totalDebits", transactionRepository.getTotalDebits(accountNumber));
        summary.put("todayTransactions", transactionRepository.getTodayTransactionCount(accountNumber));
        return summary;
    }

    public List<BusinessTransaction> getRecentTransactions(String accountNumber, int count) {
        Pageable pageable = PageRequest.of(0, count);
        return transactionRepository.findRecentTransactions(accountNumber, pageable);
    }

    public Page<BusinessTransaction> searchTransactions(String searchTerm, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "date"));
        return transactionRepository.searchTransactions(searchTerm, pageable);
    }

    // ==================== Edit with History ====================

    @Transactional
    public CurrentAccount updateAccountWithHistory(Long id, CurrentAccount details, String editedBy, String documentPath, String documentName) {
        CurrentAccount account = accountRepository.findById(id).orElse(null);
        if (account == null) return null;

        StringBuilder changes = new StringBuilder();
        StringBuilder fieldChangesJson = new StringBuilder("{");
        boolean first = true;

        if (details.getBusinessName() != null && !details.getBusinessName().equals(account.getBusinessName())) {
            changes.append("Business Name: '").append(account.getBusinessName()).append("' → '").append(details.getBusinessName()).append("'; ");
            if (!first) fieldChangesJson.append(","); first = false;
            fieldChangesJson.append("\"businessName\":{\"old\":\"").append(escapeJson(account.getBusinessName())).append("\",\"new\":\"").append(escapeJson(details.getBusinessName())).append("\"}");
            account.setBusinessName(details.getBusinessName());
        }
        if (details.getBusinessType() != null && !details.getBusinessType().equals(account.getBusinessType())) {
            changes.append("Business Type: '").append(account.getBusinessType()).append("' → '").append(details.getBusinessType()).append("'; ");
            if (!first) fieldChangesJson.append(","); first = false;
            fieldChangesJson.append("\"businessType\":{\"old\":\"").append(escapeJson(account.getBusinessType())).append("\",\"new\":\"").append(escapeJson(details.getBusinessType())).append("\"}");
            account.setBusinessType(details.getBusinessType());
        }
        if (details.getBusinessRegistrationNumber() != null && !details.getBusinessRegistrationNumber().equals(account.getBusinessRegistrationNumber())) {
            changes.append("Reg No: '").append(account.getBusinessRegistrationNumber()).append("' → '").append(details.getBusinessRegistrationNumber()).append("'; ");
            if (!first) fieldChangesJson.append(","); first = false;
            fieldChangesJson.append("\"businessRegistrationNumber\":{\"old\":\"").append(escapeJson(account.getBusinessRegistrationNumber())).append("\",\"new\":\"").append(escapeJson(details.getBusinessRegistrationNumber())).append("\"}");
            account.setBusinessRegistrationNumber(details.getBusinessRegistrationNumber());
        }
        if (details.getGstNumber() != null && !details.getGstNumber().equals(account.getGstNumber())) {
            changes.append("GST: '").append(account.getGstNumber()).append("' → '").append(details.getGstNumber()).append("'; ");
            if (!first) fieldChangesJson.append(","); first = false;
            fieldChangesJson.append("\"gstNumber\":{\"old\":\"").append(escapeJson(account.getGstNumber())).append("\",\"new\":\"").append(escapeJson(details.getGstNumber())).append("\"}");
            account.setGstNumber(details.getGstNumber());
        }
        if (details.getOwnerName() != null && !details.getOwnerName().equals(account.getOwnerName())) {
            changes.append("Owner Name: '").append(account.getOwnerName()).append("' → '").append(details.getOwnerName()).append("'; ");
            if (!first) fieldChangesJson.append(","); first = false;
            fieldChangesJson.append("\"ownerName\":{\"old\":\"").append(escapeJson(account.getOwnerName())).append("\",\"new\":\"").append(escapeJson(details.getOwnerName())).append("\"}");
            account.setOwnerName(details.getOwnerName());
        }
        if (details.getMobile() != null && !details.getMobile().equals(account.getMobile())) {
            changes.append("Mobile: '").append(account.getMobile()).append("' → '").append(details.getMobile()).append("'; ");
            if (!first) fieldChangesJson.append(","); first = false;
            fieldChangesJson.append("\"mobile\":{\"old\":\"").append(escapeJson(account.getMobile())).append("\",\"new\":\"").append(escapeJson(details.getMobile())).append("\"}");
            account.setMobile(details.getMobile());
        }
        if (details.getEmail() != null && !details.getEmail().equals(account.getEmail())) {
            changes.append("Email: '").append(account.getEmail()).append("' → '").append(details.getEmail()).append("'; ");
            if (!first) fieldChangesJson.append(","); first = false;
            fieldChangesJson.append("\"email\":{\"old\":\"").append(escapeJson(account.getEmail())).append("\",\"new\":\"").append(escapeJson(details.getEmail())).append("\"}");
            account.setEmail(details.getEmail());
        }
        if (details.getAadharNumber() != null && !details.getAadharNumber().equals(account.getAadharNumber())) {
            changes.append("Aadhaar: '").append(account.getAadharNumber()).append("' → '").append(details.getAadharNumber()).append("'; ");
            if (!first) fieldChangesJson.append(","); first = false;
            fieldChangesJson.append("\"aadharNumber\":{\"old\":\"").append(escapeJson(account.getAadharNumber())).append("\",\"new\":\"").append(escapeJson(details.getAadharNumber())).append("\"}");
            account.setAadharNumber(details.getAadharNumber());
        }
        if (details.getPanNumber() != null && !details.getPanNumber().equals(account.getPanNumber())) {
            changes.append("PAN: '").append(account.getPanNumber()).append("' → '").append(details.getPanNumber()).append("'; ");
            if (!first) fieldChangesJson.append(","); first = false;
            fieldChangesJson.append("\"panNumber\":{\"old\":\"").append(escapeJson(account.getPanNumber())).append("\",\"new\":\"").append(escapeJson(details.getPanNumber())).append("\"}");
            account.setPanNumber(details.getPanNumber());
        }
        if (details.getShopAddress() != null && !details.getShopAddress().equals(account.getShopAddress())) {
            changes.append("Address: '").append(account.getShopAddress()).append("' → '").append(details.getShopAddress()).append("'; ");
            if (!first) fieldChangesJson.append(","); first = false;
            fieldChangesJson.append("\"shopAddress\":{\"old\":\"").append(escapeJson(account.getShopAddress())).append("\",\"new\":\"").append(escapeJson(details.getShopAddress())).append("\"}");
            account.setShopAddress(details.getShopAddress());
        }
        if (details.getCity() != null && !details.getCity().equals(account.getCity())) {
            changes.append("City: '").append(account.getCity()).append("' → '").append(details.getCity()).append("'; ");
            if (!first) fieldChangesJson.append(","); first = false;
            fieldChangesJson.append("\"city\":{\"old\":\"").append(escapeJson(account.getCity())).append("\",\"new\":\"").append(escapeJson(details.getCity())).append("\"}");
            account.setCity(details.getCity());
        }
        if (details.getState() != null && !details.getState().equals(account.getState())) {
            changes.append("State: '").append(account.getState()).append("' → '").append(details.getState()).append("'; ");
            if (!first) fieldChangesJson.append(","); first = false;
            fieldChangesJson.append("\"state\":{\"old\":\"").append(escapeJson(account.getState())).append("\",\"new\":\"").append(escapeJson(details.getState())).append("\"}");
            account.setState(details.getState());
        }
        if (details.getPincode() != null && !details.getPincode().equals(account.getPincode())) {
            changes.append("Pincode: '").append(account.getPincode()).append("' → '").append(details.getPincode()).append("'; ");
            if (!first) fieldChangesJson.append(","); first = false;
            fieldChangesJson.append("\"pincode\":{\"old\":\"").append(escapeJson(account.getPincode())).append("\",\"new\":\"").append(escapeJson(details.getPincode())).append("\"}");
            account.setPincode(details.getPincode());
        }
        if (details.getBranchName() != null && !details.getBranchName().equals(account.getBranchName())) {
            changes.append("Branch: '").append(account.getBranchName()).append("' → '").append(details.getBranchName()).append("'; ");
            if (!first) fieldChangesJson.append(","); first = false;
            fieldChangesJson.append("\"branchName\":{\"old\":\"").append(escapeJson(account.getBranchName())).append("\",\"new\":\"").append(escapeJson(details.getBranchName())).append("\"}");
            account.setBranchName(details.getBranchName());
        }
        if (details.getIfscCode() != null && !details.getIfscCode().equals(account.getIfscCode())) {
            changes.append("IFSC: '").append(account.getIfscCode()).append("' → '").append(details.getIfscCode()).append("'; ");
            if (!first) fieldChangesJson.append(","); first = false;
            fieldChangesJson.append("\"ifscCode\":{\"old\":\"").append(escapeJson(account.getIfscCode())).append("\",\"new\":\"").append(escapeJson(details.getIfscCode())).append("\"}");
            account.setIfscCode(details.getIfscCode());
        }

        fieldChangesJson.append("}");

        if (changes.length() == 0) {
            return account; // No changes detected
        }

        accountRepository.save(account);

        // Save edit history
        CurrentAccountEditHistory history = new CurrentAccountEditHistory();
        history.setAccountId(account.getId());
        history.setAccountNumber(account.getAccountNumber());
        history.setEditedBy(editedBy);
        history.setEditedAt(LocalDateTime.now());
        history.setChangesDescription(changes.toString());
        history.setFieldChanges(fieldChangesJson.toString());
        history.setDocumentPath(documentPath);
        history.setDocumentName(documentName);
        editHistoryRepository.save(history);

        return account;
    }

    public List<CurrentAccountEditHistory> getEditHistory(Long accountId) {
        return editHistoryRepository.findByAccountIdOrderByEditedAtDesc(accountId);
    }

    public List<CurrentAccountEditHistory> getEditHistoryByAccountNumber(String accountNumber) {
        return editHistoryRepository.findByAccountNumberOrderByEditedAtDesc(accountNumber);
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // ==================== User Dashboard Login ====================

    public CurrentAccount loginByAccountAndMobile(String accountNumber, String mobile) {
        Optional<CurrentAccount> account = accountRepository.findByAccountNumber(accountNumber);
        if (account.isPresent() && account.get().getMobile().equals(mobile)
                && "ACTIVE".equals(account.get().getStatus())) {
            return account.get();
        }
        return null;
    }

    // ==================== Beneficiary Management ====================

    public CurrentAccountBeneficiary addBeneficiary(CurrentAccountBeneficiary beneficiary) {
        return beneficiaryRepository.save(beneficiary);
    }

    public List<CurrentAccountBeneficiary> getBeneficiaries(String accountNumber) {
        return beneficiaryRepository.findByAccountNumber(accountNumber);
    }

    public List<CurrentAccountBeneficiary> getActiveBeneficiaries(String accountNumber) {
        return beneficiaryRepository.findByAccountNumberAndStatus(accountNumber, "ACTIVE");
    }

    public CurrentAccountBeneficiary deleteBeneficiary(Long id) {
        CurrentAccountBeneficiary b = beneficiaryRepository.findById(id).orElse(null);
        if (b != null) {
            b.setStatus("DELETED");
            return beneficiaryRepository.save(b);
        }
        return null;
    }

    // ==================== Cheque Book Requests ====================

    public CurrentAccountChequeRequest requestChequeBook(CurrentAccountChequeRequest request) {
        return chequeRequestRepository.save(request);
    }

    public List<CurrentAccountChequeRequest> getChequeRequests(String accountNumber) {
        return chequeRequestRepository.findByAccountNumberOrderByRequestedAtDesc(accountNumber);
    }

    public List<CurrentAccountChequeRequest> getPendingChequeRequests() {
        return chequeRequestRepository.findByStatus("PENDING");
    }

    @Transactional
    public CurrentAccountChequeRequest approveChequeRequest(Long id, String approvedBy) {
        CurrentAccountChequeRequest req = chequeRequestRepository.findById(id).orElse(null);
        if (req != null && "PENDING".equals(req.getStatus())) {
            req.setStatus("APPROVED");
            req.setApprovedAt(LocalDateTime.now());
            req.setApprovedBy(approvedBy);
            return chequeRequestRepository.save(req);
        }
        return null;
    }

    // ==================== Vendor Payments ====================

    @Transactional
    public CurrentAccountVendorPayment processVendorPayment(CurrentAccountVendorPayment payment) {
        CurrentAccount account = accountRepository.findByAccountNumber(payment.getAccountNumber()).orElse(null);
        if (account == null || account.getAccountFrozen() || !"ACTIVE".equals(account.getStatus())) {
            return null;
        }

        Double effectiveLimit = account.getOverdraftEnabled() ?
                account.getBalance() + account.getOverdraftLimit() : account.getBalance();
        if (payment.getAmount() > effectiveLimit) {
            return null;
        }

        account.setBalance(account.getBalance() - payment.getAmount());
        accountRepository.save(account);

        payment.setStatus("PAID");
        payment.setPaidAt(LocalDateTime.now());
        CurrentAccountVendorPayment saved = vendorPaymentRepository.save(payment);

        // Also record as business transaction
        BusinessTransaction txn = new BusinessTransaction();
        txn.setAccountNumber(payment.getAccountNumber());
        txn.setTxnType("Vendor Payment");
        txn.setAmount(payment.getAmount());
        txn.setDescription("Vendor Payment to " + payment.getVendorName());
        txn.setBalance(account.getBalance());
        txn.setRecipientAccount(payment.getVendorAccount());
        txn.setRecipientName(payment.getVendorName());
        txn.setStatus("Completed");
        transactionRepository.save(txn);

        return saved;
    }

    public List<CurrentAccountVendorPayment> getVendorPayments(String accountNumber) {
        return vendorPaymentRepository.findByAccountNumberOrderByCreatedAtDesc(accountNumber);
    }

    // ==================== Dashboard Stats for User ====================

    public Map<String, Object> getUserDashboardStats(String accountNumber) {
        Map<String, Object> stats = new HashMap<>();
        Optional<CurrentAccount> accountOpt = accountRepository.findByAccountNumber(accountNumber);
        if (accountOpt.isPresent()) {
            CurrentAccount account = accountOpt.get();
            stats.put("balance", account.getBalance());
            stats.put("overdraftLimit", account.getOverdraftLimit());
            stats.put("overdraftEnabled", account.getOverdraftEnabled());

            Double totalCredits = transactionRepository.getTotalCredits(accountNumber);
            Double totalDebits = transactionRepository.getTotalDebits(accountNumber);
            Long todayTxns = transactionRepository.getTodayTransactionCount(accountNumber);

            stats.put("totalCredits", totalCredits != null ? totalCredits : 0.0);
            stats.put("totalDebits", totalDebits != null ? totalDebits : 0.0);
            stats.put("todayTransactions", todayTxns != null ? todayTxns : 0);
        }
        return stats;
    }

    // ==================== Update Business Profile ====================

    @Transactional
    public CurrentAccount updateBusinessProfile(String accountNumber, Map<String, String> updates) {
        CurrentAccount account = accountRepository.findByAccountNumber(accountNumber).orElse(null);
        if (account == null) return null;

        if (updates.containsKey("shopAddress")) account.setShopAddress(updates.get("shopAddress"));
        if (updates.containsKey("city")) account.setCity(updates.get("city"));
        if (updates.containsKey("state")) account.setState(updates.get("state"));
        if (updates.containsKey("pincode")) account.setPincode(updates.get("pincode"));
        if (updates.containsKey("mobile")) account.setMobile(updates.get("mobile"));
        if (updates.containsKey("email")) account.setEmail(updates.get("email"));

        return accountRepository.save(account);
    }

    // ==================== Authentication ====================

    public Map<String, Object> signup(String customerId, String password) {
        Map<String, Object> result = new HashMap<>();
        Optional<CurrentAccount> opt = accountRepository.findByCustomerId(customerId);
        if (opt.isEmpty()) {
            result.put("success", false);
            result.put("message", "No current account found with this Customer ID");
            return result;
        }
        CurrentAccount account = opt.get();
        if (!"ACTIVE".equals(account.getStatus()) && !"APPROVED".equals(account.getStatus())) {
            result.put("success", false);
            result.put("message", "Account is not active. Current status: " + account.getStatus());
            return result;
        }
        if (Boolean.TRUE.equals(account.getPasswordSet())) {
            result.put("success", false);
            result.put("message", "Password already set. Please login with your account number.");
            return result;
        }
        account.setPassword(passwordEncoder.encode(password));
        account.setPasswordSet(true);
        account.setLastUpdated(LocalDateTime.now());
        accountRepository.save(account);

        result.put("success", true);
        result.put("message", "Password set successfully. You can now login.");
        result.put("accountNumber", account.getAccountNumber());
        result.put("ownerName", account.getOwnerName());
        return result;
    }

    public Map<String, Object> authenticate(String accountNumber, String password) {
        Map<String, Object> result = new HashMap<>();
        Optional<CurrentAccount> opt = accountRepository.findByAccountNumber(accountNumber);
        if (opt.isEmpty()) {
            result.put("success", false);
            result.put("message", "No current account found with this account number");
            return result;
        }
        CurrentAccount account = opt.get();
        if (!Boolean.TRUE.equals(account.getPasswordSet()) || account.getPassword() == null) {
            result.put("success", false);
            result.put("message", "Password not set. Please sign up first using your Customer ID.");
            return result;
        }
        if (!"ACTIVE".equals(account.getStatus()) && !"APPROVED".equals(account.getStatus())) {
            result.put("success", false);
            result.put("message", "Account is not active. Current status: " + account.getStatus());
            return result;
        }
        if (!passwordEncoder.matches(password, account.getPassword())) {
            result.put("success", false);
            result.put("message", "Invalid password");
            return result;
        }

        result.put("success", true);
        result.put("message", "Login successful");
        result.put("account", account);
        return result;
    }

    @Transactional
    public Map<String, Object> changePassword(String accountNumber, String currentPassword, String newPassword) {
        Map<String, Object> result = new HashMap<>();
        Optional<CurrentAccount> opt = accountRepository.findByAccountNumber(accountNumber);
        if (opt.isEmpty()) {
            result.put("success", false);
            result.put("message", "Account not found");
            return result;
        }
        CurrentAccount account = opt.get();
        if (!Boolean.TRUE.equals(account.getPasswordSet()) || account.getPassword() == null) {
            result.put("success", false);
            result.put("message", "No password set for this account");
            return result;
        }
        if (!passwordEncoder.matches(currentPassword, account.getPassword())) {
            result.put("success", false);
            result.put("message", "Current password is incorrect");
            return result;
        }
        if (newPassword == null || newPassword.length() < 6) {
            result.put("success", false);
            result.put("message", "New password must be at least 6 characters");
            return result;
        }
        account.setPassword(passwordEncoder.encode(newPassword));
        account.setLastUpdated(LocalDateTime.now());
        accountRepository.save(account);
        result.put("success", true);
        result.put("message", "Password changed successfully");
        return result;
    }

    // ==================== Invoice Management ====================

    @Transactional
    public CurrentAccountInvoice createInvoice(CurrentAccountInvoice invoice) {
        invoice.setInvoiceNumber("INV-" + System.currentTimeMillis());
        return invoiceRepository.save(invoice);
    }

    public List<CurrentAccountInvoice> getInvoices(String accountNumber) {
        return invoiceRepository.findByAccountNumberOrderByCreatedAtDesc(accountNumber);
    }

    public Optional<CurrentAccountInvoice> getInvoiceByNumber(String invoiceNumber) {
        return invoiceRepository.findByInvoiceNumber(invoiceNumber);
    }

    @Transactional
    public CurrentAccountInvoice updateInvoiceStatus(Long id, String status) {
        CurrentAccountInvoice invoice = invoiceRepository.findById(id).orElse(null);
        if (invoice == null) return null;
        invoice.setStatus(status);
        return invoiceRepository.save(invoice);
    }

    @Transactional
    public CurrentAccountInvoice markInvoicePaid(Long id, Double paidAmount) {
        CurrentAccountInvoice invoice = invoiceRepository.findById(id).orElse(null);
        if (invoice == null) return null;
        invoice.setPaidAmount(paidAmount);
        if (paidAmount >= invoice.getTotalAmount()) {
            invoice.setStatus("PAID");
        }
        return invoiceRepository.save(invoice);
    }

    public void deleteInvoice(Long id) {
        invoiceRepository.deleteById(id);
    }

    // ==================== Business Loan Application (ML CIBIL) ====================

    @Transactional
    public Map<String, Object> applyForBusinessLoan(CurrentAccountBusinessLoan loan) {
        Map<String, Object> result = new HashMap<>();

        // Validate loan type limits
        double maxLimit = "Working Capital".equals(loan.getLoanType()) ? 500000.0 : 1000000.0;
        if (loan.getRequestedAmount() > maxLimit) {
            result.put("success", false);
            result.put("message", "Maximum loan amount for " + loan.getLoanType() + " is ₹" + String.format("%.0f", maxLimit));
            return result;
        }

        // Check active loans
        long activeLoans = businessLoanRepository.countByAccountNumberAndStatusIn(
            loan.getAccountNumber(), List.of("PENDING", "APPROVED", "DISBURSED")
        );
        if (activeLoans >= 3) {
            result.put("success", false);
            result.put("message", "Maximum 3 active loan applications allowed");
            return result;
        }

        // ML CIBIL Score Prediction using PAN
        int cibilScore = predictCibilScore(loan.getPanNumber(), loan.getAnnualRevenue(),
            loan.getYearsInBusiness(), loan.getRequestedAmount());
        loan.setCibilScore(cibilScore);
        loan.setCibilStatus(getCibilStatus(cibilScore));

        // Set business info from account
        Optional<CurrentAccount> accountOpt = accountRepository.findByAccountNumber(loan.getAccountNumber());
        if (accountOpt.isPresent()) {
            CurrentAccount account = accountOpt.get();
            loan.setBusinessName(account.getBusinessName());
            loan.setOwnerName(account.getOwnerName());
        }

        // Auto-decision based on CIBIL score
        if (cibilScore >= 750) {
            loan.setStatus("APPROVED");
            loan.setApprovedAmount(loan.getRequestedAmount());
            loan.setInterestRate(10.5);
            loan.setMonthlyEmi(calculateEmi(loan.getRequestedAmount(), 10.5, loan.getTenureMonths()));
            loan.setProcessedAt(LocalDateTime.now());
            loan.setProcessedBy("ML_AUTO_APPROVE");
        } else if (cibilScore >= 650) {
            double approvedAmount = loan.getRequestedAmount() * 0.75;
            loan.setStatus("APPROVED");
            loan.setApprovedAmount(approvedAmount);
            loan.setInterestRate(13.5);
            loan.setMonthlyEmi(calculateEmi(approvedAmount, 13.5, loan.getTenureMonths()));
            loan.setProcessedAt(LocalDateTime.now());
            loan.setProcessedBy("ML_AUTO_APPROVE");
        } else if (cibilScore >= 550) {
            loan.setStatus("PENDING");
        } else {
            loan.setStatus("REJECTED");
            loan.setRejectionReason("CIBIL score too low (" + cibilScore + "). Minimum required: 550");
            loan.setProcessedAt(LocalDateTime.now());
            loan.setProcessedBy("ML_AUTO_REJECT");
        }

        loan.setApplicationId("BL-" + System.currentTimeMillis());
        CurrentAccountBusinessLoan saved = businessLoanRepository.save(loan);

        result.put("success", true);
        result.put("loan", saved);
        result.put("cibilScore", cibilScore);
        result.put("cibilStatus", loan.getCibilStatus());
        result.put("message", "Loan application processed. CIBIL Score: " + cibilScore + " (" + loan.getCibilStatus() + ")");
        return result;
    }

    /**
     * ML-based CIBIL score prediction using PAN number characteristics,
     * annual revenue, years in business, and loan amount requested.
     * Uses a weighted scoring algorithm simulating ML prediction.
     */
    private int predictCibilScore(String panNumber, double annualRevenue, int yearsInBusiness, double requestedAmount) {
        int baseScore = 600;

        // PAN-based scoring (character analysis as feature extraction)
        if (panNumber != null && panNumber.length() == 10) {
            int panHash = Math.abs(panNumber.hashCode());
            int panFeature = (panHash % 150); // 0-149 range
            baseScore += (panFeature - 75); // -75 to +74 adjustment
        }

        // Revenue-based scoring
        if (annualRevenue >= 5000000) baseScore += 80;
        else if (annualRevenue >= 2000000) baseScore += 50;
        else if (annualRevenue >= 1000000) baseScore += 30;
        else if (annualRevenue >= 500000) baseScore += 10;
        else baseScore -= 20;

        // Years in business scoring
        if (yearsInBusiness >= 10) baseScore += 60;
        else if (yearsInBusiness >= 5) baseScore += 40;
        else if (yearsInBusiness >= 3) baseScore += 20;
        else if (yearsInBusiness >= 1) baseScore += 5;
        else baseScore -= 30;

        // Debt-to-revenue ratio
        double ratio = requestedAmount / Math.max(annualRevenue, 1);
        if (ratio < 0.1) baseScore += 30;
        else if (ratio < 0.25) baseScore += 15;
        else if (ratio < 0.5) baseScore += 0;
        else baseScore -= 25;

        // Clamp to CIBIL range
        return Math.max(300, Math.min(900, baseScore));
    }

    private String getCibilStatus(int score) {
        if (score >= 750) return "EXCELLENT";
        if (score >= 700) return "GOOD";
        if (score >= 650) return "FAIR";
        if (score >= 550) return "POOR";
        return "VERY_POOR";
    }

    private double calculateEmi(double principal, double annualRate, int tenureMonths) {
        double monthlyRate = annualRate / 12 / 100;
        double emi = (principal * monthlyRate * Math.pow(1 + monthlyRate, tenureMonths))
                     / (Math.pow(1 + monthlyRate, tenureMonths) - 1);
        return Math.round(emi * 100.0) / 100.0;
    }

    public List<CurrentAccountBusinessLoan> getBusinessLoans(String accountNumber) {
        return businessLoanRepository.findByAccountNumberOrderByAppliedAtDesc(accountNumber);
    }

    public List<CurrentAccountBusinessLoan> getPendingBusinessLoans() {
        return businessLoanRepository.findByStatus("PENDING");
    }

    @Transactional
    public CurrentAccountBusinessLoan approveBusinessLoan(Long id, String approvedBy, Double approvedAmount, Double interestRate) {
        CurrentAccountBusinessLoan loan = businessLoanRepository.findById(id).orElse(null);
        if (loan == null) return null;
        loan.setStatus("APPROVED");
        loan.setApprovedAmount(approvedAmount);
        loan.setInterestRate(interestRate);
        loan.setMonthlyEmi(calculateEmi(approvedAmount, interestRate, loan.getTenureMonths()));
        loan.setProcessedAt(LocalDateTime.now());
        loan.setProcessedBy(approvedBy);
        return businessLoanRepository.save(loan);
    }

    @Transactional
    public CurrentAccountBusinessLoan rejectBusinessLoan(Long id, String rejectedBy, String reason) {
        CurrentAccountBusinessLoan loan = businessLoanRepository.findById(id).orElse(null);
        if (loan == null) return null;
        loan.setStatus("REJECTED");
        loan.setRejectionReason(reason);
        loan.setProcessedAt(LocalDateTime.now());
        loan.setProcessedBy(rejectedBy);
        return businessLoanRepository.save(loan);
    }

    // ==================== Multi-User Access Management ====================

    @Transactional
    public Map<String, Object> addBusinessUser(CurrentAccountBusinessUser user) {
        Map<String, Object> result = new HashMap<>();

        // Check if email already exists for this account
        Optional<CurrentAccountBusinessUser> existing = businessUserRepository.findByEmailAndAccountNumber(
            user.getEmail(), user.getAccountNumber());
        if (existing.isPresent()) {
            result.put("success", false);
            result.put("message", "A user with this email already exists for this account");
            return result;
        }

        // Max 10 users per account
        long userCount = businessUserRepository.countByAccountNumber(user.getAccountNumber());
        if (userCount >= 10) {
            result.put("success", false);
            result.put("message", "Maximum 10 users allowed per business account");
            return result;
        }

        user.setUserId("USR-" + System.currentTimeMillis());
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        CurrentAccountBusinessUser saved = businessUserRepository.save(user);

        result.put("success", true);
        result.put("user", saved);
        result.put("message", "User added successfully with role: " + user.getRole());
        return result;
    }

    public List<CurrentAccountBusinessUser> getBusinessUsers(String accountNumber) {
        return businessUserRepository.findByAccountNumberOrderByCreatedAtDesc(accountNumber);
    }

    @Transactional
    public CurrentAccountBusinessUser updateUserRole(Long id, String role) {
        CurrentAccountBusinessUser user = businessUserRepository.findById(id).orElse(null);
        if (user == null) return null;
        user.setRole(role);
        return businessUserRepository.save(user);
    }

    @Transactional
    public CurrentAccountBusinessUser updateUserStatus(Long id, String status) {
        CurrentAccountBusinessUser user = businessUserRepository.findById(id).orElse(null);
        if (user == null) return null;
        user.setStatus(status);
        return businessUserRepository.save(user);
    }

    public void removeBusinessUser(Long id) {
        businessUserRepository.deleteById(id);
    }

    // ==================== Business User Login ====================

    public Map<String, Object> authenticateBusinessUser(String email, String password) {
        Map<String, Object> result = new HashMap<>();
        Optional<CurrentAccountBusinessUser> opt = businessUserRepository.findByEmail(email);
        if (opt.isEmpty()) {
            result.put("success", false);
            result.put("message", "No team member found with this email");
            return result;
        }
        CurrentAccountBusinessUser user = opt.get();
        if (!"ACTIVE".equals(user.getStatus())) {
            result.put("success", false);
            result.put("message", "Your account is " + user.getStatus().toLowerCase() + ". Contact your business owner.");
            return result;
        }
        if (user.getPassword() == null || !passwordEncoder.matches(password, user.getPassword())) {
            result.put("success", false);
            result.put("message", "Invalid password");
            return result;
        }
        // Get the parent business account
        Optional<CurrentAccount> accountOpt = accountRepository.findByAccountNumber(user.getAccountNumber());
        if (accountOpt.isEmpty()) {
            result.put("success", false);
            result.put("message", "Associated business account not found");
            return result;
        }
        CurrentAccount account = accountOpt.get();
        if (!"ACTIVE".equals(account.getStatus()) && !"APPROVED".equals(account.getStatus())) {
            result.put("success", false);
            result.put("message", "Business account is not active. Current status: " + account.getStatus());
            return result;
        }
        // Update last login
        user.setLastLogin(java.time.LocalDateTime.now());
        businessUserRepository.save(user);

        result.put("success", true);
        result.put("message", "Login successful");
        result.put("account", account);
        result.put("userRole", user.getRole());
        result.put("userName", user.getFullName());
        result.put("userId", user.getUserId());
        return result;
    }

    // ==================== Linked Accounts ====================

    public Map<String, Object> verifySavingsAccount(String accountNumber, String customerId) {
        Map<String, Object> result = new HashMap<>();
        Account savings = savingsAccountRepository.findByAccountNumber(accountNumber);
        if (savings == null) {
            result.put("success", false);
            result.put("message", "Savings account not found with this account number");
            return result;
        }
        if (savings.getCustomerId() == null || !savings.getCustomerId().equals(customerId)) {
            result.put("success", false);
            result.put("message", "Customer ID does not match the savings account");
            return result;
        }
        if (!"ACTIVE".equals(savings.getStatus())) {
            result.put("success", false);
            result.put("message", "Savings account is not active. Status: " + savings.getStatus());
            return result;
        }
        result.put("success", true);
        result.put("message", "Savings account verified successfully");
        result.put("accountName", savings.getName());
        result.put("accountNumber", savings.getAccountNumber());
        result.put("customerId", savings.getCustomerId());
        result.put("balance", savings.getBalance());
        return result;
    }

    @Transactional
    public Map<String, Object> linkSavingsAccount(String currentAccountNumber, String savingsAccountNumber, String savingsCustomerId, String linkedBy) {
        Map<String, Object> result = new HashMap<>();

        // Verify current account exists
        Optional<CurrentAccount> currentOpt = accountRepository.findByAccountNumber(currentAccountNumber);
        if (currentOpt.isEmpty()) {
            result.put("success", false);
            result.put("message", "Current account not found");
            return result;
        }

        // Verify savings account exists
        Account savings = savingsAccountRepository.findByAccountNumber(savingsAccountNumber);
        if (savings == null) {
            result.put("success", false);
            result.put("message", "Savings account not found");
            return result;
        }

        if (savings.getCustomerId() == null || !savings.getCustomerId().equals(savingsCustomerId)) {
            result.put("success", false);
            result.put("message", "Customer ID does not match the savings account");
            return result;
        }

        // Enforce one-link-only: check if current account already has an active link
        if (linkedAccountRepository.existsByCurrentAccountNumberAndStatus(currentAccountNumber, "ACTIVE")) {
            result.put("success", false);
            result.put("message", "This current account already has a linked savings account. Only one link is allowed.");
            return result;
        }

        // Enforce one-link-only: check if savings account already has an active link
        if (linkedAccountRepository.existsBySavingsAccountNumberAndStatus(savingsAccountNumber, "ACTIVE")) {
            result.put("success", false);
            result.put("message", "This savings account is already linked to another current account. Only one link is allowed.");
            return result;
        }

        LinkedAccount link = new LinkedAccount();
        link.setCurrentAccountNumber(currentAccountNumber);
        link.setSavingsAccountNumber(savingsAccountNumber);
        link.setSavingsCustomerId(savingsCustomerId);
        link.setLinkedBy(linkedBy);
        linkedAccountRepository.save(link);

        result.put("success", true);
        result.put("message", "Savings account linked successfully");
        result.put("link", link);
        return result;
    }

    public List<LinkedAccount> getLinkedAccounts(String currentAccountNumber) {
        return linkedAccountRepository.findByCurrentAccountNumberAndStatus(currentAccountNumber, "ACTIVE");
    }

    public Map<String, Object> getLinkedSavingsAccountDetails(String savingsAccountNumber) {
        Map<String, Object> result = new HashMap<>();
        Account savings = savingsAccountRepository.findByAccountNumber(savingsAccountNumber);
        if (savings == null) {
            result.put("success", false);
            result.put("message", "Savings account not found");
            return result;
        }
        result.put("success", true);
        result.put("name", savings.getName());
        result.put("accountNumber", savings.getAccountNumber());
        result.put("customerId", savings.getCustomerId());
        result.put("balance", savings.getBalance());
        result.put("status", savings.getStatus());
        result.put("phone", savings.getPhone());
        result.put("accountType", savings.getAccountType());
        return result;
    }

    @Transactional
    public Map<String, Object> unlinkSavingsAccount(Long linkId) {
        Map<String, Object> result = new HashMap<>();
        Optional<LinkedAccount> linkOpt = linkedAccountRepository.findById(linkId);
        if (linkOpt.isEmpty()) {
            result.put("success", false);
            result.put("message", "Link not found");
            return result;
        }
        LinkedAccount link = linkOpt.get();
        link.setStatus("INACTIVE");
        linkedAccountRepository.save(link);
        result.put("success", true);
        result.put("message", "Account unlinked successfully");
        return result;
    }

    // ==================== Switch PIN ====================

    @Transactional
    public Map<String, Object> createSwitchPin(Long linkId, String pin) {
        Map<String, Object> result = new HashMap<>();
        if (pin == null || !pin.matches("\\d{4}")) {
            result.put("success", false);
            result.put("message", "PIN must be exactly 4 digits");
            return result;
        }
        Optional<LinkedAccount> linkOpt = linkedAccountRepository.findById(linkId);
        if (linkOpt.isEmpty()) {
            result.put("success", false);
            result.put("message", "Linked account not found");
            return result;
        }
        LinkedAccount link = linkOpt.get();
        if (!"ACTIVE".equals(link.getStatus())) {
            result.put("success", false);
            result.put("message", "This link is no longer active");
            return result;
        }
        link.setSwitchPin(passwordEncoder.encode(pin));
        link.setPinCreated(true);
        linkedAccountRepository.save(link);
        result.put("success", true);
        result.put("message", "Switch PIN created successfully");
        return result;
    }

    public Map<String, Object> verifySwitchPin(Long linkId, String pin) {
        Map<String, Object> result = new HashMap<>();
        if (pin == null || !pin.matches("\\d{4}")) {
            result.put("success", false);
            result.put("message", "PIN must be exactly 4 digits");
            return result;
        }
        Optional<LinkedAccount> linkOpt = linkedAccountRepository.findById(linkId);
        if (linkOpt.isEmpty()) {
            result.put("success", false);
            result.put("message", "Linked account not found");
            return result;
        }
        LinkedAccount link = linkOpt.get();
        if (!"ACTIVE".equals(link.getStatus())) {
            result.put("success", false);
            result.put("message", "This link is no longer active");
            return result;
        }
        if (!Boolean.TRUE.equals(link.getPinCreated()) || link.getSwitchPin() == null) {
            result.put("success", false);
            result.put("message", "Switch PIN has not been created yet");
            return result;
        }
        if (!passwordEncoder.matches(pin, link.getSwitchPin())) {
            result.put("success", false);
            result.put("message", "Incorrect PIN");
            return result;
        }
        result.put("success", true);
        result.put("message", "PIN verified successfully");
        return result;
    }

    // ==================== User-Side Linked Account Lookups ====================

    public List<LinkedAccount> getLinkedAccountsBySavings(String savingsAccountNumber) {
        return linkedAccountRepository.findBySavingsAccountNumberAndStatus(savingsAccountNumber, "ACTIVE");
    }

    public Map<String, Object> getLinkedCurrentAccountDetails(String currentAccountNumber) {
        Map<String, Object> result = new HashMap<>();
        Optional<CurrentAccount> currentOpt = accountRepository.findByAccountNumber(currentAccountNumber);
        if (currentOpt.isEmpty()) {
            result.put("success", false);
            result.put("message", "Current account not found");
            return result;
        }
        CurrentAccount ca = currentOpt.get();
        result.put("success", true);
        result.put("businessName", ca.getBusinessName());
        result.put("ownerName", ca.getOwnerName());
        result.put("accountNumber", ca.getAccountNumber());
        result.put("customerId", ca.getCustomerId());
        result.put("balance", ca.getBalance());
        result.put("status", ca.getStatus());
        result.put("mobile", ca.getMobile());
        result.put("accountType", "CURRENT");
        return result;
    }
}
