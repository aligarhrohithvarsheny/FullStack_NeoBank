package com.neo.springapp.service;

import com.neo.springapp.model.Account;
import com.neo.springapp.model.EcsMandate;
import com.neo.springapp.model.EcsMandateEvent;
import com.neo.springapp.model.RecurringDeposit;
import com.neo.springapp.model.RecurringDepositContribution;
import com.neo.springapp.model.Transaction;
import com.neo.springapp.repository.EcsMandateEventRepository;
import com.neo.springapp.repository.EcsMandateRepository;
import com.neo.springapp.repository.RecurringDepositContributionRepository;
import com.neo.springapp.repository.RecurringDepositRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;

@Service
public class RecurringDepositEcsService {

    private final RecurringDepositRepository recurringDepositRepository;
    private final RecurringDepositContributionRepository contributionRepository;
    private final EcsMandateRepository mandateRepository;
    private final EcsMandateEventRepository eventRepository;
    private final AccountService accountService;
    private final TransactionService transactionService;
    private final DepositManagementHistoryService historyService;

    public RecurringDepositEcsService(RecurringDepositRepository recurringDepositRepository,
                                      RecurringDepositContributionRepository contributionRepository,
                                      EcsMandateRepository mandateRepository,
                                      EcsMandateEventRepository eventRepository,
                                      AccountService accountService,
                                      TransactionService transactionService,
                                      DepositManagementHistoryService historyService) {
        this.recurringDepositRepository = recurringDepositRepository;
        this.contributionRepository = contributionRepository;
        this.mandateRepository = mandateRepository;
        this.eventRepository = eventRepository;
        this.accountService = accountService;
        this.transactionService = transactionService;
        this.historyService = historyService;
    }

    @Transactional
    public void activateAfterConsent(Long recurringDepositId, String customerName) {
        RecurringDeposit deposit = recurringDepositRepository.findByIdForUpdate(recurringDepositId)
                .orElseThrow(() -> new IllegalArgumentException("RD record not found."));
        if (!"APPROVED".equals(deposit.getStatus())) {
            throw new IllegalStateException("Only an admin-approved RD can be activated by an ECS mandate.");
        }
        deposit.setStatus("ACTIVE");
        recurringDepositRepository.save(deposit);
        historyService.record("RD", deposit.getId(), deposit.getAccountNumber(), "ECS_CUSTOMER_CONSENT",
                customerName, "Customer separately authorized the ECS contribution mandate.", deposit.getRdAccountNumber());
    }

    @Transactional
    public void releaseMandate(String rdAccountNumber, String actor) {
        RecurringDeposit deposit = recurringDepositRepository.findByRdAccountNumberForUpdate(rdAccountNumber)
                .orElseThrow(() -> new IllegalArgumentException("RD record not found."));
        if ("ACTIVE".equals(deposit.getStatus())) {
            deposit.setStatus("APPROVED");
            recurringDepositRepository.save(deposit);
            historyService.record("RD", deposit.getId(), deposit.getAccountNumber(), "ECS_MANDATE_CANCELLED",
                    actor, "RD remains approved; a new ECS mandate requires separate customer consent.",
                    deposit.getRdAccountNumber());
        }
    }

