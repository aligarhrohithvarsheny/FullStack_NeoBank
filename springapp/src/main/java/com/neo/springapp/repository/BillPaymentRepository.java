package com.neo.springapp.repository;

import com.neo.springapp.model.BillPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BillPaymentRepository extends JpaRepository<BillPayment, Long> {
    
    // Find all bill payments by account number
    List<BillPayment> findByAccountNumber(String accountNumber);
    
    // Find all bill payments by credit card ID
    List<BillPayment> findByCreditCardId(Long creditCardId);
    
    // Find all bill payments by status
    List<BillPayment> findByStatus(String status);
    
    // Find all bill payments by bill type
    List<BillPayment> findByBillType(String billType);
    
    // Find all bill payments ordered by payment date descending
    List<BillPayment> findAllByOrderByPaymentDateDesc();
}
