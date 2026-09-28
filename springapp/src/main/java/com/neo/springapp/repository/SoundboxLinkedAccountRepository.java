package com.neo.springapp.repository;

import com.neo.springapp.model.SoundboxLinkedAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SoundboxLinkedAccountRepository extends JpaRepository<SoundboxLinkedAccount, Long> {
    List<SoundboxLinkedAccount> findBySoundboxAccountNumberOrderByRequestedAtDesc(String soundboxAccountNumber);
    List<SoundboxLinkedAccount> findByStatusOrderByRequestedAtDesc(String status);
    boolean existsBySoundboxAccountNumberAndLinkedAccountNumberAndStatusIn(
        String soundboxAccountNumber, String linkedAccountNumber, List<String> statuses);
    Optional<SoundboxLinkedAccount> findBySoundboxAccountNumberAndLinkedAccountNumberAndStatus(
        String soundboxAccountNumber, String linkedAccountNumber, String status);
}