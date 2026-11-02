package com.neo.springapp.service;

import com.neo.springapp.model.GoldLoan;
import com.neo.springapp.model.LoanAgreement;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class GoldLoanApprovalService {

    private final GoldLoanService goldLoanService;
    private final LoanAgreementService loanAgreementService;

    public GoldLoanApprovalService(
            GoldLoanService goldLoanService,
            LoanAgreementService loanAgreementService) {
        this.goldLoanService = goldLoanService;
        this.loanAgreementService = loanAgreementService;
    }

    @Transactional
    public FinalApprovalResult finalApprove(Long loanId, Long agreementId, String approvedBy) {
        LoanAgreement agreement = loanAgreementService.requireSigned(agreementId, "GOLD_LOAN", loanId);
        GoldLoan loan = goldLoanService.getGoldLoanById(loanId);
        if (loan == null) {
            return null;
        }
        if (!"Awaiting Signature".equals(loan.getStatus())) {
            throw new IllegalStateException("Gold loan must be awaiting signature before final approval.");
        }

        Map<String, Object> approvalContext = loanAgreementService.approvalContext(agreementId);
        GoldLoan approvedLoan = goldLoanService.approveGoldLoan(
                loanId, "Approved", approvedBy, approvalContext);
        if (approvedLoan == null) {
            return null;
        }
        loanAgreementService.markApproved(agreementId, approvedBy);
        return new FinalApprovalResult(approvedLoan, agreement.getSignedAt());
    }

    public record FinalApprovalResult(GoldLoan goldLoan, LocalDateTime agreementSignedAt) {}
}
