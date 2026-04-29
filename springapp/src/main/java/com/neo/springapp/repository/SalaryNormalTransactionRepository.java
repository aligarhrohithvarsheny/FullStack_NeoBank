package com.neo.springapp.repository;

import com.neo.springapp.model.SalaryNormalTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SalaryNormalTransactionRepository extends JpaRepository<SalaryNormalTransaction, Long> {

    List<SalaryNormalTransaction> findBySalaryAccountIdOrderByCreatedAtDesc(Long salaryAccountId);

    List<SalaryNormalTransaction> findByAccountNumberOrderByCreatedAtDesc(String accountNumber);

    List<SalaryNormalTransaction> findByTypeOrderByCreatedAtDesc(String type);

    @Query("SELECT SUM(t.amount) FROM SalaryNormalTransaction t WHERE t.salaryAccountId = :accountId AND t.type = :type")
    Double sumAmountByAccountAndType(@Param("accountId") Long accountId, @Param("type") String type);

    @Query("SELECT SUM(t.charge) FROM SalaryNormalTransaction t WHERE t.salaryAccountId = :accountId")
    Double totalChargesByAccount(@Param("accountId") Long accountId);

    @Query("SELECT t FROM SalaryNormalTransaction t WHERE t.salaryAccountId = :accountId AND t.createdAt >= :since ORDER BY t.createdAt DESC")
    List<SalaryNormalTransaction> findRecentByAccount(@Param("accountId") Long accountId, @Param("since") LocalDateTime since);
}
