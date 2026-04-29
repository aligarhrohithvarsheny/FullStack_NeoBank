package com.neo.springapp.repository;

import com.neo.springapp.model.CurrentAccountBusinessUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CurrentAccountBusinessUserRepository extends JpaRepository<CurrentAccountBusinessUser, Long> {
    List<CurrentAccountBusinessUser> findByAccountNumberOrderByCreatedAtDesc(String accountNumber);
    List<CurrentAccountBusinessUser> findByAccountNumberAndStatus(String accountNumber, String status);
    Optional<CurrentAccountBusinessUser> findByUserId(String userId);
    Optional<CurrentAccountBusinessUser> findByEmailAndAccountNumber(String email, String accountNumber);
    Optional<CurrentAccountBusinessUser> findByEmail(String email);
    long countByAccountNumber(String accountNumber);
}
