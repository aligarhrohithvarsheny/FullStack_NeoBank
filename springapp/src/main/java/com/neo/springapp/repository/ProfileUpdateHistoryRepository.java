package com.neo.springapp.repository;

import com.neo.springapp.model.ProfileUpdateHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProfileUpdateHistoryRepository extends JpaRepository<ProfileUpdateHistory, Long> {
    
    List<ProfileUpdateHistory> findByUserId(Long userId);
    
    List<ProfileUpdateHistory> findByAccountNumber(String accountNumber);
    
    List<ProfileUpdateHistory> findByFieldUpdated(String fieldUpdated);
    
    List<ProfileUpdateHistory> findByUserIdOrderByUpdateDateDesc(Long userId);
    
    List<ProfileUpdateHistory> findByAccountNumberOrderByUpdateDateDesc(String accountNumber);
}
