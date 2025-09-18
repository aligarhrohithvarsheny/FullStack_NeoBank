package com.neo.springapp.service;

import org.springframework.stereotype.Service;
import java.util.List;
import com.neo.springapp.model.TransferRecord;
import com.neo.springapp.repository.TransferRepository;

@Service
public class TransferService {

    private TransferRepository transferRepository;

    public TransferService(TransferRepository transferRepository) {
        this.transferRepository = transferRepository;
    }

    // Save transfer
    public TransferRecord saveTransfer(TransferRecord transfer) {
        return transferRepository.save(transfer);
    }

    // Get all transfers
    public List<TransferRecord> getAllTransfers() {
        return transferRepository.findAll();
    }

    // Get transfer by ID
    public TransferRecord getTransferById(Long id) {
        return transferRepository.findById(id).orElse(null);
    }

    // Update transfer
    public TransferRecord updateTransfer(Long id, TransferRecord transferDetails) {
        TransferRecord transfer = transferRepository.findById(id).orElse(null);
        if (transfer != null) {
            // Update only non-null fields
            if (transferDetails.getAccountNumber() != null) {
                transfer.setAccountNumber(transferDetails.getAccountNumber());
            }
            if (transferDetails.getName() != null) {
                transfer.setName(transferDetails.getName());
            }
            if (transferDetails.getPhone() != null) {
                transfer.setPhone(transferDetails.getPhone());
            }
            if (transferDetails.getIfsc() != null) {
                transfer.setIfsc(transferDetails.getIfsc());
            }
            if (transferDetails.getAmount() > 0) {
                transfer.setAmount(transferDetails.getAmount());
            }
            if (transferDetails.getTransferType() != null) {
                transfer.setTransferType(transferDetails.getTransferType());
            }
            if (transferDetails.getDate() != null) {
                transfer.setDate(transferDetails.getDate());
            }
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
