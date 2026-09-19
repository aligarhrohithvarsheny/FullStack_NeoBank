package com.neo.springapp.repository;

import com.neo.springapp.model.AdminFundTransfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AdminFundTransferRepository extends JpaRepository<AdminFundTransfer, Long> {

    List<AdminFundTransfer> findAllByOrderByPerformedAtDesc();

    @Query("SELECT t FROM AdminFundTransfer t WHERE " +
           "LOWER(t.transferId) LIKE LOWER(CONCAT('%', :term, '%')) OR " +
           "LOWER(t.senderAccountNumber) LIKE LOWER(CONCAT('%', :term, '%')) OR " +
           "LOWER(t.receiverAccountNumber) LIKE LOWER(CONCAT('%', :term, '%')) OR " +
           "LOWER(t.senderName) LIKE LOWER(CONCAT('%', :term, '%')) OR " +
           "LOWER(t.receiverName) LIKE LOWER(CONCAT('%', :term, '%')) OR " +
           "LOWER(t.senderChequeNumber) LIKE LOWER(CONCAT('%', :term, '%')) " +
           "ORDER BY t.performedAt DESC")
    List<AdminFundTransfer> search(@Param("term") String term);
}
