package com.neo.springapp.service;

import com.neo.springapp.model.*;
import com.neo.springapp.repository.*;
import org.springframework.util.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@SuppressWarnings("null")
public class MerchantOnboardingService {

    private final AgentRepository agentRepository;
    private final MerchantRepository merchantRepository;
    private final MerchantApplicationRepository applicationRepository;
    private final MerchantDeviceRepository deviceRepository;
    private final MerchantTransactionRepository transactionRepository;
    private final OtpService otpService;
    private final EmailService emailService;

    public MerchantOnboardingService(AgentRepository agentRepository,
                                      MerchantRepository merchantRepository,
                                      MerchantApplicationRepository applicationRepository,
                                      MerchantDeviceRepository deviceRepository,
                                      MerchantTransactionRepository transactionRepository,
                                      OtpService otpService,
                                      EmailService emailService) {
        this.agentRepository = agentRepository;
        this.merchantRepository = merchantRepository;
        this.applicationRepository = applicationRepository;
        this.deviceRepository = deviceRepository;
        this.transactionRepository = transactionRepository;
        this.otpService = otpService;
        this.emailService = emailService;
    }

    // ==================== Agent Authentication ====================

    public Map<String, Object> agentLoginByEmail(String email, String password) {
        Map<String, Object> result = new HashMap<>();
        Optional<Agent> agent = agentRepository.findByEmailAndPassword(email, password);
        if (agent.isPresent() && "ACTIVE".equals(agent.get().getStatus())) {
            result.put("success", true);
            result.put("agent", agent.get());
            result.put("message", "Login successful");
        } else {
            result.put("success", false);
            result.put("error", "Invalid credentials or account inactive");
        }
        return result;
    }

    public Map<String, Object> agentLoginByMobile(String mobile, String password) {
        Map<String, Object> result = new HashMap<>();
        Optional<Agent> agent = agentRepository.findByMobileAndPassword(mobile, password);
        if (agent.isPresent() && "ACTIVE".equals(agent.get().getStatus())) {
            result.put("success", true);
            result.put("agent", agent.get());
            result.put("message", "Login successful");
        } else {
            result.put("success", false);
            result.put("error", "Invalid credentials or account inactive");
        }
        return result;
    }

    public Optional<Agent> getAgentByAgentId(String agentId) {
        return agentRepository.findByAgentId(agentId);
    }

    public List<Agent> getAllAgents() {
        return agentRepository.findAll();
    }

    // ==================== Merchant CRUD ====================

    @Transactional
    public Map<String, Object> createMerchant(Merchant merchant, List<String> deviceTypes, List<Integer> deviceQuantities) {
        Map<String, Object> result = new HashMap<>();

        // Save merchant
        merchant.setStatus("PENDING");
        Merchant saved = merchantRepository.save(merchant);

        // Create applications for each device type
        List<MerchantApplication> applications = new ArrayList<>();
        if (deviceTypes != null) {
            for (int i = 0; i < deviceTypes.size(); i++) {
                MerchantApplication app = new MerchantApplication();
                app.setMerchantId(saved.getMerchantId());
                app.setDeviceType(deviceTypes.get(i));
                app.setDeviceQuantity(deviceQuantities != null && i < deviceQuantities.size() ? deviceQuantities.get(i) : 1);
                app.setAgentId(saved.getAgentId());
                app.setStatus("PENDING");
                applications.add(applicationRepository.save(app));
            }
        }

        result.put("success", true);
        result.put("merchant", saved);
        result.put("applications", applications);
        result.put("message", "Merchant onboarding application submitted successfully");
        return result;
    }

