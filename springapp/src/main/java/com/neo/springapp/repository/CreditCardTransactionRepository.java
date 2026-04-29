package com.neo.springapp.repository;

import com.neo.springapp.model.CreditCardTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CreditCardTransactionRepository extends JpaRepository<CreditCardTransaction, Long> {
    List<CreditCardTransaction> findByCreditCardId(Long creditCardId);
    List<CreditCardTransaction> findByCardNumber(String cardNumber);
    List<CreditCardTransaction> findByAccountNumber(String accountNumber);
    List<CreditCardTransaction> findByAccountNumberOrderByTransactionDateDesc(String accountNumber);
}
