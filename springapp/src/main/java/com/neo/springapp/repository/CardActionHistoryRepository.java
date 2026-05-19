package com.neo.springapp.repository;

import com.neo.springapp.model.CardActionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CardActionHistoryRepository extends JpaRepository<CardActionHistory, Long> {
    List<CardActionHistory> findByCardIdOrderByCreatedAtDesc(Long cardId);
}
