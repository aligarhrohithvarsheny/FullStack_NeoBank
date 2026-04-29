package com.neo.springapp.controller;

import com.neo.springapp.model.BillPayment;
import com.neo.springapp.model.BillPaymentRequest;
import com.neo.springapp.service.BillPaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bill-payments")
@CrossOrigin(origins = "*")
public class BillPaymentController {

    @Autowired
    private BillPaymentService billPaymentService;

    /**
     * Verify card details and send OTP for bill payment
     * Supports both debit and credit cards
     */
    @PostMapping("/verify-card")
    public ResponseEntity<Map<String, Object>> verifyCardAndSendOtp(@RequestBody Map<String, String> request) {
        try {
            String cardNumber = request.get("cardNumber");
            String cvv = request.get("cvv");
            String expiryDate = request.get("expiryDate");
            String cardholderName = request.get("cardholderName");
            String accountNumber = request.get("accountNumber");
            
            if (cardNumber == null || cvv == null || expiryDate == null || cardholderName == null || accountNumber == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "All card details are required");
                return ResponseEntity.badRequest().body(response);
            }
            
            Map<String, Object> result = billPaymentService.verifyCardAndSendOtp(
                cardNumber, cvv, expiryDate, cardholderName, accountNumber
            );
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error verifying card: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Verify OTP and process bill payment
     */
    @PostMapping("/verify-otp-and-pay")
    public ResponseEntity<?> verifyOtpAndProcessPayment(@RequestBody Map<String, Object> request) {
        try {
            BillPaymentRequest paymentRequest = new BillPaymentRequest();
            paymentRequest.setAccountNumber((String) request.get("accountNumber"));
            paymentRequest.setUserName((String) request.get("userName"));
            paymentRequest.setUserEmail((String) request.get("userEmail"));
            paymentRequest.setBillType((String) request.get("billType"));
            paymentRequest.setNetworkProvider((String) request.get("networkProvider"));
            paymentRequest.setCustomerNumber((String) request.get("customerNumber"));
            paymentRequest.setAmount(((Number) request.get("amount")).doubleValue());
            
            // Handle credit card ID (may be null for debit cards)
            if (request.get("creditCardId") != null) {
                paymentRequest.setCreditCardId(((Number) request.get("creditCardId")).longValue());
            }
            paymentRequest.setCardNumber((String) request.get("cardNumber"));
            paymentRequest.setCvv((String) request.get("cvv"));
            paymentRequest.setExpiryDate((String) request.get("expiryDate"));
            
            String otp = (String) request.get("otp");
            String cardType = (String) request.get("cardType");
            
            if (otp == null || cardType == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "OTP and card type are required");
                return ResponseEntity.badRequest().body(response);
            }
            
            BillPayment payment = billPaymentService.verifyOtpAndProcessPayment(paymentRequest, otp, cardType);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Bill payment processed successfully");
            response.put("payment", payment);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Resend OTP for bill payment
     */
    @PostMapping("/resend-otp")
    public ResponseEntity<Map<String, Object>> resendOtp(@RequestBody Map<String, String> request) {
        try {
            String cardNumber = request.get("cardNumber");
            String accountNumber = request.get("accountNumber");
            String cvv = request.get("cvv");
            String expiryDate = request.get("expiryDate");
            String cardholderName = request.get("cardholderName");
            
            if (cardNumber == null || accountNumber == null || cvv == null || expiryDate == null || cardholderName == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "All card details are required");
                return ResponseEntity.badRequest().body(response);
            }
            
            Map<String, Object> result = billPaymentService.verifyCardAndSendOtp(
                cardNumber, cvv, expiryDate, cardholderName, accountNumber
            );
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error resending OTP: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Process bill payment (legacy endpoint - kept for backward compatibility)
     */
    @PostMapping("/pay")
    public ResponseEntity<?> processBillPayment(@RequestBody BillPaymentRequest request) {
        try {
            BillPayment payment = billPaymentService.processBillPayment(request);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Bill payment processed successfully");
            response.put("payment", payment);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Get all bill payments for a user account
     */
    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<List<BillPayment>> getBillPaymentsByAccount(@PathVariable String accountNumber) {
        return ResponseEntity.ok(billPaymentService.getBillPaymentsByAccount(accountNumber));
    }

    /**
     * Get all bill payments (for admin)
     */
    @GetMapping("/all")
    public ResponseEntity<List<BillPayment>> getAllBillPayments() {
        return ResponseEntity.ok(billPaymentService.getAllBillPayments());
    }

    /**
     * Get bill payment by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<BillPayment> getBillPaymentById(@PathVariable Long id) {
        return billPaymentService.getBillPaymentById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get bill payments by credit card ID
     */
    @GetMapping("/credit-card/{creditCardId}")
    public ResponseEntity<List<BillPayment>> getBillPaymentsByCreditCard(@PathVariable Long creditCardId) {
        return ResponseEntity.ok(billPaymentService.getBillPaymentsByCreditCard(creditCardId));
    }

    /**
     * Get bill payments by type
     */
    @GetMapping("/type/{billType}")
    public ResponseEntity<List<BillPayment>> getBillPaymentsByType(@PathVariable String billType) {
        return ResponseEntity.ok(billPaymentService.getBillPaymentsByType(billType));
    }
}
