package com.neo.springapp.service;

import com.neo.springapp.model.*;
import com.neo.springapp.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
public class AtmManagementService {

    @Autowired
    private AtmMachineRepository atmMachineRepository;

    @Autowired
    private AtmCashLoadRepository atmCashLoadRepository;

    @Autowired
    private AtmTransactionRepository atmTransactionRepository;

    @Autowired
    private AtmIncidentRepository atmIncidentRepository;

    @Autowired
    private AtmServiceLogRepository atmServiceLogRepository;

    // ─── ATM Machine CRUD ──────────────────────────────────

    public List<AtmMachine> getAllAtms() {
        return atmMachineRepository.findAll();
    }

    public Optional<AtmMachine> getAtmById(String atmId) {
        return atmMachineRepository.findByAtmId(atmId);
    }

    public List<AtmMachine> getAtmsByStatus(String status) {
        return atmMachineRepository.findByStatus(status);
    }

    @Transactional
    public AtmMachine createAtm(AtmMachine atm) {
        if (atmMachineRepository.existsByAtmId(atm.getAtmId())) {
            throw new RuntimeException("ATM with ID " + atm.getAtmId() + " already exists");
        }
        return atmMachineRepository.save(atm);
    }

    @Transactional
    public AtmMachine updateAtm(String atmId, AtmMachine updates) {
        AtmMachine atm = atmMachineRepository.findByAtmId(atmId)
                .orElseThrow(() -> new RuntimeException("ATM not found: " + atmId));
        if (updates.getAtmName() != null) atm.setAtmName(updates.getAtmName());
        if (updates.getLocation() != null) atm.setLocation(updates.getLocation());
        if (updates.getCity() != null) atm.setCity(updates.getCity());
        if (updates.getState() != null) atm.setState(updates.getState());
        if (updates.getPincode() != null) atm.setPincode(updates.getPincode());
        if (updates.getMaxWithdrawalLimit() != null) atm.setMaxWithdrawalLimit(updates.getMaxWithdrawalLimit());
        if (updates.getDailyLimit() != null) atm.setDailyLimit(updates.getDailyLimit());
        if (updates.getMinThreshold() != null) atm.setMinThreshold(updates.getMinThreshold());
        if (updates.getMaxCapacity() != null) atm.setMaxCapacity(updates.getMaxCapacity());
        if (updates.getManagedBy() != null) atm.setManagedBy(updates.getManagedBy());
        if (updates.getContactNumber() != null) atm.setContactNumber(updates.getContactNumber());
        return atmMachineRepository.save(atm);
    }

    // ─── Cash Loading (Admin deposits cash into ATM) ──────

