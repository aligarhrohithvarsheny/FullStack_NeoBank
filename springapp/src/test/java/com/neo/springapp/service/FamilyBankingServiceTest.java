package com.neo.springapp.service;

import com.neo.springapp.model.JointAccountProfile;
import com.neo.springapp.model.Transaction;
import com.neo.springapp.repository.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FamilyBankingServiceTest {

    private final JointAccountInvitationRepository invitationRepository = mock(JointAccountInvitationRepository.class);
    private final JointTransferApprovalRepository transferRepository = mock(JointTransferApprovalRepository.class);
    private final MinorAccountApplicationRepository minorRepository = mock(MinorAccountApplicationRepository.class);
    private final GuardianLinkRepository guardianRepository = mock(GuardianLinkRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final AccountService accountService = mock(AccountService.class);
    private final JointAccountProfileRepository jointProfileRepository = mock(JointAccountProfileRepository.class);
    private final TransactionService transactionService = mock(TransactionService.class);
    private final FamilyBankingNotificationRepository notificationRepository = mock(FamilyBankingNotificationRepository.class);
    private final FamilyBankingAuditLogRepository auditRepository = mock(FamilyBankingAuditLogRepository.class);
    private final TransactionRepository transactionRepository = mock(TransactionRepository.class);
    private final FamilyBankingService service = new FamilyBankingService(
            invitationRepository, transferRepository, minorRepository, guardianRepository, userRepository,
            accountService, jointProfileRepository, transactionService, notificationRepository, auditRepository,
            transactionRepository);

    @Test
    void adminJointHistoryReturnsEmptyForCustomerWithoutJointAccounts() {
        when(jointProfileRepository.findByPrimaryHolderUserIdOrJointHolderUserId(7L, 7L)).thenReturn(List.of());

        assertEquals(List.of(), service.adminJointHistory(7L));
        verifyNoInteractions(transactionRepository);
    }

    @Test
    void adminJointHistoryLoadsTransactionsForAllJointAccounts() {
        JointAccountProfile profile = new JointAccountProfile();
        profile.setJointAccountNumber("JOINT-123");
        Transaction transaction = new Transaction();
        when(jointProfileRepository.findByPrimaryHolderUserIdOrJointHolderUserId(7L, 7L))
                .thenReturn(List.of(profile));
        when(transactionRepository.findByAccountNumberInOrderByDateDesc(List.of("JOINT-123")))
                .thenReturn(List.of(transaction));

        assertEquals(List.of(transaction), service.adminJointHistory(7L));
        verify(transactionRepository).findByAccountNumberInOrderByDateDesc(List.of("JOINT-123"));
    }
}
