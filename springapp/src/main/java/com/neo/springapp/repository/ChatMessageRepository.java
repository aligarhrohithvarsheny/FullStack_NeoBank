package com.neo.springapp.repository;

import com.neo.springapp.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findBySessionIdOrderByTimestampAsc(String sessionId);
    List<ChatMessage> findByUserIdOrderByTimestampDesc(String userId);
    List<ChatMessage> findByStatusOrderByTimestampDesc(String status);
    List<ChatMessage> findByAdminIdOrderByTimestampDesc(String adminId);
    Optional<ChatMessage> findFirstBySessionIdOrderByTimestampDesc(String sessionId);
    Long countByStatusAndIsRead(String status, Boolean isRead);
}





