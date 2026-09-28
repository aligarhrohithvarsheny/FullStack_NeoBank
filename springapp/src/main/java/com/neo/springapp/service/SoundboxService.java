package com.neo.springapp.service;

import com.neo.springapp.model.SoundboxDevice;
import com.neo.springapp.model.SoundboxRequest;
import com.neo.springapp.model.SoundboxTransaction;
import com.neo.springapp.model.CurrentAccount;
import com.neo.springapp.model.Admin;
import com.neo.springapp.model.FundsAllocation;
import com.neo.springapp.model.Account;
import com.neo.springapp.model.SalaryAccount;
import com.neo.springapp.model.SoundboxLinkedAccount;
import com.neo.springapp.entity.ChargeTransaction;
import com.neo.springapp.repository.AdminRepository;
import com.neo.springapp.repository.FundsAllocationRepository;
import com.neo.springapp.repository.SoundboxDeviceRepository;
import com.neo.springapp.repository.SoundboxRequestRepository;
import com.neo.springapp.repository.SoundboxTransactionRepository;
import com.neo.springapp.repository.CurrentAccountRepository;
import com.neo.springapp.repository.AccountRepository;
import com.neo.springapp.repository.SalaryAccountRepository;
import com.neo.springapp.repository.SoundboxLinkedAccountRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@SuppressWarnings("null")
public class SoundboxService {

    private final SoundboxDeviceRepository deviceRepository;
    private final SoundboxRequestRepository requestRepository;
    private final SoundboxTransactionRepository transactionRepository;
    private final CurrentAccountRepository currentAccountRepository;
    private final FundsAllocationRepository allocationRepository;
    private final AdminRepository adminRepository;
    private final ChargeManagementService chargeManagementService;
    private final AccountRepository accountRepository;
    private final SalaryAccountRepository salaryAccountRepository;
    private final SoundboxLinkedAccountRepository linkedAccountRepository;

    public SoundboxService(SoundboxDeviceRepository deviceRepository,
                           SoundboxRequestRepository requestRepository,
                           SoundboxTransactionRepository transactionRepository,
                           CurrentAccountRepository currentAccountRepository,
                           FundsAllocationRepository allocationRepository,
                           AdminRepository adminRepository,
                           ChargeManagementService chargeManagementService,
                           AccountRepository accountRepository,
                           SalaryAccountRepository salaryAccountRepository,
                           SoundboxLinkedAccountRepository linkedAccountRepository) {
        this.deviceRepository = deviceRepository;
        this.requestRepository = requestRepository;
        this.transactionRepository = transactionRepository;
        this.currentAccountRepository = currentAccountRepository;
        this.allocationRepository = allocationRepository;
        this.adminRepository = adminRepository;
        this.chargeManagementService = chargeManagementService;
        this.accountRepository = accountRepository;
        this.salaryAccountRepository = salaryAccountRepository;
        this.linkedAccountRepository = linkedAccountRepository;
    }

    public Map<String, Object> lookupLinkedAccount(String customerId, String accountNumber) {
        Map<String, Object> result = new HashMap<>();
        String normalizedCustomerId = customerId == null ? "" : customerId.trim();
        String normalizedAccountNumber = accountNumber == null ? "" : accountNumber.trim();
        if (normalizedCustomerId.isEmpty() || normalizedAccountNumber.isEmpty()) {
            result.put("success", false);
            result.put("error", "Customer ID and account number are required.");
            return result;
        }

        Map<String, Object> identity = findAccountIdentity(normalizedCustomerId, normalizedAccountNumber);
        if (identity == null) {
            result.put("success", false);
            result.put("error", "No active NeoBank account matches that customer ID and account number.");
            return result;
        }
        result.put("success", true);
        result.put("account", identity);
        return result;
    }