    @Transactional
    public Map<String, Object> loadCash(String atmId, Double amount, String loadedBy,
                                         Integer notes500, Integer notes200, Integer notes100,
                                         Integer notes2000, String remarks) {
        AtmMachine atm = atmMachineRepository.findByAtmId(atmId)
                .orElseThrow(() -> new RuntimeException("ATM not found: " + atmId));

        if (!"ACTIVE".equals(atm.getStatus()) && !"LOW_CASH".equals(atm.getStatus())) {
            throw new RuntimeException("ATM is " + atm.getStatus() + ". Cannot load cash.");
        }

        Double previousBalance = atm.getCashAvailable();
        Double newBalance = previousBalance + amount;

        if (newBalance > atm.getMaxCapacity()) {
            throw new RuntimeException("Cash would exceed ATM max capacity of ₹" + atm.getMaxCapacity());
        }

        // Update ATM balance
        atm.setCashAvailable(newBalance);
        atm.setLastCashLoaded(LocalDateTime.now());
        if (notes500 != null) atm.setNotes500Count(atm.getNotes500Count() + notes500);
        if (notes200 != null) atm.setNotes200Count(atm.getNotes200Count() + notes200);
        if (notes100 != null) atm.setNotes100Count(atm.getNotes100Count() + notes100);
        if (notes2000 != null) atm.setNotes2000Count(atm.getNotes2000Count() + notes2000);

        // If was low cash, set back to active
        if ("LOW_CASH".equals(atm.getStatus()) && newBalance >= atm.getMinThreshold()) {
            atm.setStatus("ACTIVE");
        }

        atmMachineRepository.save(atm);

        // Record the cash load
        AtmCashLoad cashLoad = new AtmCashLoad();
        cashLoad.setAtmId(atmId);
        cashLoad.setLoadedBy(loadedBy);
        cashLoad.setAmount(amount);
        cashLoad.setNotes500(notes500 != null ? notes500 : 0);
        cashLoad.setNotes200(notes200 != null ? notes200 : 0);
        cashLoad.setNotes100(notes100 != null ? notes100 : 0);
        cashLoad.setNotes2000(notes2000 != null ? notes2000 : 0);
        cashLoad.setPreviousBalance(previousBalance);
        cashLoad.setNewBalance(newBalance);
        cashLoad.setRemarks(remarks);
        atmCashLoadRepository.save(cashLoad);

        // Log service action
        logServiceAction(atmId, "CASH_LOADED", null, null, loadedBy,
                "Loaded ₹" + amount + " into ATM. New balance: ₹" + newBalance);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("previousBalance", previousBalance);
        result.put("amountLoaded", amount);
        result.put("newBalance", newBalance);
        result.put("atmId", atmId);
        return result;
    }

    // ─── ATM Status Control ───────────────────────────────

    @Transactional
    public AtmMachine changeAtmStatus(String atmId, String newStatus, String performedBy, String reason) {
        AtmMachine atm = atmMachineRepository.findByAtmId(atmId)
                .orElseThrow(() -> new RuntimeException("ATM not found: " + atmId));

        String previousStatus = atm.getStatus();
        atm.setStatus(newStatus);

        if ("MAINTENANCE".equals(newStatus) || "ACTIVE".equals(newStatus)) {
            atm.setLastServiced(LocalDateTime.now());
        }

        atmMachineRepository.save(atm);
        logServiceAction(atmId, "STATUS_CHANGE", previousStatus, newStatus, performedBy, reason);
        return atm;
    }

    @Transactional
    public void setAllAtmsOutOfService(String performedBy, String reason) {
        List<AtmMachine> activeAtms = atmMachineRepository.findByStatusNot("OUT_OF_SERVICE");
        for (AtmMachine atm : activeAtms) {
            String prev = atm.getStatus();
            atm.setStatus("OUT_OF_SERVICE");
            atmMachineRepository.save(atm);
            logServiceAction(atm.getAtmId(), "BULK_OUT_OF_SERVICE", prev, "OUT_OF_SERVICE", performedBy, reason);
        }
    }

    @Transactional
    public void setAllAtmsActive(String performedBy, String reason) {
        List<AtmMachine> outAtms = atmMachineRepository.findByStatus("OUT_OF_SERVICE");
        for (AtmMachine atm : outAtms) {
            atm.setStatus("ACTIVE");
            atmMachineRepository.save(atm);
            logServiceAction(atm.getAtmId(), "BULK_ACTIVATE", "OUT_OF_SERVICE", "ACTIVE", performedBy, reason);
        }
    }

    // ─── ATM Withdrawal (for user-facing ATM simulator) ───

