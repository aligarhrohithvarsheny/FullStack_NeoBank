package com.neo.springapp.service;

import com.neo.springapp.model.SoundboxDevice;
import com.neo.springapp.model.SoundboxRequest;
import com.neo.springapp.model.SoundboxTransaction;
import com.neo.springapp.model.CurrentAccount;
import com.neo.springapp.repository.SoundboxDeviceRepository;
import com.neo.springapp.repository.SoundboxRequestRepository;
import com.neo.springapp.repository.SoundboxTransactionRepository;
import com.neo.springapp.repository.CurrentAccountRepository;
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

    public SoundboxService(SoundboxDeviceRepository deviceRepository,
                           SoundboxRequestRepository requestRepository,
                           SoundboxTransactionRepository transactionRepository,
                           CurrentAccountRepository currentAccountRepository) {
        this.deviceRepository = deviceRepository;
        this.requestRepository = requestRepository;
        this.transactionRepository = transactionRepository;
        this.currentAccountRepository = currentAccountRepository;
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
                                               Double monthlyCharge, Double deviceCharge) {
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
        device.setDeviceCharge(request.getDeviceCharge());
        device.setChargeStatus("PAID");
        device.setActivatedAt(LocalDateTime.now());
        device.setLastActiveAt(LocalDateTime.now());
        deviceRepository.save(device);

        // Deduct device charge from current account
        Optional<CurrentAccount> optAccount = currentAccountRepository.findByAccountNumber(request.getAccountNumber());
        if (optAccount.isPresent()) {
            CurrentAccount account = optAccount.get();
            if (account.getBalance() >= deviceCharge) {
                account.setBalance(account.getBalance() - deviceCharge);
                currentAccountRepository.save(account);
            }
        }

        result.put("success", true);
        result.put("request", request);
        result.put("device", device);
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

        // Verify account exists and is active
        Optional<CurrentAccount> optAccount = currentAccountRepository.findByAccountNumber(transaction.getAccountNumber());
        if (optAccount.isEmpty()) {
            result.put("success", false);
            result.put("error", "Account not found");
            return result;
        }
        CurrentAccount account = optAccount.get();
        if (!"ACTIVE".equals(account.getStatus())) {
            result.put("success", false);
            result.put("error", "Account is not active");
            return result;
        }

        // Credit the amount to current account
        account.setBalance(account.getBalance() + transaction.getAmount());
        currentAccountRepository.save(account);

        // Check if soundbox device exists
        Optional<SoundboxDevice> optDevice = deviceRepository.findByAccountNumber(transaction.getAccountNumber());
        String voiceMessage = "Received " + transaction.getAmount().intValue() + " rupees in NeoBank Current Account";

        if (optDevice.isPresent()) {
            SoundboxDevice device = optDevice.get();
            transaction.setDeviceId(device.getDeviceId());
            device.setLastActiveAt(LocalDateTime.now());
            deviceRepository.save(device);

            if (device.getVoiceEnabled() && "ACTIVE".equals(device.getStatus())) {
                transaction.setVoicePlayed(true);
                transaction.setVoiceMessage(voiceMessage);
            }
        }

        transaction.setStatus("SUCCESS");
        transactionRepository.save(transaction);

        result.put("success", true);
        result.put("transaction", transaction);
        result.put("voiceMessage", voiceMessage);
        result.put("voiceEnabled", optDevice.isPresent() && optDevice.get().getVoiceEnabled());
        result.put("newBalance", account.getBalance());
        result.put("message", "Payment received successfully");
        return result;
    }

    public List<SoundboxTransaction> getTransactionsByAccount(String accountNumber) {
        return transactionRepository.findByAccountNumberOrderByCreatedAtDesc(accountNumber);
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
