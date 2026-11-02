package com.neo.springapp.service;

import com.neo.springapp.model.GoldLoan;
import com.neo.springapp.model.LoanAgreement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoldLoanApprovalServiceTest {

    @Mock
    private GoldLoanService goldLoanService;

    @Mock
    private LoanAgreementService loanAgreementService;

    private GoldLoanApprovalService approvalService;

    @BeforeEach
    void setUp() {
        approvalService = new GoldLoanApprovalService(goldLoanService, loanAgreementService);
    }

    @Test
    void disbursesOnlyAfterSignedAwaitingLoanAndApprovesAgreement() {
        GoldLoan loan = new GoldLoan();
        loan.setStatus("Awaiting Signature");
        LoanAgreement agreement = new LoanAgreement();
        LocalDateTime signedAt = LocalDateTime.now();
        agreement.setSignedAt(signedAt);
        Map<String, Object> context = Map.of("allocationId", 12L, "performedByAdminId", 7L);

        when(loanAgreementService.requireSigned(41L, "GOLD_LOAN", 17L)).thenReturn(agreement);
        when(goldLoanService.getGoldLoanById(17L)).thenReturn(loan);
        when(loanAgreementService.approvalContext(41L)).thenReturn(context);
        when(goldLoanService.approveGoldLoan(17L, "Approved", "Admin", context)).thenReturn(loan);

        GoldLoanApprovalService.FinalApprovalResult result =
                approvalService.finalApprove(17L, 41L, "Admin");

        assertThat(result.goldLoan()).isSameAs(loan);
        assertThat(result.agreementSignedAt()).isEqualTo(signedAt);
        verify(loanAgreementService).markApproved(41L, "Admin");
    }

    @Test
    void doesNotDisburseLoanThatIsNotAwaitingSignature() {
        LoanAgreement agreement = new LoanAgreement();
        GoldLoan loan = new GoldLoan();
        loan.setStatus("Approved");
        when(loanAgreementService.requireSigned(41L, "GOLD_LOAN", 17L)).thenReturn(agreement);
        when(goldLoanService.getGoldLoanById(17L)).thenReturn(loan);

        assertThatThrownBy(() -> approvalService.finalApprove(17L, 41L, "Admin"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("awaiting signature");

        verify(goldLoanService, never()).approveGoldLoan(
                org.mockito.ArgumentMatchers.eq(17L),
                org.mockito.ArgumentMatchers.eq("Approved"),
                org.mockito.ArgumentMatchers.eq("Admin"),
                org.mockito.ArgumentMatchers.anyMap());
        verify(loanAgreementService, never()).markApproved(41L, "Admin");
    }
}
