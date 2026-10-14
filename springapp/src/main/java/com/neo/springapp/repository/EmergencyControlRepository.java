package com.neo.springapp.repository;

import com.neo.springapp.model.EmergencyControl;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmergencyControlRepository extends JpaRepository<EmergencyControl, Long> {
    Optional<EmergencyControl> findByScopeTypeAndScopeKey(String scopeType, String scopeKey);
    List<EmergencyControl> findByScopeTypeAndScopeKeyStartingWith(String scopeType, String scopeKeyPrefix);
    List<EmergencyControl> findByScopeTypeAndActiveTrueOrderByUpdatedAtDesc(String scopeType);
    List<EmergencyControl> findAllByOrderByUpdatedAtDesc();
}