    @Transactional
    public Map<String, Object> updateMerchant(String merchantId, Merchant updated) {
        Map<String, Object> result = new HashMap<>();
        Optional<Merchant> opt = merchantRepository.findByMerchantId(merchantId);
        if (opt.isEmpty()) {
            result.put("success", false);
            result.put("error", "Merchant not found");
            return result;
        }
        Merchant existing = opt.get();
        if (!"PENDING".equals(existing.getStatus())) {
            result.put("success", false);
            result.put("error", "Can only edit applications in PENDING status");
            return result;
        }

        existing.setBusinessName(updated.getBusinessName());
        existing.setOwnerName(updated.getOwnerName());
        existing.setMobile(updated.getMobile());
        existing.setEmail(updated.getEmail());
        existing.setBusinessType(updated.getBusinessType());
        existing.setGstNumber(updated.getGstNumber());
        existing.setShopAddress(updated.getShopAddress());
        existing.setCity(updated.getCity());
        existing.setState(updated.getState());
        existing.setPincode(updated.getPincode());
        existing.setBankName(updated.getBankName());
        existing.setAccountNumber(updated.getAccountNumber());
        existing.setIfscCode(updated.getIfscCode());
        existing.setAccountHolderName(updated.getAccountHolderName());
        merchantRepository.save(existing);

        result.put("success", true);
        result.put("merchant", existing);
        return result;
    }

    @Transactional
    public Map<String, Object> updateMerchantByAdmin(String merchantId, Merchant updated) {
        Map<String, Object> result = new HashMap<>();
        Optional<Merchant> opt = merchantRepository.findByMerchantId(merchantId);
        if (opt.isEmpty()) {
            result.put("success", false);
            result.put("error", "Merchant not found");
            return result;
        }

        Merchant existing = opt.get();
        existing.setBusinessName(updated.getBusinessName());
        existing.setOwnerName(updated.getOwnerName());
        existing.setMobile(updated.getMobile());
        existing.setEmail(updated.getEmail());
        existing.setBusinessType(updated.getBusinessType());
        existing.setGstNumber(updated.getGstNumber());
        existing.setShopAddress(updated.getShopAddress());
        existing.setCity(updated.getCity());
        existing.setState(updated.getState());
        existing.setPincode(updated.getPincode());
        existing.setBankName(updated.getBankName());
        existing.setAccountNumber(updated.getAccountNumber());
        existing.setIfscCode(updated.getIfscCode());
        existing.setAccountHolderName(updated.getAccountHolderName());
        merchantRepository.save(existing);

        result.put("success", true);
        result.put("merchant", existing);
        return result;
    }

    public Optional<Merchant> getMerchantById(String merchantId) {
        return merchantRepository.findByMerchantId(merchantId);
    }

    public Optional<Merchant> getMerchantByMobile(String mobile) {
        return merchantRepository.findByMobile(mobile);
    }

    public List<Merchant> getMerchantsByAgent(String agentId) {
        return merchantRepository.findByAgentIdOrderByCreatedAtDesc(agentId);
    }

    public List<Merchant> getMerchantsByAgentAndStatus(String agentId, String status) {
        return merchantRepository.findByAgentIdAndStatusOrderByCreatedAtDesc(agentId, status);
    }

