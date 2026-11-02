package com.neo.springapp.service;

import com.neo.springapp.model.DepositManagementHistory;
import com.neo.springapp.repository.DepositManagementHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DepositManagementHistoryService {

    private final DepositManagementHistoryRepository repository;

    public DepositManagementHistoryService(DepositManagementHistoryRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void record(String productType, Long productId, String accountNumber, String action,
                       String performedBy, String reason, String details) {
        DepositManagementHistory history = new DepositManagementHistory();
        history.setProductType(productType);
        history.setProductId(productId);
        history.setAccountNumber(accountNumber);
        history.setAction(action);
        history.setPerformedBy(performedBy);
        history.setReason(reason);
        history.setDetails(details);
        repository.save(history);
    }

    @Transactional(readOnly = true)
    public List<DepositManagementHistory> getHistory(String productType, Long productId) {
        return repository.findByProductTypeAndProductIdOrderByOccurredAtDesc(productType, productId);
    }
}