    @Transactional
    public SoundboxLinkedAccount requestLinkedAccount(String soundboxAccountNumber, String customerId, String accountNumber) {
        String ownerAccountNumber = soundboxAccountNumber == null ? "" : soundboxAccountNumber.trim();
        String normalizedCustomerId = customerId == null ? "" : customerId.trim();
        String normalizedAccountNumber = accountNumber == null ? "" : accountNumber.trim();
        if (deviceRepository.findByAccountNumber(ownerAccountNumber).filter(d -> "ACTIVE".equalsIgnoreCase(d.getStatus())).isEmpty()) {
            throw new IllegalArgumentException("An active Soundbox device is required to link an account.");
        }
        Map<String, Object> identity = findAccountIdentity(normalizedCustomerId, normalizedAccountNumber);
        if (identity == null) throw new IllegalArgumentException("No active NeoBank account matches that customer ID and account number.");
        if (ownerAccountNumber.equals(normalizedAccountNumber)) throw new IllegalArgumentException("The Soundbox account is already the receiving account.");
        if (linkedAccountRepository.existsBySoundboxAccountNumberAndLinkedAccountNumberAndStatusIn(
                ownerAccountNumber, normalizedAccountNumber, Arrays.asList("PENDING", "APPROVED"))) {
            throw new IllegalArgumentException("This receiving account is already pending or linked.");
        }

        SoundboxLinkedAccount linkedAccount = new SoundboxLinkedAccount();
        linkedAccount.setSoundboxAccountNumber(ownerAccountNumber);
        linkedAccount.setLinkedAccountNumber(normalizedAccountNumber);
        linkedAccount.setLinkedCustomerId(normalizedCustomerId);
        linkedAccount.setLinkedAccountName((String) identity.get("name"));
        linkedAccount.setAccountType((String) identity.get("accountType"));
        linkedAccount.setStatus("PENDING");
        return linkedAccountRepository.save(linkedAccount);
    }

    public List<SoundboxLinkedAccount> getLinkedAccounts(String soundboxAccountNumber) {
        return linkedAccountRepository.findBySoundboxAccountNumberOrderByRequestedAtDesc(soundboxAccountNumber);
    }

    public List<SoundboxLinkedAccount> getPendingLinkedAccounts() {
        return linkedAccountRepository.findByStatusOrderByRequestedAtDesc("PENDING");
    }

    @Transactional
    public SoundboxLinkedAccount reviewLinkedAccount(Long id, String adminName, String remarks, boolean approve) {
        SoundboxLinkedAccount linkedAccount = linkedAccountRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Linked account request not found."));
        if (!"PENDING".equalsIgnoreCase(linkedAccount.getStatus())) {
            throw new IllegalArgumentException("Linked account request is no longer pending.");
        }
        if (approve && findAccountIdentity(linkedAccount.getLinkedCustomerId(), linkedAccount.getLinkedAccountNumber()) == null) {
            throw new IllegalArgumentException("The NeoBank account is no longer active or its identity no longer matches.");
        }
        linkedAccount.setStatus(approve ? "APPROVED" : "REJECTED");
        linkedAccount.setProcessedBy(adminName);
        linkedAccount.setAdminRemarks(remarks);
        linkedAccount.setProcessedAt(LocalDateTime.now());
        return linkedAccountRepository.save(linkedAccount);
    }

    private Map<String, Object> findAccountIdentity(String customerId, String accountNumber) {
        Account savings = accountRepository.findByCustomerIdAndAccountNumber(customerId, accountNumber);
        if (savings != null && "ACTIVE".equalsIgnoreCase(savings.getStatus())) {
            return accountIdentity(savings.getName(), accountNumber, customerId, "SAVINGS");
        }
        Optional<CurrentAccount> current = currentAccountRepository.findByAccountNumber(accountNumber);
        if (current.isPresent() && customerId.equals(current.get().getCustomerId()) &&
                ("ACTIVE".equalsIgnoreCase(current.get().getStatus()) || "APPROVED".equalsIgnoreCase(current.get().getStatus()))) {
            return accountIdentity(current.get().getBusinessName(), accountNumber, customerId, "CURRENT");
        }
        SalaryAccount salary = salaryAccountRepository.findByAccountNumber(accountNumber);
        if (salary != null && customerId.equals(salary.getCustomerId()) && "ACTIVE".equalsIgnoreCase(salary.getStatus())) {
            return accountIdentity(salary.getEmployeeName(), accountNumber, customerId, "SALARY");
        }
        return null;
    }

