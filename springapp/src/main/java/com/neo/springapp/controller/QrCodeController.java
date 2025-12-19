package com.neo.springapp.controller;

import com.neo.springapp.service.QrCodeService;
import com.neo.springapp.service.AccountService;
import com.neo.springapp.model.Account;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/qrcode")
public class QrCodeController {

    @Autowired
    private QrCodeService qrCodeService;
    
    @Autowired
    private AccountService accountService;

    /**
     * Generate UPI QR code for receiving money
     * @param accountNumber Account number
     * @return QR code image as Base64 string
     */
    @GetMapping("/upi/receive/{accountNumber}")
    public ResponseEntity<Map<String, Object>> generateReceiveQrCode(@PathVariable String accountNumber) {
        try {
            // Decode account number in case it's URL encoded
            if (accountNumber == null || accountNumber.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Account number is required");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            System.out.println("Generating QR code for account: " + accountNumber);
            
            Account account = accountService.getAccountByNumber(accountNumber);
            if (account == null) {
                System.err.println("Account not found: " + accountNumber);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Account not found: " + accountNumber);
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            System.out.println("Account found: " + account.getName());
            
            // Generate UPI ID from phone number or account number
            String upiId = generateUpiId(account);
            String name = account.getName() != null ? account.getName() : "NeoBank User";
            
            System.out.println("Generated UPI ID: " + upiId + " for name: " + name);
            
            // Generate QR code without amount (dynamic amount)
            String qrCodeImage = qrCodeService.generateUpiQrCode(upiId, name, null, "Payment to " + name);
            
            if (qrCodeImage == null || qrCodeImage.isEmpty()) {
                System.err.println("QR code generation returned null or empty");
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Failed to generate QR code image. Please check server logs.");
                return ResponseEntity.internalServerError().body(errorResponse);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("qrCode", qrCodeImage);
            response.put("upiId", upiId);
            response.put("name", name);
            response.put("accountNumber", accountNumber);
            
            System.out.println("QR code generated successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Exception generating QR code: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error generating QR code: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Generate UPI QR code with specific amount
     * @param accountNumber Account number
     * @param amount Amount to receive
     * @return QR code image as Base64 string
     */
    @GetMapping("/upi/receive/{accountNumber}/{amount}")
    public ResponseEntity<Map<String, Object>> generateReceiveQrCodeWithAmount(
            @PathVariable String accountNumber,
            @PathVariable Double amount) {
        try {
            if (accountNumber == null || accountNumber.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Account number is required");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            System.out.println("Generating QR code for account: " + accountNumber + " with amount: " + amount);
            
            Account account = accountService.getAccountByNumber(accountNumber);
            if (account == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Account not found");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            if (amount == null || amount <= 0) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Invalid amount");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            String upiId = generateUpiId(account);
            String name = account.getName() != null ? account.getName() : "NeoBank User";
            
            String qrCodeImage = qrCodeService.generateUpiQrCode(upiId, name, amount, "Payment of ₹" + amount + " to " + name);
            
            if (qrCodeImage == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Failed to generate QR code");
                return ResponseEntity.internalServerError().body(errorResponse);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("qrCode", qrCodeImage);
            response.put("upiId", upiId);
            response.put("name", name);
            response.put("amount", amount);
            response.put("accountNumber", accountNumber);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error generating QR code: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Generate UPI ID from account (using phone number or account number)
     */
    private String generateUpiId(Account account) {
        // If phone number exists, use it for UPI ID
        if (account.getPhone() != null && !account.getPhone().isEmpty()) {
            // Remove any non-digit characters and use phone@neobank
            String phone = account.getPhone().replaceAll("[^0-9]", "");
            if (phone.length() >= 10) {
                return phone + "@neobank";
            }
        }
        // Fallback to account number based UPI ID
        return account.getAccountNumber() + "@neobank";
    }
}