    @Transactional
    public Map<String, Object> processWithdrawal(String atmId, String accountNumber, String cardNumber,
                                                   String userName, String userEmail, Double amount,
                                                   Double userBalanceBefore, Double userBalanceAfter) {
        AtmMachine atm = atmMachineRepository.findByAtmId(atmId)
                .orElseThrow(() -> new RuntimeException("ATM not found: " + atmId));

        // Check ATM status
        if (!"ACTIVE".equals(atm.getStatus())) {
            return createFailedTransaction(atmId, accountNumber, cardNumber, userName, userEmail,
                    amount, userBalanceBefore, "ATM is " + atm.getStatus());
        }

        // Check ATM has enough cash
        if (atm.getCashAvailable() < amount) {
            return createFailedTransaction(atmId, accountNumber, cardNumber, userName, userEmail,
                    amount, userBalanceBefore, "ATM has insufficient cash");
        }

        // Check per-transaction limit
        if (amount > atm.getMaxWithdrawalLimit()) {
            return createFailedTransaction(atmId, accountNumber, cardNumber, userName, userEmail,
                    amount, userBalanceBefore,
                    "Amount exceeds ATM per-transaction limit of ₹" + atm.getMaxWithdrawalLimit());
        }

        // Check user daily limit
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        Double dailyWithdrawn = atmTransactionRepository.getUserDailyWithdrawal(accountNumber, todayStart);
        if (dailyWithdrawn + amount > atm.getDailyLimit()) {
            return createFailedTransaction(atmId, accountNumber, cardNumber, userName, userEmail,
                    amount, userBalanceBefore,
                    "Daily ATM withdrawal limit of ₹" + atm.getDailyLimit() + " exceeded");
        }

        // Debit ATM cash
        Double atmBalanceBefore = atm.getCashAvailable();
        Double atmBalanceAfter = atmBalanceBefore - amount;
        atm.setCashAvailable(atmBalanceAfter);

        // Check if ATM is now low on cash
        if (atmBalanceAfter < atm.getMinThreshold()) {
            atm.setStatus("LOW_CASH");
        }

        atmMachineRepository.save(atm);

        // Record ATM transaction
        String txnRef = "ATM" + System.currentTimeMillis() + (int)(Math.random() * 1000);
        String receiptNo = "RCP" + System.currentTimeMillis();

        AtmTransaction txn = new AtmTransaction();
        txn.setTransactionRef(txnRef);
        txn.setAtmId(atmId);
        txn.setAccountNumber(accountNumber);
        txn.setCardNumber(cardNumber);
        txn.setUserName(userName);
        txn.setUserEmail(userEmail);
        txn.setTransactionType("WITHDRAWAL");
        txn.setAmount(amount);
        txn.setBalanceBefore(userBalanceBefore);
        txn.setBalanceAfter(userBalanceAfter);
        txn.setAtmBalanceBefore(atmBalanceBefore);
        txn.setAtmBalanceAfter(atmBalanceAfter);
        txn.setStatus("SUCCESS");
        txn.setReceiptNumber(receiptNo);
        txn.setReceiptGenerated(true);
        atmTransactionRepository.save(txn);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("transactionRef", txnRef);
        result.put("receiptNumber", receiptNo);
        result.put("amount", amount);
        result.put("balanceAfter", userBalanceAfter);
        result.put("atmBalanceAfter", atmBalanceAfter);
        return result;
    }

    private Map<String, Object> createFailedTransaction(String atmId, String accountNumber,
                                                         String cardNumber, String userName, String userEmail,
                                                         Double amount, Double balanceBefore, String reason) {
        String txnRef = "ATM" + System.currentTimeMillis() + (int)(Math.random() * 1000);
        AtmTransaction txn = new AtmTransaction();
        txn.setTransactionRef(txnRef);
        txn.setAtmId(atmId);
        txn.setAccountNumber(accountNumber);
        txn.setCardNumber(cardNumber);
        txn.setUserName(userName);
        txn.setUserEmail(userEmail);
        txn.setTransactionType("WITHDRAWAL");
        txn.setAmount(amount);
        txn.setBalanceBefore(balanceBefore);
        txn.setStatus("FAILED");
        txn.setFailureReason(reason);
        txn.setReceiptGenerated(false);
        atmTransactionRepository.save(txn);

        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("message", reason);
        result.put("transactionRef", txnRef);
        return result;
    }

    // ─── Transaction Queries ──────────────────────────────

