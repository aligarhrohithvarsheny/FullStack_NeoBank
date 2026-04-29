package com.neo.springapp.repository;

import com.neo.springapp.model.SalaryAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SalaryAccountRepository extends JpaRepository<SalaryAccount, Long> {

    SalaryAccount findByAccountNumber(String accountNumber);

    SalaryAccount findByCustomerId(String customerId);

    SalaryAccount findByAadharNumber(String aadharNumber);

    SalaryAccount findByPanNumber(String panNumber);

    List<SalaryAccount> findByStatus(String status);

    List<SalaryAccount> findByCompanyName(String companyName);

    List<SalaryAccount> findByCompanyId(String companyId);

    @Query("SELECT sa FROM SalaryAccount sa WHERE " +
           "LOWER(sa.employeeName) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(sa.companyName) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(sa.accountNumber) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(sa.designation) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<SalaryAccount> search(@Param("q") String query);

    @Query("SELECT COUNT(sa) FROM SalaryAccount sa WHERE sa.status = :status")
    Long countByStatus(@Param("status") String status);

    @Query("SELECT sa.companyName, COUNT(sa) FROM SalaryAccount sa GROUP BY sa.companyName")
    List<Object[]> countByCompany();

    @Query("SELECT SUM(sa.monthlySalary) FROM SalaryAccount sa WHERE sa.status = 'Active'")
    Double totalActiveMonthlySalary();

    SalaryAccount findByEmployeeId(String employeeId);

    SalaryAccount findByEmail(String email);

    SalaryAccount findByDebitCardNumber(String debitCardNumber);

    SalaryAccount findByUpiId(String upiId);

    List<SalaryAccount> findByAccountLockedTrue();
}
