package com.neo.springapp.service;

import com.neo.springapp.model.Fasttag;
import com.neo.springapp.model.FasttagTransaction;
import com.neo.springapp.repository.FasttagRepository;
import com.neo.springapp.repository.FasttagTransactionRepository;
import com.neo.springapp.repository.FasttagEditHistoryRepository;
import com.neo.springapp.model.FasttagEditHistory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.time.LocalDateTime;

@Service
@SuppressWarnings("null")
public class FasttagService {

    @Autowired
    private FasttagRepository fasttagRepository;

    @Autowired
    private FasttagTransactionRepository transactionRepository;

    @Autowired
    private AccountService accountService;

    @Autowired
    private FasttagStickerGenerator stickerGenerator;

    @Autowired
    private FasttagEditHistoryRepository editHistoryRepository;

    @Autowired
    private ObjectMapper objectMapper;

    public List<Fasttag> listAll() {
        return fasttagRepository.findAll();
    }

    public List<Fasttag> listForUser(String userId) {
        return fasttagRepository.findByUserId(userId);
    }

    public Fasttag apply(Fasttag app) {
        // Prevent duplicate active FASTag for same vehicle number
        if (app.getVehicleNumber() != null) {
            var live = fasttagRepository.findByVehicleNumberAndStatusIn(app.getVehicleNumber(), Arrays.asList("Applied", "Approved"));
            if (live != null && !live.isEmpty()) {
                throw new RuntimeException("A FASTag for this vehicle number already exists or is active.");
            }
        }
        app.setStatus("Applied");
        if (app.getCreatedAt() == null) app.setCreatedAt(java.time.LocalDateTime.now());
        return fasttagRepository.save(app);
    }

    public Fasttag approve(Long id) {
        var opt = fasttagRepository.findById(id);
        if (opt.isEmpty()) return null;
        Fasttag t = opt.get();
        String number = "FT" + (int)(10000000 + Math.random()*90000000);
        t.setStatus("Approved");
        t.setFasttagNumber(number);
        // generate barcode number (12 digits)
        String barcode = generateBarcodeDigits(12);
        t.setBarcodeNumber(barcode);
        t.setIssueDate(LocalDateTime.now());
        t.setBalance(t.getAmount() != null ? t.getAmount() : 0.0);
        generateAndAttachSticker(t);
        Fasttag saved = fasttagRepository.save(t);

        // record initial allocation as transaction
        FasttagTransaction tx = new FasttagTransaction();
        tx.setFasttagId(saved.getId());
        tx.setFasttagNumber(saved.getFasttagNumber());
        tx.setAmount(saved.getBalance());
        tx.setType("APPROVAL_CREDIT");
        tx.setInitiatedBy("SYSTEM");
        tx.setPreviousBalance(0.0);
        tx.setNewBalance(saved.getBalance());
        transactionRepository.save(tx);

        return saved;
    }

