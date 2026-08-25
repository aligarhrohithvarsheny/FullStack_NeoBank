package com.neo.springapp.repository;

import com.neo.springapp.model.JointAccountInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface JointAccountInvitationRepository extends JpaRepository<JointAccountInvitation, Long> {
    List<JointAccountInvitation> findByInviteeUserIdOrderByCreatedAtDesc(Long userId);
    List<JointAccountInvitation> findByInviterUserIdOrderByCreatedAtDesc(Long userId);
    List<JointAccountInvitation> findByAccountNumberAndStatus(String accountNumber, String status);
}
