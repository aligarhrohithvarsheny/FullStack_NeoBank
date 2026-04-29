package com.neo.springapp.repository;

import com.neo.springapp.model.AdminAccountApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdminAccountApplicationRepository extends JpaRepository<AdminAccountApplication, Long> {

    Optional<AdminAccountApplication> findByApplicationNumber(String applicationNumber);

    Optional<AdminAccountApplication> findByAccountNumber(String accountNumber);

    List<AdminAccountApplication> findByStatus(String status);

    List<AdminAccountApplication> findByAccountType(String accountType);

    List<AdminAccountApplication> findByCreatedBy(String createdBy);

    List<AdminAccountApplication> findByStatusAndAccountType(String status, String accountType);

    List<AdminAccountApplication> findByAadharNumber(String aadharNumber);

    List<AdminAccountApplication> findByPanNumber(String panNumber);

    boolean existsByAadharNumberAndStatusNot(String aadharNumber, String status);

    boolean existsByPanNumberAndStatusNot(String panNumber, String status);

    boolean existsByPhoneAndStatusNot(String phone, String status);

    @Query("SELECT COUNT(a) FROM AdminAccountApplication a WHERE a.status = :status")
    long countByStatus(@Param("status") String status);

    @Query("SELECT COUNT(a) FROM AdminAccountApplication a WHERE a.accountType = :type")
    long countByAccountType(@Param("type") String type);

    @Query("SELECT a FROM AdminAccountApplication a WHERE a.status IN ('PENDING', 'DOCUMENTS_UPLOADED', 'ADMIN_VERIFIED') ORDER BY a.createdAt DESC")
    List<AdminAccountApplication> findPendingApplications();

    @Query("SELECT a FROM AdminAccountApplication a WHERE a.status = 'ADMIN_VERIFIED' ORDER BY a.createdAt ASC")
    List<AdminAccountApplication> findAwaitingManagerApproval();

    @Query("SELECT a FROM AdminAccountApplication a ORDER BY a.createdAt DESC")
    List<AdminAccountApplication> findAllOrderByCreatedAtDesc();

    @Query("SELECT a FROM AdminAccountApplication a WHERE " +
           "LOWER(a.fullName) LIKE LOWER(CONCAT('%', :term, '%')) OR " +
           "a.applicationNumber LIKE CONCAT('%', :term, '%') OR " +
           "a.aadharNumber LIKE CONCAT('%', :term, '%') OR " +
           "a.panNumber LIKE CONCAT('%', :term, '%') OR " +
           "a.phone LIKE CONCAT('%', :term, '%') OR " +
           "a.accountNumber LIKE CONCAT('%', :term, '%')")
    List<AdminAccountApplication> searchApplications(@Param("term") String term);
}
