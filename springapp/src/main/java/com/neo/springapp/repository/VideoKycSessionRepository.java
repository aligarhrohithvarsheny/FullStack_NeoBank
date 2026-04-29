package com.neo.springapp.repository;

import com.neo.springapp.model.VideoKycSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VideoKycSessionRepository extends JpaRepository<VideoKycSession, Long> {

    Optional<VideoKycSession> findByRoomId(String roomId);

    Optional<VideoKycSession> findByTemporaryAccountNumber(String temporaryAccountNumber);

    Optional<VideoKycSession> findByMobileNumber(String mobileNumber);

    Optional<VideoKycSession> findByEmail(String email);

    Optional<VideoKycSession> findByVerificationNumber(String verificationNumber);

    List<VideoKycSession> findByKycStatus(String kycStatus);

    Page<VideoKycSession> findByKycStatus(String kycStatus, Pageable pageable);

    Page<VideoKycSession> findAll(Pageable pageable);

    @Query("SELECT v FROM VideoKycSession v WHERE v.kycStatus IN :statuses ORDER BY v.createdAt DESC")
    List<VideoKycSession> findByKycStatusIn(@Param("statuses") List<String> statuses);

    @Query("SELECT v FROM VideoKycSession v WHERE v.kycStatus IN :statuses")
    Page<VideoKycSession> findByKycStatusIn(@Param("statuses") List<String> statuses, Pageable pageable);

    @Query("SELECT v FROM VideoKycSession v WHERE v.mobileNumber = :mobile AND v.kycStatus != 'Approved'")
    List<VideoKycSession> findPendingByMobile(@Param("mobile") String mobileNumber);

    @Query("SELECT v FROM VideoKycSession v WHERE v.email = :email AND v.kycStatus != 'Approved'")
    List<VideoKycSession> findPendingByEmail(@Param("email") String email);

    @Query("SELECT COUNT(v) FROM VideoKycSession v WHERE v.mobileNumber = :mobile")
    long countByMobileNumber(@Param("mobile") String mobileNumber);

    @Query("SELECT COUNT(v) FROM VideoKycSession v WHERE v.kycStatus = :status")
    long countByKycStatus(@Param("status") String status);

    @Query("SELECT v FROM VideoKycSession v WHERE v.sessionActive = true AND v.assignedAdminId = :adminId")
    Optional<VideoKycSession> findActiveSessionByAdmin(@Param("adminId") Long adminId);

    @Query("SELECT v FROM VideoKycSession v WHERE " +
           "LOWER(v.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "v.temporaryAccountNumber LIKE CONCAT('%', :search, '%') OR " +
           "v.mobileNumber LIKE CONCAT('%', :search, '%') OR " +
           "v.customerId LIKE CONCAT('%', :search, '%') OR " +
           "v.verificationNumber LIKE CONCAT('%', :search, '%')")
    Page<VideoKycSession> searchSessions(@Param("search") String search, Pageable pageable);

    @Query("SELECT v FROM VideoKycSession v WHERE v.bookedSlotId = :slotId")
    List<VideoKycSession> findByBookedSlotId(@Param("slotId") Long slotId);
}
