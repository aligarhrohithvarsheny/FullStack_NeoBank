package com.neo.springapp.controller;

import com.neo.springapp.model.SoundboxDevice;
import com.neo.springapp.model.SoundboxRequest;
import com.neo.springapp.model.SoundboxTransaction;
import com.neo.springapp.service.SoundboxService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/soundbox")
public class SoundboxController {

    private final SoundboxService soundboxService;

    public SoundboxController(SoundboxService soundboxService) {
        this.soundboxService = soundboxService;
    }

    // ==================== Request Operations ====================

    @PostMapping("/requests/apply")
    public ResponseEntity<Map<String, Object>> applyForSoundbox(@RequestBody SoundboxRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            SoundboxRequest saved = soundboxService.applyForSoundbox(request);
            response.put("success", true);
            response.put("request", saved);
            response.put("message", "Soundbox application submitted successfully");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/requests/account/{accountNumber}")
    public ResponseEntity<List<SoundboxRequest>> getRequestsByAccount(@PathVariable String accountNumber) {
        return ResponseEntity.ok(soundboxService.getRequestsByAccount(accountNumber));
    }

    @GetMapping("/requests/pending")
    public ResponseEntity<List<SoundboxRequest>> getPendingRequests() {
        return ResponseEntity.ok(soundboxService.getPendingRequests());
    }

    @GetMapping("/requests/all")
    public ResponseEntity<List<SoundboxRequest>> getAllRequests() {
        return ResponseEntity.ok(soundboxService.getAllRequests());
    }

    @PutMapping("/requests/approve/{id}")
    public ResponseEntity<Map<String, Object>> approveRequest(
            @PathVariable Long id,
            @RequestParam String adminName,
            @RequestParam String deviceId,
            @RequestParam(required = false) Double monthlyCharge,
            @RequestParam(required = false) Double deviceCharge) {
        Map<String, Object> result = soundboxService.approveRequest(id, adminName, deviceId, monthlyCharge, deviceCharge);
        if ((boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.badRequest().body(result);
    }

    @PutMapping("/requests/reject/{id}")
    public ResponseEntity<Map<String, Object>> rejectRequest(
            @PathVariable Long id,
            @RequestParam String adminName,
            @RequestParam(required = false, defaultValue = "") String remarks) {
        Map<String, Object> result = soundboxService.rejectRequest(id, adminName, remarks);
        if ((boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.badRequest().body(result);
    }

    // ==================== Device Operations ====================

    @GetMapping("/devices/account/{accountNumber}")
    public ResponseEntity<Map<String, Object>> getDeviceByAccount(@PathVariable String accountNumber) {
        Map<String, Object> response = new HashMap<>();
        var device = soundboxService.getDeviceByAccount(accountNumber);
        if (device.isPresent()) {
            response.put("success", true);
            response.put("device", device.get());
            return ResponseEntity.ok(response);
        }
        response.put("success", false);
        response.put("error", "No device found for this account");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/devices/all")
    public ResponseEntity<List<SoundboxDevice>> getAllDevices() {
        return ResponseEntity.ok(soundboxService.getAllDevices());
    }

    @GetMapping("/devices/status/{status}")
    public ResponseEntity<List<SoundboxDevice>> getDevicesByStatus(@PathVariable String status) {
        return ResponseEntity.ok(soundboxService.getDevicesByStatus(status));
    }

    @PutMapping("/devices/settings/{accountNumber}")
    public ResponseEntity<Map<String, Object>> updateDeviceSettings(
            @PathVariable String accountNumber,
            @RequestBody Map<String, Object> settings) {
        Map<String, Object> response = new HashMap<>();
        try {
            SoundboxDevice updated = soundboxService.updateDeviceSettings(accountNumber, settings);
            response.put("success", true);
            response.put("device", updated);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/devices/toggle/{id}")
    public ResponseEntity<Map<String, Object>> toggleDeviceStatus(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            SoundboxDevice toggled = soundboxService.toggleDeviceStatus(id);
            response.put("success", true);
            response.put("device", toggled);
            response.put("message", "Device status changed to " + toggled.getStatus());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/devices/link-upi/{accountNumber}")
    public ResponseEntity<Map<String, Object>> linkUpi(
            @PathVariable String accountNumber,
            @RequestParam String upiId) {
        Map<String, Object> response = new HashMap<>();
        try {
            SoundboxDevice device = soundboxService.linkUpi(accountNumber, upiId);
            response.put("success", true);
            response.put("device", device);
            response.put("message", "UPI ID linked successfully");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/devices/remove-upi/{accountNumber}")
    public ResponseEntity<Map<String, Object>> removeUpi(
            @PathVariable String accountNumber,
            @RequestParam String upiId) {
        Map<String, Object> response = new HashMap<>();
        try {
            SoundboxDevice device = soundboxService.removeUpi(accountNumber, upiId);
            response.put("success", true);
            response.put("device", device);
            response.put("message", "UPI ID removed successfully");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/devices/charges/{id}")
    public ResponseEntity<Map<String, Object>> updateCharges(
            @PathVariable Long id,
            @RequestParam(required = false) Double monthlyCharge,
            @RequestParam(required = false) Double deviceCharge) {
        Map<String, Object> response = new HashMap<>();
        try {
            SoundboxDevice device = soundboxService.updateCharges(id, monthlyCharge, deviceCharge);
            response.put("success", true);
            response.put("device", device);
            response.put("message", "Charges updated successfully");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ==================== Payment / Transaction Operations ====================

    @PostMapping("/payment/process")
    public ResponseEntity<Map<String, Object>> processPayment(@RequestBody SoundboxTransaction transaction) {
        Map<String, Object> result = soundboxService.processPayment(transaction);
        if ((boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.badRequest().body(result);
    }

    @GetMapping("/transactions/{accountNumber}")
    public ResponseEntity<List<SoundboxTransaction>> getTransactionsByAccount(@PathVariable String accountNumber) {
        return ResponseEntity.ok(soundboxService.getTransactionsByAccount(accountNumber));
    }

    @GetMapping("/transactions/{accountNumber}/paginated")
    public ResponseEntity<Page<SoundboxTransaction>> getTransactionsPaginated(
            @PathVariable String accountNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(soundboxService.getTransactionsByAccountPaginated(accountNumber, page, size));
    }

    // ==================== Statistics ====================

    @GetMapping("/stats/user/{accountNumber}")
    public ResponseEntity<Map<String, Object>> getUserStats(@PathVariable String accountNumber) {
        return ResponseEntity.ok(soundboxService.getUserSoundboxStats(accountNumber));
    }

    @GetMapping("/stats/admin")
    public ResponseEntity<Map<String, Object>> getAdminStats() {
        return ResponseEntity.ok(soundboxService.getAdminSoundboxStats());
    }
}
