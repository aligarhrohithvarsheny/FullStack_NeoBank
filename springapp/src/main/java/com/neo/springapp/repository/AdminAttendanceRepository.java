package com.neo.springapp.repository;

import com.neo.springapp.model.AdminAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AdminAttendanceRepository extends JpaRepository<AdminAttendance, Long> {

    Optional<AdminAttendance> findByAdminIdAndAttendanceDate(Long adminId, LocalDate attendanceDate);

    @Query("SELECT a FROM AdminAttendance a WHERE a.attendanceDate BETWEEN :fromDate AND :toDate ORDER BY a.attendanceDate DESC, a.verifiedAt DESC")
    List<AdminAttendance> findByAttendanceDateBetween(@Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

    @Query("SELECT a FROM AdminAttendance a WHERE a.adminId = :adminId AND a.attendanceDate BETWEEN :fromDate AND :toDate ORDER BY a.attendanceDate ASC")
    List<AdminAttendance> findByAdminIdAndAttendanceDateBetween(@Param("adminId") Long adminId,
                                                                @Param("fromDate") LocalDate fromDate,
                                                                @Param("toDate") LocalDate toDate);

    @Query("SELECT a FROM AdminAttendance a WHERE a.attendanceDate = :date ORDER BY a.verifiedAt DESC")
    List<AdminAttendance> findByAttendanceDate(@Param("date") LocalDate date);
}

