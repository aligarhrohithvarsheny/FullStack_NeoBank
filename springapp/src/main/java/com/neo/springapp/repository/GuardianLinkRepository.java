package com.neo.springapp.repository;

import com.neo.springapp.model.GuardianLink;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GuardianLinkRepository extends JpaRepository<GuardianLink, Long> {
    List<GuardianLink> findByGuardianUserIdAndStatus(Long guardianUserId, String status);
    List<GuardianLink> findByChildUserIdAndStatus(Long childUserId, String status);
}
