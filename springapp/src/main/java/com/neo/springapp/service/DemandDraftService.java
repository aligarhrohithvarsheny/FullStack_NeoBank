package com.neo.springapp.service;

import com.itextpdf.html2pdf.HtmlConverter;
import com.neo.springapp.model.Account;
import com.neo.springapp.model.DemandDraft;
import com.neo.springapp.model.Transaction;
import com.neo.springapp.repository.AccountRepository;
import com.neo.springapp.repository.DemandDraftRepository;
import com.neo.springapp.repository.ChequeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class DemandDraftService {
    private final DemandDraftRepository draftRepository;
    private final AccountRepository accountRepository;
    private final TransactionService transactionService;
    private final ChequeRepository chequeRepository;

    public DemandDraftService(DemandDraftRepository draftRepository, AccountRepository accountRepository, TransactionService transactionService, ChequeRepository chequeRepository) {
        this.draftRepository = draftRepository; this.accountRepository = accountRepository; this.transactionService = transactionService; this.chequeRepository = chequeRepository;
    }

    public Account verifyCheque(String accountNumber, String chequeNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber);
        if (account == null || chequeNumber == null || chequeNumber.isBlank()) return null;
        if (chequeRepository.findByChequeNumber(chequeNumber.trim()).filter(cheque -> accountNumber.equals(cheque.getAccountNumber())).isEmpty()) return null;
        return account;
    }

    public List<DemandDraft> findByAccount(String accountNumber) { return draftRepository.findByAccountNumberOrderByCreatedAtDesc(accountNumber); }
    public List<DemandDraft> findAll() { return draftRepository.findAll(org.springframework.data.domain.Sort.by("createdAt").descending()); }

    @Transactional
    public DemandDraft create(String accountNumber, DemandDraft request) {
        Account account = accountRepository.findByAccountNumber(accountNumber);
        if (account == null) throw new IllegalArgumentException("Savings account not found");
        if (!"ACTIVE".equalsIgnoreCase(account.getStatus())) throw new IllegalArgumentException("Account is not active");
        if (request.getChequeNumber() == null || request.getChequeNumber().isBlank()) throw new IllegalArgumentException("Cheque number is required");
        if (chequeRepository.findByChequeNumber(request.getChequeNumber().trim()).filter(cheque -> accountNumber.equals(cheque.getAccountNumber())).isEmpty()) throw new IllegalArgumentException("Cheque number is not allocated to this savings account");
        if (draftRepository.findByChequeNumberAndAccountNumber(request.getChequeNumber().trim(), accountNumber).isPresent()) throw new IllegalArgumentException("Cheque number already used");
        if (request.getAmount() == null || request.getAmount().signum() <= 0) throw new IllegalArgumentException("Amount must be greater than zero");
        BigDecimal balance = BigDecimal.valueOf(account.getBalance() == null ? 0 : account.getBalance());
        if (balance.compareTo(request.getAmount()) < 0) throw new IllegalArgumentException("Insufficient balance");
        request.setAccountNumber(accountNumber); request.setUserName(account.getName()); request.setAvailableBalance(balance); request.setLockedAmount(request.getAmount()); request.setStatus("PENDING"); request.setDraftDate(request.getDraftDate() == null ? LocalDate.now() : request.getDraftDate());
        return draftRepository.save(request);
    }

    @Transactional
    public DemandDraft update(Long id, Map<String,Object> data, String admin) {
        DemandDraft draft = draftRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("DD not found"));
        if (!"PENDING".equals(draft.getStatus())) throw new IllegalArgumentException("Only pending DDs can be edited");
        String before = draft.toString();
        if (data.get("payeeName") != null) draft.setPayeeName(String.valueOf(data.get("payeeName")));
        if (data.get("payeeAccountNumber") != null) draft.setPayeeAccountNumber(String.valueOf(data.get("payeeAccountNumber")));
        if (data.get("reason") != null) draft.setReason(String.valueOf(data.get("reason")));
        if (data.get("draftDate") != null) draft.setDraftDate(LocalDate.parse(String.valueOf(data.get("draftDate"))));
        if (data.get("amount") != null) {
            BigDecimal amount = new BigDecimal(String.valueOf(data.get("amount")));
            if (amount.signum() <= 0 || draft.getAvailableBalance().compareTo(amount) < 0) throw new IllegalArgumentException("Invalid amount or insufficient balance");
            draft.setAmount(amount); draft.setLockedAmount(amount);
        }
        draft.setEditHistory((draft.getEditHistory() == null ? "" : draft.getEditHistory() + "\n") + LocalDateTime.now() + " by " + (admin == null ? "Admin" : admin) + " | before=" + before + " | after=" + draft);
        return draftRepository.save(draft);
    }

    @Transactional
    public DemandDraft approve(Long id, String admin) {
        DemandDraft draft = draftRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("DD not found"));
        if (!"PENDING".equals(draft.getStatus())) throw new IllegalArgumentException("DD is not pending");
        draft.setDdNumber("DD" + System.currentTimeMillis());
        Account account = accountRepository.findByAccountNumber(draft.getAccountNumber());
        Double newBalance = account.getBalance() - draft.getAmount().doubleValue(); account.setBalance(newBalance); account.setLastUpdated(LocalDateTime.now()); accountRepository.save(account);
        Transaction transaction = new Transaction(); transaction.setAccountNumber(account.getAccountNumber()); transaction.setUserName(account.getName()); transaction.setAmount(draft.getAmount().doubleValue()); transaction.setType("Debit"); transaction.setMerchant("Demand Draft"); transaction.setDescription("Demand Draft " + draft.getDdNumber()); transaction.setBalance(newBalance); transaction.setStatus("Completed"); transactionService.saveTransaction(transaction);
        draft.setStatus("APPROVED"); draft.setApprovedBy(admin); draft.setApprovedAt(LocalDateTime.now()); draft.setLockedAmount(null); return draftRepository.save(draft);
    }

    @Transactional public DemandDraft reject(Long id, String admin, String reason) { DemandDraft d = draftRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("DD not found")); d.setStatus("REJECTED"); d.setApprovedBy(admin + (reason == null ? "" : " - " + reason)); d.setLockedAmount(null); return draftRepository.save(d); }

    public byte[] pdf(Long id) throws Exception {
        DemandDraft d = draftRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("DD not found"));
        if (!"APPROVED".equals(d.getStatus())) throw new IllegalArgumentException("DD is not approved");
        String html = "<html><body style='font-family:Arial;border:3px solid #123;padding:35px'><h1 style='text-align:center'>NEOBANK</h1><h2 style='text-align:center'>DEMAND DRAFT</h2><hr><p><b>DD Number:</b> "+d.getDdNumber()+"</p><p><b>Customer:</b> "+d.getUserName()+"</p><p><b>Account:</b> "+d.getAccountNumber()+"</p><p><b>Payee:</b> "+d.getPayeeName()+"</p><p><b>Payee Account:</b> "+d.getPayeeAccountNumber()+"</p><p><b>Amount:</b> ₹"+d.getAmount()+"</p><p><b>Date:</b> "+d.getDraftDate()+"</p><p><b>Time:</b> "+d.getApprovedAt()+"</p><br><p style='text-align:right'><b>Authorized Signature</b><br>NeoBank Official Stamp</p></body></html>";
        ByteArrayOutputStream output = new ByteArrayOutputStream(); HtmlConverter.convertToPdf(html, output); return output.toByteArray();
    }
}
