package com.neo.springapp.controller;

import com.neo.springapp.model.*;
import com.neo.springapp.service.MerchantOnboardingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@RestController
@RequestMapping("/api/merchant-onboarding")
public class MerchantOnboardingController {

    private final MerchantOnboardingService service;
    private static final String UPLOAD_DIR = "uploads/merchant-docs/";

    public MerchantOnboardingController(MerchantOnboardingService service) {
        this.service = service;
    }

    // ==================== Agent Authentication ====================

    @PostMapping("/agent/login")
    public ResponseEntity<Map<String, Object>> agentLogin(@RequestBody Map<String, String> credentials) {
        String email = credentials.get("email");
        String mobile = credentials.get("mobile");
        String password = credentials.get("password");

        Map<String, Object> result;
        if (email != null && !email.isEmpty()) {
            result = service.agentLoginByEmail(email, password);
        } else if (mobile != null && !mobile.isEmpty()) {
            result = service.agentLoginByMobile(mobile, password);
        } else {
            result = new HashMap<>();
            result.put("success", false);
            result.put("error", "Email or mobile is required");
        }

        if (Boolean.TRUE.equals(result.get("success"))) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.badRequest().body(result);
    }

    @GetMapping("/agent/{agentId}")
    public ResponseEntity<Map<String, Object>> getAgent(@PathVariable String agentId) {
        Map<String, Object> response = new HashMap<>();
        var agent = service.getAgentByAgentId(agentId);
        if (agent.isPresent()) {
            response.put("success", true);
            response.put("agent", agent.get());
            return ResponseEntity.ok(response);
        }
        response.put("success", false);
        response.put("error", "Agent not found");
        return ResponseEntity.badRequest().body(response);
    }

    @GetMapping("/agent/{agentId}/stats")
    public ResponseEntity<Map<String, Object>> getAgentStats(@PathVariable String agentId) {
        return ResponseEntity.ok(service.getAgentStats(agentId));
    }

    // ==================== Merchant Login (Email OTP) ====================

    @PostMapping("/merchant/login/send-otp")
    public ResponseEntity<Map<String, Object>> sendMerchantLoginOtp(@RequestBody Map<String, String> body) {
        Map<String, Object> result = service.sendMerchantLoginOtp(body.get("email"));
        if (Boolean.TRUE.equals(result.get("success"))) return ResponseEntity.ok(result);
        return ResponseEntity.badRequest().body(result);
    }

    @PostMapping("/merchant/login/verify-otp")
    public ResponseEntity<Map<String, Object>> verifyMerchantLoginOtp(@RequestBody Map<String, String> body) {
        Map<String, Object> result = service.verifyMerchantLoginOtp(body.get("email"), body.get("otp"));
        if (Boolean.TRUE.equals(result.get("success"))) return ResponseEntity.ok(result);
        return ResponseEntity.badRequest().body(result);
    }

    @GetMapping("/merchant/{merchantId}/dashboard")
    public ResponseEntity<Map<String, Object>> getMerchantDashboard(@PathVariable String merchantId) {
        Map<String, Object> result = service.getMerchantPortalDetails(merchantId);
        if (Boolean.TRUE.equals(result.get("success"))) return ResponseEntity.ok(result);
        return ResponseEntity.badRequest().body(result);
    }

    @PutMapping("/merchant/{merchantId}/devices/{deviceId}/status")
    public ResponseEntity<Map<String, Object>> updateMerchantDeviceStatus(
            @PathVariable String merchantId,
            @PathVariable String deviceId,
            @RequestParam String status) {
        Map<String, Object> result = service.updateMerchantDeviceStatus(merchantId, deviceId, status);
        if (Boolean.TRUE.equals(result.get("success"))) return ResponseEntity.ok(result);
        return ResponseEntity.badRequest().body(result);
    }

    @PostMapping("/merchant/{merchantId}/charges/debit")
    public ResponseEntity<Map<String, Object>> debitMerchantSpeakerCharges(
            @PathVariable String merchantId, @RequestBody Map<String, Object> body) {
        Object amt = body.get("amount");
        BigDecimal amount = amt == null ? null : new BigDecimal(String.valueOf(amt));
        String reason = (String) body.get("reason");
        Map<String, Object> result = service.debitMerchantSpeakerCharges(merchantId, amount, reason);
        if (Boolean.TRUE.equals(result.get("success"))) return ResponseEntity.ok(result);
        return ResponseEntity.badRequest().body(result);
    }

