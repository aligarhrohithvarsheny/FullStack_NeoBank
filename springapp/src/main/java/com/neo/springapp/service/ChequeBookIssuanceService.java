package com.neo.springapp.service;

import com.neo.springapp.model.BusinessChequeBankRange;
import com.neo.springapp.model.BusinessChequeLeaf;
import com.neo.springapp.model.ChequeBankRange;
import com.neo.springapp.model.ChequeLeaf;
import com.neo.springapp.model.SavingsChequeBankRange;
import com.neo.springapp.model.SavingsChequeLeaf;
import com.neo.springapp.repository.AccountRepository;
import com.neo.springapp.repository.BusinessChequeBankRangeRepository;
import com.neo.springapp.repository.BusinessChequeLeafRepository;
import com.neo.springapp.repository.ChequeBankRangeRepository;
import com.neo.springapp.repository.ChequeLeafRepository;
import com.neo.springapp.repository.CurrentAccountRepository;
import com.neo.springapp.repository.SalaryAccountRepository;
import com.neo.springapp.repository.SavingsChequeBankRangeRepository;
import com.neo.springapp.repository.SavingsChequeLeafRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ChequeBookIssuanceService {

    private static final int BOOK_SIZE = 30;
    private static final double BOOK_CHARGE = 199.0;

    @Autowired private AccountRepository accountRepository;
    @Autowired private SalaryAccountRepository salaryAccountRepository;
    @Autowired private CurrentAccountRepository currentAccountRepository;
    @Autowired private ChequeLeafRepository chequeLeafRepository;
    @Autowired private BusinessChequeLeafRepository businessChequeLeafRepository;
    @Autowired private SavingsChequeLeafRepository savingsChequeLeafRepository;
    @Autowired private ChequeBankRangeRepository chequeBankRangeRepository;
    @Autowired private BusinessChequeBankRangeRepository businessChequeBankRangeRepository;
    @Autowired private SavingsChequeBankRangeRepository savingsChequeBankRangeRepository;
    @Autowired private AccountService accountService;
    @Autowired private TransactionService transactionService;

    @Transactional
    public synchronized Map<String, Object> requestBook(Long accountId, String accountType, String authorizedAccountNumber) {
        if (accountId == null || accountId <= 0 || accountType == null || accountType.isBlank()
                || authorizedAccountNumber == null || authorizedAccountNumber.isBlank()) {
            throw new IllegalArgumentException("A signed-in account is required");
        }

        String type = accountType.replaceAll("[^A-Za-z]", "").toUpperCase(Locale.ROOT);
        if ("SAVING".equals(type)) type = "SAVINGS";
        if ("BUSINESS".equals(type)) type = "CURRENT";

        String accountNumber;
        List<String> existingNumbers = new ArrayList<>();
        switch (type) {
            case "SAVINGS" -> {
                var account = accountRepository.findById(accountId)
                        .orElseThrow(() -> new IllegalArgumentException("Savings account not found"));
                accountNumber = account.getAccountNumber();
                savingsChequeLeafRepository.findByAccountIdOrderByLeafNumberAsc(accountId)
                        .forEach(leaf -> existingNumbers.add(leaf.getLeafNumber()));
            }
            case "SALARY" -> {
                var account = salaryAccountRepository.findById(accountId)
                        .orElseThrow(() -> new IllegalArgumentException("Salary account not found"));
                accountNumber = account.getAccountNumber();
                chequeLeafRepository.findBySalaryAccountIdOrderByLeafNumberAsc(accountId)
                        .forEach(leaf -> existingNumbers.add(leaf.getLeafNumber()));
            }
            case "CURRENT" -> {
                var account = currentAccountRepository.findById(accountId)
                        .orElseThrow(() -> new IllegalArgumentException("Current account not found"));
                accountNumber = account.getAccountNumber();
                businessChequeLeafRepository.findByCurrentAccountIdOrderByLeafNumberAsc(accountId)
                        .forEach(leaf -> existingNumbers.add(leaf.getLeafNumber()));
            }
            default -> throw new IllegalArgumentException("Unsupported account type");
        }

        if (!accountNumber.equals(authorizedAccountNumber)) {
            throw new SecurityException("The cheque book account does not match the signed-in account");
        }

        if (hasAvailableLeaf(type, accountId)) {
            throw new IllegalStateException("Use the available cheque numbers before requesting another book");
        }

        Double balanceAfterCharge = accountService.debitBalance(accountNumber, BOOK_CHARGE, type);
        if (balanceAfterCharge == null) {
            throw new IllegalStateException("Insufficient balance to pay the ₹199 cheque book charge");
        }

        long firstSerial = findGlobalMaximumSerial() + 1;
        long lastSerial = firstSerial + BOOK_SIZE - 1;
        String serialFrom = String.format("%06d", firstSerial);
        String serialTo = String.format("%06d", lastSerial);
        String bookNumber = "CB-" + System.currentTimeMillis() + "-" + accountId;
        saveBook(type, accountId, bookNumber, serialFrom, serialTo, firstSerial);

        transactionService.createTransferTransaction(
                accountNumber, "Cheque book issuance fee (" + bookNumber + ")", BOOK_CHARGE, "Debit", balanceAfterCharge);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "30 cheque numbers issued. ₹199 debited from your account.");
        response.put("accountNumber", accountNumber);
        response.put("accountType", type);
        response.put("chequeBookNumber", bookNumber);
        response.put("serialFrom", serialFrom);
        response.put("serialTo", serialTo);
        response.put("charge", BOOK_CHARGE);
        response.put("balanceAfterCharge", balanceAfterCharge);
        return response;
    }

    private boolean hasAvailableLeaf(String type, Long accountId) {
        return switch (type) {
            case "SAVINGS" -> !savingsChequeLeafRepository.findByAccountIdAndStatusOrderByLeafNumberAsc(accountId, "AVAILABLE").isEmpty();
            case "SALARY" -> !chequeLeafRepository.findBySalaryAccountIdAndStatusOrderByLeafNumberAsc(accountId, "AVAILABLE").isEmpty();
            case "CURRENT" -> !businessChequeLeafRepository.findByCurrentAccountIdAndStatusOrderByLeafNumberAsc(accountId, "AVAILABLE").isEmpty();
            default -> false;
        };
    }

    private long findGlobalMaximumSerial() {
        return java.util.stream.Stream.of(
                        chequeLeafRepository.findAll().stream().map(ChequeLeaf::getLeafNumber),
                        businessChequeLeafRepository.findAll().stream().map(BusinessChequeLeaf::getLeafNumber),
                        savingsChequeLeafRepository.findAll().stream().map(SavingsChequeLeaf::getLeafNumber))
                .flatMap(stream -> stream)
                .mapToLong(this::parseSerial)
                .max()
                .orElse(0L);
    }

    private long parseSerial(String serial) {
        try {
            return Long.parseLong(serial);
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private void saveBook(String type, Long accountId, String bookNumber,
                          String serialFrom, String serialTo, long firstSerial) {
        if ("SALARY".equals(type)) {
            chequeLeafRepository.saveAll(createSalaryLeaves(accountId, firstSerial));
            chequeBankRangeRepository.save(new ChequeBankRange(accountId, bookNumber, serialFrom, serialTo));
        } else if ("CURRENT".equals(type)) {
            businessChequeLeafRepository.saveAll(createBusinessLeaves(accountId, firstSerial));
            businessChequeBankRangeRepository.save(
                    new BusinessChequeBankRange(accountId, bookNumber, serialFrom, serialTo));
        } else {
            savingsChequeLeafRepository.saveAll(createSavingsLeaves(accountId, firstSerial));
            savingsChequeBankRangeRepository.save(new SavingsChequeBankRange(accountId, bookNumber, serialFrom, serialTo));
        }
    }

    private List<ChequeLeaf> createSalaryLeaves(Long accountId, long firstSerial) {
        Long userId = accountId;
        List<ChequeLeaf> leaves = new ArrayList<>(BOOK_SIZE);
        for (int i = 0; i < BOOK_SIZE; i++) {
            leaves.add(new ChequeLeaf(accountId, userId, String.format("%06d", firstSerial + i)));
        }
        return leaves;
    }

    private List<BusinessChequeLeaf> createBusinessLeaves(Long accountId, long firstSerial) {
        List<BusinessChequeLeaf> leaves = new ArrayList<>(BOOK_SIZE);
        for (int i = 0; i < BOOK_SIZE; i++) {
            leaves.add(new BusinessChequeLeaf(accountId, accountId, String.format("%06d", firstSerial + i)));
        }
        return leaves;
    }

    private List<SavingsChequeLeaf> createSavingsLeaves(Long accountId, long firstSerial) {
        List<SavingsChequeLeaf> leaves = new ArrayList<>(BOOK_SIZE);
        for (int i = 0; i < BOOK_SIZE; i++) {
            leaves.add(new SavingsChequeLeaf(accountId, accountId, String.format("%06d", firstSerial + i)));
        }
        return leaves;
    }
}
