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

    // Delete transfer
    public boolean deleteTransfer(Long id) {
        if (transferRepository.existsById(id)) {
            transferRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
