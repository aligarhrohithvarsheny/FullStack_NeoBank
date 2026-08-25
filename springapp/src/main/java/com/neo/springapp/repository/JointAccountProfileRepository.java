package com.neo.springapp.repository;

import com.neo.springapp.model.JointAccountProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface JointAccountProfileRepository extends JpaRepository<JointAccountProfile, Long> {
    List<JointAccountProfile> findByPrimaryHolderUserIdOrJointHolderUserId(Long primary, Long joint);
    boolean existsByPrimaryHolderUserIdAndJointHolderUserId(Long primary, Long joint);
    Optional<JointAccountProfile> findByJointAccountNumber(String accountNumber);
}