    @PutMapping("/admin/merchant/{merchantId}/devices/{deviceId}/status")
    public ResponseEntity<Map<String, Object>> adminUpdateMerchantDeviceStatus(
            @PathVariable String merchantId,
            @PathVariable String deviceId,
            @RequestParam String status) {
        Map<String, Object> result = service.updateMerchantDeviceStatus(merchantId, deviceId, status);
        if (Boolean.TRUE.equals(result.get("success"))) return ResponseEntity.ok(result);
        return ResponseEntity.badRequest().body(result);
    }

    @PostMapping("/admin/merchant/{merchantId}/devices/{deviceId}/charges/debit")
    public ResponseEntity<Map<String, Object>> adminDebitMerchantSpeakerCharges(
            @PathVariable String merchantId,
            @PathVariable String deviceId,
            @RequestBody Map<String, Object> body) {
        Object amt = body.get("amount");
        BigDecimal amount = amt == null ? null : new BigDecimal(String.valueOf(amt));
        String reason = (String) body.get("reason");
        Map<String, Object> result = service.debitMerchantSpeakerCharges(merchantId, amount, reason);
        if (Boolean.TRUE.equals(result.get("success"))) return ResponseEntity.ok(result);
        return ResponseEntity.badRequest().body(result);
    }

    @PostMapping("/admin/merchant/{merchantId}/devices/{deviceId}/link-upi")
    public ResponseEntity<Map<String, Object>> adminLinkMerchantUpi(
            @PathVariable String merchantId,
            @PathVariable String deviceId,
            @RequestBody Map<String, Object> body) {
        String upiId = (String) body.get("upiId");
        String beneficiaryName = (String) body.get("beneficiaryName");
        Object amt = body.get("verifyDepositAmount");
        BigDecimal verifyDepositAmount = amt == null ? null : new BigDecimal(String.valueOf(amt));
        Map<String, Object> result = service.adminLinkMerchantUpi(merchantId, deviceId, upiId, beneficiaryName, verifyDepositAmount);
        if (Boolean.TRUE.equals(result.get("success"))) return ResponseEntity.ok(result);
        return ResponseEntity.badRequest().body(result);
    }

    // ==================== Merchant Operations ====================

