package com.neo.springapp.repository;

import com.neo.springapp.model.EmployeeTimeTracking;
import com.neo.springapp.model.EmployeeTimeTracking.TimeTrackingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeTimeTrackingRepository extends JpaRepository<EmployeeTimeTracking, Long> {
    
    // Find today's record for an employee
    Optional<EmployeeTimeTracking> findByAdminIdAndTrackingDate(String adminId, LocalDate date);
    
    // Find all records for an employee in a date range
    @Query("SELECT e FROM EmployeeTimeTracking e WHERE e.adminId = :adminId AND e.trackingDate BETWEEN :startDate AND :endDate ORDER BY e.trackingDate DESC")
    List<EmployeeTimeTracking> findByAdminIdAndDateRange(
        @Param("adminId") String adminId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
    
    // Find all records for a specific date
    @Query("SELECT e FROM EmployeeTimeTracking e WHERE e.trackingDate = :date ORDER BY e.adminName ASC")
    List<EmployeeTimeTracking> findByDate(@Param("date") LocalDate date);
    
    // Find records by status for a specific date
    @Query("SELECT e FROM EmployeeTimeTracking e WHERE e.trackingDate = :date AND e.status = :status")
    List<EmployeeTimeTracking> findByDateAndStatus(
        @Param("date") LocalDate date,
        @Param("status") TimeTrackingStatus status
    );
    
    // Paginated search for all time records
    @Query("SELECT e FROM EmployeeTimeTracking e WHERE " +
           "(:status IS NULL OR e.status = :status) AND " +
           "(:search IS NULL OR LOWER(e.adminName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(e.idCardNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(e.adminEmail) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY e.trackingDate DESC, e.adminName ASC")
    Page<EmployeeTimeTracking> findAllWithFilters(
        @Param("status") TimeTrackingStatus status,
        @Param("search") String search,
        Pageable pageable
    );
    
    // Get employees with pending checkouts
    @Query("SELECT e FROM EmployeeTimeTracking e WHERE e.trackingDate = CURRENT_DATE AND e.status = 'ACTIVE'")
    List<EmployeeTimeTracking> findPendingCheckouts();
    
    // Count employees checked in for a date
    @Query("SELECT COUNT(e) FROM EmployeeTimeTracking e WHERE e.trackingDate = :date AND e.status IN ('ACTIVE', 'CHECKED_OUT')")
    long countCheckedInForDate(@Param("date") LocalDate date);
    
    // Count absent employees for a date
    @Query("SELECT COUNT(e) FROM EmployeeTimeTracking e WHERE e.trackingDate = :date AND e.status = 'ABSENT'")
    long countAbsentForDate(@Param("date") LocalDate date);
    
    // Count on-leave employees for a date
    @Query("SELECT COUNT(e) FROM EmployeeTimeTracking e WHERE e.trackingDate = :date AND e.status = 'ON_LEAVE'")
    long countOnLeaveForDate(@Param("date") LocalDate date);
    
    // Get average working hours for a date
    @Query("SELECT AVG(e.totalWorkingHours) FROM EmployeeTimeTracking e WHERE e.trackingDate = :date AND e.status = 'CHECKED_OUT'")
    Double getAverageWorkingHoursForDate(@Param("date") LocalDate date);
    
    // Get employee's working hours for a time period
    @Query("SELECT SUM(e.totalWorkingHours) FROM EmployeeTimeTracking e WHERE e.adminId = :adminId AND e.trackingDate BETWEEN :startDate AND :endDate AND e.status = 'CHECKED_OUT'")
    Double getTotalWorkingHours(
        @Param("adminId") String adminId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
    
    // Get total overtime hours
    @Query("SELECT SUM(e.overtimeHours) FROM EmployeeTimeTracking e WHERE e.adminId = :adminId AND e.trackingDate BETWEEN :startDate AND :endDate")
    Double getTotalOvertimeHours(
        @Param("adminId") String adminId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
}
