package com.neo.springapp.repository;

import com.neo.springapp.model.ProfileUpdateRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProfileUpdateRequestRepository extends JpaRepository<ProfileUpdateRequest, Long> {
    
    List<ProfileUpdateRequest> findByUserId(Long userId);
    
    List<ProfileUpdateRequest> findByAccountNumber(String accountNumber);
    
    List<ProfileUpdateRequest> findByStatus(String status);
    
    List<ProfileUpdateRequest> findByStatusIn(List<String> statuses);
    
    List<ProfileUpdateRequest> findByUserIdAndStatus(Long userId, String status);
}
