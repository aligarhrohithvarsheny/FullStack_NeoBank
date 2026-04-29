package com.neo.springapp.service;

import com.neo.springapp.model.GlobalTransactionSequence;
import com.neo.springapp.repository.GlobalTransactionSequenceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service to generate globally incrementing transaction IDs
 * 
 * This ensures that every transaction across all types and accounts
 * receives a unique, sequentially incrementing ID.
 * 
 * Example: User A Transaction 1 = ID 1, User B Transaction 1 = ID 2, User A Transaction 2 = ID 3, etc.
 */
@Service
@Slf4j
public class GlobalTransactionIdGenerator {

    private final GlobalTransactionSequenceRepository sequenceRepository;

    public GlobalTransactionIdGenerator(GlobalTransactionSequenceRepository sequenceRepository) {
        this.sequenceRepository = sequenceRepository;
    }

    /**
     * Generate the next global transaction ID
     * Thread-safe operation using database-level locking
     * 
     * @return the next sequential transaction number
     */
    @Transactional
    public synchronized Long getNextTransactionId() {
        try {
            // Get the current sequence (there should be only one record)
            GlobalTransactionSequence sequence = sequenceRepository.getSequence();
            
            if (sequence == null) {
                // Initialize sequence if it doesn't exist
                sequence = new GlobalTransactionSequence();
                sequence.setCurrentSequence(1L);
                sequence = sequenceRepository.save(sequence);
                log.info("Initialized global transaction sequence");
                return 1L;
            }

            // Get current sequence and increment
            Long nextId = sequence.getCurrentSequence();
            sequence.setCurrentSequence(nextId + 1);
            sequenceRepository.save(sequence);
            
            log.debug("Generated transaction ID: {}", nextId);
            return nextId;
        } catch (Exception e) {
            log.error("Error generating transaction ID", e);
            throw new RuntimeException("Failed to generate transaction ID", e);
        }
    }

    /**
     * Generate formatted transaction ID with prefix
     * Format: TXN-<sequence-number>
     * 
     * @return formatted transaction ID like TXN-1, TXN-2, TXN-3, etc.
     */
    @Transactional
    public String getNextFormattedTransactionId() {
        Long id = getNextTransactionId();
        return String.format("TXN-%010d", id); // 10-digit number with leading zeros
    }

    /**
     * Generate prefix-based transaction ID
     * Format: <prefix>-<sequence-number>
     * 
     * @param prefix the prefix for the transaction (e.g., "BILL", "TRF", "SAL", "CC")
     * @return formatted transaction ID like BILL-0000000001, TRF-0000000002, etc.
     */
    @Transactional
    public String getNextFormattedTransactionId(String prefix) {
        Long id = getNextTransactionId();
        return String.format("%s-%010d", prefix, id);
    }

    /**
     * Get the current sequence number without incrementing
     * Useful for display/logging purposes
     * 
     * @return current sequence number
     */
    public Long getCurrentSequence() {
        GlobalTransactionSequence sequence = sequenceRepository.getSequence();
        if (sequence == null) {
            return 0L;
        }
        return sequence.getCurrentSequence();
    }

    /**
     * Initialize sequence to a specific value
     * WARNING: Only use if you know what you're doing (e.g., migrating from an existing system)
     * 
     * @param startValue the starting value for the sequence
     */
    @Transactional
    public void initializeSequence(Long startValue) {
        GlobalTransactionSequence sequence = sequenceRepository.getSequence();
        if (sequence == null) {
            sequence = new GlobalTransactionSequence();
        }
        sequence.setCurrentSequence(startValue);
        sequenceRepository.save(sequence);
        log.warn("Transaction sequence initialized to: {}", startValue);
    }
}
