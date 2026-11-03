package com.neo.springapp.repository;

import com.neo.springapp.model.LoanAgreement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LoanAgreementRepository extends JpaRepository<LoanAgreement, Long> {
    Optional<LoanAgreement> findByShareTokenHash(String shareTokenHash);
    Optional<LoanAgreement> findTopByLoanTypeAndLoanIdOrderByIssuedAtDesc(String loanType, Long loanId);
    List<LoanAgreement> findAllByLoanTypeAndLoanIdOrderByIssuedAtDesc(String loanType, Long loanId);
    List<LoanAgreement> findAllByOrderByIssuedAtDesc();
    List<LoanAgreement> findAllByAccountNumberIgnoreCaseOrderByIssuedAtDesc(String accountNumber);
}
