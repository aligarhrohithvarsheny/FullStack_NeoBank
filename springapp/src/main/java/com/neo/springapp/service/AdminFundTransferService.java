package com.neo.springapp.service;

import com.neo.springapp.model.Account;
import com.neo.springapp.model.AdminFundTransfer;
import com.neo.springapp.model.BusinessChequeRequest;
import com.neo.springapp.model.Cheque;
import com.neo.springapp.model.ChequeRequest;
import com.neo.springapp.model.CurrentAccount;
import com.neo.springapp.model.SalaryAccount;
import com.neo.springapp.repository.AccountRepository;
import com.neo.springapp.repository.AdminFundTransferRepository;
import com.neo.springapp.repository.BusinessChequeRequestRepository;
import com.neo.springapp.repository.ChequeRepository;
import com.neo.springapp.repository.ChequeRequestRepository;
import com.neo.springapp.repository.CurrentAccountRepository;
import com.neo.springapp.repository.SalaryAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AdminFundTransferService {

    public static final double CHARGE_RATE = 0.005; // 0.5%

    @Autowired
    private AdminFundTransferRepository repository;

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CurrentAccountRepository currentAccountRepository;

    @Autowired
    private SalaryAccountRepository salaryAccountRepository;

    @Autowired
    private ChequeRepository chequeRepository;

    @Autowired
    private ChequeRequestRepository chequeRequestRepository;

    @Autowired
    private BusinessChequeRequestRepository businessChequeRequestRepository;

    public List<AdminFundTransfer> getAll(String search) {
        if (search != null && !search.isBlank()) {
            return repository.search(search.trim());
        }
        return repository.findAllByOrderByPerformedAtDesc();
    }

    // Verify sender's cheque number belongs to the given account and names match; also returns the balance
    // so the admin UI can then load the signed signature document for visual verification.
    public Map<String, Object> verifySenderCheque(String accountNumber, String chequeNumber) {
        Map<String, Object> result = new HashMap<>();

        ResolvedCheque resolved = resolveCheque(chequeNumber, accountNumber);
        if (resolved == null) {
            result.put("valid", false);
            result.put("message", "Approved cheque number not found for this account");
            return result;
        }

        Map<String, Object> accInfo = accountService.verifyAccountByNumber(accountNumber.trim());
        if (!Boolean.TRUE.equals(accInfo.get("found"))) {
            result.put("valid", false);
            result.put("message", "Sender account not found in bank records");
            return result;
        }

        String holderName = String.valueOf(accInfo.get("name"));
        boolean nameMatches = resolved.accountHolderName == null
            || holderName.equalsIgnoreCase(resolved.accountHolderName);

        result.put("valid", true);
        result.put("nameMatches", nameMatches);
        result.put("chequeNumber", resolved.chequeNumber);
        result.put("chequeAccountHolderName", resolved.accountHolderName != null
            ? resolved.accountHolderName : holderName);
        result.put("chequeStatus", resolved.status);
        result.put("chequeSource", resolved.source);
        result.put("accountHolderName", holderName);
        result.put("accountNumber", accountNumber);
        result.put("accountType", accInfo.get("accountType"));
        result.put("balance", getBalance(accountNumber, (String) accInfo.get("accountType")));
        result.put("message", nameMatches
                ? "Cheque verified — name and account number match."
                : "Cheque found, but the name on the cheque does not match the account holder name.");
        return result;
    }

    // Fetch receiver's name/details for any account type (savings/current/salary)
    public Map<String, Object> verifyReceiver(String accountNumber) {
        Map<String, Object> accInfo = accountService.verifyAccountByNumber(accountNumber.trim());
        if (!Boolean.TRUE.equals(accInfo.get("found"))) {
            accInfo.put("valid", false);
            return accInfo;
        }
        accInfo.put("valid", true);
        accInfo.put("balance", getBalance(accountNumber, (String) accInfo.get("accountType")));
        return accInfo;
    }

    @Transactional
    public Map<String, Object> processTransfer(String senderAccountNumber, String senderChequeNumber,
            String receiverAccountNumber, Double amount, String description, String performedBy) {
        Map<String, Object> result = new HashMap<>();

        if (amount == null || amount <= 0) {
            throw new RuntimeException("Amount must be greater than 0");
        }
        if (senderAccountNumber == null || receiverAccountNumber == null) {
            throw new RuntimeException("Sender and receiver account numbers are required");
        }
        if (senderAccountNumber.trim().equalsIgnoreCase(receiverAccountNumber.trim())) {
            throw new RuntimeException("Sender and receiver accounts cannot be the same");
        }

        Map<String, Object> senderInfo = accountService.verifyAccountByNumber(senderAccountNumber.trim());
        if (!Boolean.TRUE.equals(senderInfo.get("found"))) {
            throw new RuntimeException("Sender account not found");
        }
        Map<String, Object> receiverInfo = accountService.verifyAccountByNumber(receiverAccountNumber.trim());
        if (!Boolean.TRUE.equals(receiverInfo.get("found"))) {
            throw new RuntimeException("Receiver account not found");
        }

        String senderType = (String) senderInfo.get("accountType");
        String receiverType = (String) receiverInfo.get("accountType");

        ResolvedCheque resolved = null;
        if (senderChequeNumber != null && !senderChequeNumber.isBlank()) {
            resolved = resolveCheque(senderChequeNumber, senderAccountNumber);
            if (resolved == null) {
                throw new RuntimeException("Approved cheque number not found for sender account");
            }
        }

        double senderBalance = getBalance(senderAccountNumber, senderType);
        double transferCharge = round2(amount * CHARGE_RATE);
        double totalDebit = amount + transferCharge;
        if (senderBalance < totalDebit) {
            throw new RuntimeException("Insufficient sender balance. Need ₹" + totalDebit + " (amount + 0.5% charge)");
        }

        adjustBalance(senderType, senderAccountNumber, -totalDebit);
        adjustBalance(receiverType, receiverAccountNumber, amount);

        AdminFundTransfer transfer = new AdminFundTransfer();
        transfer.setTransferId("AFT" + System.currentTimeMillis());
        transfer.setSenderAccountNumber(senderAccountNumber.trim());
        transfer.setSenderName((String) senderInfo.get("name"));
        transfer.setSenderAccountType(senderType);
        transfer.setSenderChequeNumber(senderChequeNumber);
        transfer.setReceiverAccountNumber(receiverAccountNumber.trim());
        transfer.setReceiverName((String) receiverInfo.get("name"));
        transfer.setReceiverAccountType(receiverType);
        transfer.setAmount(amount);
        transfer.setTransferCharge(transferCharge);
        transfer.setDescription(description);
        transfer.setStatus("COMPLETED");
        transfer.setPerformedBy(performedBy != null && !performedBy.isBlank() ? performedBy : "Admin");
        transfer.setPerformedAt(LocalDateTime.now());

        AdminFundTransfer saved = repository.save(transfer);
        if (resolved != null) {
            markChequeUsed(resolved, saved.getTransferId(), senderAccountNumber, performedBy);
        }

        result.put("success", true);
        result.put("message", "Transfer completed. ₹" + transferCharge + " (0.5%) charged as processing fee.");
        result.put("transfer", saved);
        return result;
    }

    private ResolvedCheque resolveCheque(String chequeNumber, String accountNumber) {
        String number = chequeNumber == null ? "" : chequeNumber.trim();
        String account = accountNumber == null ? "" : accountNumber.trim();

        Optional<Cheque> standard = chequeRepository.findByChequeNumber(number);
        if (standard.isPresent()) {
            Cheque cheque = standard.get();
            if ("ACTIVE".equalsIgnoreCase(cheque.getStatus())
                    && account.equalsIgnoreCase(String.valueOf(cheque.getAccountNumber()))) {
                return new ResolvedCheque("CHEQUES", cheque.getChequeNumber(), cheque.getAccountHolderName(), cheque.getStatus(), cheque);
            }
        }

        for (ChequeRequest request : chequeRequestRepository.findAllByChequeNumber(number)) {
            if (!"APPROVED".equalsIgnoreCase(request.getStatus())) {
                continue;
            }
            SalaryAccount salaryAccount = salaryAccountRepository.findById(request.getSalaryAccountId()).orElse(null);
            if (salaryAccount != null && account.equalsIgnoreCase(salaryAccount.getAccountNumber())) {
                return new ResolvedCheque("SALARY_CHEQUE_REQUESTS", request.getChequeNumber(), null, request.getStatus(), request);
            }
        }

        for (BusinessChequeRequest request : businessChequeRequestRepository.findAllByChequeNumber(number)) {
            if (!"APPROVED".equalsIgnoreCase(request.getStatus())) {
                continue;
            }
            CurrentAccount currentAccount = currentAccountRepository.findById(request.getCurrentAccountId()).orElse(null);
            if (currentAccount != null && account.equalsIgnoreCase(currentAccount.getAccountNumber())) {
                return new ResolvedCheque("BUSINESS_CHEQUE_REQUESTS", request.getChequeNumber(), null, request.getStatus(), request);
            }
        }
        return null;
    }

    private void markChequeUsed(ResolvedCheque resolved, String transferId, String senderAccountNumber, String performedBy) {
        if (resolved.value instanceof Cheque cheque) {
            cheque.markUsed("ADMIN_FUND_TRANSFER", transferId);
            chequeRepository.save(cheque);
        } else if (resolved.value instanceof ChequeRequest request) {
            request.setStatus("COMPLETED");
            request.setTransactionReference(transferId);
            request.setDebitedFromAccount(senderAccountNumber);
            request.setApprovedBy(performedBy != null && !performedBy.isBlank() ? performedBy : request.getApprovedBy());
            request.setUpdatedAt(LocalDateTime.now());
            chequeRequestRepository.save(request);
        } else if (resolved.value instanceof BusinessChequeRequest request) {
            request.setStatus("COMPLETED");
            request.setTransactionReference(transferId);
            request.setUpdatedAt(LocalDateTime.now());
            businessChequeRequestRepository.save(request);
        }
    }

    private static class ResolvedCheque {
        private final String source;
        private final String chequeNumber;
        private final String accountHolderName;
        private final String status;
        private final Object value;

        private ResolvedCheque(String source, String chequeNumber, String accountHolderName, String status, Object value) {
            this.source = source;
            this.chequeNumber = chequeNumber;
            this.accountHolderName = accountHolderName;
            this.status = status;
            this.value = value;
        }
    }

    @Transactional
    public Map<String, Object> editTransfer(Long id, String description, String editedBy) {
        AdminFundTransfer transfer = repository.findById(id).orElseThrow(() -> new RuntimeException("Transfer not found"));
        if ("REVERTED".equals(transfer.getStatus())) {
            throw new RuntimeException("Cannot edit a reverted transfer");
        }
        if (description != null) {
            transfer.setDescription(description);
        }
        transfer.setEditedBy(editedBy != null && !editedBy.isBlank() ? editedBy : "Admin");
        transfer.setEditedAt(LocalDateTime.now());
        AdminFundTransfer saved = repository.save(transfer);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Transfer details updated");
        result.put("transfer", saved);
        return result;
    }

    @Transactional
    public Map<String, Object> revertTransfer(Long id, String revertedBy, String reason) {
        AdminFundTransfer transfer = repository.findById(id).orElseThrow(() -> new RuntimeException("Transfer not found"));
        if ("REVERTED".equals(transfer.getStatus())) {
            throw new RuntimeException("This transfer has already been reverted");
        }
        if (transfer.getPerformedAt() != null && LocalDateTime.now().isAfter(transfer.getPerformedAt().plusHours(24))) {
            throw new RuntimeException("Transfer can only be reverted within 24 hours");
        }

        double revertCharge = round2(transfer.getAmount() * CHARGE_RATE);
        double refundToSender = transfer.getAmount() + transfer.getTransferCharge() - revertCharge;
        double debitFromReceiver = transfer.getAmount();

        double receiverBalance = getBalance(transfer.getReceiverAccountNumber(), transfer.getReceiverAccountType());
        if (receiverBalance < debitFromReceiver) {
            throw new RuntimeException("Receiver account has insufficient balance to revert this transfer");
        }

        adjustBalance(transfer.getReceiverAccountType(), transfer.getReceiverAccountNumber(), -debitFromReceiver);
        adjustBalance(transfer.getSenderAccountType(), transfer.getSenderAccountNumber(), refundToSender);

        transfer.setStatus("REVERTED");
        transfer.setRevertCharge(revertCharge);
        transfer.setRevertedBy(revertedBy != null && !revertedBy.isBlank() ? revertedBy : "Admin");
        transfer.setRevertedAt(LocalDateTime.now());
        transfer.setRevertReason(reason);
        AdminFundTransfer saved = repository.save(transfer);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Transfer reverted in real-time. ₹" + revertCharge + " (0.5%) charged as revert processing fee.");
        result.put("transfer", saved);
        return result;
    }

    private double getBalance(String accountNumber, String accountType) {
        if (accountType == null) return 0.0;
        if (accountType.startsWith("Savings")) {
            Account a = accountRepository.findByAccountNumber(accountNumber);
            return a != null && a.getBalance() != null ? a.getBalance() : 0.0;
        } else if (accountType.startsWith("Current")) {
            return currentAccountRepository.findByAccountNumber(accountNumber)
                    .map(CurrentAccount::getBalance).orElse(0.0);
        } else if (accountType.startsWith("Salary")) {
            SalaryAccount s = salaryAccountRepository.findByAccountNumber(accountNumber);
            return s != null && s.getBalance() != null ? s.getBalance() : 0.0;
        }
        return 0.0;
    }

    private void adjustBalance(String accountType, String accountNumber, double delta) {
        if (accountType == null) {
            throw new RuntimeException("Unknown account type for " + accountNumber);
        }
        if (accountType.startsWith("Savings")) {
            Account a = accountRepository.findByAccountNumber(accountNumber);
            if (a == null) throw new RuntimeException("Savings account not found: " + accountNumber);
            a.setBalance((a.getBalance() != null ? a.getBalance() : 0.0) + delta);
            a.setLastUpdated(LocalDateTime.now());
            accountRepository.save(a);
        } else if (accountType.startsWith("Current")) {
            CurrentAccount ca = currentAccountRepository.findByAccountNumber(accountNumber)
                    .orElseThrow(() -> new RuntimeException("Current account not found: " + accountNumber));
            ca.setBalance((ca.getBalance() != null ? ca.getBalance() : 0.0) + delta);
            currentAccountRepository.save(ca);
        } else if (accountType.startsWith("Salary")) {
            SalaryAccount sa = salaryAccountRepository.findByAccountNumber(accountNumber);
            if (sa == null) throw new RuntimeException("Salary account not found: " + accountNumber);
            sa.setBalance((sa.getBalance() != null ? sa.getBalance() : 0.0) + delta);
            sa.setUpdatedAt(LocalDateTime.now());
            salaryAccountRepository.save(sa);
        } else {
            throw new RuntimeException("Unsupported account type: " + accountType);
        }
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
