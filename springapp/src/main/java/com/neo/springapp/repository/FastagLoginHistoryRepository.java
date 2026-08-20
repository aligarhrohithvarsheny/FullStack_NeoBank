package com.neo.springapp.repository;

import com.neo.springapp.model.FastagLoginHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FastagLoginHistoryRepository extends JpaRepository<FastagLoginHistory, Long> {
    List<FastagLoginHistory> findAllByOrderByLoginTimeDesc();
    List<FastagLoginHistory> findByGmailIdOrderByLoginTimeDesc(String gmailId);
}