    @PostMapping("/merchants")
    public ResponseEntity<Map<String, Object>> createMerchant(@RequestBody Map<String, Object> payload) {
        Map<String, Object> response = new HashMap<>();
        try {
            Merchant merchant = new Merchant();
            merchant.setBusinessName((String) payload.get("businessName"));
            merchant.setOwnerName((String) payload.get("ownerName"));
            merchant.setMobile((String) payload.get("mobile"));
            merchant.setEmail((String) payload.get("email"));
            merchant.setBusinessType((String) payload.get("businessType"));
            merchant.setGstNumber((String) payload.get("gstNumber"));
            merchant.setShopAddress((String) payload.get("shopAddress"));
            merchant.setCity((String) payload.get("city"));
            merchant.setState((String) payload.get("state"));
            merchant.setPincode((String) payload.get("pincode"));
            merchant.setBankName((String) payload.get("bankName"));
            merchant.setAccountNumber((String) payload.get("accountNumber"));
            merchant.setIfscCode((String) payload.get("ifscCode"));
            merchant.setAccountHolderName((String) payload.get("accountHolderName"));
            merchant.setAgentId((String) payload.get("agentId"));

            @SuppressWarnings("unchecked")
            List<String> deviceTypes = (List<String>) payload.get("deviceTypes");
            @SuppressWarnings("unchecked")
            List<Integer> deviceQuantities = (List<Integer>) payload.get("deviceQuantities");

            response = service.createMerchant(merchant, deviceTypes, deviceQuantities);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/merchants/{merchantId}")
    public ResponseEntity<Map<String, Object>> updateMerchant(
            @PathVariable String merchantId, @RequestBody Merchant updated) {
        Map<String, Object> result = service.updateMerchant(merchantId, updated);
        if (Boolean.TRUE.equals(result.get("success"))) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.badRequest().body(result);
    }

    @PutMapping("/admin/merchants/{merchantId}")
    public ResponseEntity<Map<String, Object>> updateMerchantByAdmin(
            @PathVariable String merchantId, @RequestBody Merchant updated) {
        Map<String, Object> result = service.updateMerchantByAdmin(merchantId, updated);
        if (Boolean.TRUE.equals(result.get("success"))) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.badRequest().body(result);
    }

    @GetMapping("/merchants/{merchantId}")
    public ResponseEntity<Map<String, Object>> getMerchant(@PathVariable String merchantId) {
        Map<String, Object> response = new HashMap<>();
        var merchant = service.getMerchantById(merchantId);
        if (merchant.isPresent()) {
            response.put("success", true);
            response.put("merchant", merchant.get());
            response.put("applications", service.getApplicationsByMerchant(merchantId));
            response.put("devices", service.getDevicesByMerchant(merchantId));
            return ResponseEntity.ok(response);
        }
        response.put("success", false);
        response.put("error", "Merchant not found");
        return ResponseEntity.badRequest().body(response);
    }

    @GetMapping("/merchants/mobile/{mobile}")
    public ResponseEntity<Map<String, Object>> getMerchantByMobile(@PathVariable String mobile) {
        Map<String, Object> response = new HashMap<>();
        var merchant = service.getMerchantByMobile(mobile);
        if (merchant.isPresent()) {
            response.put("success", true);
            response.put("merchant", merchant.get());
            return ResponseEntity.ok(response);
        }
        response.put("success", false);
        response.put("error", "Merchant not found");
        return ResponseEntity.badRequest().body(response);
    }

    @GetMapping("/merchants/agent/{agentId}")
    public ResponseEntity<List<Merchant>> getMerchantsByAgent(@PathVariable String agentId) {
        return ResponseEntity.ok(service.getMerchantsByAgent(agentId));
    }

    @GetMapping("/merchants/agent/{agentId}/status/{status}")
    public ResponseEntity<List<Merchant>> getMerchantsByAgentAndStatus(
            @PathVariable String agentId, @PathVariable String status) {
        return ResponseEntity.ok(service.getMerchantsByAgentAndStatus(agentId, status));
    }

    @GetMapping("/merchants/all")
    public ResponseEntity<List<Merchant>> getAllMerchants() {
        return ResponseEntity.ok(service.getAllMerchants());
    }

    @GetMapping("/merchants/status/{status}")
    public ResponseEntity<List<Merchant>> getMerchantsByStatus(@PathVariable String status) {
        return ResponseEntity.ok(service.getMerchantsByStatus(status));
    }

    // ==================== Admin Approve / Reject ====================

    @PutMapping("/merchants/{merchantId}/approve")
    public ResponseEntity<Map<String, Object>> approveMerchant(
            @PathVariable String merchantId, @RequestParam String adminName) {
        Map<String, Object> result = service.approveMerchant(merchantId, adminName);
        if (Boolean.TRUE.equals(result.get("success"))) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.badRequest().body(result);
    }

    @PutMapping("/merchants/{merchantId}/reject")
    public ResponseEntity<Map<String, Object>> rejectMerchant(
            @PathVariable String merchantId,
            @RequestParam String adminName,
            @RequestParam(required = false, defaultValue = "") String reason) {
        Map<String, Object> result = service.rejectMerchant(merchantId, adminName, reason);
        if (Boolean.TRUE.equals(result.get("success"))) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.badRequest().body(result);
    }

    // ==================== Applications ====================

    @GetMapping("/applications/all")
    public ResponseEntity<List<MerchantApplication>> getAllApplications() {
        return ResponseEntity.ok(service.getAllApplications());
    }

    @GetMapping("/applications/status/{status}")
    public ResponseEntity<List<MerchantApplication>> getApplicationsByStatus(@PathVariable String status) {
        return ResponseEntity.ok(service.getApplicationsByStatus(status));
    }

    @GetMapping("/applications/merchant/{merchantId}")
    public ResponseEntity<List<MerchantApplication>> getApplicationsByMerchant(@PathVariable String merchantId) {
        return ResponseEntity.ok(service.getApplicationsByMerchant(merchantId));
    }

    // ==================== Devices ====================

    @GetMapping("/devices/merchant/{merchantId}")
    public ResponseEntity<List<MerchantDevice>> getDevicesByMerchant(@PathVariable String merchantId) {
        return ResponseEntity.ok(service.getDevicesByMerchant(merchantId));
    }

    @GetMapping("/devices/all")
    public ResponseEntity<List<MerchantDevice>> getAllDevices() {
        return ResponseEntity.ok(service.getAllDevices());
    }

    @PutMapping("/devices/toggle/{id}")
    public ResponseEntity<Map<String, Object>> toggleDevice(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            MerchantDevice device = service.toggleDeviceStatus(id);
            response.put("success", true);
            response.put("device", device);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ==================== Transactions ====================

    @GetMapping("/transactions/merchant/{merchantId}")
    public ResponseEntity<List<MerchantTransaction>> getTransactionsByMerchant(@PathVariable String merchantId) {
        return ResponseEntity.ok(service.getTransactionsByMerchant(merchantId));
    }

    // ==================== File Upload ====================

    @PostMapping("/merchants/{merchantId}/upload")
    public ResponseEntity<Map<String, Object>> uploadDocuments(
            @PathVariable String merchantId,
            @RequestParam(required = false) MultipartFile shopPhoto,
            @RequestParam(required = false) MultipartFile ownerIdProof,
            @RequestParam(required = false) MultipartFile bankProof) {
        Map<String, Object> response = new HashMap<>();
        try {
            var opt = service.getMerchantById(merchantId);
            if (opt.isEmpty()) {
                response.put("success", false);
                response.put("error", "Merchant not found");
                return ResponseEntity.badRequest().body(response);
            }
            Merchant merchant = opt.get();

            Path uploadPath = Paths.get(UPLOAD_DIR + merchantId);
            Files.createDirectories(uploadPath);

            if (shopPhoto != null && !shopPhoto.isEmpty()) {
                String filename = "shop_" + System.currentTimeMillis() + "_" + sanitizeFilename(shopPhoto.getOriginalFilename());
                Files.copy(shopPhoto.getInputStream(), uploadPath.resolve(filename));
                merchant.setShopPhotoPath(UPLOAD_DIR + merchantId + "/" + filename);
            }
            if (ownerIdProof != null && !ownerIdProof.isEmpty()) {
                String filename = "id_" + System.currentTimeMillis() + "_" + sanitizeFilename(ownerIdProof.getOriginalFilename());
                Files.copy(ownerIdProof.getInputStream(), uploadPath.resolve(filename));
                merchant.setOwnerIdProofPath(UPLOAD_DIR + merchantId + "/" + filename);
            }
            if (bankProof != null && !bankProof.isEmpty()) {
                String filename = "bank_" + System.currentTimeMillis() + "_" + sanitizeFilename(bankProof.getOriginalFilename());
                Files.copy(bankProof.getInputStream(), uploadPath.resolve(filename));
                merchant.setBankProofPath(UPLOAD_DIR + merchantId + "/" + filename);
            }

            // Save paths - use updateMerchant through service won't work since it checks PENDING
            // Direct save via service method needed
            response.put("success", true);
            response.put("message", "Documents uploaded successfully");
            response.put("merchant", merchant);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            response.put("success", false);
            response.put("error", "File upload failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ==================== Admin Stats ====================

    @GetMapping("/stats/admin")
    public ResponseEntity<Map<String, Object>> getAdminStats() {
        return ResponseEntity.ok(service.getAdminStats());
    }

    // ==================== Document Access ====================

    @GetMapping("/documents/{merchantId}/{filename}")
    public ResponseEntity<byte[]> getDocument(@PathVariable String merchantId, @PathVariable String filename) {
        try {
            String sanitizedMerchantId = merchantId.replaceAll("[^a-zA-Z0-9]", "");
            String sanitizedFilename = sanitizeFilename(filename);
            Path filePath = Paths.get(UPLOAD_DIR + sanitizedMerchantId + "/" + sanitizedFilename).normalize();

            // Verify the path is within the upload directory
            if (!filePath.startsWith(Paths.get(UPLOAD_DIR).normalize())) {
                return ResponseEntity.badRequest().build();
            }

            byte[] data = Files.readAllBytes(filePath);
            return ResponseEntity.ok()
                    .header("Content-Type", "application/octet-stream")
                    .body(data);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ==================== Agent Management (Admin) ====================

    @PostMapping("/agents")
    public ResponseEntity<Map<String, Object>> createAgent(@RequestBody Agent agent) {
        Map<String, Object> result = service.createAgent(agent);
        if (Boolean.TRUE.equals(result.get("success"))) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.badRequest().body(result);
    }

    @GetMapping("/agents/all")
    public ResponseEntity<Map<String, Object>> getAllAgents() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("agents", service.getAllAgents());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/agents/stats")
    public ResponseEntity<Map<String, Object>> getAgentManagementStats() {
        return ResponseEntity.ok(service.getAgentManagementStats());
    }

    @PutMapping("/agents/{agentId}/profile")
    public ResponseEntity<Map<String, Object>> updateAgentProfile(
            @PathVariable String agentId, @RequestBody Map<String, String> updates) {
        Map<String, Object> result = service.updateAgentProfile(agentId, updates);
        if (Boolean.TRUE.equals(result.get("success"))) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.badRequest().body(result);
    }

    @PutMapping("/agents/{agentId}/freeze")
    public ResponseEntity<Map<String, Object>> freezeAgent(@PathVariable String agentId) {
        Map<String, Object> result = service.freezeAgent(agentId);
        if (Boolean.TRUE.equals(result.get("success"))) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.badRequest().body(result);
    }

    @PutMapping("/agents/{agentId}/unfreeze")
    public ResponseEntity<Map<String, Object>> unfreezeAgent(@PathVariable String agentId) {
        Map<String, Object> result = service.unfreezeAgent(agentId);
        if (Boolean.TRUE.equals(result.get("success"))) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.badRequest().body(result);
    }

    @PutMapping("/agents/{agentId}/deactivate")
    public ResponseEntity<Map<String, Object>> deactivateAgent(@PathVariable String agentId) {
        Map<String, Object> result = service.deactivateAgent(agentId);
        if (Boolean.TRUE.equals(result.get("success"))) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.badRequest().body(result);
    }

    @PutMapping("/agents/{agentId}/reactivate")
    public ResponseEntity<Map<String, Object>> reactivateAgent(@PathVariable String agentId) {
        Map<String, Object> result = service.reactivateAgent(agentId);
        if (Boolean.TRUE.equals(result.get("success"))) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.badRequest().body(result);
    }

    @PostMapping("/agents/{agentId}/generate-otp")
    public ResponseEntity<Map<String, Object>> generateAgentOtp(@PathVariable String agentId) {
        Map<String, Object> result = service.generateAgentOtp(agentId);
        if (Boolean.TRUE.equals(result.get("success"))) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.badRequest().body(result);
    }

    @PostMapping("/agents/{agentId}/verify-otp")
    public ResponseEntity<Map<String, Object>> verifyAgentOtp(
            @PathVariable String agentId, @RequestBody Map<String, String> body) {
        String otp = body.get("otp");
        Map<String, Object> result = service.verifyAgentOtp(agentId, otp);
        if (Boolean.TRUE.equals(result.get("success"))) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.badRequest().body(result);
    }

    @PostMapping("/agents/{agentId}/upload-idcard")
    public ResponseEntity<Map<String, Object>> uploadAgentIdCard(
            @PathVariable String agentId, @RequestParam("idCard") MultipartFile idCard) {
        Map<String, Object> response = new HashMap<>();
        try {
            var agentOpt = service.getAgentByAgentId(agentId);
            if (agentOpt.isEmpty()) {
                response.put("success", false);
                response.put("error", "Agent not found");
                return ResponseEntity.badRequest().body(response);
            }

            Path uploadPath = Paths.get("uploads/agent-idcards/");
            Files.createDirectories(uploadPath);

            String filename = "idcard_" + agentId + "_" + System.currentTimeMillis() + "_" + sanitizeFilename(idCard.getOriginalFilename());
            Files.copy(idCard.getInputStream(), uploadPath.resolve(filename));

            String idCardPath = "uploads/agent-idcards/" + filename;
            Map<String, String> updates = new HashMap<>();
            updates.put("idCardPath", idCardPath);
            service.updateAgentProfile(agentId, updates);

            response.put("success", true);
            response.put("message", "ID card uploaded successfully");
            response.put("idCardPath", idCardPath);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            response.put("success", false);
            response.put("error", "Upload failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/agents/{agentId}/upload-photo")
    public ResponseEntity<Map<String, Object>> uploadAgentPhoto(
            @PathVariable String agentId, @RequestParam("photo") MultipartFile photo) {
        Map<String, Object> response = new HashMap<>();
        try {
            var agentOpt = service.getAgentByAgentId(agentId);
            if (agentOpt.isEmpty()) {
                response.put("success", false);
                response.put("error", "Agent not found");
                return ResponseEntity.badRequest().body(response);
            }

            Path uploadPath = Paths.get("uploads/agent-photos/");
            Files.createDirectories(uploadPath);

            String filename = "photo_" + agentId + "_" + System.currentTimeMillis() + "_" + sanitizeFilename(photo.getOriginalFilename());
            Files.copy(photo.getInputStream(), uploadPath.resolve(filename));

            String photoPath = "uploads/agent-photos/" + filename;
            Map<String, String> updates = new HashMap<>();
            updates.put("profilePhotoPath", photoPath);
            service.updateAgentProfile(agentId, updates);

            response.put("success", true);
            response.put("message", "Profile photo uploaded successfully");
            response.put("profilePhotoPath", photoPath);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            response.put("success", false);
            response.put("error", "Upload failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    private String sanitizeFilename(String filename) {
        if (filename == null) return "file";
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
