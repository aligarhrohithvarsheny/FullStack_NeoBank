package com.neo.springapp.service;

import com.neo.springapp.model.TransferRecord;
import com.neo.springapp.repository.TransferRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
public class TransferService {

    private final TransferRepository transferRepository;

    public TransferService(TransferRepository transferRepository) {
        this.transferRepository = transferRepository;
    }

    // Save transfer
    public TransferRecord saveTransfer(TransferRecord transfer) {
        return transferRepository.save(transfer);
    }

    // Get all transfers with pagination and sorting
    public Page<TransferRecord> getAllTransfers(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return transferRepository.findAll(pageable);
    }

    // Enhanced search functionality
    public Page<TransferRecord> searchTransfers(String searchTerm, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return transferRepository.searchTransfers(searchTerm, pageable);
    }

    // Advanced filtering with multiple criteria
    public Page<TransferRecord> findTransfersWithFilters(
            String senderAccountNumber,
            String recipientAccountNumber,
            TransferRecord.TransferType transferType,
            String status,
            Double minAmount,
            Double maxAmount,
            LocalDateTime startDate,
            LocalDateTime endDate,
            int page,
            int size) {
        Pageable pageable = PageRequest.of(page, size);
        return transferRepository.findTransfersWithFilters(
                senderAccountNumber, recipientAccountNumber, transferType, status,
                minAmount, maxAmount, startDate, endDate, pageable);
    }

    // Get transfers by sender account number with pagination
    public Page<TransferRecord> getTransfersBySenderAccount(String accountNumber, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return transferRepository.findBySenderAccountNumberOrderByDateDesc(accountNumber, pageable);
    }

    // Get transfers by recipient account number with pagination
    public Page<TransferRecord> getTransfersByRecipientAccount(String accountNumber, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return transferRepository.findByRecipientAccountNumberOrderByDateDesc(accountNumber, pageable);
    }

    // Get transfers by sender name with pagination
    public Page<TransferRecord> getTransfersBySenderName(String senderName, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return transferRepository.findBySenderNameContainingIgnoreCaseOrderByDateDesc(senderName, pageable);
    }

    // Get transfers by recipient name with pagination
    public Page<TransferRecord> getTransfersByRecipientName(String recipientName, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return transferRepository.findByRecipientNameContainingIgnoreCaseOrderByDateDesc(recipientName, pageable);
    }

    // Get transfers by transfer type with pagination
    public Page<TransferRecord> getTransfersByType(TransferRecord.TransferType transferType, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return transferRepository.findByTransferTypeOrderByDateDesc(transferType, pageable);
    }

    // Get transfers by status with pagination
    public Page<TransferRecord> getTransfersByStatus(String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return transferRepository.findByStatusOrderByDateDesc(status, pageable);
    }

    // Get transfers by date range with pagination
    public Page<TransferRecord> getTransfersByDateRange(LocalDateTime startDate, LocalDateTime endDate, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return transferRepository.findByDateBetweenOrderByDateDesc(startDate, endDate, pageable);
    }

    // Get transfers by amount range with pagination
    public Page<TransferRecord> getTransfersByAmountRange(Double minAmount, Double maxAmount, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return transferRepository.findByAmountBetweenOrderByDateDesc(minAmount, maxAmount, pageable);
    }

    // Get transfers by IFSC code with pagination
    public Page<TransferRecord> getTransfersByIfsc(String ifsc, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return transferRepository.findByIfscOrderByDateDesc(ifsc, pageable);
    }

    // Get transfer summary by sender account
    public Object[] getTransferSummaryBySenderAccount(String accountNumber) {
        return transferRepository.getTransferSummaryBySenderAccount(accountNumber);
    }

    // Get recent transfers (mini statement)
    public List<TransferRecord> getRecentTransfers(String accountNumber) {
        Pageable pageable = PageRequest.of(0, 5);
        return transferRepository.findTop5BySenderAccountNumberOrderByDateDesc(accountNumber, pageable);
    }

    // Get transfers with custom sorting
    public Page<TransferRecord> getTransfersWithSorting(String sortBy, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return transferRepository.findAllWithSorting(sortBy, pageable);
    }

