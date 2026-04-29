package com.neo.springapp.repository;

import com.neo.springapp.model.CurrentAccountEditHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CurrentAccountEditHistoryRepository extends JpaRepository<CurrentAccountEditHistory, Long> {

    List<CurrentAccountEditHistory> findByAccountIdOrderByEditedAtDesc(Long accountId);

    List<CurrentAccountEditHistory> findByAccountNumberOrderByEditedAtDesc(String accountNumber);

    List<CurrentAccountEditHistory> findByEditedByOrderByEditedAtDesc(String editedBy);
}
