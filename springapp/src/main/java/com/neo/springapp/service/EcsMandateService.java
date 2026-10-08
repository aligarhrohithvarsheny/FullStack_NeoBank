package com.neo.springapp.service;

import com.neo.springapp.model.Account;
import com.neo.springapp.model.EcsMandate;
import com.neo.springapp.model.EcsMandateEvent;
import com.neo.springapp.model.EmiPayment;
import com.neo.springapp.model.GoldLoan;
import com.neo.springapp.model.Loan;
import com.neo.springapp.repository.EcsMandateEventRepository;
import com.neo.springapp.repository.EcsMandateRepository;
import com.neo.springapp.repository.EmiPaymentRepository;
import com.neo.springapp.repository.GoldLoanRepository;
import com.neo.springapp.repository.LoanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ECS (auto-debit) mandates linking a loan to a savings account. EMIs are debited
 * through EmiService.payEmi so balances, transactions, EMI schedule and branch
 * interest credit are all updated exactly like a manual EMI payment.
 */
@Service
@SuppressWarnings("null")
public class EcsMandateService {

    private static final List<String> LIVE_STATUSES = List.of("ACTIVE", "PAUSED");

    @Autowired private EcsMandateRepository mandateRepository;
    @Autowired private EcsMandateEventRepository eventRepository;
    @Autowired private EmiPaymentRepository emiPaymentRepository;
    @Autowired private LoanRepository loanRepository;
    @Autowired private GoldLoanRepository goldLoanRepository;
    @Autowired private AccountService accountService;
    @Autowired private EmiService emiService;

    // ---------- Loan / account lookup ----------

    private Map<String, Object> resolveLoan(String loanAccountNumber) {
        Map<String, Object> info = new LinkedHashMap<>();
        Loan loan = loanRepository.findByLoanAccountNumber(loanAccountNumber).orElse(null);
        if (loan != null) {
            info.put("loanAccountNumber", loan.getLoanAccountNumber());
            info.put("loanType", loan.getType() != null ? loan.getType() + " Loan" : "Loan");
            info.put("status", loan.getStatus());
            info.put("amount", loan.getAmount());
            info.put("tenure", loan.getTenure());
            info.put("interestRate", loan.getInterestRate());
            info.put("accountNumber", loan.getAccountNumber());
            info.put("userName", loan.getUserName());
            return info;
        }
        GoldLoan gold = goldLoanRepository.findByLoanAccountNumber(loanAccountNumber).orElse(null);
        if (gold != null) {
            info.put("loanAccountNumber", gold.getLoanAccountNumber());
            info.put("loanType", "Gold Loan");
            info.put("status", gold.getStatus());
            info.put("amount", gold.getLoanAmount());
            info.put("tenure", gold.getTenure());
            info.put("interestRate", gold.getInterestRate());
            info.put("accountNumber", gold.getAccountNumber());
            info.put("userName", gold.getUserName());
            return info;
        }
        return null;
    }

    private List<EmiPayment> emisOf(String loanAccountNumber) {
        return emiPaymentRepository.findByLoanAccountNumberOrderByEmiNumberAsc(loanAccountNumber);
    }

    private Double currentEmiAmount(String loanAccountNumber) {
        List<EmiPayment> emis = emisOf(loanAccountNumber);
        return emis.stream().filter(e -> "Pending".equals(e.getStatus())).findFirst()
                .map(EmiPayment::getTotalAmount)
                .orElse(emis.isEmpty() ? null : emis.get(emis.size() - 1).getTotalAmount());
    }

    private static LocalDate parseDob(String dob) {
        if (dob == null || dob.isBlank()) return null;
        String v = dob.trim();
        for (String f : new String[]{"yyyy-MM-dd", "dd/MM/yyyy", "dd-MM-yyyy", "MM/dd/yyyy", "yyyy/MM/dd"}) {
            try {
                return LocalDate.parse(v.length() > 10 ? v.substring(0, 10) : v, DateTimeFormatter.ofPattern(f));
            } catch (Exception ignored) { }
        }
        return null;
    }

    private static boolean dobMatches(String accountDob, String givenDob) {
        if (accountDob == null || givenDob == null) return false;
        LocalDate a = parseDob(accountDob);
        LocalDate b = parseDob(givenDob);
        if (a != null && b != null) return a.equals(b);
        return accountDob.trim().equalsIgnoreCase(givenDob.trim());
    }

