package com.neo.springapp.controller;

import com.neo.springapp.model.CurrentAccount;
import com.neo.springapp.model.SoundboxDevice;
import com.neo.springapp.repository.CurrentAccountRepository;
import com.neo.springapp.repository.SoundboxDeviceRepository;
import com.neo.springapp.service.EmailService;
import com.neo.springapp.service.OtpService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/soundbox/merchant")
public class MerchantSoundboxController {

    private final CurrentAccountRepository currentAccountRepository;
    private final SoundboxDeviceRepository soundboxDeviceRepository;
    private final OtpService otpService;
    private final EmailService emailService;

    public MerchantSoundboxController(
            CurrentAccountRepository currentAccountRepository,
            SoundboxDeviceRepository soundboxDeviceRepository,
            OtpService otpService,
            EmailService emailService) {
        this.currentAccountRepository = currentAccountRepository;
        this.soundboxDeviceRepository = soundboxDeviceRepository;
        this.otpService = otpService;
        this.emailService = emailService;
    }

    /**
     * POST /api/soundbox/merchant/send-otp
     * Validates current account number, checks soundbox device exists, sends OTP to registered email
     */
    @PostMapping("/send-otp")
    public ResponseEntity<Map<String, Object>> sendOtp(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();

        String accountNumber = request.get("accountNumber");
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "Account number is required.");
            return ResponseEntity.badRequest().body(response);
        }

        accountNumber = accountNumber.trim();

        // Find current account
        Optional<CurrentAccount> accountOpt = currentAccountRepository.findByAccountNumber(accountNumber);
        if (accountOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "No current account found with this account number.");
            return ResponseEntity.badRequest().body(response);
        }

        CurrentAccount account = accountOpt.get();

        // Check account is active
        if (!"ACTIVE".equalsIgnoreCase(account.getStatus()) && !"APPROVED".equalsIgnoreCase(account.getStatus())) {
            response.put("success", false);
            response.put("message", "Account is not active. Current status: " + account.getStatus());
            return ResponseEntity.badRequest().body(response);
        }

        // Check if soundbox device exists for this account
        Optional<SoundboxDevice> deviceOpt = soundboxDeviceRepository.findByAccountNumber(accountNumber);
        if (deviceOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "No soundbox device found for this account. Please apply for a soundbox first.");
            return ResponseEntity.badRequest().body(response);
        }

        SoundboxDevice device = deviceOpt.get();
        if (!"ACTIVE".equalsIgnoreCase(device.getStatus())) {
            response.put("success", false);
            response.put("message", "Your soundbox device is not active. Current status: " + device.getStatus());
            return ResponseEntity.badRequest().body(response);
        }

        // Generate and send OTP to registered email
        String email = account.getEmail();
        if (email == null || email.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "No email registered with this account. Please contact support.");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            String otp = otpService.generateOtp();
            otpService.storeOtp(email, otp);
            boolean sent = emailService.sendOtpEmail(email, otp);

            if (sent) {
                // Mask email for display
                String maskedEmail = maskEmail(email);
                response.put("success", true);
                response.put("maskedEmail", maskedEmail);
                response.put("businessName", account.getBusinessName());
                response.put("message", "OTP sent successfully to " + maskedEmail);
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Failed to send OTP. Please try again.");
                return ResponseEntity.internalServerError().body(response);
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to send OTP. Please try again.");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * POST /api/soundbox/merchant/verify-otp
     * Verifies OTP and returns merchant session data with device info
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, Object>> verifyOtp(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();

        String accountNumber = request.get("accountNumber");
        String otp = request.get("otp");

        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "Account number is required.");
            return ResponseEntity.badRequest().body(response);
        }

        if (otp == null || otp.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "OTP is required.");
            return ResponseEntity.badRequest().body(response);
        }

        if (otp.trim().length() != 6) {
            response.put("success", false);
            response.put("message", "OTP must be 6 digits.");
            return ResponseEntity.badRequest().body(response);
        }

        accountNumber = accountNumber.trim();
        otp = otp.trim();

        // Find current account
        Optional<CurrentAccount> accountOpt = currentAccountRepository.findByAccountNumber(accountNumber);
        if (accountOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "Account not found.");
            return ResponseEntity.badRequest().body(response);
        }

        CurrentAccount account = accountOpt.get();
        String email = account.getEmail();

        // Verify OTP
        boolean valid = otpService.verifyOtp(email, otp);
        if (!valid) {
            response.put("success", false);
            response.put("message", "Invalid or expired OTP. Please try again.");
            return ResponseEntity.badRequest().body(response);
        }

        // OTP valid - get device info
        Optional<SoundboxDevice> deviceOpt = soundboxDeviceRepository.findByAccountNumber(accountNumber);

        Map<String, Object> merchantData = new HashMap<>();
        merchantData.put("accountNumber", account.getAccountNumber());
        merchantData.put("businessName", account.getBusinessName());
        merchantData.put("ownerName", account.getOwnerName());
        merchantData.put("email", email);
        merchantData.put("mobile", account.getMobile());
        merchantData.put("upiId", account.getUpiId());

        response.put("success", true);
        response.put("message", "Login successful!");
        response.put("merchant", merchantData);

        if (deviceOpt.isPresent()) {
            response.put("device", deviceOpt.get());
        }

        return ResponseEntity.ok(response);
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        String[] parts = email.split("@");
        String name = parts[0];
        if (name.length() <= 2) return name + "***@" + parts[1];
        return name.substring(0, 2) + "***@" + parts[1];
    }
}