    public List<Merchant> getAllMerchants() {
        return merchantRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Merchant> getMerchantsByStatus(String status) {
        return merchantRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    // ==================== Admin Approve / Reject ====================

    @Transactional
    public Map<String, Object> approveMerchant(String merchantId, String adminName) {
        Map<String, Object> result = new HashMap<>();
        Optional<Merchant> opt = merchantRepository.findByMerchantId(merchantId);
        if (opt.isEmpty()) {
            result.put("success", false);
            result.put("error", "Merchant not found");
            return result;
        }
        Merchant merchant = opt.get();
        if (!"PENDING".equals(merchant.getStatus())) {
            result.put("success", false);
            result.put("error", "Merchant is not in PENDING status");
            return result;
        }

        merchant.setStatus("APPROVED");
        merchant.setActivatedAt(LocalDateTime.now());
        merchantRepository.save(merchant);

        // Approve all pending applications and create devices
        List<MerchantApplication> apps = applicationRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId);
        List<MerchantDevice> devices = new ArrayList<>();
        for (MerchantApplication app : apps) {
            if ("PENDING".equals(app.getStatus())) {
                app.setStatus("APPROVED");
                app.setProcessedBy(adminName);
                app.setProcessedAt(LocalDateTime.now());
                applicationRepository.save(app);

                // Create devices for each quantity
                for (int i = 0; i < app.getDeviceQuantity(); i++) {
                    MerchantDevice device = new MerchantDevice();
                    device.setDeviceType(app.getDeviceType());
                    device.setMerchantId(merchantId);
                    device.setApplicationId(app.getApplicationId());
                    device.setStatus("ACTIVE");
                    device.setActivatedAt(LocalDateTime.now());
                    device.setLastActiveAt(LocalDateTime.now());
                    devices.add(deviceRepository.save(device));
                }
            }
        }

        result.put("success", true);
        result.put("merchant", merchant);
        result.put("devices", devices);
        result.put("message", "Merchant approved and devices activated");
        return result;
    }

    @Transactional
    public Map<String, Object> rejectMerchant(String merchantId, String adminName, String reason) {
        Map<String, Object> result = new HashMap<>();
        Optional<Merchant> opt = merchantRepository.findByMerchantId(merchantId);
        if (opt.isEmpty()) {
            result.put("success", false);
            result.put("error", "Merchant not found");
            return result;
        }
        Merchant merchant = opt.get();
        merchant.setStatus("REJECTED");
        merchant.setRejectionReason(reason);
        merchantRepository.save(merchant);

        // Reject all pending applications
        List<MerchantApplication> apps = applicationRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId);
        for (MerchantApplication app : apps) {
            if ("PENDING".equals(app.getStatus())) {
                app.setStatus("REJECTED");
                app.setProcessedBy(adminName);
                app.setProcessedAt(LocalDateTime.now());
                app.setAdminRemarks(reason);
                applicationRepository.save(app);
            }
        }

        result.put("success", true);
        result.put("merchant", merchant);
        result.put("message", "Merchant application rejected");
        return result;
    }

    // ==================== Applications ====================

