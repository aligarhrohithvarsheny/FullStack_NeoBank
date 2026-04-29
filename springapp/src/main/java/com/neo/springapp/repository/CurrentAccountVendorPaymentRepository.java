package com.neo.springapp.repository;

import com.neo.springapp.model.CurrentAccountVendorPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CurrentAccountVendorPaymentRepository extends JpaRepository<CurrentAccountVendorPayment, Long> {
    List<CurrentAccountVendorPayment> findByAccountNumberOrderByCreatedAtDesc(String accountNumber);
    List<CurrentAccountVendorPayment> findByAccountNumberAndStatus(String accountNumber, String status);
}
