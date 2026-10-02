package com.neo.springapp.repository;

import com.neo.springapp.model.UserEmailHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserEmailHistoryRepository extends JpaRepository<UserEmailHistory, Long> {
    Optional<UserEmailHistory> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
}
