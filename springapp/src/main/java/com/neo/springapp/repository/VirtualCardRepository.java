package com.neo.springapp.repository;

import com.neo.springapp.model.VirtualCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VirtualCardRepository extends JpaRepository<VirtualCard, Long> {

    List<VirtualCard> findByAccountNumber(String accountNumber);

    Optional<VirtualCard> findByCardNumber(String cardNumber);

    List<VirtualCard> findByAccountNumberAndStatus(String accountNumber, String status);

    boolean existsByCardNumber(String cardNumber);
}