    private Map<String, Object> accountIdentity(String name, String accountNumber, String customerId, String accountType) {
        Map<String, Object> identity = new HashMap<>();
        identity.put("name", name);
        identity.put("accountNumber", accountNumber);
        identity.put("customerId", customerId);
        identity.put("accountType", accountType);
        return identity;
    }

    // ==================== Soundbox Request Operations ====================

    public SoundboxRequest applyForSoundbox(SoundboxRequest request) {
        // Check if account already has a pending/approved request
        if (requestRepository.existsByAccountNumberAndStatusIn(
                request.getAccountNumber(), Arrays.asList("PENDING", "APPROVED"))) {
            throw new RuntimeException("You already have an active soundbox request");
        }
        // Check if device already exists for this account
        if (deviceRepository.existsByAccountNumber(request.getAccountNumber())) {
            throw new RuntimeException("A soundbox device is already linked to your account");
        }
        request.setStatus("PENDING");
        return requestRepository.save(request);
    }

    public List<SoundboxRequest> getRequestsByAccount(String accountNumber) {
        return requestRepository.findByAccountNumber(accountNumber);
    }

    public List<SoundboxRequest> getPendingRequests() {
        return requestRepository.findByStatusOrderByRequestedAtDesc("PENDING");
    }

    public List<SoundboxRequest> getAllRequests() {
        return requestRepository.findAll(Sort.by(Sort.Direction.DESC, "requestedAt"));
    }

    @Transactional
    public Map<String, Object> approveRequest(Long requestId, String adminName, String deviceId,
                                               Double monthlyCharge, Double deviceCharge, Long allocationId) {
        Map<String, Object> result = new HashMap<>();
        Optional<SoundboxRequest> optRequest = requestRepository.findById(requestId);
        if (optRequest.isEmpty()) {
            result.put("success", false);
            result.put("error", "Request not found");
            return result;
        }

        SoundboxRequest request = optRequest.get();
        if (!"PENDING".equals(request.getStatus())) {
            result.put("success", false);
            result.put("error", "Request is not in PENDING status");
            return result;
        }

        FundsAllocation allocation = allocationRepository.findById(allocationId).orElse(null);
        if (allocation == null || !"ACTIVE".equalsIgnoreCase(allocation.getStatus()) ||
                !"VERIFIED".equalsIgnoreCase(allocation.getAccountStatus()) ||
                !Boolean.TRUE.equals(allocation.getChargeManagementEnabled()) ||
                allocation.getAllocationAccountId() == null) {
            result.put("success", false);
            result.put("error", "Select an active allocation with a verified linked account and charge management enabled.");
            return result;
        }

        Admin allocationManager = allocation.getManagerId() == null
            ? null : adminRepository.findById(allocation.getManagerId()).orElse(null);
        String allocationCity = allocationManager == null ? null : allocationManager.getAssignedCity();
        if (request.getCity() != null && !request.getCity().isBlank() && allocationCity != null &&
                !request.getCity().trim().equalsIgnoreCase(allocationCity.trim())) {
            result.put("success", false);
            result.put("error", "Selected allocation belongs to a different city than this Soundbox request.");
            return result;
        }

        Double charge = deviceCharge != null ? deviceCharge : request.getDeviceCharge();
        if (charge == null || charge <= 0) {
            result.put("success", false);
            result.put("error", "A positive Soundbox device charge is required.");
            return result;
        }

        ChargeTransaction chargeTransaction = chargeManagementService.processCharge(
            allocation.getId(),
            "SOUNDBOX",
            "Soundbox device charge for request " + request.getRequestId(),
            java.math.BigDecimal.valueOf(charge),
            request.getAccountNumber(),
            request.getOwnerName(),
            "CURRENT_ACCOUNT",
            "SOUNDBOX-" + request.getRequestId(),
            null,
            null
        );

        // Update request
        request.setStatus("APPROVED");
        request.setProcessedBy(adminName);
        request.setProcessedAt(LocalDateTime.now());
        request.setAssignedDeviceId(deviceId);
        if (monthlyCharge != null) request.setMonthlyCharge(monthlyCharge);
        if (deviceCharge != null) request.setDeviceCharge(deviceCharge);
        requestRepository.save(request);

        // Create device
        SoundboxDevice device = new SoundboxDevice();
        device.setDeviceId(deviceId);
        device.setAccountNumber(request.getAccountNumber());
        device.setBusinessName(request.getBusinessName());
        device.setOwnerName(request.getOwnerName());
        device.setStatus("ACTIVE");
        device.setMonthlyCharge(request.getMonthlyCharge());
        device.setDeviceCharge(charge);
        device.setChargeStatus("PAID");
        device.setActivatedAt(LocalDateTime.now());
        device.setLastActiveAt(LocalDateTime.now());
        deviceRepository.save(device);

        CurrentAccount ownerAccount = currentAccountRepository.findByAccountNumber(request.getAccountNumber()).orElse(null);
        if (ownerAccount != null && ownerAccount.getCustomerId() != null &&
            !linkedAccountRepository.existsBySoundboxAccountNumberAndLinkedAccountNumberAndStatusIn(
                request.getAccountNumber(), request.getAccountNumber(), Arrays.asList("PENDING", "APPROVED"))) {
            SoundboxLinkedAccount primaryAccount = new SoundboxLinkedAccount();
            primaryAccount.setSoundboxAccountNumber(request.getAccountNumber());
            primaryAccount.setLinkedAccountNumber(request.getAccountNumber());
            primaryAccount.setLinkedCustomerId(ownerAccount.getCustomerId());
            primaryAccount.setLinkedAccountName(ownerAccount.getBusinessName());
            primaryAccount.setAccountType("CURRENT");
            primaryAccount.setStatus("APPROVED");
            primaryAccount.setProcessedBy(adminName);
            primaryAccount.setProcessedAt(LocalDateTime.now());
            linkedAccountRepository.save(primaryAccount);
        }

        result.put("success", true);
        result.put("request", request);
        result.put("device", device);
        result.put("chargeTransactionId", chargeTransaction.getChargeTransactionId());
        result.put("allocationId", allocation.getId());
        result.put("message", "Soundbox approved and device assigned");
        return result;
    }

