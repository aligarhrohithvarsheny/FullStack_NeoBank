package com.neo.springapp.repository;

import com.neo.springapp.model.VideoKycSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface VideoKycSlotRepository extends JpaRepository<VideoKycSlot, Long> {

    List<VideoKycSlot> findBySlotDateAndIsActiveTrueOrderBySlotTimeAsc(LocalDate slotDate);

    @Query("SELECT s FROM VideoKycSlot s WHERE s.slotDate >= :today AND s.isActive = true AND s.currentBookings < s.maxBookings ORDER BY s.slotDate ASC, s.slotTime ASC")
    List<VideoKycSlot> findAvailableSlots(@Param("today") LocalDate today);

    @Query("SELECT s FROM VideoKycSlot s WHERE s.slotDate >= :startDate AND s.slotDate <= :endDate AND s.isActive = true ORDER BY s.slotDate ASC, s.slotTime ASC")
    List<VideoKycSlot> findSlotsByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    List<VideoKycSlot> findByIsActiveTrueOrderBySlotDateAscSlotTimeAsc();

    @Query("SELECT COUNT(s) FROM VideoKycSlot s WHERE s.slotDate = :date AND s.isActive = true")
    long countByDate(@Param("date") LocalDate date);
}
