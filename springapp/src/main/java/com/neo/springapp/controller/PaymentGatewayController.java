package com.neo.springapp.controller;

import com.neo.springapp.model.*;
import com.neo.springapp.service.PaymentGatewayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payment-gateway")
@CrossOrigin(origins = "*")
public class PaymentGatewayController {

    @Autowired
    private PaymentGatewayService paymentGatewayService;

    // ==================== MERCHANT ENDPOINTS ====================

    @PostMapping("/merchants/register")
    public ResponseEntity<Map<String, Object>> registerMerchant(@RequestBody Map<String, String> request) {
        try {
            PgMerchant merchant = paymentGatewayService.registerMerchant(request);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("merchant", merchant);
            response.put("message", "Merchant registered successfully. Save your API keys securely.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/merchants/{merchantId}")
    public ResponseEntity<Map<String, Object>> getMerchant(@PathVariable String merchantId) {
        return paymentGatewayService.getMerchantById(merchantId)
                .map(merchant -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("merchant", merchant);
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {
                    Map<String, Object> error = new HashMap<>();
                    error.put("success", false);
                    error.put("error", "Merchant not found");
                    return ResponseEntity.badRequest().body(error);
                });
    }

    @GetMapping("/merchants")
    public ResponseEntity<List<PgMerchant>> getAllMerchants() {
        return ResponseEntity.ok(paymentGatewayService.getAllMerchants());
    }

    @PostMapping("/merchants/{merchantId}/link-account")
    public ResponseEntity<Map<String, Object>> linkMerchantAccount(
            @PathVariable String merchantId,
            @RequestBody Map<String, String> request) {
        try {
            String accountNumber = request.get("accountNumber");
            Map<String, Object> result = paymentGatewayService.linkMerchantReceivingAccount(merchantId, accountNumber);
            if (Boolean.TRUE.equals(result.get("success"))) {
                return ResponseEntity.ok(result);
            }
            return ResponseEntity.badRequest().body(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // ==================== ORDER ENDPOINTS ====================

    @PostMapping("/orders/create")
    public ResponseEntity<Map<String, Object>> createOrder(@RequestBody Map<String, Object> request) {
        try {
            PgOrder order = paymentGatewayService.createOrder(request);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("order", order);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<Map<String, Object>> getOrder(@PathVariable String orderId) {
        return paymentGatewayService.getOrderById(orderId)
                .map(order -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("order", order);
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {
                    Map<String, Object> error = new HashMap<>();
                    error.put("success", false);
                    error.put("error", "Order not found");
                    return ResponseEntity.badRequest().body(error);
                });
    }

    @GetMapping("/orders/merchant/{merchantId}")
    public ResponseEntity<List<PgOrder>> getOrdersByMerchant(@PathVariable String merchantId) {
        return ResponseEntity.ok(paymentGatewayService.getOrdersByMerchant(merchantId));
    }

    // Called after a salary/business UPI QR payment to @ezyvault — marks the order PAID
    // without re-debiting the payer (debit already happened via UPI payment path).
    @PostMapping("/orders/{orderId}/complete")
    public ResponseEntity<Map<String, Object>> completeOrderFromQrPayment(
            @PathVariable String orderId,
            @RequestBody Map<String, Object> request) {
        try {
            Map<String, Object> result = paymentGatewayService.completeOrderFromQrPayment(orderId, request);
            if (Boolean.TRUE.equals(result.get("success"))) {
                return ResponseEntity.ok(result);
            }
            return ResponseEntity.badRequest().body(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // ==================== PAYMENT ENDPOINTS ====================

    @PostMapping("/pay")
    public ResponseEntity<Map<String, Object>> processPayment(@RequestBody Map<String, String> request) {
        try {
            PgTransaction txn = paymentGatewayService.processPayment(request);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("transaction", txn);
            response.put("message", "Payment processed successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/transactions/{transactionId}")
    public ResponseEntity<Map<String, Object>> getTransaction(@PathVariable String transactionId) {
        return paymentGatewayService.getTransactionById(transactionId)
                .map(txn -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("transaction", txn);
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {
                    Map<String, Object> error = new HashMap<>();
                    error.put("success", false);
                    error.put("error", "Transaction not found");
                    return ResponseEntity.badRequest().body(error);
                });
    }

    @GetMapping("/transactions/merchant/{merchantId}")
    public ResponseEntity<List<PgTransaction>> getTransactionsByMerchant(@PathVariable String merchantId) {
        return ResponseEntity.ok(paymentGatewayService.getTransactionsByMerchant(merchantId));
    }

    // ==================== REFUND ENDPOINTS ====================

    @PostMapping("/refunds")
    public ResponseEntity<Map<String, Object>> processRefund(@RequestBody Map<String, Object> request) {
        try {
            PgRefund refund = paymentGatewayService.processRefund(request);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("refund", refund);
            response.put("message", "Refund processed successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/refunds/merchant/{merchantId}")
    public ResponseEntity<List<PgRefund>> getRefundsByMerchant(@PathVariable String merchantId) {
        return ResponseEntity.ok(paymentGatewayService.getRefundsByMerchant(merchantId));
    }

    @GetMapping("/refunds/transaction/{transactionId}")
    public ResponseEntity<List<PgRefund>> getRefundsByTransaction(@PathVariable String transactionId) {
        return ResponseEntity.ok(paymentGatewayService.getRefundsByTransaction(transactionId));
    }

    // ==================== SIGNATURE VERIFICATION ====================

    @PostMapping("/verify-signature")
    public ResponseEntity<Map<String, Object>> verifySignature(@RequestBody Map<String, String> request) {
        String orderId = request.get("orderId");
        String transactionId = request.get("transactionId");
        String signature = request.get("signature");
        String secretKey = request.get("secretKey");

        boolean valid = paymentGatewayService.verifySignature(orderId, transactionId, signature, secretKey);

        Map<String, Object> response = new HashMap<>();
        response.put("valid", valid);
        response.put("message", valid ? "Signature verified" : "Invalid signature - possible tampering detected");
        return ResponseEntity.ok(response);
    }

    // ==================== ANALYTICS ====================

    @GetMapping("/analytics/{merchantId}")
    public ResponseEntity<Map<String, Object>> getMerchantAnalytics(@PathVariable String merchantId) {
        try {
            Map<String, Object> analytics = paymentGatewayService.getMerchantAnalytics(merchantId);
            return ResponseEntity.ok(analytics);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // ==================== OTP LOGIN ENDPOINTS ====================

    @PostMapping("/login/send-otp")
    public ResponseEntity<Map<String, Object>> sendLoginOtp(@RequestBody Map<String, String> request) {
        Map<String, Object> result = paymentGatewayService.sendLoginOtp(request.get("email"));
        if (Boolean.TRUE.equals(result.get("success"))) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.badRequest().body(result);
    }

    @PostMapping("/login/verify-otp")
    public ResponseEntity<Map<String, Object>> verifyLoginOtp(@RequestBody Map<String, String> request) {
        Map<String, Object> result = paymentGatewayService.verifyLoginOtp(request.get("email"), request.get("otp"));
        if (Boolean.TRUE.equals(result.get("success"))) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.badRequest().body(result);
    }

    // ==================== UPI QR PAYMENT SESSION ====================

    @PostMapping("/payment-session/create")
    public ResponseEntity<Map<String, Object>> createPaymentSession(@RequestBody Map<String, Object> request) {
        try {
            Map<String, Object> result = paymentGatewayService.createPaymentSession(request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // ==================== NAME VERIFICATION ====================

    @PostMapping("/verify-name")
    public ResponseEntity<Map<String, Object>> verifyPayerName(@RequestBody Map<String, String> request) {
        try {
            Map<String, Object> result = paymentGatewayService.verifyPayerName(
                    request.get("orderId"), request.get("payerName"), request.get("payerAccount"));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // ==================== ADMIN PG MANAGEMENT ====================

    @GetMapping("/admin/pending-merchants")
    public ResponseEntity<List<PgMerchant>> getPendingMerchants() {
        return ResponseEntity.ok(paymentGatewayService.getPendingMerchants());
    }

    @GetMapping("/admin/approved-merchants")
    public ResponseEntity<List<PgMerchant>> getApprovedMerchants() {
        return ResponseEntity.ok(paymentGatewayService.getApprovedMerchants());
    }

    @PostMapping("/admin/approve/{merchantId}")
    public ResponseEntity<Map<String, Object>> approveMerchant(
            @PathVariable String merchantId, @RequestBody Map<String, String> request) {
        try {
            String adminName = request.getOrDefault("adminName", "Admin");
            Map<String, Object> result = paymentGatewayService.adminApproveMerchant(merchantId, adminName, request);
            if (Boolean.TRUE.equals(result.get("success"))) {
                return ResponseEntity.ok(result);
            }
            return ResponseEntity.badRequest().body(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/admin/reject/{merchantId}")
    public ResponseEntity<Map<String, Object>> rejectMerchant(
            @PathVariable String merchantId, @RequestBody Map<String, String> request) {
        try {
            Map<String, Object> result = paymentGatewayService.adminRejectMerchant(
                    merchantId, request.getOrDefault("reason", "Rejected by admin"));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/admin/toggle-login/{merchantId}")
    public ResponseEntity<Map<String, Object>> toggleMerchantLogin(
            @PathVariable String merchantId, @RequestBody Map<String, Object> request) {
        try {
            boolean enable = Boolean.TRUE.equals(request.get("enable"));
            Map<String, Object> result = paymentGatewayService.adminToggleLogin(merchantId, enable);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // ==================== SETTLEMENT & CREDIT ====================

    @PostMapping("/settle/{transactionId}")
    public ResponseEntity<Map<String, Object>> settleTransaction(@PathVariable String transactionId) {
        try {
            Map<String, Object> result = paymentGatewayService.creditMerchantAccount(transactionId);
            if (Boolean.TRUE.equals(result.get("success"))) {
                return ResponseEntity.ok(result);
            }
            return ResponseEntity.badRequest().body(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/ledger/merchant/{merchantId}")
    public ResponseEntity<List<?>> getLedgerByMerchant(@PathVariable String merchantId) {
        return ResponseEntity.ok(paymentGatewayService.getLedgerByMerchant(merchantId));
    }

    @GetMapping("/ledger/account/{accountNumber}")
    public ResponseEntity<List<?>> getLedgerByAccount(@PathVariable String accountNumber) {
        return ResponseEntity.ok(paymentGatewayService.getLedgerByAccount(accountNumber));
    }

    // ==================== VERIFY LINKED ACCOUNT ====================

    @GetMapping("/verify-account/{accountNumber}")
    public ResponseEntity<Map<String, Object>> verifyLinkedAccount(@PathVariable String accountNumber) {
        return ResponseEntity.ok(paymentGatewayService.verifyLinkedAccount(accountNumber));
    }

    // ==================== CARD PAYMENT WITH OTP ====================

    @PostMapping("/card/verify-and-send-otp")
    public ResponseEntity<Map<String, Object>> verifyCardAndSendOtp(@RequestBody Map<String, String> request) {
        try {
            Map<String, Object> result = paymentGatewayService.verifyCardAndSendOtp(request);
            if (Boolean.TRUE.equals(result.get("success"))) {
                return ResponseEntity.ok(result);
            }
            return ResponseEntity.badRequest().body(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/card/verify-otp-and-pay")
    public ResponseEntity<Map<String, Object>> verifyCardOtpAndPay(@RequestBody Map<String, Object> request) {
        try {
            Map<String, Object> result = paymentGatewayService.verifyCardOtpAndProcessPayment(request);
            if (Boolean.TRUE.equals(result.get("success"))) {
                return ResponseEntity.ok(result);
            }
            return ResponseEntity.badRequest().body(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // ==================== PAYMENT LINKS ====================

    @GetMapping("/upi/verify/{upiId}")
    public ResponseEntity<Map<String, Object>> verifyAnyUpiId(@PathVariable String upiId) {
        return ResponseEntity.ok(paymentGatewayService.verifyAnyUpiId(upiId));
    }

    @PostMapping("/payment-links/send")
    public ResponseEntity<Map<String, Object>> sendPaymentLink(@RequestBody Map<String, Object> request) {
        try {
            Map<String, Object> result = paymentGatewayService.sendPaymentLink(request);
            if (Boolean.TRUE.equals(result.get("success"))) return ResponseEntity.ok(result);
            return ResponseEntity.badRequest().body(result);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @GetMapping("/payment-links/merchant/{merchantId}")
    public ResponseEntity<List<?>> getMerchantPaymentLinks(@PathVariable String merchantId) {
        return ResponseEntity.ok(paymentGatewayService.getMerchantPaymentLinks(merchantId));
    }

    @GetMapping("/payment-links/customer/{upiId}")
    public ResponseEntity<List<?>> getCustomerPaymentLinks(@PathVariable String upiId) {
        return ResponseEntity.ok(paymentGatewayService.getCustomerPaymentLinks(upiId));
    }

    @GetMapping("/payment-links/customer/{upiId}/pending")
    public ResponseEntity<List<?>> getPendingCustomerLinks(@PathVariable String upiId) {
        return ResponseEntity.ok(paymentGatewayService.getPendingCustomerPaymentLinks(upiId));
    }

    @PostMapping("/payment-links/{linkToken}/pay")
    public ResponseEntity<Map<String, Object>> payPaymentLink(
            @PathVariable String linkToken, @RequestBody Map<String, Object> request) {
        try {
            Map<String, Object> result = paymentGatewayService.payPaymentLink(linkToken, request);
            if (Boolean.TRUE.equals(result.get("success"))) return ResponseEntity.ok(result);
            return ResponseEntity.badRequest().body(result);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @PostMapping("/payment-links/{linkToken}/cancel")
    public ResponseEntity<Map<String, Object>> cancelPaymentLink(@PathVariable String linkToken) {
        try {
            Map<String, Object> result = paymentGatewayService.cancelPaymentLink(linkToken);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    // ==================== DELETE ORDER (CREATED ONLY) ====================

    @DeleteMapping("/orders/{orderId}")
    public ResponseEntity<Map<String, Object>> deleteOrder(@PathVariable String orderId) {
        try {
            Map<String, Object> result = paymentGatewayService.deleteOrder(orderId);
            if (Boolean.TRUE.equals(result.get("success"))) return ResponseEntity.ok(result);
            return ResponseEntity.badRequest().body(result);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    // ==================== PAY ORDER WITH UPI ID ====================

    @PostMapping("/orders/{orderId}/pay-with-upi")
    public ResponseEntity<Map<String, Object>> payOrderWithUpiId(
            @PathVariable String orderId,
            @RequestBody Map<String, Object> request) {
        try {
            Map<String, Object> result = paymentGatewayService.payOrderWithUpiId(orderId, request);
            if (Boolean.TRUE.equals(result.get("success"))) return ResponseEntity.ok(result);
            return ResponseEntity.badRequest().body(result);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }
}