    @Transactional
    public Map<String, Object> rejectRequest(Long requestId, String adminName, String remarks) {
        Map<String, Object> result = new HashMap<>();
        Optional<SoundboxRequest> optRequest = requestRepository.findById(requestId);
        if (optRequest.isEmpty()) {
            result.put("success", false);
            result.put("error", "Request not found");
            return result;
        }

        SoundboxRequest request = optRequest.get();
        request.setStatus("REJECTED");
        request.setProcessedBy(adminName);
        request.setProcessedAt(LocalDateTime.now());
        request.setAdminRemarks(remarks);
        requestRepository.save(request);

        result.put("success", true);
        result.put("request", request);
        result.put("message", "Soundbox request rejected");
        return result;
    }

    // ==================== Device Operations ====================

    public Optional<SoundboxDevice> getDeviceByAccount(String accountNumber) {
        return deviceRepository.findByAccountNumber(accountNumber);
    }

    public Optional<SoundboxDevice> getDeviceById(String deviceId) {
        return deviceRepository.findByDeviceId(deviceId);
    }

    public List<SoundboxDevice> getAllDevices() {
        return deviceRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    public List<SoundboxDevice> getDevicesByStatus(String status) {
        return deviceRepository.findByStatus(status);
    }

    @Transactional
    public SoundboxDevice updateDeviceSettings(String accountNumber, Map<String, Object> settings) {
        Optional<SoundboxDevice> optDevice = deviceRepository.findByAccountNumber(accountNumber);
        if (optDevice.isEmpty()) {
            throw new RuntimeException("No soundbox device found for this account");
        }
        SoundboxDevice device = optDevice.get();

        if (settings.containsKey("voiceEnabled")) {
            device.setVoiceEnabled((Boolean) settings.get("voiceEnabled"));
        }
        if (settings.containsKey("voiceLanguage")) {
            device.setVoiceLanguage((String) settings.get("voiceLanguage"));
        }
        if (settings.containsKey("volumeMode")) {
            device.setVolumeMode((String) settings.get("volumeMode"));
        }
        if (settings.containsKey("linkedUpi")) {
            device.setLinkedUpi((String) settings.get("linkedUpi"));
        }
        return deviceRepository.save(device);
    }

    @Transactional
    public SoundboxDevice toggleDeviceStatus(Long deviceId) {
        Optional<SoundboxDevice> optDevice = deviceRepository.findById(deviceId);
        if (optDevice.isEmpty()) throw new RuntimeException("Device not found");
        SoundboxDevice device = optDevice.get();
        if ("ACTIVE".equals(device.getStatus())) {
            device.setStatus("INACTIVE");
        } else {
            device.setStatus("ACTIVE");
            device.setLastActiveAt(LocalDateTime.now());
        }
        return deviceRepository.save(device);
    }

    @Transactional
    public SoundboxDevice linkUpi(String accountNumber, String upiId) {
        Optional<SoundboxDevice> optDevice = deviceRepository.findByAccountNumber(accountNumber);
        if (optDevice.isEmpty()) throw new RuntimeException("No soundbox device found");
        SoundboxDevice device = optDevice.get();
        String existing = device.getLinkedUpi();
        if (existing == null || existing.isEmpty()) {
            device.setLinkedUpi(upiId);
        } else {
            device.setLinkedUpi(existing + "," + upiId);
        }
        return deviceRepository.save(device);
    }

    @Transactional
    public SoundboxDevice removeUpi(String accountNumber, String upiId) {
        Optional<SoundboxDevice> optDevice = deviceRepository.findByAccountNumber(accountNumber);
        if (optDevice.isEmpty()) throw new RuntimeException("No soundbox device found");
        SoundboxDevice device = optDevice.get();
        String existing = device.getLinkedUpi();
        if (existing != null) {
            String[] upis = existing.split(",");
            StringBuilder sb = new StringBuilder();
            for (String u : upis) {
                if (!u.trim().equals(upiId.trim())) {
                    if (sb.length() > 0) sb.append(",");
                    sb.append(u.trim());
                }
            }
            device.setLinkedUpi(sb.toString());
        }
        return deviceRepository.save(device);
    }

    // ==================== Transaction / Payment Operations ====================

    @Transactional
    public Map<String, Object> processPayment(SoundboxTransaction transaction) {
        Map<String, Object> result = new HashMap<>();
        if (transaction.getAmount() == null || !Double.isFinite(transaction.getAmount()) || transaction.getAmount() <= 0) {
            result.put("success", false);
            result.put("error", "Payment amount must be greater than zero.");
            return result;
        }
        if (transaction.getTxnType() != null && !"CREDIT".equalsIgnoreCase(transaction.getTxnType())) {
            result.put("success", false);
            result.put("error", "Soundbox receive-payment requests must be credits.");
            return result;
        }
        if (transaction.getPaymentMethod() == null || !Arrays.asList("UPI", "QR", "NFC")
                .contains(transaction.getPaymentMethod().trim().toUpperCase(Locale.ROOT))) {
            result.put("success", false);
            result.put("error", "Unsupported Soundbox payment method.");
            return result;
        }
        if (transaction.getSoundboxAccountNumber() == null || transaction.getAccountNumber() == null) {
            result.put("success", false);
            result.put("error", "Soundbox and receiving account numbers are required.");
            return result;
        }
        SoundboxLinkedAccount linkedAccount = linkedAccountRepository
            .findBySoundboxAccountNumberAndLinkedAccountNumberAndStatus(
                transaction.getSoundboxAccountNumber(), transaction.getAccountNumber(), "APPROVED")
            .orElse(null);
        if (linkedAccount == null) {
            result.put("success", false);
            result.put("error", "This receiving account has not been approved for this Soundbox.");
            return result;
        }
        SoundboxDevice device = deviceRepository.findByAccountNumber(transaction.getSoundboxAccountNumber()).orElse(null);
        if (device == null || !"ACTIVE".equalsIgnoreCase(device.getStatus())) {
            result.put("success", false);
            result.put("error", "Soundbox device is not active.");
            return result;
        }
        transaction.setDeviceId(device.getDeviceId());
        transaction.setTxnType("CREDIT");
        transaction.setAccountType(linkedAccount.getAccountType());
        transaction.setStatus("PENDING");
        transaction.setVoicePlayed(false);
        transactionRepository.save(transaction);
        String voiceMessage = "Payment received. " + transaction.getAmount().intValue() + " rupees, pending admin approval.";
        result.put("success", true);
        result.put("transaction", transaction);
        result.put("voiceMessage", voiceMessage);
        result.put("pendingApproval", true);
        result.put("message", "Payment recorded and awaiting admin approval.");
        return result;
    }

    public List<SoundboxTransaction> getPendingPayments() {
        return transactionRepository.findByStatusOrderByCreatedAtDesc("PENDING");
    }

    @Transactional
    public Map<String, Object> reviewPayment(Long id, String adminName, boolean approve) {
        Map<String, Object> result = new HashMap<>();
        SoundboxTransaction transaction = transactionRepository.findById(id).orElse(null);
        if (transaction == null || !"PENDING".equalsIgnoreCase(transaction.getStatus())) {
            result.put("success", false);
            result.put("error", "Pending Soundbox payment not found.");
            return result;
        }
        if (approve) {
            SoundboxLinkedAccount linkedAccount = linkedAccountRepository
                .findBySoundboxAccountNumberAndLinkedAccountNumberAndStatus(
                    transaction.getSoundboxAccountNumber(), transaction.getAccountNumber(), "APPROVED")
                .orElse(null);
            if (linkedAccount == null || !creditLinkedAccount(linkedAccount, transaction.getAmount())) {
                result.put("success", false);
                result.put("error", "Linked account is unavailable or inactive; payment was not approved.");
                return result;
            }
            transaction.setStatus("SUCCESS");
            transaction.setApprovedBy(adminName);
            transaction.setApprovedAt(LocalDateTime.now());
            transaction.setVoicePlayed(true);
            transaction.setVoiceMessage("Payment approved and credited to " + transaction.getAccountNumber());
            result.put("message", "Payment approved and credited.");
        } else {
            transaction.setStatus("REJECTED");
            transaction.setApprovedBy(adminName);
            transaction.setApprovedAt(LocalDateTime.now());
            result.put("message", "Payment rejected.");
        }
        transactionRepository.save(transaction);
        result.put("success", true);
        result.put("transaction", transaction);
        return result;
    }

    private boolean creditLinkedAccount(SoundboxLinkedAccount linkedAccount, Double amount) {
        switch (linkedAccount.getAccountType()) {
            case "SAVINGS": {
                Account account = accountRepository.findByCustomerIdAndAccountNumber(
                    linkedAccount.getLinkedCustomerId(), linkedAccount.getLinkedAccountNumber());
                if (account == null || !"ACTIVE".equalsIgnoreCase(account.getStatus()) ||
                        !linkedAccount.getLinkedCustomerId().equals(account.getCustomerId())) return false;
                account.setBalance((account.getBalance() == null ? 0 : account.getBalance()) + amount);
                account.setLastUpdated(LocalDateTime.now());
                accountRepository.save(account);
                return true;
            }
            case "CURRENT": {
                CurrentAccount account = currentAccountRepository.findByAccountNumber(linkedAccount.getLinkedAccountNumber()).orElse(null);
                if (account == null || !linkedAccount.getLinkedCustomerId().equals(account.getCustomerId()) ||
                    !("ACTIVE".equalsIgnoreCase(account.getStatus()) || "APPROVED".equalsIgnoreCase(account.getStatus()))) return false;
                account.setBalance((account.getBalance() == null ? 0 : account.getBalance()) + amount);
                currentAccountRepository.save(account);
                return true;
            }
            case "SALARY": {
                SalaryAccount account = salaryAccountRepository.findByAccountNumber(linkedAccount.getLinkedAccountNumber());
                if (account == null || !"ACTIVE".equalsIgnoreCase(account.getStatus()) ||
                    !linkedAccount.getLinkedCustomerId().equals(account.getCustomerId())) return false;
                account.setBalance((account.getBalance() == null ? 0 : account.getBalance()) + amount);
                salaryAccountRepository.save(account);
                return true;
            }
            default:
                return false;
        }
    }

    public List<SoundboxTransaction> getTransactionsByAccount(String accountNumber) {
        return transactionRepository.findByAccountNumberOrderByCreatedAtDesc(accountNumber);
    }

    public List<SoundboxTransaction> getTransactionsBySoundbox(String soundboxAccountNumber) {
        return transactionRepository.findBySoundboxAccountNumberOrderByCreatedAtDesc(soundboxAccountNumber);
    }

    public Page<SoundboxTransaction> getTransactionsByAccountPaginated(String accountNumber, int page, int size) {
        return transactionRepository.findByAccountNumber(accountNumber,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    // ==================== Statistics ====================

    public Map<String, Object> getUserSoundboxStats(String accountNumber) {
        Map<String, Object> stats = new HashMap<>();
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();

        Optional<SoundboxDevice> device = deviceRepository.findByAccountNumber(accountNumber);
        stats.put("hasDevice", device.isPresent());
        if (device.isPresent()) {
            stats.put("device", device.get());
            stats.put("deviceStatus", device.get().getStatus());
            stats.put("voiceEnabled", device.get().getVoiceEnabled());
        }

        Double totalReceived = transactionRepository.getTotalReceivedByAccount(accountNumber);
        Double todayReceived = transactionRepository.getTodayReceivedByAccount(accountNumber, startOfDay);
        long todayCount = transactionRepository.countTodayTransactions(accountNumber, startOfDay);

        stats.put("totalReceived", totalReceived);
        stats.put("todayReceived", todayReceived);
        stats.put("todayTransactions", todayCount);

        return stats;
    }

    public Map<String, Object> getSoundboxStats(String soundboxAccountNumber) {
        Map<String, Object> stats = new HashMap<>();
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        Optional<SoundboxDevice> device = deviceRepository.findByAccountNumber(soundboxAccountNumber);
        stats.put("hasDevice", device.isPresent());
        device.ifPresent(value -> {
            stats.put("device", value);
            stats.put("deviceStatus", value.getStatus());
            stats.put("voiceEnabled", value.getVoiceEnabled());
        });
        stats.put("totalReceived", transactionRepository.getTotalReceivedBySoundbox(soundboxAccountNumber));
        stats.put("todayReceived", transactionRepository.getTodayReceivedBySoundbox(soundboxAccountNumber, startOfDay));
        stats.put("todayTransactions", transactionRepository.countTodayTransactionsBySoundbox(soundboxAccountNumber, startOfDay));
        return stats;
    }

    public Map<String, Object> getAdminSoundboxStats() {
        Map<String, Object> stats = new HashMap<>();
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();

        stats.put("totalDevices", deviceRepository.countTotalDevices());
        stats.put("activeDevices", deviceRepository.countActiveDevices());
        stats.put("pendingRequests", requestRepository.countPendingRequests());
        stats.put("totalTransactions", transactionRepository.countSuccessfulTransactions());
        stats.put("totalRevenue", transactionRepository.getTotalTransactionAmount());
        stats.put("todayRevenue", transactionRepository.getTodayTotalAmount(startOfDay));

        // Calculate charges revenue
        List<SoundboxDevice> allDevices = deviceRepository.findAll();
        double monthlyChargesRevenue = allDevices.stream()
                .filter(d -> "PAID".equals(d.getChargeStatus()))
                .mapToDouble(d -> d.getMonthlyCharge() != null ? d.getMonthlyCharge() : 0)
                .sum();
        double deviceChargesRevenue = allDevices.stream()
                .mapToDouble(d -> d.getDeviceCharge() != null ? d.getDeviceCharge() : 0)
                .sum();
        stats.put("monthlyChargesRevenue", monthlyChargesRevenue);
        stats.put("deviceChargesRevenue", deviceChargesRevenue);

        return stats;
    }

    // ==================== Admin: Update Charges ====================

    @Transactional
    public SoundboxDevice updateCharges(Long deviceId, Double monthlyCharge, Double deviceCharge) {
        Optional<SoundboxDevice> optDevice = deviceRepository.findById(deviceId);
        if (optDevice.isEmpty()) throw new RuntimeException("Device not found");
        SoundboxDevice device = optDevice.get();
        if (monthlyCharge != null) device.setMonthlyCharge(monthlyCharge);
        if (deviceCharge != null) device.setDeviceCharge(deviceCharge);
        return deviceRepository.save(device);
    }
}
