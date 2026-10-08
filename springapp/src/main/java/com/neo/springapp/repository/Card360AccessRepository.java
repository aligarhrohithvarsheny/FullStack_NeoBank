package com.neo.springapp.repository;

import com.neo.springapp.model.Card360Access;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface Card360AccessRepository extends JpaRepository<Card360Access, Long> {
    Optional<Card360Access> findByAccountNumber(String accountNumber);
}
