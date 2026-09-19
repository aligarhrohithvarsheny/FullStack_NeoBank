package com.neo.springapp.repository;

import com.neo.springapp.model.AdminCashTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AdminCashTransactionRepository extends JpaRepository<AdminCashTransaction, Long> {

    List<AdminCashTransaction> findAllByOrderByPerformedAtDesc();

    @Query("SELECT t FROM AdminCashTransaction t WHERE " +
           "LOWER(t.refId) LIKE LOWER(CONCAT('%', :term, '%')) OR " +
           "LOWER(t.accountNumber) LIKE LOWER(CONCAT('%', :term, '%')) OR " +
           "LOWER(t.accountHolderName) LIKE LOWER(CONCAT('%', :term, '%')) OR " +
           "LOWER(t.description) LIKE LOWER(CONCAT('%', :term, '%')) " +
           "ORDER BY t.performedAt DESC")
    List<AdminCashTransaction> search(@Param("term") String term);
}
