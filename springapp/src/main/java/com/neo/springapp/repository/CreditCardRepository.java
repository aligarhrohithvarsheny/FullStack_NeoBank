package com.neo.springapp.repository;

import com.neo.springapp.model.CreditCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CreditCardRepository extends JpaRepository<CreditCard, Long> {
    List<CreditCard> findByAccountNumber(String accountNumber);
    List<CreditCard> findByStatus(String status);
    Optional<CreditCard> findByCardNumber(String cardNumber);
    List<CreditCard> findByUserNameContainingIgnoreCase(String userName);
}