    /** Admin "fetch details" step: loan + savings account + DOB verification. */
    public Map<String, Object> fetchDetails(String loanAccountNumber, String savingsAccountNumber, String dob) {
        Map<String, Object> res = new LinkedHashMap<>();
        if (loanAccountNumber == null || loanAccountNumber.isBlank()) {
            return fail("Loan account number is required");
        }
        Map<String, Object> loan = resolveLoan(loanAccountNumber.trim());
        if (loan == null) return fail("Loan account not found");

        String savings = (savingsAccountNumber == null || savingsAccountNumber.isBlank())
                ? (String) loan.get("accountNumber") : savingsAccountNumber.trim();
        Account account = savings == null ? null : accountService.getAccountByNumber(savings);
        if (account == null) return fail("Savings account not found");

        List<EmiPayment> emis = emisOf(loanAccountNumber.trim());
        long pending = emis.stream().filter(e -> "Pending".equals(e.getStatus())).count();
        EmiPayment next = emis.stream().filter(e -> "Pending".equals(e.getStatus())).findFirst().orElse(null);

        res.put("success", true);
        res.put("loan", loan);
        res.put("emiAmount", currentEmiAmount(loanAccountNumber.trim()));
        res.put("pendingEmis", pending);
        res.put("totalEmis", emis.size());
        res.put("nextDueDate", next != null ? next.getDueDate().toString() : null);
        res.put("savingsAccountNumber", account.getAccountNumber());
        res.put("customerName", account.getName());
        res.put("customerId", account.getCustomerId());
        res.put("accountBalance", account.getBalance());
        res.put("accountDob", account.getDob());
        res.put("dobMatches", dob != null && !dob.isBlank() && dobMatches(account.getDob(), dob));
        res.put("existingMandate", mandateRepository.existsByLoanAccountNumberAndStatusIn(
                loanAccountNumber.trim(), LIVE_STATUSES));
        return res;
    }

