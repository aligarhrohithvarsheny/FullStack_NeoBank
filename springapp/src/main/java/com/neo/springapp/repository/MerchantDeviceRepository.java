package com.neo.springapp.repository;

import com.neo.springapp.model.MerchantDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MerchantDeviceRepository extends JpaRepository<MerchantDevice, Long> {
    Optional<MerchantDevice> findByDeviceId(String deviceId);
    List<MerchantDevice> findByMerchantId(String merchantId);
    List<MerchantDevice> findByDeviceType(String deviceType);
    List<MerchantDevice> findByStatus(String status);
    List<MerchantDevice> findByMerchantIdAndDeviceType(String merchantId, String deviceType);

    @Query("SELECT COUNT(d) FROM MerchantDevice d WHERE d.status = 'ACTIVE'")
    long countActiveDevices();

    @Query("SELECT COUNT(d) FROM MerchantDevice d WHERE d.deviceType = :type")
    long countByDeviceType(String type);
}
