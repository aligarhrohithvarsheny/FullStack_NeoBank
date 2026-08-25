package com.neo.springapp.repository;

import com.neo.springapp.model.MinorAccountApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MinorAccountApplicationRepository extends JpaRepository<MinorAccountApplication, Long> {
    List<MinorAccountApplication> findByGuardianUserIdOrderByCreatedAtDesc(Long guardianUserId);
    List<MinorAccountApplication> findByStatusOrderByCreatedAtAsc(String status);
}