    /** Approved loans (personal/education/gold ...) that have no live mandate yet. */
    public List<Map<String, Object>> eligibleLoans() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Loan l : loanRepository.findByStatus("Approved")) {
            addEligible(out, l.getLoanAccountNumber());
        }
        for (GoldLoan g : goldLoanRepository.findByStatus("Approved")) {
            addEligible(out, g.getLoanAccountNumber());
        }
        return out;
    }

    private void addEligible(List<Map<String, Object>> out, String loanAccountNumber) {
        if (loanAccountNumber == null
                || mandateRepository.existsByLoanAccountNumberAndStatusIn(loanAccountNumber, LIVE_STATUSES)) return;
        Map<String, Object> info = resolveLoan(loanAccountNumber);
        if (info == null) return;
        info.put("emiAmount", currentEmiAmount(loanAccountNumber));
        out.add(info);
    }

    private Map<String, Object> fail(String message) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("success", false);
        r.put("message", message);
        return r;
    }

    // ---------- Admin actions ----------

    public Map<String, Object> link(Map<String, Object> req, String admin) {
        String loanNo = str(req.get("loanAccountNumber"));
        String savings = str(req.get("savingsAccountNumber"));
        String dob = str(req.get("dob"));
        Integer day = toInt(req.get("debitDay"));
        Double limit = toDouble(req.get("amountLimit"));

        if (loanNo == null || savings == null || dob == null) return fail("Loan account, savings account and DOB are required");
        if (day == null || day < 1 || day > 31) return fail("Debit date must be between 1 and 31");

        Map<String, Object> loan = resolveLoan(loanNo);
        if (loan == null) return fail("Loan account not found");
        if (!"Approved".equals(loan.get("status"))) return fail("Only approved loans can be linked to an ECS mandate");
        if (mandateRepository.existsByLoanAccountNumberAndStatusIn(loanNo, LIVE_STATUSES)) {
            return fail("An ECS mandate is already linked to this loan");
        }
        Account account = accountService.getAccountByNumber(savings);
        if (account == null) return fail("Savings account not found");
        if (!dobMatches(account.getDob(), dob)) return fail("Date of birth does not match the savings account holder");

        Double emi = currentEmiAmount(loanNo);
        if (emi == null) return fail("EMI schedule not available for this loan");
        if (limit == null || limit <= 0) limit = emi;
        if (limit < emi) return fail("Amount limit cannot be less than the EMI amount (₹" + emi + ")");

        EcsMandate m = new EcsMandate();
        m.setMandateId("ECS" + System.currentTimeMillis());
        m.setLoanAccountNumber(loanNo);
        m.setLoanType((String) loan.get("loanType"));
        m.setSavingsAccountNumber(savings);
        m.setCustomerName(account.getName());
        m.setCustomerId(account.getCustomerId());
        m.setDob(account.getDob());
        m.setDebitDay(day);
        m.setAmountLimit(limit);
        m.setEmiAmount(emi);
        m.setCreatedBy(admin);
        m = mandateRepository.save(m);
        syncPendingEmiAccount(loanNo, savings);
        log(m, "LINKED", null, null, "Mandate linked: debit on day " + day + ", limit ₹" + limit, admin);

        runDueDebits(m, true);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("success", true);
        r.put("message", "ECS mandate linked successfully");
        r.put("mandate", mandateRepository.findById(m.getId()).orElse(m));
        return r;
    }

    public Map<String, Object> changeAccount(Long id, String newSavings, String dob, String admin) {
        EcsMandate m = mandateRepository.findById(id).orElse(null);
        if (m == null) return fail("Mandate not found");
        if ("CANCELLED".equals(m.getStatus())) return fail("Mandate is cancelled");
        if (newSavings == null || newSavings.isBlank()) return fail("New savings account number is required");
        Account account = accountService.getAccountByNumber(newSavings.trim());
        if (account == null) return fail("Savings account not found");
        if (!dobMatches(account.getDob(), dob)) return fail("Date of birth does not match the new account holder");

        String old = m.getSavingsAccountNumber();
        m.setSavingsAccountNumber(account.getAccountNumber());
        m.setCustomerName(account.getName());
        m.setCustomerId(account.getCustomerId());
        m.setDob(account.getDob());
        mandateRepository.save(m);
        syncPendingEmiAccount(m.getLoanAccountNumber(), account.getAccountNumber());
        log(m, "ACCOUNT_CHANGED", null, null, "Debit account changed from " + old + " to " + account.getAccountNumber(), admin);
        return ok(m, "Linked bank account updated");
    }

    public Map<String, Object> changeDate(Long id, Integer day, String admin) {
        EcsMandate m = mandateRepository.findById(id).orElse(null);
        if (m == null) return fail("Mandate not found");
        if ("CANCELLED".equals(m.getStatus())) return fail("Mandate is cancelled");
        if (day == null || day < 1 || day > 31) return fail("Debit date must be between 1 and 31");
        Integer old = m.getDebitDay();
        m.setDebitDay(day);
        m.setLastAttemptDate(null);
        mandateRepository.save(m);
        log(m, "DATE_CHANGED", null, null, "Debit date changed from " + old + " to " + day, admin);
        return ok(m, "Debit date updated");
    }

    public Map<String, Object> changeLimit(Long id, Double limit, String admin) {
        EcsMandate m = mandateRepository.findById(id).orElse(null);
        if (m == null) return fail("Mandate not found");
        if ("CANCELLED".equals(m.getStatus())) return fail("Mandate is cancelled");
        Double emi = currentEmiAmount(m.getLoanAccountNumber());
        if (limit == null || limit <= 0 || (emi != null && limit < emi)) {
            return fail("Amount limit must be at least the EMI amount (₹" + emi + ")");
        }
        m.setAmountLimit(limit);
        mandateRepository.save(m);
        log(m, "LIMIT_CHANGED", null, limit, "Amount limit set to ₹" + limit, admin);
        return ok(m, "Amount limit updated");
    }

    public Map<String, Object> pause(Long id, String actorRole, String actor) {
        EcsMandate m = mandateRepository.findById(id).orElse(null);
        if (m == null) return fail("Mandate not found");
        if (!"ACTIVE".equals(m.getStatus())) return fail("Only active mandates can be paused");
        m.setStatus("PAUSED");
        m.setPausedBy(actorRole);
        mandateRepository.save(m);
        log(m, "PAUSED", null, null, "Auto-debit paused by " + actorRole, actor);
        return ok(m, "Mandate paused");
    }

    public Map<String, Object> resume(Long id, String actorRole, String actor) {
        EcsMandate m = mandateRepository.findById(id).orElse(null);
        if (m == null) return fail("Mandate not found");
        if (!"PAUSED".equals(m.getStatus())) return fail("Mandate is not paused");
        if ("ADMIN".equals(m.getPausedBy()) && !"ADMIN".equals(actorRole)) {
            return fail("This mandate was paused by the bank. Please contact support.");
        }
        m.setStatus("ACTIVE");
        m.setPausedBy(null);
        m.setLastAttemptDate(null);
        mandateRepository.save(m);
        log(m, "RESUMED", null, null, "Auto-debit resumed by " + actorRole, actor);
        return ok(m, "Mandate resumed");
    }

    public Map<String, Object> cancel(Long id, String admin) {
        EcsMandate m = mandateRepository.findById(id).orElse(null);
        if (m == null) return fail("Mandate not found");
        if ("CANCELLED".equals(m.getStatus())) return fail("Mandate is already cancelled");
        m.setStatus("CANCELLED");
        m.setCancelRequested(false);
        m.setCancelledBy(admin);
        m.setCancelledAt(LocalDateTime.now());
        mandateRepository.save(m);
        log(m, "CANCELLED", null, null, "Mandate cancelled by " + admin, admin);
        return ok(m, "Mandate cancelled");
    }

    public Map<String, Object> rejectCancelRequest(Long id, String note, String admin) {
        EcsMandate m = mandateRepository.findById(id).orElse(null);
        if (m == null) return fail("Mandate not found");
        if (!Boolean.TRUE.equals(m.getCancelRequested())) return fail("No cancel request pending");
        m.setCancelRequested(false);
        m.setCancelRejectionNote(note);
        mandateRepository.save(m);
        log(m, "CANCEL_REJECTED", null, null, "Cancel request rejected" + (note != null ? ": " + note : ""), admin);
        return ok(m, "Cancel request rejected");
    }

    // ---------- User actions ----------

    public Map<String, Object> requestCancel(Long id, String accountNumber, String reason) {
        EcsMandate m = mandateRepository.findById(id).orElse(null);
        if (m == null || !isOwner(m, accountNumber)) return fail("Mandate not found");
        if ("CANCELLED".equals(m.getStatus())) return fail("Mandate is already cancelled");
        if (Boolean.TRUE.equals(m.getCancelRequested())) return fail("Cancel request already submitted");
        m.setCancelRequested(true);
        m.setCancelReason(reason);
        m.setCancelRequestedAt(LocalDateTime.now());
        m.setCancelRejectionNote(null);
        mandateRepository.save(m);
        log(m, "CANCEL_REQUESTED", null, null, "Cancel requested by customer" + (reason != null ? ": " + reason : ""), accountNumber);
        return ok(m, "Cancel request sent to the bank");
    }

    /** Public (pre-login) lookup: loan + savings account + DOB must all match a mandate. */
    public Map<String, Object> publicLookup(String loanNo, String savings, String dob) {
        if (loanNo == null || savings == null || dob == null) return fail("Loan account, savings account and DOB are required");
        EcsMandate m = findPublicMandate(loanNo.trim(), savings.trim(), dob.trim());
        if (m == null) return fail("No ECS mandate found for the details provided");
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("success", true);
        r.put("id", m.getId());
        r.put("mandateId", m.getMandateId());
        r.put("loanAccountNumber", m.getLoanAccountNumber());
        r.put("loanType", m.getLoanType());
        r.put("savingsAccountNumber", m.getSavingsAccountNumber());
        r.put("customerName", m.getCustomerName());
        r.put("debitDay", m.getDebitDay());
        r.put("emiAmount", m.getEmiAmount());
        r.put("amountLimit", m.getAmountLimit());
        r.put("status", m.getStatus());
        r.put("cancelRequested", m.getCancelRequested());
        r.put("cancelRejectionNote", m.getCancelRejectionNote());
        return r;
    }

    public Map<String, Object> publicRequestCancel(String loanNo, String savings, String dob, String reason) {
        if (loanNo == null || savings == null || dob == null) return fail("Loan account, savings account and DOB are required");
        EcsMandate m = findPublicMandate(loanNo.trim(), savings.trim(), dob.trim());
        if (m == null) return fail("No ECS mandate found for the details provided");
        return requestCancel(m.getId(), m.getSavingsAccountNumber(), reason);
    }

    private EcsMandate findPublicMandate(String loanNo, String savings, String dob) {
        for (EcsMandate m : mandateRepository.findByLoanAccountNumberOrderByCreatedAtDesc(loanNo)) {
            if (!"CANCELLED".equals(m.getStatus()) && savings.equals(m.getSavingsAccountNumber())
                    && dobMatches(m.getDob(), dob)) return m;
        }
        return null;
    }

    public boolean isOwner(EcsMandate m, String accountNumber) {
        if (accountNumber == null) return false;
        if (accountNumber.equals(m.getSavingsAccountNumber())) return true;
        Map<String, Object> loan = resolveLoan(m.getLoanAccountNumber());
        return loan != null && accountNumber.equals(loan.get("accountNumber"));
    }

    // ---------- Queries ----------

    public List<EcsMandate> all() {
        return mandateRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<EcsMandate> forAccount(String accountNumber) {
        Map<Long, EcsMandate> byId = new LinkedHashMap<>();
        mandateRepository.findBySavingsAccountNumberOrderByCreatedAtDesc(accountNumber)
                .forEach(m -> byId.put(m.getId(), m));
        for (EcsMandate m : mandateRepository.findAllByOrderByCreatedAtDesc()) {
            Map<String, Object> loan = resolveLoan(m.getLoanAccountNumber());
            if (loan != null && accountNumber.equals(loan.get("accountNumber"))) byId.put(m.getId(), m);
        }
        return byId.values().stream()
                .sorted(Comparator.comparing(EcsMandate::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    public EcsMandate get(Long id) {
        return mandateRepository.findById(id).orElse(null);
    }

    public Map<String, Object> details(Long id) {
        EcsMandate m = mandateRepository.findById(id).orElse(null);
        if (m == null) return fail("Mandate not found");
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("success", true);
        r.put("mandate", m);
        r.put("loan", resolveLoan(m.getLoanAccountNumber()));
        r.put("emis", emisOf(m.getLoanAccountNumber()));
        r.put("events", eventRepository.findByMandateDbIdOrderByCreatedAtDesc(m.getId()));
        r.put("accountBalance", accountService.getBalanceByAccountNumber(m.getSavingsAccountNumber()));
        return r;
    }

    // ---------- Debit engine ----------

    private LocalDate effectiveDebitDate(EcsMandate m, EmiPayment emi) {
        LocalDate due = emi.getDueDate();
        int day = Math.min(m.getDebitDay(), due.lengthOfMonth());
        return due.withDayOfMonth(day);
    }

    public Map<String, Object> debitNow(Long id, String admin) {
        EcsMandate m = mandateRepository.findById(id).orElse(null);
        if (m == null) return fail("Mandate not found");
        if (!"ACTIVE".equals(m.getStatus())) return fail("Mandate is not active");
        int debited = runDueDebits(m, true);
        m = mandateRepository.findById(id).orElse(m);
        Map<String, Object> r = ok(m, debited > 0 ? debited + " EMI(s) debited" : "No EMI debited: " + m.getLastDebitMessage());
        r.put("success", debited > 0);
        return r;
    }

    /** Daily run: debits every due EMI for all active mandates. */
    @Scheduled(cron = "0 0 10 * * ?")
    public void processAllDueMandates() {
        for (EcsMandate m : mandateRepository.findByStatus("ACTIVE")) {
            try {
                runDueDebits(m, false);
            } catch (Exception e) {
                System.err.println("ECS debit error for " + m.getMandateId() + ": " + e.getMessage());
            }
        }
    }

    /** Returns number of EMIs debited. */
    private int runDueDebits(EcsMandate m, boolean force) {
        LocalDate today = LocalDate.now();
        if (!force && today.equals(m.getLastAttemptDate())) return 0;

        List<EmiPayment> pending = emisOf(m.getLoanAccountNumber()).stream()
                .filter(e -> "Pending".equals(e.getStatus()))
                .sorted(Comparator.comparing(EmiPayment::getEmiNumber))
                .collect(Collectors.toList());

        if (pending.isEmpty()) {
            if (!emisOf(m.getLoanAccountNumber()).isEmpty()) {
                m.setStatus("CANCELLED");
                m.setCancelledBy("SYSTEM");
                m.setCancelledAt(LocalDateTime.now());
                m.setLastDebitMessage("Loan closed - no pending EMIs");
                mandateRepository.save(m);
                log(m, "CANCELLED", null, null, "Mandate closed automatically: loan has no pending EMIs", "SYSTEM");
            }
            return 0;
        }

        int debited = 0;
        for (EmiPayment emi : pending) {
            if (effectiveDebitDate(m, emi).isAfter(today)) break;

            if (emi.getTotalAmount() > m.getAmountLimit()) {
                recordFailure(m, emi, "EMI ₹" + emi.getTotalAmount() + " exceeds mandate limit ₹" + m.getAmountLimit());
                break;
            }
            Map<String, Object> result;
            try {
                result = emiService.payEmi(emi.getId(), m.getSavingsAccountNumber());
            } catch (Exception e) {
                recordFailure(m, emi, "Debit error: " + e.getMessage());
                break;
            }
            if (Boolean.TRUE.equals(result.get("success"))) {
                debited++;
                m.setSuccessfulDebits(m.getSuccessfulDebits() + 1);
                m.setLastDebitAt(LocalDateTime.now());
                m.setLastDebitStatus("SUCCESS");
                m.setLastDebitMessage("EMI #" + emi.getEmiNumber() + " debited");
                log(m, "DEBIT_SUCCESS", emi.getEmiNumber(), emi.getTotalAmount(),
                        "EMI #" + emi.getEmiNumber() + " auto-debited from " + m.getSavingsAccountNumber(), "SYSTEM");
            } else {
                recordFailure(m, emi, String.valueOf(result.get("message")));
                break;
            }
        }
        m.setLastAttemptDate(today);
        Double next = currentEmiAmount(m.getLoanAccountNumber());
        if (next != null) m.setEmiAmount(next);
        mandateRepository.save(m);
        return debited;
    }

    private void recordFailure(EcsMandate m, EmiPayment emi, String message) {
        m.setFailedDebits(m.getFailedDebits() + 1);
        m.setLastDebitStatus("FAILED");
        m.setLastDebitMessage(message);
        log(m, "DEBIT_FAILED", emi.getEmiNumber(), emi.getTotalAmount(), message, "SYSTEM");
    }

    // ---------- helpers ----------

    private void syncPendingEmiAccount(String loanAccountNumber, String savings) {
        List<EmiPayment> pending = emisOf(loanAccountNumber).stream()
                .filter(e -> "Pending".equals(e.getStatus()))
                .collect(Collectors.toList());
        pending.forEach(e -> e.setAccountNumber(savings));
        if (!pending.isEmpty()) emiPaymentRepository.saveAll(pending);
    }

    private void log(EcsMandate m, String type, Integer emiNo, Double amount, String msg, String actor) {
        EcsMandateEvent ev = new EcsMandateEvent();
        ev.setMandateDbId(m.getId());
        ev.setMandateId(m.getMandateId());
        ev.setLoanAccountNumber(m.getLoanAccountNumber());
        ev.setSavingsAccountNumber(m.getSavingsAccountNumber());
        ev.setEventType(type);
        ev.setEmiNumber(emiNo);
        ev.setAmount(amount);
        ev.setMessage(msg);
        ev.setActor(actor);
        eventRepository.save(ev);
    }

    private Map<String, Object> ok(EcsMandate m, String message) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("success", true);
        r.put("message", message);
        r.put("mandate", m);
        return r;
    }

    private static String str(Object o) {
        return o == null || o.toString().isBlank() ? null : o.toString().trim();
    }

    private static Integer toInt(Object o) {
        try { return o == null ? null : (int) Double.parseDouble(o.toString()); } catch (Exception e) { return null; }
    }

    private static Double toDouble(Object o) {
        try { return o == null || o.toString().isBlank() ? null : Double.parseDouble(o.toString()); } catch (Exception e) { return null; }
    }
}
