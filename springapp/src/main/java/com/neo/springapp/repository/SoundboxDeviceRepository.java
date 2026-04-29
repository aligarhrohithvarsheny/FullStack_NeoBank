package com.neo.springapp.repository;

import com.neo.springapp.model.SoundboxDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SoundboxDeviceRepository extends JpaRepository<SoundboxDevice, Long> {

    Optional<SoundboxDevice> findByDeviceId(String deviceId);

    Optional<SoundboxDevice> findByAccountNumber(String accountNumber);

    List<SoundboxDevice> findByStatus(String status);

    List<SoundboxDevice> findByAccountNumberIn(List<String> accountNumbers);

    @Query("SELECT COUNT(d) FROM SoundboxDevice d WHERE d.status = 'ACTIVE'")
    long countActiveDevices();

    @Query("SELECT COUNT(d) FROM SoundboxDevice d")
    long countTotalDevices();

    boolean existsByAccountNumber(String accountNumber);
}
