package com.neo.springapp.service;

import com.neo.springapp.model.Account;
import com.neo.springapp.model.HomeLoan;
import com.neo.springapp.model.HomeLoanEvent;
import com.neo.springapp.model.Transaction;
import com.neo.springapp.repository.AccountRepository;
import com.neo.springapp.repository.HomeLoanEventRepository;
import com.neo.springapp.repository.HomeLoanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class HomeLoanService {

    private static final double PROCESSING_FEE_PCT = 1.0;
    private static final double GST_PCT = 18.0;
    private static final double LEGAL_CHARGES = 1000.0;
    private static final double PREPAYMENT_CHARGE_PCT = 1.0;
    private static final double CLOSURE_CHARGE_PCT = 2.0;
    private static final double RENEWAL_FEE_PCT = 0.5;
    private static final Path UPLOAD_DIR = Paths.get("uploads", "home-loan-documents");
    private static final Set<String> DOC_TYPES = Set.of("fdReceipt", "model", "signature");

    @Autowired private HomeLoanRepository repo;
    @Autowired private HomeLoanEventRepository eventRepo;
    @Autowired private AccountRepository accountRepository;
    @Autowired private TransactionService transactionService;
    @Autowired private HomeLoanAnalysisService analysisService;

    private static double n(Double v) { return v == null ? 0.0 : v; }
    private static double r2(double v) { return Math.round(v * 100.0) / 100.0; }

    // ---------- Application ----------

    @Transactional
    public HomeLoan submit(HomeLoan in) {
        if (in.getAccountNumber() == null || in.getAccountNumber().isBlank()) throw new RuntimeException("Account number is required");
        Account account = accountRepository.findByAccountNumber(in.getAccountNumber());
        if (account == null) throw new RuntimeException("Account not found");
        if (n(in.getAmount()) < 100000) throw new RuntimeException("Minimum home loan amount is 1,00,000");
        if (in.getTenure() == null || in.getTenure() < 12 || in.getTenure() > 360) throw new RuntimeException("Tenure must be between 12 and 360 months");
        if (n(in.getInterestRate()) <= 0 || in.getInterestRate() > 30) throw new RuntimeException("Enter a valid interest rate");

        HomeLoan h = new HomeLoan();
        h.setAccountNumber(in.getAccountNumber());
        h.setUserName(account.getName());
        h.setUserEmail(in.getUserEmail());
        h.setPan(account.getPan());
        h.setAmount(in.getAmount());
        h.setTenure(in.getTenure());
        h.setInterestRate(in.getInterestRate());
        h.setPurpose(in.getPurpose());
        h.setPropertyAddress(in.getPropertyAddress());
        h.setPropertyValue(in.getPropertyValue());
        h.setApplicationId("HL" + java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDate.now())
                + String.format("%05d", new Random().nextInt(100000)));
        h.setStatus("Submitted");
        h.setFdReceiptStatus("Pending");
        h.setModelDocStatus("Pending");
        h.setSignatureStatus("Pending");
        Map<String, Object> a = analysisService.analyze(h.getAccountNumber(), h.getAmount(), h.getTenure(), h.getInterestRate(), h.getPropertyValue());
        h.setApprovalProbability((Double) a.get("approvalProbability"));
        h.setRiskBand((String) a.get("riskBand"));
        h = repo.save(h);
        event(h, "SUBMITTED", h.getUserName(), "Home loan application " + h.getApplicationId() + " submitted for "
                + r2(h.getAmount()) + " over " + h.getTenure() + " months @ " + h.getInterestRate() + "%", null, null, null, null);
        return h;
    }

    public List<HomeLoan> all() { return repo.findAllByOrderByApplicationDateDesc(); }
    public List<HomeLoan> byAccount(String acc) { return repo.findByAccountNumberOrderByApplicationDateDesc(acc); }
    public HomeLoan get(Long id) { return repo.findById(id).orElseThrow(() -> new RuntimeException("Home loan not found")); }

    // ---------- Workflow ----------

    @Transactional
    public HomeLoan advance(Long id, String admin) {
        HomeLoan h = get(id);
        String from = h.getStatus();
        switch (from) {
            case "Submitted": h.setStatus("Under Review"); break;
            case "Under Review": h.setStatus("Documents Required"); break;
            case "Documents Submitted":
                if (!allDocsVerified(h)) throw new RuntimeException("Verify all three documents (or request re-upload) first");
                h.setStatus("Documents Verified");
                break;
            case "Documents Required": throw new RuntimeException("Waiting for the customer to upload documents");
            case "Documents Verified": throw new RuntimeException("Ready for approval - use Approve");
            default: throw new RuntimeException("Cannot advance from status " + from);
        }
        h.setReviewedBy(admin);
        touch(h);
        repo.save(h);
        event(h, "STATUS", admin, from + " -> " + h.getStatus(), null, null, null, null);
        return h;
    }

    @Transactional
    public HomeLoan reject(Long id, String admin, String reason) {
        HomeLoan h = get(id);
        if ("Approved".equals(h.getStatus()) || "Closed".equals(h.getStatus()) || "Cancelled".equals(h.getStatus())) throw new RuntimeException("Loan already approved/closed/cancelled");
        h.setStatus("Rejected");
        h.setRejectionReason(reason);
        h.setReviewedBy(admin);
        touch(h);
        repo.save(h);
        event(h, "STATUS", admin, "Rejected: " + (reason == null ? "-" : reason), null, null, null, null);
        return h;
    }

    public HomeLoan cancel(Long id, String accountNumber, String by) {
        HomeLoan h = get(id);
        if (accountNumber == null || !accountNumber.equals(h.getAccountNumber())) throw new RuntimeException("Application does not belong to this account");
        if (("Approved".equals(h.getStatus()) || "Closed".equals(h.getStatus()) || "Rejected".equals(h.getStatus()) || "Cancelled".equals(h.getStatus())))
            throw new RuntimeException("Application can no longer be cancelled (status: " + h.getStatus() + ")");
        h.setStatus("Cancelled");
        touch(h);
        repo.save(h);
        event(h, "STATUS", by, "Application cancelled by customer", null, null, null, null);
        return h;
    }

    // ---------- Documents ----------

    @Transactional
    public HomeLoan uploadDocument(Long id, String type, MultipartFile file) throws IOException {
        if (!DOC_TYPES.contains(type)) throw new RuntimeException("Unknown document type");
        if (file == null || file.isEmpty()) throw new RuntimeException("File is empty");
        HomeLoan h = get(id);
        if (!List.of("Documents Required", "Documents Submitted").contains(h.getStatus()))
            throw new RuntimeException("Documents can be uploaded only when requested by the bank");
        String orig = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String ext = orig.contains(".") ? orig.substring(orig.lastIndexOf('.')).toLowerCase() : "";
        if (!List.of(".pdf", ".png", ".jpg", ".jpeg").contains(ext)) throw new RuntimeException("Only PDF, PNG or JPG files are allowed");
        Files.createDirectories(UPLOAD_DIR);
        String name = h.getApplicationId() + "-" + type + "-" + System.currentTimeMillis() + ext;
        Files.copy(file.getInputStream(), UPLOAD_DIR.resolve(name), StandardCopyOption.REPLACE_EXISTING);
        switch (type) {
            case "fdReceipt": h.setFdReceiptPath(name); h.setFdReceiptStatus("Pending"); h.setFdReceiptRemark(null); break;
            case "model": h.setModelDocPath(name); h.setModelDocStatus("Pending"); h.setModelDocRemark(null); break;
            default: h.setSignaturePath(name); h.setSignatureStatus("Pending"); h.setSignatureRemark(null);
        }
        boolean complete = h.getFdReceiptPath() != null && h.getModelDocPath() != null && h.getSignaturePath() != null
                && !"Reupload Required".equals(h.getFdReceiptStatus()) && !"Reupload Required".equals(h.getModelDocStatus())
                && !"Reupload Required".equals(h.getSignatureStatus());
        if (complete) h.setStatus("Documents Submitted");
        touch(h);
        repo.save(h);
        event(h, "DOCUMENT", h.getUserName(), "Uploaded " + docLabel(type) + " (" + orig + ")", null, null, null, null);
        return h;
    }

    @Transactional
    public HomeLoan verifyDocument(Long id, String type, String decision, String remark, String admin) {
        if (!DOC_TYPES.contains(type)) throw new RuntimeException("Unknown document type");
        if (!List.of("Verified", "Reupload Required").contains(decision)) throw new RuntimeException("Decision must be Verified or Reupload Required");
        HomeLoan h = get(id);
        switch (type) {
            case "fdReceipt": requireFile(h.getFdReceiptPath()); h.setFdReceiptStatus(decision); h.setFdReceiptRemark(remark); break;
            case "model": requireFile(h.getModelDocPath()); h.setModelDocStatus(decision); h.setModelDocRemark(remark); break;
            default: requireFile(h.getSignaturePath()); h.setSignatureStatus(decision); h.setSignatureRemark(remark);
        }
        if ("Reupload Required".equals(decision)) h.setStatus("Documents Required");
        else if (allDocsVerified(h)) h.setStatus("Documents Verified");
        h.setReviewedBy(admin);
        touch(h);
        repo.save(h);
        event(h, "DOCUMENT", admin, docLabel(type) + ": " + decision + (remark == null || remark.isBlank() ? "" : " - " + remark), null, null, null, null);
        return h;
    }

    public Map<String, Object> documentFile(Long id, String type) throws IOException {
        HomeLoan h = get(id);
        String name = switch (type) {
            case "fdReceipt" -> h.getFdReceiptPath();
            case "model" -> h.getModelDocPath();
            case "signature" -> h.getSignaturePath();
            default -> null;
        };
        if (name == null) throw new RuntimeException("Document not uploaded");
        Path p = UPLOAD_DIR.resolve(name).normalize();
        if (!p.startsWith(UPLOAD_DIR.normalize()) || !Files.exists(p)) throw new RuntimeException("File missing");
        Map<String, Object> m = new HashMap<>();
        m.put("path", p);
        m.put("name", name);
        return m;
    }

    private boolean allDocsVerified(HomeLoan h) {
        return "Verified".equals(h.getFdReceiptStatus()) && "Verified".equals(h.getModelDocStatus()) && "Verified".equals(h.getSignatureStatus())
                && h.getFdReceiptPath() != null && h.getModelDocPath() != null && h.getSignaturePath() != null;
    }
    private void requireFile(String p) { if (p == null) throw new RuntimeException("Document not uploaded yet"); }
    private String docLabel(String t) { return "fdReceipt".equals(t) ? "FD receipt" : "model".equals(t) ? "Home loan model" : "Signature"; }

    // ---------- Edit / approve ----------

    @Transactional
    public HomeLoan adminEdit(Long id, Map<String, Object> f, String admin) {
        HomeLoan h = get(id);
        if ("Closed".equals(h.getStatus()) || "Rejected".equals(h.getStatus()) || "Cancelled".equals(h.getStatus())) throw new RuntimeException("Closed/rejected/cancelled loans cannot be edited");
        boolean approved = "Approved".equals(h.getStatus());
        StringBuilder diff = new StringBuilder();
        if (!approved) {
            if (f.get("amount") != null) { double v = num(f.get("amount")); if (v < 100000) throw new RuntimeException("Minimum amount is 1,00,000"); diff.append("amount ").append(h.getAmount()).append("->").append(v).append("; "); h.setAmount(v); }
            if (f.get("tenure") != null) { int v = (int) num(f.get("tenure")); if (v < 12 || v > 360) throw new RuntimeException("Tenure must be 12-360 months"); diff.append("tenure ").append(h.getTenure()).append("->").append(v).append("; "); h.setTenure(v); }
        }
        if (f.get("interestRate") != null) {
            double v = num(f.get("interestRate"));
            if (v <= 0 || v > 30) throw new RuntimeException("Invalid interest rate");
            if (n(h.getInterestRate()) != v) event(h, "RATE_CHANGE", admin, "Interest rate " + n(h.getInterestRate()) + " -> " + v, null, null, null, null);
            diff.append("rate ").append(h.getInterestRate()).append("->").append(v).append("; ");
            h.setInterestRate(v);
            if (approved) {
                h.setEmi(r2(HomeLoanAnalysisService.emi(n(h.getRemainingPrincipal()), v, h.getRemainingTenure())));
            }
        }
        if (f.get("propertyValue") != null) { h.setPropertyValue(num(f.get("propertyValue"))); diff.append("propertyValue; "); }
        if (f.get("propertyAddress") != null) { h.setPropertyAddress(String.valueOf(f.get("propertyAddress"))); diff.append("propertyAddress; "); }
        if (f.get("purpose") != null) { h.setPurpose(String.valueOf(f.get("purpose"))); diff.append("purpose; "); }
        if (f.get("adminNotes") != null) { h.setAdminNotes(String.valueOf(f.get("adminNotes"))); diff.append("notes; "); }
        if (!approved && (f.get("processingFee") != null)) { diff.append("processingFee override; "); h.setProcessingFee(num(f.get("processingFee"))); }
        h.setReviewedBy(admin);
        touch(h);
        Map<String, Object> a = analysisService.analyze(h.getAccountNumber(), h.getAmount(), h.getTenure(), h.getInterestRate(), h.getPropertyValue());
        h.setApprovalProbability((Double) a.get("approvalProbability"));
        h.setRiskBand((String) a.get("riskBand"));
        repo.save(h);
        event(h, "EDITED", admin, "Edited: " + diff, null, null, null, null);
        return h;
    }

    public Map<String, Object> chargesPreview(double amount) {
        double fee = r2(amount * PROCESSING_FEE_PCT / 100);
        double gst = r2(fee * GST_PCT / 100);
        double total = r2(fee + gst + LEGAL_CHARGES);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("loanAmount", amount);
        m.put("processingFee", fee);
        m.put("gstOnFee", gst);
        m.put("legalCharges", LEGAL_CHARGES);
        m.put("totalCharges", total);
        m.put("netDisbursed", r2(amount - total));
        return m;
    }

    @Transactional
    public HomeLoan approve(Long id, String admin) {
        HomeLoan h = get(id);
        if (!"Documents Verified".equals(h.getStatus())) throw new RuntimeException("All documents must be verified before approval");
        Account acc = accountRepository.findByAccountNumber(h.getAccountNumber());
        if (acc == null) throw new RuntimeException("Account not found");
        double amount = n(h.getAmount());
        double fee = h.getProcessingFee() != null ? h.getProcessingFee() : r2(amount * PROCESSING_FEE_PCT / 100);
        double gst = r2(fee * GST_PCT / 100);
        double total = r2(fee + gst + LEGAL_CHARGES);
        h.setProcessingFee(fee);
        h.setGstOnFee(gst);
        h.setLegalCharges(LEGAL_CHARGES);
        h.setTotalCharges(total);
        h.setNetDisbursed(r2(amount - total));
        h.setEmi(r2(HomeLoanAnalysisService.emi(amount, h.getInterestRate(), h.getTenure())));
        h.setRemainingTenure(h.getTenure());
        h.setRemainingPrincipal(amount);
        h.setPaidEmis(0);
        h.setPrincipalPaid(0.0);
        h.setInterestPaid(0.0);
        h.setPrepaidAmount(0.0);
        h.setLoanAccountNumber("HLN" + System.currentTimeMillis());
        h.setApprovalDate(LocalDateTime.now());
        h.setApprovedBy(admin);
        h.setNextEmiDate(LocalDate.now().plusMonths(1));
        h.setStatus("Approved");
        touch(h);
        double newBal = r2(n(acc.getBalance()) + h.getNetDisbursed());
        acc.setBalance(newBal);
        accountRepository.save(acc);
        saveTxn(h, "Home Loan Disbursement", h.getNetDisbursed(), "Loan Credit", newBal,
                "Home Loan " + h.getLoanAccountNumber() + " disbursed (" + r2(amount) + " less charges " + total + ")");
        repo.save(h);
        event(h, "DISBURSED", admin, "Approved. Sanctioned " + r2(amount) + ", charges deducted " + total
                + " (fee " + fee + " + GST " + gst + " + legal " + LEGAL_CHARGES + "), net credited " + h.getNetDisbursed(),
                null, h.getNetDisbursed(), null, null, amount);
        return h;
    }

    // ---------- Repayments ----------

    private HomeLoan activeLoan(Long id) {
        HomeLoan h = get(id);
        if (!"Approved".equals(h.getStatus())) throw new RuntimeException("Loan is not active");
        return h;
    }

    private double monthlyInterest(HomeLoan h) { return r2(n(h.getRemainingPrincipal()) * n(h.getInterestRate()) / 1200.0); }

    @Transactional
    public HomeLoan payEmi(Long id, String actor) { return payEmi(id, actor, null); }

    @Transactional
    public HomeLoan payEmi(Long id, String actor, String ref) {
        HomeLoan h = activeLoan(id);
        double interest = monthlyInterest(h);
        double rp = n(h.getRemainingPrincipal());
        double principal = Math.min(r2(n(h.getEmi()) - interest), rp);
        boolean last = h.getRemainingTenure() <= 1 || principal >= rp - 0.01;
        if (last) principal = rp;
        double debit = r2(principal + interest);
        if (ref == null) debitAccount(h, debit, "Home Loan EMI", "Home Loan EMI " + (h.getPaidEmis() + 1) + " - " + h.getLoanAccountNumber());
        h.setPrincipalPaid(r2(n(h.getPrincipalPaid()) + principal));
        h.setInterestPaid(r2(n(h.getInterestPaid()) + interest));
        h.setRemainingPrincipal(r2(rp - principal));
        h.setPaidEmis(h.getPaidEmis() + 1);
        h.setRemainingTenure(Math.max(0, h.getRemainingTenure() - 1));
        h.setNextEmiDate(n(h.getRemainingPrincipal()) <= 0 ? null : h.getNextEmiDate().plusMonths(1));
        if (n(h.getRemainingPrincipal()) <= 0.01) { h.setRemainingPrincipal(0.0); h.setStatus("Closed"); h.setClosureDate(LocalDateTime.now()); h.setClosureAmount(0.0); }
        touch(h);
        repo.save(h);
        event(h, "EMI", actor, "EMI #" + h.getPaidEmis() + " paid" + refNote(ref), debit, null, principal, interest, h.getRemainingPrincipal());
        return h;
    }

    @Transactional
    public HomeLoan payInterest(Long id, String actor) {
        HomeLoan h = activeLoan(id);
        double interest = monthlyInterest(h);
        debitAccount(h, interest, "Home Loan Interest", "Interest-only payment - " + h.getLoanAccountNumber());
        h.setInterestPaid(r2(n(h.getInterestPaid()) + interest));
        h.setNextEmiDate(h.getNextEmiDate().plusMonths(1));
        touch(h);
        repo.save(h);
        event(h, "INTEREST", actor, "Interest-only payment for cycle (principal unchanged)", interest, null, 0.0, interest, h.getRemainingPrincipal());
        return h;
    }

    public Map<String, Object> emiDue(Long id) {
        HomeLoan h = activeLoan(id);
        double interest = monthlyInterest(h);
        double rp = n(h.getRemainingPrincipal());
        double principal = Math.min(r2(n(h.getEmi()) - interest), rp);
        if (h.getRemainingTenure() <= 1 || principal >= rp - 0.01) principal = rp;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("emiNumber", h.getPaidEmis() + 1);
        m.put("principalAmount", principal);
        m.put("interestAmount", interest);
        m.put("totalAmount", r2(principal + interest));
        m.put("dueDate", h.getNextEmiDate());
        return m;
    }
    public Map<String, Object> prepayPreview(Long id, double amount) {
        HomeLoan h = activeLoan(id);
        if (amount <= 0 || amount >= n(h.getRemainingPrincipal())) throw new RuntimeException("Prepayment must be above 0 and below outstanding principal; use closure to pay in full");
        double charge = r2(amount * PREPAYMENT_CHARGE_PCT / 100);
        double gst = r2(charge * GST_PCT / 100);
        double newPrincipal = r2(n(h.getRemainingPrincipal()) - amount);
        int newTenure = tenureFor(newPrincipal, h.getInterestRate(), n(h.getEmi()), h.getRemainingTenure());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("prepayAmount", amount);
        m.put("charge", charge);
        m.put("gst", gst);
        m.put("totalDebit", r2(amount + charge + gst));
        m.put("newOutstanding", newPrincipal);
        m.put("newRemainingTenure", newTenure);
        m.put("tenureSaved", h.getRemainingTenure() - newTenure);
        return m;
    }

    @Transactional
    public HomeLoan prepay(Long id, double amount, String actor) {
        return prepay(id, amount, "REDUCE_TENURE", actor, null);
    }

    public Map<String, Object> prepayPreview(Long id, double amount, String adjustment) {
        Map<String, Object> m = prepayPreview(id, amount);
        HomeLoan h = activeLoan(id);
        if ("REDUCE_EMI".equalsIgnoreCase(adjustment)) {
            double newEmi = r2(HomeLoanAnalysisService.emi(n(h.getRemainingPrincipal()) - amount, h.getInterestRate(), h.getRemainingTenure()));
            m.put("newRemainingTenure", h.getRemainingTenure());
            m.put("tenureSaved", 0);
            m.put("newEmi", newEmi);
        } else {
            m.put("newEmi", h.getEmi());
        }
        return m;
    }

    @Transactional
    public HomeLoan prepay(Long id, double amount, String adjustment, String actor, String ref) {
        boolean reduceEmi = "REDUCE_EMI".equalsIgnoreCase(adjustment);
        Map<String, Object> p = prepayPreview(id, amount, adjustment);
        HomeLoan h = activeLoan(id);
        double total = (Double) p.get("totalDebit");
        if (ref == null) debitAccount(h, total, "Home Loan Prepayment", "Part-prepayment " + amount + " (+charges) - " + h.getLoanAccountNumber());
        h.setRemainingPrincipal((Double) p.get("newOutstanding"));
        h.setRemainingTenure((Integer) p.get("newRemainingTenure"));
        if (reduceEmi) h.setEmi((Double) p.get("newEmi"));
        h.setPrincipalPaid(r2(n(h.getPrincipalPaid()) + amount));
        h.setPrepaidAmount(r2(n(h.getPrepaidAmount()) + amount));
        touch(h);
        repo.save(h);
        event(h, "PREPAYMENT", actor, "Prepaid " + amount + ", charges " + p.get("charge") + " + GST " + p.get("gst")
                + (reduceEmi ? ". EMI reduced to " + h.getEmi() : ". Tenure reduced by " + p.get("tenureSaved") + " months") + refNote(ref),
                total, null, amount, 0.0, h.getRemainingPrincipal());
        return h;
    }
    public Map<String, Object> closurePreview(Long id) {
        HomeLoan h = activeLoan(id);
        double rp = n(h.getRemainingPrincipal());
        double interest = monthlyInterest(h);
        double charge = r2(rp * CLOSURE_CHARGE_PCT / 100);
        double gst = r2(charge * GST_PCT / 100);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("outstandingPrincipal", rp);
        m.put("accruedInterest", interest);
        m.put("closureCharge", charge);
        m.put("gst", gst);
        m.put("totalPayable", r2(rp + interest + charge + gst));
        return m;
    }

    @Transactional
    public HomeLoan close(Long id, String actor) { return close(id, actor, null); }

    @Transactional
    public HomeLoan close(Long id, String actor, String ref) {
        Map<String, Object> p = closurePreview(id);
        HomeLoan h = activeLoan(id);
        double total = (Double) p.get("totalPayable");
        if (ref == null) debitAccount(h, total, "Home Loan Closure", "Home loan foreclosure - " + h.getLoanAccountNumber());
        double principal = n(h.getRemainingPrincipal());
        h.setPrincipalPaid(r2(n(h.getPrincipalPaid()) + principal));
        h.setInterestPaid(r2(n(h.getInterestPaid()) + (Double) p.get("accruedInterest")));
        h.setRemainingPrincipal(0.0);
        h.setRemainingTenure(0);
        h.setNextEmiDate(null);
        h.setStatus("Closed");
        h.setClosureDate(LocalDateTime.now());
        h.setClosureAmount(total);
        touch(h);
        repo.save(h);
        event(h, "CLOSURE", actor, "Loan closed. Principal " + principal + ", interest " + p.get("accruedInterest")
                + ", charges " + p.get("closureCharge") + " + GST " + p.get("gst") + refNote(ref), total, null, principal, (Double) p.get("accruedInterest"), 0.0);
        return h;
    }

    @Transactional
    public HomeLoan renew(Long id, int months, String actor) {
        HomeLoan h = activeLoan(id);
        if (months < 6 || months > 120) throw new RuntimeException("Renewal extension must be 6-120 months");
        if (h.getRemainingTenure() + months > 360) throw new RuntimeException("Total remaining tenure cannot exceed 360 months");
        double fee = r2(n(h.getRemainingPrincipal()) * RENEWAL_FEE_PCT / 100);
        double gst = r2(fee * GST_PCT / 100);
        debitAccount(h, r2(fee + gst), "Home Loan Renewal Fee", "Renewal fee - " + h.getLoanAccountNumber());
        h.setRemainingTenure(h.getRemainingTenure() + months);
        h.setTenure(h.getTenure() + months);
        h.setEmi(r2(HomeLoanAnalysisService.emi(n(h.getRemainingPrincipal()), h.getInterestRate(), h.getRemainingTenure())));
        touch(h);
        repo.save(h);
        event(h, "RENEWED", actor, "Tenure extended by " + months + " months, new EMI " + h.getEmi() + ", fee " + fee + " + GST " + gst,
                r2(fee + gst), null, 0.0, 0.0, h.getRemainingPrincipal());
        return h;
    }

    private int tenureFor(double principal, double rate, double emi, int fallback) {
        double r = rate / 1200.0;
        if (r == 0) return (int) Math.max(1, Math.ceil(principal / emi));
        double x = 1 - principal * r / emi;
        if (x <= 0) return fallback;
        return (int) Math.max(1, Math.min(fallback, Math.ceil(-Math.log(x) / Math.log(1 + r))));
    }

    // ---------- Schedules / statements ----------

    public List<Map<String, Object>> schedule(Long id) {
        HomeLoan h = get(id);
        if (h.getEmi() == null) throw new RuntimeException("Schedule is available after approval");
        List<Map<String, Object>> rows = new ArrayList<>();
        double bal = n(h.getRemainingPrincipal());
        double emi = n(h.getEmi());
        LocalDate due = h.getNextEmiDate() == null ? LocalDate.now() : h.getNextEmiDate();
        int paid = h.getPaidEmis();
        for (int i = 1; i <= h.getRemainingTenure() && bal > 0.01; i++) {
            double interest = r2(bal * h.getInterestRate() / 1200.0);
            double principal = i == h.getRemainingTenure() ? bal : Math.min(r2(emi - interest), bal);
            bal = r2(bal - principal);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("installment", paid + i);
            m.put("dueDate", due.plusMonths(i - 1).toString());
            m.put("emi", r2(principal + interest));
            m.put("principal", principal);
            m.put("interest", interest);
            m.put("balance", bal);
            rows.add(m);
        }
        return rows;
    }

    public Map<String, Object> statement(Long id) {
        HomeLoan h = get(id);
        List<HomeLoanEvent> events = eventRepo.findByHomeLoanIdOrderByEventDateAscIdAsc(id);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("loan", h);
        m.put("entries", events);
        m.put("totalDebited", r2(events.stream().mapToDouble(e -> n(e.getDebit())).sum()));
        m.put("totalCredited", r2(events.stream().mapToDouble(e -> n(e.getCredit())).sum()));
        m.put("generatedAt", LocalDateTime.now().toString());
        return m;
    }

    public Map<String, Object> consolidated(String accountNumber) {
        List<HomeLoan> loans = byAccount(accountNumber);
        List<HomeLoanEvent> events = eventRepo.findByAccountNumberOrderByEventDateAscIdAsc(accountNumber);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("accountNumber", accountNumber);
        m.put("loans", loans);
        m.put("entries", events);
        m.put("totalSanctioned", r2(loans.stream().filter(l -> l.getLoanAccountNumber() != null).mapToDouble(l -> n(l.getAmount())).sum()));
        m.put("totalOutstanding", r2(loans.stream().filter(l -> "Approved".equals(l.getStatus())).mapToDouble(l -> n(l.getRemainingPrincipal())).sum()));
        m.put("totalPrincipalPaid", r2(loans.stream().mapToDouble(l -> n(l.getPrincipalPaid())).sum()));
        m.put("totalInterestPaid", r2(loans.stream().mapToDouble(l -> n(l.getInterestPaid())).sum()));
        m.put("totalDebited", r2(events.stream().mapToDouble(e -> n(e.getDebit())).sum()));
        m.put("generatedAt", LocalDateTime.now().toString());
        return m;
    }

    public Map<String, Object> analysis(Long id) {
        HomeLoan h = get(id);
        return analysisService.analyze(h.getAccountNumber(), h.getAmount(), h.getTenure(), h.getInterestRate(), h.getPropertyValue());
    }

    // ---------- Admin: top-up, upload, rate revert, NOC ----------

    @Transactional
    public HomeLoan topup(Long id, double amount, Double topupRate, Integer extraMonths, String note, String admin) {
        HomeLoan h = activeLoan(id);
        if (amount < 10000) throw new RuntimeException("Minimum top-up amount is 10,000");
        double rp = n(h.getRemainingPrincipal());
        double rate = topupRate == null ? n(h.getInterestRate()) : topupRate;
        if (rate <= 0 || rate > 30) throw new RuntimeException("Invalid top-up interest rate");
        int add = extraMonths == null ? 0 : extraMonths;
        int newTenure = h.getRemainingTenure() + add;
        if (add < 0 || newTenure < 1 || newTenure > 360) throw new RuntimeException("Total remaining tenure must be 1-360 months");
        double newPrincipal = r2(rp + amount);
        double blended = r2((rp * n(h.getInterestRate()) + amount * rate) / newPrincipal);
        double oldRate = n(h.getInterestRate()), oldEmi = n(h.getEmi());

        Account acc = accountRepository.findByAccountNumber(h.getAccountNumber());
        if (acc == null) throw new RuntimeException("Account not found");
        double nb = r2(n(acc.getBalance()) + amount);
        acc.setBalance(nb);
        accountRepository.save(acc);
        saveTxn(h, "Home Loan Top-up", amount, "Credit", nb, "Home loan top-up released - " + h.getLoanAccountNumber());

        h.setRemainingPrincipal(newPrincipal);
        h.setRemainingTenure(newTenure);
        h.setTenure(h.getTenure() + add);
        h.setAmount(r2(n(h.getAmount()) + amount));
        h.setTopupTotal(r2(n(h.getTopupTotal()) + amount));
        h.setInterestRate(blended);
        h.setEmi(r2(HomeLoanAnalysisService.emi(newPrincipal, blended, newTenure)));
        h.setReviewedBy(admin);
        touch(h);
        repo.save(h);
        if (blended != oldRate) event(h, "RATE_CHANGE", admin, "Interest rate " + oldRate + " -> " + blended + " (top-up blended)", null, null, null, null);
        event(h, "TOPUP", admin, "Top-up " + amount + " released at " + rate + "% (blended " + blended + "%). EMI " + oldEmi + " -> " + h.getEmi()
                + ", tenure +" + add + " months" + (note == null || note.isBlank() ? "" : ". " + note), null, amount, 0.0, 0.0, newPrincipal);
        return h;
    }

    @Transactional
    public HomeLoan adminUploadDocument(Long id, String type, MultipartFile file, String admin) throws IOException {
        if (!DOC_TYPES.contains(type)) throw new RuntimeException("Unknown document type");
        if (file == null || file.isEmpty()) throw new RuntimeException("File is empty");
        HomeLoan h = get(id);
        String orig = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String ext = orig.contains(".") ? orig.substring(orig.lastIndexOf('.')).toLowerCase() : "";
        if (!List.of(".pdf", ".png", ".jpg", ".jpeg").contains(ext)) throw new RuntimeException("Only PDF, PNG or JPG files are allowed");
        Files.createDirectories(UPLOAD_DIR);
        String name = h.getApplicationId() + "-" + type + "-" + System.currentTimeMillis() + ext;
        Files.copy(file.getInputStream(), UPLOAD_DIR.resolve(name), StandardCopyOption.REPLACE_EXISTING);
        String remark = "Uploaded by admin " + admin;
        switch (type) {
            case "fdReceipt": h.setFdReceiptPath(name); h.setFdReceiptStatus("Verified"); h.setFdReceiptRemark(remark); break;
            case "model": h.setModelDocPath(name); h.setModelDocStatus("Verified"); h.setModelDocRemark(remark); break;
            default: h.setSignaturePath(name); h.setSignatureStatus("Verified"); h.setSignatureRemark(remark);
        }
        if (List.of("Submitted", "Under Review", "Documents Required", "Documents Submitted").contains(h.getStatus()) && allDocsVerified(h)) h.setStatus("Documents Verified");
        touch(h);
        repo.save(h);
        event(h, "DOCUMENT", admin, "Admin uploaded " + docLabel(type) + " (" + orig + ")", null, null, null, null);
        return h;
    }

    public List<Map<String, Object>> rateHistory(Long id) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (HomeLoanEvent e : eventRepo.findByHomeLoanIdOrderByEventDateAscIdAsc(id)) {
            if (!"RATE_CHANGE".equals(e.getEventType()) && !"RATE_REVERTED".equals(e.getEventType())) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("type", e.getEventType());
            m.put("date", e.getEventDate());
            m.put("actor", e.getActor());
            m.put("details", e.getDetails());
            out.add(m);
        }
        return out;
    }

    private static double[] parseRates(String details) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("([0-9.]+) -> ([0-9.]+)").matcher(details == null ? "" : details);
        return m.find() ? new double[]{Double.parseDouble(m.group(1)), Double.parseDouble(m.group(2))} : null;
    }

    @Transactional
    public HomeLoan revertRate(Long id, String admin) {
        HomeLoan h = get(id);
        if (List.of("Closed", "Rejected", "Cancelled").contains(h.getStatus())) throw new RuntimeException("Loan is not editable");
        Deque<double[]> stack = new ArrayDeque<>();
        for (HomeLoanEvent e : eventRepo.findByHomeLoanIdOrderByEventDateAscIdAsc(id)) {
            if ("RATE_CHANGE".equals(e.getEventType())) { double[] r = parseRates(e.getDetails()); if (r != null) stack.push(r); }
            else if ("RATE_REVERTED".equals(e.getEventType()) && !stack.isEmpty()) stack.pop();
        }
        if (stack.isEmpty()) throw new RuntimeException("No interest rate change to revert");
        double[] last = stack.peek();
        double cur = n(h.getInterestRate());
        h.setInterestRate(last[0]);
        if ("Approved".equals(h.getStatus()) && h.getRemainingTenure() != null && h.getRemainingTenure() > 0)
            h.setEmi(r2(HomeLoanAnalysisService.emi(n(h.getRemainingPrincipal()), last[0], h.getRemainingTenure())));
        h.setReviewedBy(admin);
        touch(h);
        repo.save(h);
        event(h, "RATE_REVERTED", admin, "Interest rate reverted " + cur + " -> " + last[0], null, null, null, null);
        return h;
    }

    @Transactional
    public HomeLoan adminClose(Long id, String admin, String reference) {
        return close(id, admin, "OFFLINE" + (reference == null || reference.isBlank() ? "" : "-" + reference));
    }

    @Transactional
    public Map<String, Object> noc(Long id, String admin) {
        HomeLoan h = get(id);
        if (!"Closed".equals(h.getStatus())) throw new RuntimeException("NOC can be issued only for closed loans");
        if (h.getNocNumber() == null) {
            h.setNocNumber("NOC-" + h.getLoanAccountNumber() + "-" + LocalDate.now().toString().replace("-", ""));
            h.setNocDate(LocalDateTime.now());
            touch(h);
            repo.save(h);
            event(h, "NOC", admin == null ? "System" : admin, "No Objection Certificate " + h.getNocNumber() + " issued", null, null, null, null);
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("loan", h);
        m.put("nocNumber", h.getNocNumber());
        m.put("issuedOn", h.getNocDate());
        return m;
    }

    // ---------- helpers ----------

    private void debitAccount(HomeLoan h, double amount, String merchant, String desc) {
        Account acc = accountRepository.findByAccountNumber(h.getAccountNumber());
        if (acc == null) throw new RuntimeException("Account not found");
        if (n(acc.getBalance()) < amount) throw new RuntimeException("Insufficient balance. Required " + amount);
        double nb = r2(n(acc.getBalance()) - amount);
        acc.setBalance(nb);
        accountRepository.save(acc);
        saveTxn(h, merchant, amount, "Debit", nb, desc);
    }

    private void saveTxn(HomeLoan h, String merchant, double amount, String type, double balance, String desc) {
        Transaction t = new Transaction();
        t.setMerchant(merchant);
        t.setAmount(amount);
        t.setType(type);
        t.setAccountNumber(h.getAccountNumber());
        t.setUserName(h.getUserName());
        t.setDescription(desc);
        t.setDate(LocalDateTime.now());
        t.setStatus("Completed");
        t.setBalance(balance);
        transactionService.saveTransaction(t);
    }

    private void event(HomeLoan h, String type, String actor, String details, Double debit, Double credit, Double principal, Double interest) {
        event(h, type, actor, details, debit, credit, principal, interest, h.getRemainingPrincipal());
    }

    private void event(HomeLoan h, String type, String actor, String details, Double debit, Double credit, Double principal, Double interest, Double balanceAfter) {
        HomeLoanEvent e = new HomeLoanEvent();
        e.setHomeLoanId(h.getId());
        e.setApplicationId(h.getApplicationId());
        e.setAccountNumber(h.getAccountNumber());
        e.setEventType(type);
        e.setActor(actor);
        e.setDetails(details);
        e.setDebit(debit);
        e.setCredit(credit);
        e.setPrincipalComponent(principal);
        e.setInterestComponent(interest);
        e.setBalanceAfter(balanceAfter);
        eventRepo.save(e);
    }

    private String refNote(String ref) { return ref == null ? "" : " | Paid via Admin Funds Transfer " + ref; }
    private void touch(HomeLoan h) { h.setLastUpdated(LocalDateTime.now()); }
    private double num(Object o) { return Double.parseDouble(String.valueOf(o)); }
}
