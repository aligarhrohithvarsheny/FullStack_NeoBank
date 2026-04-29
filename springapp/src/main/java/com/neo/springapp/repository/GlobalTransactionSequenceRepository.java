package com.neo.springapp.repository;

import com.neo.springapp.model.GlobalTransactionSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Repository for managing global transaction sequences
 */
@Repository
public interface GlobalTransactionSequenceRepository extends JpaRepository<GlobalTransactionSequence, Long> {

    /**
     * Get the first (and only) sequence record
     * Uses native SQL query to properly handle LIMIT clause
     */
    @Query(value = "SELECT * FROM global_transaction_sequence LIMIT 1", nativeQuery = true)
    GlobalTransactionSequence getSequence();
}