    public Page<AtmTransaction> getAllTransactions(int page, int size) {
        return atmTransactionRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
    }

    public Page<AtmTransaction> getTransactionsByAtm(String atmId, int page, int size) {
        return atmTransactionRepository.findByAtmIdOrderByCreatedAtDesc(atmId, PageRequest.of(page, size));
    }

    public Page<AtmTransaction> getTransactionsByAccount(String accountNumber, int page, int size) {
        return atmTransactionRepository.findByAccountNumberOrderByCreatedAtDesc(accountNumber, PageRequest.of(page, size));
    }

    public Page<AtmTransaction> getTransactionsByStatus(String status, int page, int size) {
        return atmTransactionRepository.findByStatusOrderByCreatedAtDesc(status, PageRequest.of(page, size));
    }

    public Page<AtmTransaction> getTransactionsWithReceipts(int page, int size) {
        return atmTransactionRepository.findWithReceipts(PageRequest.of(page, size));
    }

    public Page<AtmTransaction> getTransactionsByDateRange(LocalDateTime start, LocalDateTime end, int page, int size) {
        return atmTransactionRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(start, end, PageRequest.of(page, size));
    }

    // ─── Incident Management ──────────────────────────────

    @Transactional
    public AtmIncident createIncident(String atmId, String incidentType, String accountNumber,
                                       String cardNumber, String userName, String description,
                                       Double amountInvolved) {
        AtmMachine atm = atmMachineRepository.findByAtmId(atmId)
                .orElseThrow(() -> new RuntimeException("ATM not found: " + atmId));

        String incidentRef = "INC" + System.currentTimeMillis();

        AtmIncident incident = new AtmIncident();
        incident.setIncidentRef(incidentRef);
        incident.setAtmId(atmId);
        incident.setIncidentType(incidentType);
        incident.setAccountNumber(accountNumber);
        incident.setCardNumber(cardNumber);
        incident.setUserName(userName);
        incident.setDescription(description);
        incident.setAmountInvolved(amountInvolved != null ? amountInvolved : 0.0);
        incident.setStatus("OPEN");

        // If it's a card stuck incident, auto-log it
        if ("CARD_STUCK".equals(incidentType) || "CASH_NOT_DISPENSED".equals(incidentType)) {
            logServiceAction(atmId, "INCIDENT_REPORTED", atm.getStatus(), atm.getStatus(),
                    "SYSTEM", incidentType + ": " + description);
        }

        return atmIncidentRepository.save(incident);
    }

    @Transactional
    public AtmIncident resolveIncident(String incidentRef, String resolution, String resolvedBy) {
        AtmIncident incident = atmIncidentRepository.findByIncidentRef(incidentRef)
                .orElseThrow(() -> new RuntimeException("Incident not found: " + incidentRef));
        incident.setStatus("RESOLVED");
        incident.setResolution(resolution);
        incident.setResolvedBy(resolvedBy);
        incident.setResolvedAt(LocalDateTime.now());
        return atmIncidentRepository.save(incident);
    }

    public Page<AtmIncident> getAllIncidents(int page, int size) {
        return atmIncidentRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
    }

    public Page<AtmIncident> getIncidentsByStatus(String status, int page, int size) {
        return atmIncidentRepository.findByStatusOrderByCreatedAtDesc(status, PageRequest.of(page, size));
    }

    public Page<AtmIncident> getIncidentsByType(String type, int page, int size) {
        return atmIncidentRepository.findByIncidentTypeOrderByCreatedAtDesc(type, PageRequest.of(page, size));
    }

    // ─── Cash Load History ────────────────────────────────

    public Page<AtmCashLoad> getCashLoadHistory(String atmId, int page, int size) {
        return atmCashLoadRepository.findByAtmIdOrderByLoadedAtDesc(atmId, PageRequest.of(page, size));
    }

    // ─── Service Log ──────────────────────────────────────

