package com.neo.springapp.controller;

import com.neo.springapp.model.TransferRecord;
import com.neo.springapp.service.TransferService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/transfers")
@CrossOrigin(origins = "http://localhost:4200")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    // Create new transfer
    @PostMapping
    public ResponseEntity<TransferRecord> createTransfer(@RequestBody TransferRecord transfer) {
        TransferRecord saved = transferService.saveTransfer(transfer);
        return ResponseEntity.ok(saved);
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

    // Delete transfer
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransfer(@PathVariable Long id) {
        boolean deleted = transferService.deleteTransfer(id);
        if (deleted) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}
