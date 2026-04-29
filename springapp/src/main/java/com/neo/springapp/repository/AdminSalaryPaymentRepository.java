package com.neo.springapp.repository;

import com.neo.springapp.model.AdminSalaryPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AdminSalaryPaymentRepository extends JpaRepository<AdminSalaryPayment, Long> {

    Optional<AdminSalaryPayment> findByAdminIdAndSalaryDate(Long adminId, LocalDate salaryDate);

    @Query("SELECT p FROM AdminSalaryPayment p WHERE p.salaryDate BETWEEN :fromDate AND :toDate ORDER BY p.salaryDate DESC, p.paidAt DESC")
    List<AdminSalaryPayment> findBySalaryDateBetween(@Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

    @Query("SELECT p FROM AdminSalaryPayment p WHERE p.adminId = :adminId AND p.salaryDate BETWEEN :fromDate AND :toDate ORDER BY p.salaryDate ASC")
    List<AdminSalaryPayment> findByAdminIdAndSalaryDateBetween(@Param("adminId") Long adminId,
                                                               @Param("fromDate") LocalDate fromDate,
                                                               @Param("toDate") LocalDate toDate);
}

