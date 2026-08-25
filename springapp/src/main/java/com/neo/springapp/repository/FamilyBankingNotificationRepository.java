package com.neo.springapp.repository;

import com.neo.springapp.model.FamilyBankingNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FamilyBankingNotificationRepository extends JpaRepository<FamilyBankingNotification, Long> {
    List<FamilyBankingNotification> findByRecipientUserIdOrderByCreatedAtDesc(Long recipientUserId);
}