    private String generateBarcodeDigits(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) sb.append((int)(Math.random() * 10));
        return sb.toString();
    }

    public Fasttag rechargeByTag(String fasttagNumber, Double amount, String initiatedBy, String initiatedById) {
        Fasttag t = fasttagRepository.findByFasttagNumber(fasttagNumber);
        if (t == null) return null;
        double prev = t.getBalance() == null ? 0.0 : t.getBalance();
        // If initiated by ADMIN, debit from user's account
        String accountDebited = null;
        if ("ADMIN".equalsIgnoreCase(initiatedBy)) {
            // attempt to debit assigned account if present; otherwise fallback to userId
            String accountToDebit = t.getAssignedAccountId() != null ? t.getAssignedAccountId() : t.getUserId();
            try {
                Double remaining = accountService.debitBalance(accountToDebit, amount);
                if (remaining == null) {
                    throw new RuntimeException("Insufficient funds in the debit account");
                }
                accountDebited = accountToDebit;
            } catch (Exception e) {
                throw new RuntimeException("Failed to debit account: " + e.getMessage());
            }
        }

        t.setBalance(prev + (amount == null ? 0.0 : amount));
        Fasttag saved = fasttagRepository.save(t);

        FasttagTransaction tx = new FasttagTransaction();
        tx.setFasttagId(saved.getId());
        tx.setFasttagNumber(saved.getFasttagNumber());
        tx.setAmount(amount);
        tx.setType("RECHARGE");
        tx.setInitiatedBy(initiatedBy == null ? "USER" : initiatedBy);
        tx.setInitiatedById(initiatedById);
        tx.setPreviousBalance(prev);
        tx.setNewBalance(saved.getBalance());
        tx.setAccountDebited(accountDebited);
        transactionRepository.save(tx);

        return saved;
    }

    // Admin recharge specifying an explicit account to debit (overrides assignedAccountId)
    public Fasttag rechargeByTagWithDebitAccount(String fasttagNumber, Double amount, String initiatedById, String accountToDebit) {
        Fasttag t = fasttagRepository.findByFasttagNumber(fasttagNumber);
        if (t == null) return null;
        double prev = t.getBalance() == null ? 0.0 : t.getBalance();
        String accountDebited = null;
        if (accountToDebit != null) {
            try {
                Double remaining = accountService.debitBalance(accountToDebit, amount);
                if (remaining == null) {
                    throw new RuntimeException("Insufficient funds in the debit account");
                }
                accountDebited = accountToDebit;
            } catch (Exception e) {
                throw new RuntimeException("Failed to debit account: " + e.getMessage());
            }
        } else {
            // fallback to normal behaviour
            return rechargeByTag(fasttagNumber, amount, "ADMIN", initiatedById);
        }

        t.setBalance(prev + (amount == null ? 0.0 : amount));
        Fasttag saved = fasttagRepository.save(t);

        FasttagTransaction tx = new FasttagTransaction();
        tx.setFasttagId(saved.getId());
        tx.setFasttagNumber(saved.getFasttagNumber());
        tx.setAmount(amount);
        tx.setType("RECHARGE");
        tx.setInitiatedBy("ADMIN");
        tx.setInitiatedById(initiatedById);
        tx.setPreviousBalance(prev);
        tx.setNewBalance(saved.getBalance());
        tx.setAccountDebited(accountDebited);
        transactionRepository.save(tx);

        return saved;
    }

    // Assign an account to a FASTag (one account per tag). Returns updated Fasttag.
    public Fasttag assignAccount(Long fasttagId, String accountId) {
        var opt = fasttagRepository.findById(fasttagId);
        if (opt.isEmpty()) return null;
        // ensure account not already assigned to another tag
        Fasttag existing = fasttagRepository.findByAssignedAccountId(accountId);
        if (existing != null && !existing.getId().equals(fasttagId)) {
            throw new RuntimeException("Account already assigned to another FASTag");
        }
        Fasttag t = opt.get();
        t.setAssignedAccountId(accountId);
        t.setAssignedAt(LocalDateTime.now());
        return fasttagRepository.save(t);
    }

    public Map<String,Object> getAssignedAccountInfo(Long fasttagId) {
        var opt = fasttagRepository.findById(fasttagId);
        if (opt.isEmpty()) return null;
        Fasttag t = opt.get();
        if (t.getAssignedAccountId() == null) return null;
        Map<String,Object> info = new HashMap<>();
        info.put("accountId", t.getAssignedAccountId());
        info.put("assignedAt", t.getAssignedAt());
        try {
            var acc = accountService.getAccountById(Long.valueOf(t.getAssignedAccountId()));
            info.put("account", acc);
        } catch (Exception e) {
            // ignore
        }
        return info;
    }

    public List<FasttagTransaction> transactionsForTag(Long fasttagId) {
        return transactionRepository.findByFasttagIdOrderByCreatedAtDesc(fasttagId);
    }

    public Fasttag closeFasttag(Long id) {
        var opt = fasttagRepository.findById(id);
        if (opt.isEmpty()) return null;
        Fasttag t = opt.get();
        t.setStatus("Closed");
        Fasttag saved = fasttagRepository.save(t);

        // record closure as transaction
        FasttagTransaction tx = new FasttagTransaction();
        tx.setFasttagId(saved.getId());
        tx.setFasttagNumber(saved.getFasttagNumber());
        tx.setAmount(0.0);
        tx.setType("CLOSE");
        tx.setInitiatedBy("ADMIN");
        tx.setPreviousBalance(saved.getBalance());
        tx.setNewBalance(saved.getBalance());
        transactionRepository.save(tx);

        return saved;
    }

    public Fasttag getById(Long id) {
        return fasttagRepository.findById(id).orElse(null);
    }

    public Fasttag updateAdminDetails(Long id, Fasttag updates, String changedBy) {
        Fasttag current = getById(id);
        if (current == null) return null;

        String before = snapshot(current);
        current.setUserId(updates.getUserId());
        current.setUserName(updates.getUserName());
        current.setVehicleDetails(updates.getVehicleDetails());
        current.setVehicleNumber(updates.getVehicleNumber());
        current.setAadharNumber(updates.getAadharNumber());
        current.setPanNumber(updates.getPanNumber());
        current.setDob(updates.getDob());
        current.setVehicleType(updates.getVehicleType());
        current.setAmount(updates.getAmount());
        current.setBank(updates.getBank());
        current.setChassisNumber(updates.getChassisNumber());
        current.setEngineNumber(updates.getEngineNumber());
        current.setMake(updates.getMake());
        current.setModel(updates.getModel());
        current.setFuelType(updates.getFuelType());
        current.setMobileNumber(updates.getMobileNumber());
        current.setEmail(updates.getEmail());
        current.setDispatchAddress(updates.getDispatchAddress());
        current.setPinCode(updates.getPinCode());
        current.setCity(updates.getCity());
        current.setState(updates.getState());
        current.setTagIssuanceFee(updates.getTagIssuanceFee());
        current.setTagSecurityDeposit(updates.getTagSecurityDeposit());
        current.setTagUploadAmount(updates.getTagUploadAmount());
        current.setTagTotalAmount(updates.getTagTotalAmount());
        current.setDebitAccountNumber(updates.getDebitAccountNumber());
        current.setTermsAccepted(updates.getTermsAccepted());
        current.setStatus(updates.getStatus());
        current.setFasttagNumber(updates.getFasttagNumber());
        current.setBarcodeNumber(updates.getBarcodeNumber());
        current.setIssueDate(updates.getIssueDate());
        current.setBalance(updates.getBalance());
        current.setAssignedAccountId(updates.getAssignedAccountId());
        current.setAssignedAt(updates.getAssignedAt());

        Fasttag saved = fasttagRepository.save(current);
        FasttagEditHistory history = new FasttagEditHistory();
        history.setFasttagId(saved.getId());
        history.setFasttagNumber(saved.getFasttagNumber());
        history.setChangedBy(changedBy == null || changedBy.isBlank() ? "admin" : changedBy);
        history.setBeforeValues(before);
        history.setAfterValues(snapshot(saved));
        editHistoryRepository.save(history);
        return saved;
    }

    public List<FasttagEditHistory> editHistoryForTag(Long id) {
        return editHistoryRepository.findByFasttagIdOrderByChangedAtDesc(id);
    }

    private String snapshot(Fasttag tag) {
        try {
            Map<String, Object> values = new HashMap<>();
            values.put("userId", tag.getUserId());
            values.put("userName", tag.getUserName());
            values.put("vehicleDetails", tag.getVehicleDetails());
            values.put("vehicleNumber", tag.getVehicleNumber());
            values.put("aadharNumber", tag.getAadharNumber());
            values.put("panNumber", tag.getPanNumber());
            values.put("dob", tag.getDob());
            values.put("vehicleType", tag.getVehicleType());
            values.put("amount", tag.getAmount());
            values.put("bank", tag.getBank());
            values.put("chassisNumber", tag.getChassisNumber());
            values.put("engineNumber", tag.getEngineNumber());
            values.put("make", tag.getMake());
            values.put("model", tag.getModel());
            values.put("fuelType", tag.getFuelType());
            values.put("mobileNumber", tag.getMobileNumber());
            values.put("email", tag.getEmail());
            values.put("dispatchAddress", tag.getDispatchAddress());
            values.put("pinCode", tag.getPinCode());
            values.put("city", tag.getCity());
            values.put("state", tag.getState());
            values.put("status", tag.getStatus());
            values.put("fasttagNumber", tag.getFasttagNumber());
            values.put("barcodeNumber", tag.getBarcodeNumber());
            values.put("balance", tag.getBalance());
            return objectMapper.writeValueAsString(values);
        } catch (Exception e) {
            return "{}";
        }
    }

    public Fasttag ensureStickerForApprovedTag(Fasttag tag) {
        if (tag == null || !"Approved".equalsIgnoreCase(tag.getStatus())) {
            return tag;
        }
        if (tag.getStickerPath() != null && !tag.getStickerPath().isBlank()) {
            java.io.File existingFile = new java.io.File(tag.getStickerPath());
            if (existingFile.exists()) {
                return tag;
            }
        }
        if (tag.getFasttagNumber() == null || tag.getFasttagNumber().isBlank()) {
            tag.setFasttagNumber("FT" + (int) (10000000 + Math.random() * 90000000));
        }
        if (tag.getBarcodeNumber() == null || tag.getBarcodeNumber().isBlank()) {
            tag.setBarcodeNumber(generateBarcodeDigits(12));
        }
        if (tag.getIssueDate() == null) {
            tag.setIssueDate(LocalDateTime.now());
        }
        generateAndAttachSticker(tag);
        return fasttagRepository.save(tag);
    }

    public byte[] getStickerBytes(Fasttag tag) throws Exception {
        tag = ensureStickerForApprovedTag(tag);
        if (tag.getStickerPath() != null && !tag.getStickerPath().isBlank()) {
            java.io.File file = new java.io.File(tag.getStickerPath());
            if (file.exists()) {
                return java.nio.file.Files.readAllBytes(file.toPath());
            }
        }
        return stickerGenerator.generateStickerPngBytes(
                tag.getFasttagNumber(),
                tag.getBarcodeNumber(),
                tag.getUserName() == null ? "" : tag.getUserName(),
                tag.getVehicleNumber() == null ? "" : tag.getVehicleNumber(),
                tag.getBank() == null ? "NeoBank" : tag.getBank(),
                tag.getIssueDate() == null ? LocalDateTime.now() : tag.getIssueDate());
    }

    public Fasttag save(Fasttag fasttag) {
        return fasttagRepository.save(fasttag);
    }

    public Fasttag rechargeByVehicleNumber(String vehicleNumber, Double amount, String userId) {
        var tags = fasttagRepository.findByVehicleNumberAndStatusIn(vehicleNumber, Arrays.asList("Approved"));
        if (tags == null || tags.isEmpty()) return null;
        Fasttag t = tags.get(0);
        double prev = t.getBalance() == null ? 0.0 : t.getBalance();
        t.setBalance(prev + (amount == null ? 0.0 : amount));
        Fasttag saved = fasttagRepository.save(t);

        FasttagTransaction tx = new FasttagTransaction();
        tx.setFasttagId(saved.getId());
        tx.setFasttagNumber(saved.getFasttagNumber());
        tx.setAmount(amount);
        tx.setType("RECHARGE");
        tx.setInitiatedBy("USER");
        tx.setInitiatedById(userId);
        tx.setPreviousBalance(prev);
        tx.setNewBalance(saved.getBalance());
        transactionRepository.save(tx);

        return saved;
    }

    /**
     * Find FASTag applications linked to an email address
     */
    public List<Fasttag> findByEmail(String email) {
        return fasttagRepository.findByEmail(email);
    }

    private void generateAndAttachSticker(Fasttag tag) {
        try {
            String outputDir = "uploads/fasttag-stickers";
            String stickerPath = stickerGenerator.generateStickerPdf(
                    tag.getFasttagNumber(),
                    tag.getBarcodeNumber(),
                    tag.getUserName() == null ? "" : tag.getUserName(),
                    tag.getVehicleNumber() == null ? "" : tag.getVehicleNumber(),
                    tag.getBank() == null ? "NeoBank" : tag.getBank(),
                    tag.getIssueDate() == null ? LocalDateTime.now() : tag.getIssueDate(),
                    outputDir
            );
            tag.setStickerPath(stickerPath);
        } catch (Exception e) {
            // Sticker generation failure should not block FASTag business flows.
            System.err.println("Failed to generate FASTag sticker: " + e.getMessage());
        }
    }
}
