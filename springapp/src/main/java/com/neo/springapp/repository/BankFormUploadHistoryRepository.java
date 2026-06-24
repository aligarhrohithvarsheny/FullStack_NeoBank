package com.neo.springapp.repository;

import com.neo.springapp.model.BankFormUploadHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BankFormUploadHistoryRepository extends JpaRepository<BankFormUploadHistory, Long> {
    List<BankFormUploadHistory> findByUploadIdOrderByPerformedAtDesc(Long uploadId);
}
