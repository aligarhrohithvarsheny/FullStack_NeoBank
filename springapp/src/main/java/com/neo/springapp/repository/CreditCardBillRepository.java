package com.neo.springapp.repository;

import com.neo.springapp.model.CreditCardBill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CreditCardBillRepository extends JpaRepository<CreditCardBill, Long> {
    List<CreditCardBill> findByCreditCardId(Long creditCardId);
    List<CreditCardBill> findByCardNumber(String cardNumber);
    List<CreditCardBill> findByAccountNumber(String accountNumber);
    Optional<CreditCardBill> findFirstByCreditCardIdOrderByBillGenerationDateDesc(Long creditCardId);
    List<CreditCardBill> findByStatus(String status);
}