    public Page<AtmServiceLog> getServiceLog(String atmId, int page, int size) {
        return atmServiceLogRepository.findByAtmIdOrderByCreatedAtDesc(atmId, PageRequest.of(page, size));
    }

    public Page<AtmServiceLog> getAllServiceLogs(int page, int size) {
        return atmServiceLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
    }

    // ─── Dashboard Statistics ─────────────────────────────

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();

        stats.put("totalAtms", atmMachineRepository.count());
        stats.put("activeAtms", atmMachineRepository.countByStatus("ACTIVE"));
        stats.put("outOfServiceAtms", atmMachineRepository.countByStatus("OUT_OF_SERVICE"));
        stats.put("maintenanceAtms", atmMachineRepository.countByStatus("MAINTENANCE"));
        stats.put("lowCashAtms", atmMachineRepository.findLowCashAtms().size());
        stats.put("totalCashInAtms", atmMachineRepository.getTotalCashAcrossAllAtms());
        stats.put("totalCashInActiveAtms", atmMachineRepository.getTotalCashInActiveAtms());
        stats.put("totalWithdrawals", atmTransactionRepository.getTotalWithdrawals());
        stats.put("todayWithdrawals", atmTransactionRepository.getTotalWithdrawalsSince(todayStart));
        stats.put("todayTransactions", atmTransactionRepository.countTransactionsSince(todayStart));
        stats.put("totalTransactions", atmTransactionRepository.count());
        stats.put("successTransactions", atmTransactionRepository.countByStatus("SUCCESS"));
        stats.put("failedTransactions", atmTransactionRepository.countByStatus("FAILED"));
        stats.put("openIncidents", atmIncidentRepository.countOpenIncidents());
        stats.put("totalIncidents", atmIncidentRepository.count());
        stats.put("todayCashLoaded", atmCashLoadRepository.getTotalCashLoadedSince(todayStart));
        stats.put("lowCashAtmsList", atmMachineRepository.findLowCashAtms());

        return stats;
    }

    // ─── Helper ───────────────────────────────────────────

    private void logServiceAction(String atmId, String action, String prevStatus,
                                   String newStatus, String performedBy, String reason) {
        AtmServiceLog log = new AtmServiceLog();
        log.setAtmId(atmId);
        log.setAction(action);
        log.setPreviousStatus(prevStatus);
        log.setNewStatus(newStatus);
        log.setPerformedBy(performedBy);
        log.setReason(reason);
        atmServiceLogRepository.save(log);
    }

    // ─── ATM Status Check (for user-facing ATM) ──────────

    public Map<String, Object> getAtmStatusForUser(String atmId) {
        Map<String, Object> result = new HashMap<>();
        Optional<AtmMachine> atmOpt = atmMachineRepository.findByAtmId(atmId);
        if (atmOpt.isEmpty()) {
            result.put("available", false);
            result.put("message", "ATM not found");
            return result;
        }
        AtmMachine atm = atmOpt.get();
        boolean available = "ACTIVE".equals(atm.getStatus()) && atm.getCashAvailable() > 0;
        result.put("available", available);
        result.put("status", atm.getStatus());
        result.put("atmName", atm.getAtmName());
        result.put("location", atm.getLocation());
        result.put("maxWithdrawalLimit", atm.getMaxWithdrawalLimit());
        if (!available) {
            if ("OUT_OF_SERVICE".equals(atm.getStatus())) {
                result.put("message", "This ATM is currently out of service. Please try another ATM.");
            } else if ("MAINTENANCE".equals(atm.getStatus())) {
                result.put("message", "This ATM is under maintenance. Please try again later.");
            } else if (atm.getCashAvailable() <= 0) {
                result.put("message", "This ATM has no cash available. Please try another ATM.");
            }
        }
        return result;
    }

    public List<AtmMachine> getActiveAtmsForUser() {
        return atmMachineRepository.findByStatus("ACTIVE");
    }
}
