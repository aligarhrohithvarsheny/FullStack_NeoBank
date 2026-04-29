package com.neo.springapp.repository;

import com.neo.springapp.model.CurrentAccountInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CurrentAccountInvoiceRepository extends JpaRepository<CurrentAccountInvoice, Long> {
    List<CurrentAccountInvoice> findByAccountNumberOrderByCreatedAtDesc(String accountNumber);
    List<CurrentAccountInvoice> findByAccountNumberAndStatus(String accountNumber, String status);
    Optional<CurrentAccountInvoice> findByInvoiceNumber(String invoiceNumber);
    long countByAccountNumber(String accountNumber);
}
