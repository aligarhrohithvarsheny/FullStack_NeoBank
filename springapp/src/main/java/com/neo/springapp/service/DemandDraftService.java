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
        if (chequeRepository.findByChequeNumber(chequeNumber.trim())
                .filter(cheque -> accountNumber.equals(cheque.getAccountNumber()) && cheque.isAvailable())
                .isEmpty()) return null;
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
        com.neo.springapp.model.Cheque cheque = chequeRepository.findByChequeNumber(request.getChequeNumber().trim())
                .filter(c -> accountNumber.equals(c.getAccountNumber()))
                .orElseThrow(() -> new IllegalArgumentException("Cheque number is not allocated to this savings account"));
        if (!cheque.isAvailable()) throw new IllegalArgumentException("Cheque number has already been used and cannot be reused");
        if (draftRepository.findByChequeNumberAndAccountNumber(request.getChequeNumber().trim(), accountNumber).isPresent()) throw new IllegalArgumentException("Cheque number already used");
        if (request.getAmount() == null || request.getAmount().signum() <= 0) throw new IllegalArgumentException("Amount must be greater than zero");
        BigDecimal balance = BigDecimal.valueOf(account.getBalance() == null ? 0 : account.getBalance());
        if (balance.compareTo(request.getAmount()) < 0) throw new IllegalArgumentException("Insufficient balance");
        request.setAccountNumber(accountNumber); request.setUserName(account.getName()); request.setAvailableBalance(balance); request.setLockedAmount(request.getAmount()); request.setStatus("PENDING"); request.setDraftDate(request.getDraftDate() == null ? LocalDate.now() : request.getDraftDate());
        DemandDraft saved = draftRepository.save(request);
        // Mark the cheque as USED so it moves to the "Used" section and can never be reused for another DD/draw
        cheque.markUsed("DEMAND_DRAFT", saved.getId() == null ? saved.getChequeNumber() : ("DD-ID-" + saved.getId()));
        chequeRepository.save(cheque);
        return saved;
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

    @Transactional
    public DemandDraft reject(Long id, String admin, String reason) {
        DemandDraft d = draftRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("DD not found"));
        d.setStatus("REJECTED"); d.setApprovedBy(admin + (reason == null ? "" : " - " + reason)); d.setLockedAmount(null);
        // Release the cheque back to ACTIVE since it was never actually consumed
        chequeRepository.findByChequeNumber(d.getChequeNumber()).filter(c -> "USED".equals(c.getStatus()) && d.getAccountNumber().equals(c.getAccountNumber())).ifPresent(c -> {
            c.setStatus("ACTIVE"); c.setUsedDate(null); c.setUsedFor(null); c.setUsedReference(null);
            chequeRepository.save(c);
        });
        return draftRepository.save(d);
    }

    public byte[] pdf(Long id) throws Exception {
        DemandDraft d = draftRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("DD not found"));
        if (!"APPROVED".equals(d.getStatus())) throw new IllegalArgumentException("DD is not approved");
        String html = generateDemandDraftHtml(d);
        ByteArrayOutputStream output = new ByteArrayOutputStream(); HtmlConverter.convertToPdf(html, output); return output.toByteArray();
    }

    // Builds the NeoBank-stamped Demand Draft HTML (same visual language as the cheque PDF:
    // bordered container, caret bank logo, individual date-digit boxes, amount boxes and an
    // official round stamp/seal) so admin-side and user-side downloads look identical.
    private String generateDemandDraftHtml(DemandDraft d) {
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy");
        LocalDate draftDate = d.getDraftDate() != null ? d.getDraftDate() : LocalDate.now();
        String[] dateParts = draftDate.format(formatter).split("-");
        String day1 = dateParts[0].substring(0, 1), day2 = dateParts[0].substring(1, 2);
        String month1 = dateParts[1].substring(0, 1), month2 = dateParts[1].substring(1, 2);
        String year = dateParts[2];

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>Demand Draft - NeoBank</title><style>")
            .append("@page { size: A4 landscape; margin: 0; }")
            .append("body { font-family: 'Arial', sans-serif; margin: 0; padding: 0; background: linear-gradient(135deg, #fff5e6 0%, #ffe8cc 100%); }")
            .append(".dd-wrapper { width: 100vw; height: 100vh; display: flex; justify-content: center; align-items: center; padding: 20px; box-sizing: border-box; }")
            .append(".dd-container { width: 900px; min-height: 420px; background: white; position: relative; border: 2px solid #333; box-shadow: 0 8px 32px rgba(0,0,0,0.15); }")
            .append(".dd-background { position: absolute; top: 0; left: 0; width: 100%; height: 100%; opacity: 0.03; background-image: radial-gradient(circle, #ff8c42 2px, transparent 2px); background-size: 40px 40px; pointer-events: none; }")
            .append(".dd-content { position: relative; z-index: 1; padding: 30px 40px; }")
            .append(".dd-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 20px; padding-bottom: 15px; border-bottom: 2px solid #333; }")
            .append(".bank-name-section { display: flex; align-items: center; gap: 10px; }")
            .append(".bank-caret { font-size: 24px; color: #333; font-weight: bold; }")
            .append(".bank-name { font-size: 22px; font-weight: bold; color: #000; letter-spacing: 1px; }")
            .append(".dd-title { font-size: 16px; font-weight: bold; letter-spacing: 3px; color: #333; margin-top: 4px; }")
            .append(".date-section { display: flex; gap: 8px; align-items: flex-end; }")
            .append(".date-box { width: 35px; height: 35px; border: 2px solid #333; display: inline-flex; align-items: center; justify-content: center; font-size: 16px; font-weight: bold; background: white; }")
            .append(".date-label { font-size: 10px; text-align: center; margin-top: 2px; color: #666; }")
            .append(".date-row { display: flex; flex-direction: column; align-items: center; gap: 2px; }")
            .append(".dd-body { display: grid; grid-template-columns: 1fr 1fr; gap: 30px; margin: 20px 0; }")
            .append(".field-group { display: flex; align-items: baseline; gap: 10px; margin-bottom: 18px; }")
            .append(".field-label { font-size: 13px; font-weight: bold; color: #333; min-width: 110px; }")
            .append(".field-value { flex: 1; border-bottom: 2px solid #000; height: 25px; font-size: 14px; padding-bottom: 2px; }")
            .append(".amount-figures-section { display: flex; gap: 8px; align-items: center; margin-top: 10px; }")
            .append(".amount-box { min-width: 40px; height: 45px; border: 2px solid #333; display: inline-flex; align-items: center; justify-content: center; font-size: 16px; font-weight: bold; background: white; padding: 0 6px; }")
            .append(".rupee-symbol { font-size: 20px; font-weight: bold; margin-right: 5px; }")
            .append(".dd-footer { display: flex; justify-content: space-between; align-items: flex-end; margin-top: 40px; padding-top: 20px; border-top: 1px solid #ccc; }")
            .append(".signature-section { display: flex; flex-direction: column; align-items: center; gap: 5px; }")
            .append(".signature-line { width: 250px; border-bottom: 2px solid #000; height: 30px; }")
            .append(".signature-label { font-size: 11px; color: #666; margin-top: 5px; }")
            .append(".dd-stamp { width: 130px; height: 130px; border-radius: 50%; border: 3px double #b8860b; display: flex; flex-direction: column; align-items: center; justify-content: center; transform: rotate(-12deg); color: #b8860b; text-align: center; }")
            .append(".dd-stamp .stamp-title { font-size: 13px; font-weight: bold; letter-spacing: 1px; }")
            .append(".dd-stamp .stamp-sub { font-size: 9px; margin-top: 4px; letter-spacing: 1px; }")
            .append(".dd-number-display { position: absolute; top: 10px; right: 15px; font-size: 11px; color: #666; }")
            .append("@media print { body { background: white; } .dd-wrapper { padding: 0; } .dd-container { box-shadow: none; border: none; } }")
            .append("</style></head><body>")
            .append("<div class=\"dd-wrapper\"><div class=\"dd-container\"><div class=\"dd-background\"></div>")
            .append("<div class=\"dd-number-display\">DD No: ").append(d.getDdNumber() == null ? "" : d.getDdNumber()).append("</div>")
            .append("<div class=\"dd-content\">")
            .append("<div class=\"dd-header\"><div class=\"bank-name-section\">")
            .append("<span class=\"bank-caret\">&#9650;</span><div><div class=\"bank-name\">NEOBANK</div><div class=\"dd-title\">DEMAND DRAFT</div></div>")
            .append("</div>")
            .append("<div class=\"date-section\">")
            .append("<div class=\"date-row\"><div class=\"date-box\">").append(day1).append("</div><div class=\"date-label\">D</div></div>")
            .append("<div class=\"date-row\"><div class=\"date-box\">").append(day2).append("</div><div class=\"date-label\">D</div></div>")
            .append("<div class=\"date-row\"><div class=\"date-box\">").append(month1).append("</div><div class=\"date-label\">M</div></div>")
            .append("<div class=\"date-row\"><div class=\"date-box\">").append(month2).append("</div><div class=\"date-label\">M</div></div>")
            .append("<div class=\"date-row\"><div class=\"date-box\">").append(year.length() > 0 ? year.substring(0,1) : "").append("</div><div class=\"date-label\">Y</div></div>")
            .append("<div class=\"date-row\"><div class=\"date-box\">").append(year.length() > 1 ? year.substring(1,2) : "").append("</div><div class=\"date-label\">Y</div></div>")
            .append("<div class=\"date-row\"><div class=\"date-box\">").append(year.length() > 2 ? year.substring(2,3) : "").append("</div><div class=\"date-label\">Y</div></div>")
            .append("<div class=\"date-row\"><div class=\"date-box\">").append(year.length() > 3 ? year.substring(3,4) : "").append("</div><div class=\"date-label\">Y</div></div>")
            .append("</div></div>")
            .append("<div class=\"dd-body\"><div>")
            .append("<div class=\"field-group\"><span class=\"field-label\">Pay To</span><span class=\"field-value\">").append(nullSafe(d.getPayeeName())).append("</span></div>")
            .append("<div class=\"field-group\"><span class=\"field-label\">Payee A/c No.</span><span class=\"field-value\">").append(nullSafe(d.getPayeeAccountNumber())).append("</span></div>")
            .append("<div class=\"field-group\"><span class=\"field-label\">Purchaser</span><span class=\"field-value\">").append(nullSafe(d.getUserName())).append("</span></div>")
            .append("<div class=\"field-group\"><span class=\"field-label\">Purchaser A/c</span><span class=\"field-value\">").append(nullSafe(d.getAccountNumber())).append("</span></div>")
            .append("<div class=\"field-group\"><span class=\"field-label\">Cheque Ref.</span><span class=\"field-value\">").append(nullSafe(d.getChequeNumber())).append("</span></div>")
            .append("</div><div>")
            .append("<div class=\"amount-figures-section\"><span class=\"rupee-symbol\">&#8377;</span><div class=\"amount-box\">").append(d.getAmount() == null ? "0.00" : d.getAmount().toString()).append("</div></div>")
            .append("<div class=\"field-group\" style=\"margin-top:20px\"><span class=\"field-label\">Reason</span><span class=\"field-value\">").append(nullSafe(d.getReason())).append("</span></div>")
            .append("</div></div>")
            .append("<div class=\"dd-footer\">")
            .append("<div class=\"signature-section\"><div class=\"signature-line\"></div><div class=\"signature-label\">Authorized Signatory</div></div>")
            .append("<div class=\"dd-stamp\"><span class=\"stamp-title\">NEOBANK</span><span class=\"stamp-sub\">OFFICIAL SEAL</span><span class=\"stamp-sub\">AUTHORIZED</span></div>")
            .append("</div>")
            .append("</div></div></div></body></html>");
        return html.toString();
    }

    private static String nullSafe(String v) { return v == null ? "" : v; }
}
