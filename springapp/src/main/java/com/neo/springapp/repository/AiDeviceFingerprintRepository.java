package com.neo.springapp.repository;

import com.neo.springapp.model.AiDeviceFingerprint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AiDeviceFingerprintRepository extends JpaRepository<AiDeviceFingerprint, Long> {

    Optional<AiDeviceFingerprint> findByEntityIdAndDeviceHash(String entityId, String deviceHash);

    List<AiDeviceFingerprint> findByEntityIdAndEntityTypeOrderByLastSeenAtDesc(String entityId, String entityType);

    List<AiDeviceFingerprint> findByEntityIdAndIsTrustedTrue(String entityId);

    @Query("SELECT COUNT(d) FROM AiDeviceFingerprint d WHERE d.entityId = :entityId")
    long countDevicesForEntity(@Param("entityId") String entityId);

    @Query("SELECT d FROM AiDeviceFingerprint d WHERE d.entityId = :entityId AND d.isTrusted = false ORDER BY d.lastSeenAt DESC")
    List<AiDeviceFingerprint> findUntrustedDevices(@Param("entityId") String entityId);
}