    // Enhanced Statistics Methods
    public Map<String, Object> getTransferStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // Count by status
        stats.put("pendingCount", transferRepository.countByStatus("Pending"));
        stats.put("completedCount", transferRepository.countByStatus("Completed"));
        stats.put("failedCount", transferRepository.countByStatus("Failed"));
        
        // Count by transfer type
        stats.put("neftCount", transferRepository.countByTransferType(TransferRecord.TransferType.NEFT));
        stats.put("rtgsCount", transferRepository.countByTransferType(TransferRecord.TransferType.RTGS));
        
        // Average transfer amount
        Double avgAmount = transferRepository.getAverageTransferAmount();
        stats.put("averageTransferAmount", avgAmount != null ? avgAmount : 0.0);
        
        return stats;
    }

    public Map<String, Object> getTransferStatisticsByAccount(String accountNumber) {
        Map<String, Object> stats = new HashMap<>();
        
        // Total transfer amount by sender
        Double totalAmount = transferRepository.getTotalTransferAmountBySender(accountNumber);
        stats.put("totalTransferAmount", totalAmount != null ? totalAmount : 0.0);
        
        // Transfer summary
        Object[] summary = transferRepository.getTransferSummaryBySenderAccount(accountNumber);
        if (summary != null && summary.length >= 2) {
            stats.put("totalTransfers", summary[0]);
            stats.put("totalAmount", summary[1]);
        }
        
        return stats;
    }

    public Map<String, Object> getTransferStatisticsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, Object> stats = new HashMap<>();
        
        Object[] dateStats = transferRepository.getTransferStatisticsByDateRange(startDate, endDate);
        if (dateStats != null && dateStats.length >= 2) {
            stats.put("transferCount", dateStats[0]);
            stats.put("totalAmount", dateStats[1]);
        }
        
        return stats;
    }

    // Recent transfers for dashboard
    public List<TransferRecord> getRecentTransfersBySender(String accountNumber, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return transferRepository.findRecentTransfersBySender(accountNumber, pageable);
    }

    public List<TransferRecord> getRecentTransfersByRecipient(String accountNumber, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return transferRepository.findRecentTransfersByRecipient(accountNumber, pageable);
    }

    // Processing methods
    public List<TransferRecord> getPendingTransfersForProcessing(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return transferRepository.findPendingTransfersForProcessing(pageable);
    }

    public List<TransferRecord> getFailedTransfersForRetry(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return transferRepository.findFailedTransfersForRetry(pageable);
    }

    // Analytics methods
    public List<Object[]> getTransferVolumeByMonth() {
        return transferRepository.getTransferVolumeByMonth();
    }

    public List<Object[]> getTopRecipientsBySender(String accountNumber, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return transferRepository.getTopRecipientsBySender(accountNumber, pageable);
    }

    // Get transfer by ID
    public TransferRecord getTransferById(Long id) {
        return transferRepository.findById(id).orElse(null);
    }

    // Update transfer status
    public TransferRecord updateTransferStatus(Long id, String status) {
        TransferRecord transfer = transferRepository.findById(id).orElse(null);
        if (transfer != null) {
            transfer.setStatus(status);
            return transferRepository.save(transfer);
        }
        return null;
    }

    // Process transfer (business logic)
    public TransferRecord processTransfer(Long id, String status, String processedBy) {
        TransferRecord transfer = transferRepository.findById(id).orElse(null);
        if (transfer != null) {
            transfer.setStatus(status);
            // Add processed timestamp and processed by if needed
            return transferRepository.save(transfer);
        }
        return null;
    }

    // Delete transfer
    public boolean deleteTransfer(Long id) {
        if (transferRepository.existsById(id)) {
            transferRepository.deleteById(id);
            return true;
        }
        return false;
    }

    // Validation methods
    public boolean validateTransferAmount(Double amount, Double availableBalance) {
        return amount != null && amount > 0 && amount <= availableBalance;
    }

    public boolean validateTransferData(TransferRecord transfer) {
        return transfer.getSenderAccountNumber() != null &&
               transfer.getRecipientAccountNumber() != null &&
               transfer.getAmount() != null &&
               transfer.getAmount() > 0 &&
               transfer.getTransferType() != null;
    }
}