    public List<MerchantApplication> getApplicationsByMerchant(String merchantId) {
        return applicationRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId);
    }

    public List<MerchantApplication> getApplicationsByAgent(String agentId) {
        return applicationRepository.findByAgentIdOrderByCreatedAtDesc(agentId);
    }

    public List<MerchantApplication> getAllApplications() {
        return applicationRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<MerchantApplication> getApplicationsByStatus(String status) {
        return applicationRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    // ==================== Devices ====================

    public List<MerchantDevice> getDevicesByMerchant(String merchantId) {
        return deviceRepository.findByMerchantId(merchantId);
    }

    public List<MerchantDevice> getAllDevices() {
        return deviceRepository.findAll();
    }

    @Transactional
    public MerchantDevice toggleDeviceStatus(Long deviceId) {
        Optional<MerchantDevice> opt = deviceRepository.findById(deviceId);
        if (opt.isEmpty()) throw new RuntimeException("Device not found");
        MerchantDevice device = opt.get();
        if ("ACTIVE".equals(device.getStatus())) {
            device.setStatus("INACTIVE");
        } else {
            device.setStatus("ACTIVE");
            device.setLastActiveAt(LocalDateTime.now());
        }
        return deviceRepository.save(device);
    }

    // ==================== Transactions ====================

    public List<MerchantTransaction> getTransactionsByMerchant(String merchantId) {
        return transactionRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId);
    }

    // ==================== Merchant Portal Login (Email OTP) ====================

    public Map<String, Object> sendMerchantLoginOtp(String email) {
        Map<String, Object> result = new HashMap<>();
        if (!StringUtils.hasText(email)) {
            result.put("success", false);
            result.put("error", "Email is required");
            return result;
        }

        String normalizedEmail = email.trim().toLowerCase();
        Optional<Merchant> merchantOpt = merchantRepository.findByEmail(normalizedEmail);
        if (merchantOpt.isEmpty()) {
            result.put("success", false);
            result.put("error", "No merchant found with this email");
            return result;
        }

        Merchant merchant = merchantOpt.get();
        String otp = otpService.generateOtp();
        otpService.storeOtp(normalizedEmail, otp);
        emailService.sendOtpEmailWithReason(normalizedEmail, otp, "Merchant Portal Login");

        result.put("success", true);
        result.put("maskedEmail", maskEmail(normalizedEmail));
        result.put("businessName", merchant.getBusinessName());
        result.put("message", "OTP sent successfully");
        return result;
    }

    public Map<String, Object> verifyMerchantLoginOtp(String email, String otp) {
        Map<String, Object> result = new HashMap<>();
        if (!StringUtils.hasText(email) || !StringUtils.hasText(otp)) {
            result.put("success", false);
            result.put("error", "Email and OTP are required");
            return result;
        }

        String normalizedEmail = email.trim().toLowerCase();
        Optional<Merchant> merchantOpt = merchantRepository.findByEmail(normalizedEmail);
        if (merchantOpt.isEmpty()) {
            result.put("success", false);
            result.put("error", "Merchant not found");
            return result;
        }

        if (!otpService.verifyOtp(normalizedEmail, otp.trim())) {
            result.put("success", false);
            result.put("error", "Invalid or expired OTP");
            return result;
        }

        Merchant merchant = merchantOpt.get();
        List<MerchantDevice> devices = deviceRepository.findByMerchantId(merchant.getMerchantId());
        Map<String, Object> dashboard = getMerchantPortalDetails(merchant.getMerchantId());

        result.put("success", true);
        result.put("merchant", merchant);
        result.put("devices", devices);
        result.put("dashboard", dashboard.get("dashboard"));
        result.put("message", "Login successful");
        return result;
    }

    public Map<String, Object> getMerchantPortalDetails(String merchantId) {
        Map<String, Object> result = new HashMap<>();
        Optional<Merchant> merchantOpt = merchantRepository.findByMerchantId(merchantId);
        if (merchantOpt.isEmpty()) {
            result.put("success", false);
            result.put("error", "Merchant not found");
            return result;
        }

        List<MerchantDevice> devices = deviceRepository.findByMerchantId(merchantId);
        List<MerchantTransaction> transactions = transactionRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId);

        BigDecimal totalCredit = BigDecimal.ZERO;
        BigDecimal totalDebitCharges = BigDecimal.ZERO;
        for (MerchantTransaction t : transactions) {
            BigDecimal amount = t.getAmount() == null ? BigDecimal.ZERO : t.getAmount();
            if ("DEBIT_CHARGE".equalsIgnoreCase(t.getPaymentMode())) {
                totalDebitCharges = totalDebitCharges.add(amount);
            } else {
                totalCredit = totalCredit.add(amount);
            }
        }

        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("totalTransactions", transactions.size());
        dashboard.put("totalCredit", totalCredit);
        dashboard.put("totalDebitCharges", totalDebitCharges);
        dashboard.put("speakerActive", devices.stream().anyMatch(d -> "ACTIVE".equalsIgnoreCase(d.getStatus())));
        dashboard.put("speakerPaused", devices.stream().anyMatch(d -> "PAUSED".equalsIgnoreCase(d.getStatus())));
        dashboard.put("speakerClosed", devices.stream().allMatch(d -> "INACTIVE".equalsIgnoreCase(d.getStatus())));

        result.put("success", true);
        result.put("merchant", merchantOpt.get());
        result.put("devices", devices);
        result.put("transactions", transactions);
        result.put("dashboard", dashboard);
        return result;
    }

    @Transactional
    public Map<String, Object> updateMerchantDeviceStatus(String merchantId, String deviceId, String status) {
        Map<String, Object> result = new HashMap<>();
        Optional<Merchant> merchantOpt = merchantRepository.findByMerchantId(merchantId);
        if (merchantOpt.isEmpty()) {
            result.put("success", false);
            result.put("error", "Merchant not found");
            return result;
        }

        Optional<MerchantDevice> deviceOpt = deviceRepository.findByDeviceId(deviceId);
        if (deviceOpt.isEmpty() || !merchantId.equals(deviceOpt.get().getMerchantId())) {
            result.put("success", false);
            result.put("error", "Device not found for merchant");
            return result;
        }

        String normalized = status == null ? "" : status.trim().toUpperCase();
        if (!List.of("ACTIVE", "INACTIVE", "PAUSED").contains(normalized)) {
            result.put("success", false);
            result.put("error", "Invalid status. Use ACTIVE, INACTIVE, or PAUSED");
            return result;
        }

        MerchantDevice device = deviceOpt.get();
        device.setStatus(normalized);
        if ("ACTIVE".equals(normalized)) {
            device.setLastActiveAt(LocalDateTime.now());
            if (device.getActivatedAt() == null) device.setActivatedAt(LocalDateTime.now());
        }
        deviceRepository.save(device);

        result.put("success", true);
        result.put("device", device);
        result.put("message", "Device status updated");
        return result;
    }

    @Transactional
    public Map<String, Object> debitMerchantSpeakerCharges(String merchantId, BigDecimal amount, String reason) {
        Map<String, Object> result = new HashMap<>();
        Optional<Merchant> merchantOpt = merchantRepository.findByMerchantId(merchantId);
        if (merchantOpt.isEmpty()) {
            result.put("success", false);
            result.put("error", "Merchant not found");
            return result;
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            result.put("success", false);
            result.put("error", "Amount must be greater than 0");
            return result;
        }

        List<MerchantDevice> devices = deviceRepository.findByMerchantId(merchantId);
        String deviceId = devices.isEmpty() ? null : devices.get(0).getDeviceId();

        MerchantTransaction txn = new MerchantTransaction();
        txn.setMerchantId(merchantId);
        txn.setDeviceId(deviceId);
        txn.setAmount(amount);
        txn.setPaymentMode("DEBIT_CHARGE");
        txn.setPayerName(reason != null && !reason.isBlank() ? reason : "Speaker Monthly Charges");
        txn.setStatus("SUCCESS");
        transactionRepository.save(txn);

        result.put("success", true);
        result.put("transaction", txn);
        result.put("message", "Speaker charges debited");
        return result;
    }

    @Transactional
    public Map<String, Object> adminLinkMerchantUpi(
            String merchantId,
            String deviceId,
            String upiId,
            String beneficiaryName,
            BigDecimal verifyDepositAmount) {
        Map<String, Object> result = new HashMap<>();
        Optional<Merchant> merchantOpt = merchantRepository.findByMerchantId(merchantId);
        if (merchantOpt.isEmpty()) {
            result.put("success", false);
            result.put("error", "Merchant not found");
            return result;
        }
        if (!StringUtils.hasText(upiId)) {
            result.put("success", false);
            result.put("error", "UPI ID is required");
            return result;
        }

        Optional<MerchantDevice> deviceOpt = deviceRepository.findByDeviceId(deviceId);
        if (deviceOpt.isEmpty() || !merchantId.equals(deviceOpt.get().getMerchantId())) {
            result.put("success", false);
            result.put("error", "Device not found for merchant");
            return result;
        }

        MerchantDevice device = deviceOpt.get();
        device.setLinkedUpi(upiId.trim());
        deviceRepository.save(device);

        if (verifyDepositAmount != null && verifyDepositAmount.compareTo(BigDecimal.ZERO) > 0) {
            MerchantTransaction verifyTxn = new MerchantTransaction();
            verifyTxn.setMerchantId(merchantId);
            verifyTxn.setDeviceId(device.getDeviceId());
            verifyTxn.setAmount(verifyDepositAmount);
            verifyTxn.setPaymentMode("UPI_LINK_VERIFY");
            verifyTxn.setPayerName(StringUtils.hasText(beneficiaryName) ? beneficiaryName.trim() : "UPI Verification");
            verifyTxn.setStatus("SUCCESS");
            transactionRepository.save(verifyTxn);
            result.put("verificationTransaction", verifyTxn);
        }

        result.put("success", true);
        result.put("device", device);
        result.put("message", "UPI linked successfully");
        return result;
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        String[] parts = email.split("@");
        String name = parts[0];
        if (name.length() <= 2) return name + "***@" + parts[1];
        return name.substring(0, 2) + "***@" + parts[1];
    }

    // ==================== Stats ====================

    public Map<String, Object> getAgentStats(String agentId) {
        Map<String, Object> stats = new HashMap<>();
        List<Merchant> merchants = merchantRepository.findByAgentIdOrderByCreatedAtDesc(agentId);
        stats.put("totalMerchants", merchants.size());
        stats.put("pending", merchants.stream().filter(m -> "PENDING".equals(m.getStatus())).count());
        stats.put("approved", merchants.stream().filter(m -> "APPROVED".equals(m.getStatus())).count());
        stats.put("rejected", merchants.stream().filter(m -> "REJECTED".equals(m.getStatus())).count());
        return stats;
    }

    public Map<String, Object> getAdminStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalMerchants", merchantRepository.count());
        stats.put("pending", merchantRepository.countByStatus("PENDING"));
        stats.put("approved", merchantRepository.countByStatus("APPROVED"));
        stats.put("rejected", merchantRepository.countByStatus("REJECTED"));
        stats.put("totalDevices", deviceRepository.count());
        stats.put("activeDevices", deviceRepository.countActiveDevices());
        stats.put("soundboxCount", deviceRepository.countByDeviceType("SOUNDBOX"));
        stats.put("posCount", deviceRepository.countByDeviceType("POS"));
        stats.put("totalAgents", agentRepository.count());
        return stats;
    }

    // ==================== Agent Account Management (Admin) ====================

    @Transactional
    public Map<String, Object> createAgent(Agent agent) {
        Map<String, Object> result = new HashMap<>();

        if (agentRepository.existsByEmail(agent.getEmail())) {
            result.put("success", false);
            result.put("error", "Email already registered");
            return result;
        }
        if (agentRepository.existsByMobile(agent.getMobile())) {
            result.put("success", false);
            result.put("error", "Mobile number already registered");
            return result;
        }

        agent.setStatus("ACTIVE");
        Agent saved = agentRepository.save(agent);

        result.put("success", true);
        result.put("agent", saved);
        result.put("message", "Agent account created successfully");
        return result;
    }

    @Transactional
    public Map<String, Object> updateAgentProfile(String agentId, Map<String, String> updates) {
        Map<String, Object> result = new HashMap<>();
        Optional<Agent> opt = agentRepository.findByAgentId(agentId);
        if (opt.isEmpty()) {
            result.put("success", false);
            result.put("error", "Agent not found");
            return result;
        }

        Agent agent = opt.get();
        if (updates.containsKey("name")) agent.setName(updates.get("name"));
        if (updates.containsKey("role")) agent.setRole(updates.get("role"));
        if (updates.containsKey("bio")) agent.setBio(updates.get("bio"));
        if (updates.containsKey("region")) agent.setRegion(updates.get("region"));
        if (updates.containsKey("mobile")) {
            String newMobile = updates.get("mobile");
            if (!newMobile.equals(agent.getMobile()) && agentRepository.existsByMobile(newMobile)) {
                result.put("success", false);
                result.put("error", "Mobile number already in use");
                return result;
            }
            agent.setMobile(newMobile);
        }
        if (updates.containsKey("email")) {
            String newEmail = updates.get("email");
            if (!newEmail.equals(agent.getEmail()) && agentRepository.existsByEmail(newEmail)) {
                result.put("success", false);
                result.put("error", "Email already in use");
                return result;
            }
            agent.setEmail(newEmail);
        }
        if (updates.containsKey("password") && !updates.get("password").isEmpty()) {
            agent.setPassword(updates.get("password"));
        }

        agentRepository.save(agent);
        result.put("success", true);
        result.put("agent", agent);
        result.put("message", "Agent profile updated");
        return result;
    }

    @Transactional
    public Map<String, Object> freezeAgent(String agentId) {
        Map<String, Object> result = new HashMap<>();
        Optional<Agent> opt = agentRepository.findByAgentId(agentId);
        if (opt.isEmpty()) {
            result.put("success", false);
            result.put("error", "Agent not found");
            return result;
        }
        Agent agent = opt.get();
        agent.setStatus("FROZEN");
        agent.setFrozenAt(LocalDateTime.now());
        agentRepository.save(agent);
        result.put("success", true);
        result.put("message", "Agent account frozen");
        return result;
    }

    @Transactional
    public Map<String, Object> unfreezeAgent(String agentId) {
        Map<String, Object> result = new HashMap<>();
        Optional<Agent> opt = agentRepository.findByAgentId(agentId);
        if (opt.isEmpty()) {
            result.put("success", false);
            result.put("error", "Agent not found");
            return result;
        }
        Agent agent = opt.get();
        agent.setStatus("ACTIVE");
        agent.setFrozenAt(null);
        agentRepository.save(agent);
        result.put("success", true);
        result.put("message", "Agent account unfrozen");
        return result;
    }

    @Transactional
    public Map<String, Object> deactivateAgent(String agentId) {
        Map<String, Object> result = new HashMap<>();
        Optional<Agent> opt = agentRepository.findByAgentId(agentId);
        if (opt.isEmpty()) {
            result.put("success", false);
            result.put("error", "Agent not found");
            return result;
        }
        Agent agent = opt.get();
        agent.setStatus("DEACTIVATED");
        agent.setDeactivatedAt(LocalDateTime.now());
        agentRepository.save(agent);
        result.put("success", true);
        result.put("message", "Agent account deactivated");
        return result;
    }

    @Transactional
    public Map<String, Object> reactivateAgent(String agentId) {
        Map<String, Object> result = new HashMap<>();
        Optional<Agent> opt = agentRepository.findByAgentId(agentId);
        if (opt.isEmpty()) {
            result.put("success", false);
            result.put("error", "Agent not found");
            return result;
        }
        Agent agent = opt.get();
        agent.setStatus("ACTIVE");
        agent.setDeactivatedAt(null);
        agent.setFrozenAt(null);
        agentRepository.save(agent);
        result.put("success", true);
        result.put("message", "Agent account reactivated");
        return result;
    }

    public Map<String, Object> generateAgentOtp(String agentId) {
        Map<String, Object> result = new HashMap<>();
        Optional<Agent> opt = agentRepository.findByAgentId(agentId);
        if (opt.isEmpty()) {
            result.put("success", false);
            result.put("error", "Agent not found");
            return result;
        }
        Agent agent = opt.get();
        String otp = String.valueOf(100000 + new Random().nextInt(900000));
        agent.setOtp(otp);
        agent.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        agentRepository.save(agent);
        result.put("success", true);
        result.put("otp", otp);
        result.put("message", "OTP generated (valid 5 min)");
        return result;
    }

    public Map<String, Object> verifyAgentOtp(String agentId, String otp) {
        Map<String, Object> result = new HashMap<>();
        Optional<Agent> opt = agentRepository.findByAgentId(agentId);
        if (opt.isEmpty()) {
            result.put("success", false);
            result.put("error", "Agent not found");
            return result;
        }
        Agent agent = opt.get();
        if (agent.getOtp() != null && agent.getOtp().equals(otp)
                && agent.getOtpExpiry() != null && agent.getOtpExpiry().isAfter(LocalDateTime.now())) {
            agent.setOtp(null);
            agent.setOtpExpiry(null);
            agentRepository.save(agent);
            result.put("success", true);
            result.put("agent", agent);
            result.put("message", "OTP verified successfully");
        } else {
            result.put("success", false);
            result.put("error", "Invalid or expired OTP");
        }
        return result;
    }

    public Map<String, Object> getAgentManagementStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalAgents", agentRepository.count());
        stats.put("activeAgents", agentRepository.countByStatus("ACTIVE"));
        stats.put("frozenAgents", agentRepository.countByStatus("FROZEN"));
        stats.put("deactivatedAgents", agentRepository.countByStatus("DEACTIVATED"));
        return stats;
    }
}
