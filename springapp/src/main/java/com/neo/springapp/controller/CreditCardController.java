package com.neo.springapp.controller;

import com.neo.springapp.model.*;
import com.neo.springapp.service.CreditCardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/credit-cards")
@CrossOrigin(origins = "*")
public class CreditCardController {

    @Autowired
    private CreditCardService creditCardService;

    // Admin: Get all credit cards
    @GetMapping("/all")
    public ResponseEntity<List<CreditCard>> getAllCreditCards() {
        return ResponseEntity.ok(creditCardService.getAllCreditCards());
    }

    // User: Get credit cards by account number
    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<List<CreditCard>> getCreditCardsByAccount(@PathVariable String accountNumber) {
        return ResponseEntity.ok(creditCardService.getCreditCardsByAccount(accountNumber));
    }

    // Get credit card by ID
    @GetMapping("/{id}")
    public ResponseEntity<CreditCard> getCreditCardById(@PathVariable Long id) {
        Optional<CreditCard> card = creditCardService.getCreditCardById(id);
        return card.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    // Create credit card from approved request
    @PostMapping("/create")
    public ResponseEntity<CreditCard> createCreditCard(@RequestBody CreditCardRequest request) {
        // Generate card details
        String cardNumber = generateCardNumber();
        String cvv = String.format("%03d", (int)(Math.random() * 1000));
        String expiryMonth = String.format("%02d", (int)(Math.random() * 12) + 1);
        String expiryYear = String.valueOf(LocalDateTime.now().getYear() + 3);
        String expiryDate = expiryMonth + "/" + expiryYear.substring(2);
        
        CreditCard creditCard = creditCardService.createCreditCardFromRequest(request, cardNumber, cvv, expiryDate);
        return ResponseEntity.ok(creditCard);
    }

    // Update credit card
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCreditCard(@PathVariable Long id, @RequestBody Map<String, Object> updateData) {
        Optional<CreditCard> existingCardOpt = creditCardService.getCreditCardById(id);
        if (existingCardOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        CreditCard existingCard = existingCardOpt.get();
        
        // Update approved limit if provided
        if (updateData.containsKey("approvedLimit")) {
            Double newLimit = ((Number) updateData.get("approvedLimit")).doubleValue();
            existingCard.setApprovedLimit(newLimit);
            // Recalculate available limit and usage
            existingCard.calculateAvailableLimit();
            existingCard.calculateUsageLimit();
        }
        
        // Update other fields if provided
        if (updateData.containsKey("status")) {
            existingCard.setStatus((String) updateData.get("status"));
        }

        // Update user-set spending limit if provided (must be within approved limit)
        if (updateData.containsKey("userSetSpendingLimit")) {
            Double newSpendingLimit = ((Number) updateData.get("userSetSpendingLimit")).doubleValue();
            if (newSpendingLimit < 0) {
                Map<String, Object> err = new HashMap<>();
                err.put("error", "Spending limit cannot be negative");
                return ResponseEntity.badRequest().body(err);
            }
            if (existingCard.getApprovedLimit() != null && newSpendingLimit > existingCard.getApprovedLimit()) {
                Map<String, Object> err = new HashMap<>();
                err.put("error", "Spending limit cannot exceed approved limit");
                err.put("approvedLimit", existingCard.getApprovedLimit());
                return ResponseEntity.badRequest().body(err);
            }
            existingCard.setUserSetSpendingLimit(newSpendingLimit);
        }
        
        return ResponseEntity.ok(creditCardService.updateCreditCard(existingCard));
    }

    // Set PIN
    @PutMapping("/{id}/set-pin")
    public ResponseEntity<?> setPin(@PathVariable Long id, @RequestBody Map<String, String> request) {
        String pin = request.get("pin");
        if (pin == null || pin.isEmpty()) {
            return ResponseEntity.badRequest().body("PIN is required");
        }
        boolean success = creditCardService.setPin(id, pin);
        return success ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    // Get transactions
    @GetMapping("/{id}/transactions")
    public ResponseEntity<List<CreditCardTransaction>> getTransactions(@PathVariable Long id) {
        return ResponseEntity.ok(creditCardService.getTransactionsByCardId(id));
    }

    @GetMapping("/transactions/{transactionId}/cheque-image")
    public ResponseEntity<byte[]> downloadChequeImage(@PathVariable Long transactionId) {
        CreditCardTransaction transaction = creditCardService.getChequeImageTransaction(transactionId);
        if (transaction == null) return ResponseEntity.notFound().build();
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        try {
            if (transaction.getChequeImageType() != null) mediaType = MediaType.parseMediaType(transaction.getChequeImageType());
        } catch (Exception ignored) { }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\""
                        + (transaction.getChequeImageName() == null ? "cheque-image" : transaction.getChequeImageName()) + "\"")
                .contentType(mediaType)
                .body(transaction.getChequeImage());
    }

    @GetMapping("/account/{accountNumber}/transactions")
    public ResponseEntity<List<CreditCardTransaction>> getTransactionsByAccount(@PathVariable String accountNumber) {
        return ResponseEntity.ok(creditCardService.getTransactionsByAccount(accountNumber));
    }

    // Add transaction
    @PostMapping("/transactions")
    public ResponseEntity<CreditCardTransaction> addTransaction(@RequestBody CreditCardTransaction transaction) {
        return ResponseEntity.ok(creditCardService.addTransaction(transaction));
    }

    // Generate bill
    @PostMapping("/{id}/generate-bill")
    public ResponseEntity<CreditCardBill> generateBill(@PathVariable Long id) {
        CreditCardBill bill = creditCardService.generateBill(id);
        return bill != null ? ResponseEntity.ok(bill) : ResponseEntity.notFound().build();
    }

    // Get bills
    @GetMapping("/{id}/bills")
    public ResponseEntity<List<CreditCardBill>> getBills(@PathVariable Long id) {
        return ResponseEntity.ok(creditCardService.getBillsByCardId(id));
    }

    @GetMapping("/account/{accountNumber}/bills")
    public ResponseEntity<List<CreditCardBill>> getBillsByAccount(@PathVariable String accountNumber) {
        return ResponseEntity.ok(creditCardService.getBillsByAccount(accountNumber));
    }

    // Pay bill
    @PostMapping("/bills/{billId}/pay")
    public ResponseEntity<CreditCardBill> payBill(@PathVariable Long billId, @RequestParam Double amount) {
        CreditCardBill bill = creditCardService.payBill(billId, amount);
        return bill != null ? ResponseEntity.ok(bill) : ResponseEntity.notFound().build();
    }

    // Admin: pay a bill by cash, user account debit, or cheque
    @PostMapping("/bills/{billId}/admin-pay")
    public ResponseEntity<?> adminPayBill(@PathVariable Long billId,
                                          @RequestBody AdminCreditCardPaymentRequest request) {
        try {
            return ResponseEntity.ok(creditCardService.payBillAsAdmin(billId, request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // Admin: verify a cheque number before using it to pay a credit card bill
    @GetMapping("/verify-cheque")
    public ResponseEntity<?> verifyChequeForCardPayment(@RequestParam String chequeNumber,
                                                        @RequestParam String debitAccountNumber) {
        return ResponseEntity.ok(creditCardService.verifyChequeForCardPayment(chequeNumber, debitAccountNumber));
    }

    // Admin: transfer amount from credit card's available limit to any account (savings/current/salary)
    @PostMapping("/{id}/transfer-to-account")
    public ResponseEntity<?> transferToAccount(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        try {
            String destinationAccountNumber = String.valueOf(request.get("destinationAccountNumber"));
            Double amount = request.get("amount") == null ? null : ((Number) request.get("amount")).doubleValue();
            String adminName = request.get("adminName") == null ? null : String.valueOf(request.get("adminName"));
            return ResponseEntity.ok(creditCardService.transferToAccount(id, destinationAccountNumber, amount, adminName));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // Get current admin-configurable transfer fee percent
    @GetMapping("/transfer-fee-percent")
    public ResponseEntity<?> getTransferFeePercent() {
        return ResponseEntity.ok(Map.of("transferFeePercent", creditCardService.getTransferFeePercent()));
    }

    // Admin: update the transfer fee percent
    @PutMapping("/transfer-fee-percent")
    public ResponseEntity<?> updateTransferFeePercent(@RequestBody Map<String, Object> request) {
        try {
            double feePercent = ((Number) request.get("transferFeePercent")).doubleValue();
            String updatedBy = request.get("adminName") == null ? null : String.valueOf(request.get("adminName"));
            return ResponseEntity.ok(creditCardService.updateTransferFeePercent(feePercent, updatedBy));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // Get statement
    @GetMapping("/{id}/statement")
    public ResponseEntity<List<CreditCardTransaction>> getStatement(
            @PathVariable Long id,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        LocalDateTime start = startDate != null ? LocalDateTime.parse(startDate) : LocalDateTime.now().minusMonths(1);
        LocalDateTime end = endDate != null ? LocalDateTime.parse(endDate) : LocalDateTime.now();
        return ResponseEntity.ok(creditCardService.getStatement(id, start, end));
    }

    // Close credit card
    @PutMapping("/{id}/close")
    public ResponseEntity<?> closeCreditCard(@PathVariable Long id) {
        boolean success = creditCardService.closeCreditCard(id);
        if (success) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().body("Cannot close credit card with outstanding balance");
    }

    // Calculate overdue and penalties (admin cron job)
    @PostMapping("/calculate-overdue")
    public ResponseEntity<?> calculateOverdue() {
        creditCardService.calculateOverdueAndPenalties();
        return ResponseEntity.ok().build();
    }

    private String generateCardNumber() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            sb.append((int)(Math.random() * 10));
        }
        return sb.toString();
    }
}