    @Transactional
    public boolean debitContribution(EcsMandate mandate, LocalDate today) {
        RecurringDeposit deposit = recurringDepositRepository.findByRdAccountNumberForUpdate(mandate.getLoanAccountNumber())
                .orElse(null);
        if (deposit == null || !"ACTIVE".equals(deposit.getStatus())
                || mandate.getCustomerConsentAt() == null || mandate.getDebitDay() == null) {
            return false;
        }

        YearMonth currentMonth = YearMonth.from(today);
        if (mandate.getLastAttemptDate() != null
                && YearMonth.from(mandate.getLastAttemptDate()).equals(currentMonth)) return false;
        if (!YearMonth.from(mandate.getCustomerConsentAt()).isBefore(currentMonth)) return false;
        LocalDate scheduledDate = today.withDayOfMonth(Math.min(mandate.getDebitDay(), today.lengthOfMonth()));
        if (today.isBefore(scheduledDate)) return false;

        if (deposit.getPaidInstallments() >= deposit.getTenureMonths()) {
            if (!today.isBefore(deposit.getMaturityDate())) {
                deposit.setStatus("MATURED");
                mandate.setStatus("CANCELLED");
                mandate.setCancelledBy("SYSTEM");
                mandate.setCancelledAt(java.time.LocalDateTime.now());
                mandate.setLastDebitMessage("RD term completed; maturity payout requires separate bank processing.");
                recurringDepositRepository.save(deposit);
                mandateRepository.save(mandate);
                event(mandate, "CANCELLED", null, null, mandate.getLastDebitMessage(), "SYSTEM");
            }
            return false;
        }

        String contributionMonth = currentMonth.toString();
        String key = "RD" + deposit.getId() + "-" + contributionMonth;
        if (contributionRepository.existsByIdempotencyKey(key)) {
            mandate.setLastAttemptDate(today);
            mandateRepository.save(mandate);
            return false;
        }

        double amount = deposit.getMonthlyInstallment();
        if (amount > mandate.getAmountLimit()) {
            fail(mandate, today, amount, "RD installment exceeds the authorized ECS amount limit.");
            return false;
        }
        Account account = accountService.getAccountByNumber(mandate.getSavingsAccountNumber());
        if (account == null || !account.getAccountNumber().equals(deposit.getAccountNumber())) {
            fail(mandate, today, amount, "The linked debit account no longer matches the RD account.");
            return false;
        }
        Double balance = accountService.debitBalance(account.getAccountNumber(), amount, account.getAccountType());
        if (balance == null) {
            fail(mandate, today, amount, "Insufficient available balance for the RD installment.");
            return false;
        }

        String transactionRef = "RDC" + deposit.getId() + currentMonth.toString().replace("-", "");
        Transaction transaction = new Transaction();
        transaction.setAccountNumber(account.getAccountNumber());
        transaction.setUserName(account.getName());
        transaction.setType("Debit");
        transaction.setAmount(amount);
        transaction.setBalance(balance);
        transaction.setCategory("RD_CONTRIBUTION");
        transaction.setMerchant("NeoBank Recurring Deposit");
        transaction.setDescription("Recurring deposit contribution " + deposit.getRdAccountNumber()
                + " for " + contributionMonth);
        transaction.setReferenceNumber(transactionRef);
        transaction.setRecipientAccountNumber(deposit.getRdAccountNumber());
        transaction.setDate(java.time.LocalDateTime.now());
        transactionService.saveTransaction(transaction);

        RecurringDepositContribution contribution = new RecurringDepositContribution();
        contribution.setRecurringDepositId(deposit.getId());
        contribution.setIdempotencyKey(key);
        contribution.setContributionMonth(contributionMonth);
        contribution.setAmount(amount);
        contribution.setSourceAccountNumber(account.getAccountNumber());
        contribution.setTransactionId(transaction.getTransactionId());
        contribution.setPostedDate(today);
        contributionRepository.save(contribution);

        deposit.setPaidInstallments(deposit.getPaidInstallments() + 1);
        deposit.setPaidPrincipal(round(deposit.getPaidPrincipal() + amount));
        if (deposit.getPaidInstallments() >= deposit.getTenureMonths()
                && !today.isBefore(deposit.getMaturityDate())) {
            deposit.setStatus("MATURED");
            mandate.setStatus("CANCELLED");
            mandate.setCancelledBy("SYSTEM");
            mandate.setCancelledAt(java.time.LocalDateTime.now());
            mandate.setLastDebitMessage("Final RD installment posted; maturity payout requires separate bank processing.");
        }
        recurringDepositRepository.save(deposit);

        mandate.setLastAttemptDate(today);
        mandate.setLastDebitAt(java.time.LocalDateTime.now());
        mandate.setLastDebitStatus("SUCCESS");
        mandate.setLastDebitMessage("RD installment " + deposit.getPaidInstallments() + " credited to contribution ledger.");
        mandate.setSuccessfulDebits((mandate.getSuccessfulDebits() == null ? 0 : mandate.getSuccessfulDebits()) + 1);
        mandateRepository.save(mandate);
        event(mandate, "DEBIT_SUCCESS", deposit.getPaidInstallments(), amount, mandate.getLastDebitMessage(), "SYSTEM");
        if ("CANCELLED".equals(mandate.getStatus())) {
            event(mandate, "CANCELLED", null, null, mandate.getLastDebitMessage(), "SYSTEM");
        }
        historyService.record("RD", deposit.getId(), deposit.getAccountNumber(), "INSTALLMENT_CREDITED",
                "SYSTEM", "ECS installment credited to the recurring-deposit contribution ledger.",
                contributionMonth + " · ₹" + amount + " · " + transactionRef);
        return true;
    }

    private void fail(EcsMandate mandate, LocalDate today, Double amount, String message) {
        mandate.setLastAttemptDate(today);
        mandate.setLastDebitStatus("FAILED");
        mandate.setLastDebitMessage(message);
        mandate.setFailedDebits((mandate.getFailedDebits() == null ? 0 : mandate.getFailedDebits()) + 1);
        mandateRepository.save(mandate);
        event(mandate, "DEBIT_FAILED", null, amount, message, "SYSTEM");
    }

    private void event(EcsMandate mandate, String type, Integer installment, Double amount, String message, String actor) {
        EcsMandateEvent event = new EcsMandateEvent();
        event.setMandateDbId(mandate.getId());
        event.setMandateId(mandate.getMandateId());
        event.setLoanAccountNumber(mandate.getLoanAccountNumber());
        event.setSavingsAccountNumber(mandate.getSavingsAccountNumber());
        event.setEventType(type);
        event.setEmiNumber(installment);
        event.setAmount(amount);
        event.setMessage(message);
        event.setActor(actor);
        eventRepository.save(event);
    }

    private double round(double amount) {
        return Math.round(amount * 100.0) / 100.0;
    }
}
