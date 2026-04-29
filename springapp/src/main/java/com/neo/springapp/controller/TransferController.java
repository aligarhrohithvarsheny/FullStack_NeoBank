package com.neo.springapp.controller;

import com.neo.springapp.model.TransferRecord;
import com.neo.springapp.service.TransferService;
import com.neo.springapp.service.AccountService;
import com.neo.springapp.service.TransactionService;
import com.neo.springapp.service.PdfService;
import com.neo.springapp.service.FraudDetectionService;
import com.neo.springapp.service.AiSecurityService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {

    private final TransferService transferService;
    private final AccountService accountService;
    private final TransactionService transactionService;
    private final PdfService pdfService;
    private final FraudDetectionService fraudDetectionService;
    private final AiSecurityService aiSecurityService;

    public TransferController(TransferService transferService, AccountService accountService, TransactionService transactionService, PdfService pdfService, FraudDetectionService fraudDetectionService, AiSecurityService aiSecurityService) {
        this.transferService = transferService;
        this.accountService = accountService;
        this.transactionService = transactionService;
        this.pdfService = pdfService;
        this.fraudDetectionService = fraudDetectionService;
        this.aiSecurityService = aiSecurityService;
    }

    // Create new transfer
    @PostMapping
    public ResponseEntity<Map<String, Object>> createTransfer(
            @RequestBody Map<String, Object> transferData,
            @RequestHeader(value = "X-Forwarded-For", required = false) String forwardedFor) {
        try {
            // Extract transfer data
            String senderAccountNumber = (String) transferData.get("senderAccountNumber");
            String recipientAccountNumber = (String) transferData.get("recipientAccountNumber");
            String recipientName = (String) transferData.get("recipientName");
            String phone = (String) transferData.get("phone");
            String ifsc = (String) transferData.get("ifsc");
            Double amount = Double.valueOf(transferData.get("amount").toString());
            String transferType = (String) transferData.get("transferType");
            String description = (String) transferData.get("description");
            String clientIp = (String) transferData.get("clientIp");
            if (clientIp == null || clientIp.isEmpty()) {
                clientIp = forwardedFor != null ? forwardedFor.split(",")[0].trim() : null;
            }
            String location = (String) transferData.get("location");
            String deviceInfo = (String) transferData.get("deviceInfo");

            // Validate transfer data
            if (senderAccountNumber == null || recipientAccountNumber == null || amount == null || amount <= 0) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Invalid transfer data");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Check if sender account is closed
            com.neo.springapp.model.Account senderAccount = accountService.getAccountByNumber(senderAccountNumber);
            if (senderAccount == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Sender account not found");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            if ("CLOSED".equalsIgnoreCase(senderAccount.getStatus())) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Cannot transfer from a closed account");
                errorResponse.put("message", "The sender account is closed and cannot perform transfers");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Check if recipient account is closed (if it exists in our system)
            com.neo.springapp.model.Account recipientAccount = accountService.getAccountByNumber(recipientAccountNumber);
            if (recipientAccount != null && "CLOSED".equalsIgnoreCase(recipientAccount.getStatus())) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Cannot transfer to a closed account");
                errorResponse.put("message", "The recipient account is closed and cannot receive transfers");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Check sender account balance
            Double currentBalance = accountService.getBalanceByAccountNumber(senderAccountNumber);
            if (currentBalance == null || currentBalance < amount) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Insufficient balance");
                errorResponse.put("currentBalance", currentBalance);
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // AI Security: Analyze transaction for anomalies
            try {
                Map<String, Object> aiAssessment = aiSecurityService.analyzeTransaction(
                        senderAccountNumber, senderAccount.getName(), amount,
                        recipientAccountNumber, transferType, currentBalance,
                        clientIp, location, deviceInfo);
                Boolean shouldBlock = (Boolean) aiAssessment.get("shouldBlock");
                if (Boolean.TRUE.equals(shouldBlock)) {
                    Map<String, Object> blockedResponse = new HashMap<>();
                    blockedResponse.put("error", "Transaction blocked by AI Security");
                    blockedResponse.put("message", "This transaction has been flagged as high-risk by our AI security system. Please contact the bank.");
                    blockedResponse.put("riskScore", aiAssessment.get("riskScore"));
                    blockedResponse.put("riskLevel", aiAssessment.get("riskLevel"));
                    return ResponseEntity.badRequest().body(blockedResponse);
                }
            } catch (Exception aiEx) {
                System.err.println("AI Security transaction analysis error: " + aiEx.getMessage());
            }

            // Create transfer record
            TransferRecord transfer = new TransferRecord();
            transfer.setSenderAccountNumber(senderAccountNumber);
            transfer.setRecipientAccountNumber(recipientAccountNumber);
            transfer.setRecipientName(recipientName);
            transfer.setPhone(phone);
            transfer.setIfsc(ifsc);
            transfer.setAmount(amount);
            TransferRecord.TransferType type = TransferRecord.TransferType.valueOf(transferType);
            transfer.setTransferType(type);
            transfer.setDate(LocalDateTime.now());

            // Handle IMPS (immediate) vs NEFT (2 hours delay)
            String senderName = senderAccount != null && senderAccount.getName() != null ? senderAccount.getName() : "Unknown";
            if (type == TransferRecord.TransferType.IMPS) {
                // IMPS: Process immediately
                transfer.setStatus("Completed");
                transfer.setProcessedAt(LocalDateTime.now());
                
                // Save transfer
                TransferRecord savedTransfer = transferService.saveTransfer(transfer);

                // Real-time AI fraud detection
                try {
                    fraudDetectionService.analyzeTransferAndAlertIfSuspicious(senderAccountNumber, senderName, recipientAccountNumber, recipientName, amount, clientIp != null ? clientIp : "", location != null ? location : "", deviceInfo != null ? deviceInfo : "");
                } catch (Exception e) {
                    System.err.println("Fraud detection analysis failed: " + e.getMessage());
                }

                // Update sender account balance (debit)
                Double newBalance = accountService.debitBalance(senderAccountNumber, amount);

                // Create transaction record for sender
                transactionService.createTransferTransaction(
                    senderAccountNumber, 
                    description, 
                    amount, 
                    "Debit", 
                    newBalance
                );

                // Update recipient account balance (credit) if account exists
                Double recipientBalance = accountService.getBalanceByAccountNumber(recipientAccountNumber);
                if (recipientBalance != null) {
                    Double newRecipientBalance = accountService.creditBalance(recipientAccountNumber, amount);
                    transactionService.createTransferTransaction(
                        recipientAccountNumber, 
                        "Transfer received from " + senderAccountNumber, 
                        amount, 
                        "Credit", 
                        newRecipientBalance
                    );
                }

                // Prepare response
                Map<String, Object> response = new HashMap<>();
                response.put("id", savedTransfer.getId());
                response.put("transfer", savedTransfer);
                response.put("newBalance", newBalance);
                response.put("message", "IMPS Transfer completed successfully");
                response.put("processedImmediately", true);

                return ResponseEntity.ok(response);
            } else if (type == TransferRecord.TransferType.NEFT) {
                // NEFT: Schedule for processing after minimum 2 hours
                transfer.setStatus("Pending");
                transfer.setScheduledProcessingTime(LocalDateTime.now().plusHours(2));
                
                // Save transfer
                TransferRecord savedTransfer = transferService.saveTransfer(transfer);

                // Real-time AI fraud detection
                try {
                    fraudDetectionService.analyzeTransferAndAlertIfSuspicious(senderAccountNumber, senderName, recipientAccountNumber, recipientName, amount, clientIp != null ? clientIp : "", location != null ? location : "", deviceInfo != null ? deviceInfo : "");
                } catch (Exception e) {
                    System.err.println("Fraud detection analysis failed: " + e.getMessage());
                }

                // Debit sender account immediately (amount is reserved)
                Double newBalance = accountService.debitBalance(senderAccountNumber, amount);

                // Create pending transaction record for sender
                transactionService.createTransferTransaction(
                    senderAccountNumber, 
                    description + " (NEFT - Pending)", 
                    amount, 
                    "Debit", 
                    newBalance
                );

                // Prepare response
                Map<String, Object> response = new HashMap<>();
                response.put("id", savedTransfer.getId());
                response.put("transfer", savedTransfer);
                response.put("newBalance", newBalance);
                response.put("message", "NEFT Transfer scheduled. Amount will be credited after minimum 2 hours.");
                response.put("processedImmediately", false);
                response.put("scheduledProcessingTime", savedTransfer.getScheduledProcessingTime());

                // Process NEFT transfer in background after 2 hours
                processNeftTransferAsync(savedTransfer.getId());

                return ResponseEntity.ok(response);
            } else {
                // RTGS: Process immediately (existing behavior)
                transfer.setStatus("Completed");
                transfer.setProcessedAt(LocalDateTime.now());
                
                // Save transfer
                TransferRecord savedTransfer = transferService.saveTransfer(transfer);

                // Update sender account balance (debit)
                Double newBalance = accountService.debitBalance(senderAccountNumber, amount);

                // Create transaction record for sender
                transactionService.createTransferTransaction(
                    senderAccountNumber, 
                    description, 
                    amount, 
                    "Debit", 
                    newBalance
                );

                // Update recipient account balance (credit) if account exists
                Double recipientBalance = accountService.getBalanceByAccountNumber(recipientAccountNumber);
                if (recipientBalance != null) {
                    Double newRecipientBalance = accountService.creditBalance(recipientAccountNumber, amount);
                    transactionService.createTransferTransaction(
                        recipientAccountNumber, 
                        "Transfer received from " + senderAccountNumber, 
                        amount, 
                        "Credit", 
                        newRecipientBalance
                    );
                }

                // Prepare response
                Map<String, Object> response = new HashMap<>();
                response.put("id", savedTransfer.getId());
                response.put("transfer", savedTransfer);
                response.put("newBalance", newBalance);
                response.put("message", "RTGS Transfer completed successfully");
                response.put("processedImmediately", true);

                return ResponseEntity.ok(response);
            }

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Transfer failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    // Get all transfers with pagination and sorting
    @GetMapping
    public Page<TransferRecord> getAllTransfers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "date") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return transferService.getAllTransfers(page, size, sortBy, sortDir);
    }

    // Enhanced search functionality
    @GetMapping("/search")
    public Page<TransferRecord> searchTransfers(
            @RequestParam String searchTerm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return transferService.searchTransfers(searchTerm, page, size);
    }

    // Advanced filtering with multiple criteria
    @GetMapping("/filter")
    public Page<TransferRecord> findTransfersWithFilters(
            @RequestParam(required = false) String senderAccountNumber,
            @RequestParam(required = false) String recipientAccountNumber,
            @RequestParam(required = false) TransferRecord.TransferType transferType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Double minAmount,
            @RequestParam(required = false) Double maxAmount,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        LocalDateTime start = startDate != null ? LocalDateTime.parse(startDate) : null;
        LocalDateTime end = endDate != null ? LocalDateTime.parse(endDate) : null;
        
        return transferService.findTransfersWithFilters(
                senderAccountNumber, recipientAccountNumber, transferType, status,
                minAmount, maxAmount, start, end, page, size);
    }

    // Get transfers by sender account number with pagination
    @GetMapping("/sender/{accountNumber}")
    public Page<TransferRecord> getTransfersBySenderAccount(
            @PathVariable String accountNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return transferService.getTransfersBySenderAccount(accountNumber, page, size);
    }

    // Get transfers by recipient account number with pagination
    @GetMapping("/recipient/{accountNumber}")
    public Page<TransferRecord> getTransfersByRecipientAccount(
            @PathVariable String accountNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return transferService.getTransfersByRecipientAccount(accountNumber, page, size);
    }

    // Get transfers by sender name with pagination
    @GetMapping("/sender-name/{senderName}")
    public Page<TransferRecord> getTransfersBySenderName(
            @PathVariable String senderName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return transferService.getTransfersBySenderName(senderName, page, size);
    }

    // Get transfers by recipient name with pagination
    @GetMapping("/recipient-name/{recipientName}")
    public Page<TransferRecord> getTransfersByRecipientName(
            @PathVariable String recipientName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return transferService.getTransfersByRecipientName(recipientName, page, size);
    }

    // Get transfers by transfer type with pagination
    @GetMapping("/type/{transferType}")
    public Page<TransferRecord> getTransfersByType(
            @PathVariable TransferRecord.TransferType transferType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return transferService.getTransfersByType(transferType, page, size);
    }

    // Get transfers by status with pagination
    @GetMapping("/status/{status}")
    public Page<TransferRecord> getTransfersByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return transferService.getTransfersByStatus(status, page, size);
    }

    // Get transfers by date range with pagination
    @GetMapping("/date-range")
    public Page<TransferRecord> getTransfersByDateRange(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        LocalDateTime start = LocalDateTime.parse(startDate);
        LocalDateTime end = LocalDateTime.parse(endDate);
        return transferService.getTransfersByDateRange(start, end, page, size);
    }

    // Get transfers by amount range with pagination
    @GetMapping("/amount-range")
    public Page<TransferRecord> getTransfersByAmountRange(
            @RequestParam Double minAmount,
            @RequestParam Double maxAmount,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return transferService.getTransfersByAmountRange(minAmount, maxAmount, page, size);
    }

    // Get transfers by IFSC code with pagination
    @GetMapping("/ifsc/{ifsc}")
    public Page<TransferRecord> getTransfersByIfsc(
            @PathVariable String ifsc,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return transferService.getTransfersByIfsc(ifsc, page, size);
    }

    // Statistics endpoints
    @GetMapping("/statistics")
    public Map<String, Object> getTransferStatistics() {
        return transferService.getTransferStatistics();
    }

    @GetMapping("/statistics/account/{accountNumber}")
    public Map<String, Object> getTransferStatisticsByAccount(@PathVariable String accountNumber) {
        return transferService.getTransferStatisticsByAccount(accountNumber);
    }

    @GetMapping("/statistics/date-range")
    public Map<String, Object> getTransferStatisticsByDateRange(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        LocalDateTime start = LocalDateTime.parse(startDate);
        LocalDateTime end = LocalDateTime.parse(endDate);
        return transferService.getTransferStatisticsByDateRange(start, end);
    }

    // Analytics endpoints
    @GetMapping("/analytics/volume-by-month")
    public List<Object[]> getTransferVolumeByMonth() {
        return transferService.getTransferVolumeByMonth();
    }

    @GetMapping("/analytics/top-recipients/{accountNumber}")
    public List<Object[]> getTopRecipientsBySender(
            @PathVariable String accountNumber,
            @RequestParam(defaultValue = "10") int limit) {
        return transferService.getTopRecipientsBySender(accountNumber, limit);
    }

    // Recent transfers for dashboard
    @GetMapping("/recent/sender/{accountNumber}")
    public List<TransferRecord> getRecentTransfersBySender(
            @PathVariable String accountNumber,
            @RequestParam(defaultValue = "5") int limit) {
        return transferService.getRecentTransfersBySender(accountNumber, limit);
    }

    @GetMapping("/recent/recipient/{accountNumber}")
    public List<TransferRecord> getRecentTransfersByRecipient(
            @PathVariable String accountNumber,
            @RequestParam(defaultValue = "5") int limit) {
        return transferService.getRecentTransfersByRecipient(accountNumber, limit);
    }

    // Processing endpoints
    @GetMapping("/pending")
    public List<TransferRecord> getPendingTransfersForProcessing(
            @RequestParam(defaultValue = "10") int limit) {
        return transferService.getPendingTransfersForProcessing(limit);
    }

    @GetMapping("/failed")
    public List<TransferRecord> getFailedTransfersForRetry(
            @RequestParam(defaultValue = "10") int limit) {
        return transferService.getFailedTransfersForRetry(limit);
    }

    // Get transfer summary by sender account
    @GetMapping("/summary/{accountNumber}")
    public Object[] getTransferSummary(@PathVariable String accountNumber) {
        return transferService.getTransferSummaryBySenderAccount(accountNumber);
    }

    // Get recent transfers (mini statement)
    @GetMapping("/recent/{accountNumber}")
    public List<TransferRecord> getRecentTransfers(@PathVariable String accountNumber) {
        return transferService.getRecentTransfers(accountNumber);
    }

    // Get transfer by ID
    @GetMapping("/{id}")
    public ResponseEntity<TransferRecord> getTransferById(@PathVariable Long id) {
        TransferRecord transfer = transferService.getTransferById(id);
        if (transfer != null) {
            return ResponseEntity.ok(transfer);
        }
        return ResponseEntity.notFound().build();
    }

    // Update transfer status
    @PutMapping("/{id}/status")
    public ResponseEntity<TransferRecord> updateTransferStatus(@PathVariable Long id, @RequestParam String status) {
        TransferRecord updatedTransfer = transferService.updateTransferStatus(id, status);
        if (updatedTransfer != null) {
            return ResponseEntity.ok(updatedTransfer);
        }
        return ResponseEntity.notFound().build();
    }

    // Process transfer
    @PutMapping("/{id}/process")
    public ResponseEntity<TransferRecord> processTransfer(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false) String processedBy) {
        TransferRecord updatedTransfer = transferService.processTransfer(id, status, processedBy);
        if (updatedTransfer != null) {
            return ResponseEntity.ok(updatedTransfer);
        }
        return ResponseEntity.notFound().build();
    }

    // Validate transfer amount
    @PostMapping("/validate-amount")
    public ResponseEntity<Map<String, Object>> validateTransferAmount(
            @RequestParam Double amount,
            @RequestParam Double availableBalance) {
        Map<String, Object> response = new java.util.HashMap<>();
        boolean isValid = transferService.validateTransferAmount(amount, availableBalance);
        response.put("valid", isValid);
        response.put("message", isValid ? "Amount is valid" : "Amount exceeds available balance");
        return ResponseEntity.ok(response);
    }

    // Cancel transfer (within 3 minutes for NEFT/IMPS)
    @PutMapping("/{id}/cancel")
    public ResponseEntity<Map<String, Object>> cancelTransfer(@PathVariable Long id) {
        try {
            TransferRecord transfer = transferService.getTransferById(id);
            if (transfer == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Transfer not found");
                return ResponseEntity.notFound().build();
            }

            // Check if transfer can be cancelled
            if (!transfer.canBeCancelled()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Transfer cannot be cancelled. Only NEFT/IMPS transfers within 3 minutes can be cancelled.");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Cancel the transfer
            TransferRecord cancelledTransfer = transferService.cancelTransfer(id);
            
            // Reverse the transaction (credit back to sender, debit from recipient)
            Double newSenderBalance = accountService.creditBalance(transfer.getSenderAccountNumber(), transfer.getAmount());
            Double newRecipientBalance = accountService.debitBalance(transfer.getRecipientAccountNumber(), transfer.getAmount());
            
            // Create transaction records for the reversal
            transactionService.createTransferTransaction(
                transfer.getSenderAccountNumber(), 
                "Transfer cancelled - Amount credited back", 
                transfer.getAmount(), 
                "Credit", 
                newSenderBalance
            );
            
            transactionService.createTransferTransaction(
                transfer.getRecipientAccountNumber(), 
                "Transfer cancelled - Amount debited", 
                transfer.getAmount(), 
                "Debit", 
                newRecipientBalance
            );

            Map<String, Object> response = new HashMap<>();
            response.put("transfer", cancelledTransfer);
            response.put("newSenderBalance", newSenderBalance);
            response.put("message", "Transfer cancelled successfully");
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Transfer cancellation failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    // Download transfer receipt as PDF
    @GetMapping("/{id}/receipt")
    public ResponseEntity<byte[]> downloadTransferReceipt(@PathVariable Long id) {
        try {
            TransferRecord transfer = transferService.getTransferById(id);
            if (transfer == null) {
                return ResponseEntity.notFound().build();
            }

            byte[] pdfBytes = pdfService.generateTransferReceipt(transfer);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "transfer_receipt_" + transfer.getTransferId() + ".pdf");
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);
                    
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // Delete transfer
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransfer(@PathVariable Long id) {
        boolean deleted = transferService.deleteTransfer(id);
        if (deleted) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    // Process NEFT transfer asynchronously after 2 hours
    private void processNeftTransferAsync(Long transferId) {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.schedule(() -> {
            try {
                TransferRecord transfer = transferService.getTransferById(transferId);
                if (transfer != null && "Pending".equals(transfer.getStatus()) && 
                    transfer.getTransferType() == TransferRecord.TransferType.NEFT) {
                    
                    // Check if it's been at least 2 hours
                    if (transfer.canProcessNeft()) {
                        // Process the NEFT transfer
                        String recipientAccountNumber = transfer.getRecipientAccountNumber();
                        Double amount = transfer.getAmount();
                        
                        // Update recipient account balance (credit) if account exists
                        Double recipientBalance = accountService.getBalanceByAccountNumber(recipientAccountNumber);
                        if (recipientBalance != null) {
                            Double newRecipientBalance = accountService.creditBalance(recipientAccountNumber, amount);
                            transactionService.createTransferTransaction(
                                recipientAccountNumber, 
                                "NEFT Transfer received from " + transfer.getSenderAccountNumber(), 
                                amount, 
                                "Credit", 
                                newRecipientBalance
                            );
                        }
                        
                        // Update transfer status
                        transfer.setStatus("Completed");
                        transfer.setProcessedAt(LocalDateTime.now());
                        transferService.saveTransfer(transfer);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error processing NEFT transfer: " + e.getMessage());
                e.printStackTrace();
            }
        }, 2, TimeUnit.HOURS);
    }
}
