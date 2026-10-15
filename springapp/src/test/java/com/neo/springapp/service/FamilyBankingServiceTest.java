package com.neo.springapp.service;

import com.neo.springapp.model.JointAccountProfile;
import com.neo.springapp.model.Account;
import com.neo.springapp.model.MinorAccountApplication;
import com.neo.springapp.model.User;
import com.neo.springapp.model.Transaction;
import com.neo.springapp.repository.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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

    @Test
    void adminCreatesMinorSavingsAccountWithoutPanAndLinksItToFathersAccount() {
        Account fatherAccount = new Account();
        fatherAccount.setId(5L);
        fatherAccount.setAccountNumber("NEO-100");
        fatherAccount.setAccountType("Savings");
        fatherAccount.setStatus("ACTIVE");
        User father = new User();
        father.setId(7L);
        father.setAccount(fatherAccount);
        when(accountService.getAccountByNumber("NEO-100")).thenReturn(fatherAccount);
        when(userRepository.findByAccount_AccountNumber("NEO-100")).thenReturn(Optional.of(father));
        when(accountService.generateUniqueAccountNumberForNewAccount()).thenReturn("MINOR-100");
        when(accountService.generateCustomerIdForAccount(any(Account.class))).thenReturn("123456789");
        when(accountService.saveAccount(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(minorRepository.save(any(MinorAccountApplication.class))).thenAnswer(invocation -> {
            MinorAccountApplication application = invocation.getArgument(0);
            application.setId(42L);
            return application;
        });
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User child = invocation.getArgument(0);
            child.setId(9L);
            return child;
        });

        MinorAccountApplication result = service.createMinorAccount("admin@neobank.local", "Asha",
                LocalDate.now().minusYears(10), "NEO-100", "123456789012", "9876543210",
                "Neo City", "Form 60 declaration received", false);

        assertEquals(7L, result.getGuardianUserId());
        assertEquals("MINOR-100", result.getAssignedAccountNumber());
        assertEquals("INACTIVE", result.getAccountStatus());
        assertEquals("ACTIVE", result.getStatus());
        assertEquals(5000.0, result.getMonthlyLimit());
        assertEquals(2000.0, result.getDailyLimit());
        org.mockito.ArgumentCaptor<Account> accountCaptor = org.mockito.ArgumentCaptor.forClass(Account.class);
        verify(accountService).saveAccount(accountCaptor.capture());
        Account minorAccount = accountCaptor.getValue();
        assertNull(minorAccount.getPan());
        assertEquals("Form 60 declaration received", minorAccount.getForm60());
        assertEquals(5L, minorAccount.getParentAccountId());
        assertTrue(minorAccount.isChildAccount());
        assertEquals("INACTIVE", minorAccount.getStatus());
        assertTrue(minorAccount.getAge() < 18);
        verify(guardianRepository).save(any());
    }

    @Test
    void adminCanFetchFatherSavingsAccountDetailsBeforeLinking() {
        Account fatherAccount = new Account();
        fatherAccount.setAccountNumber("NEO-100");
        fatherAccount.setName("Ravi Kumar");
        fatherAccount.setAccountType("Savings");
        fatherAccount.setStatus("ACTIVE");
        fatherAccount.setPhone("9876543210");
        fatherAccount.setCustomerId("123456789");
        User father = new User();
        father.setId(7L);
        father.setEmail("ravi@example.com");
        father.setAccount(fatherAccount);
        when(accountService.getAccountByNumber("NEO-100")).thenReturn(fatherAccount);
        when(userRepository.findByAccount_AccountNumber("NEO-100")).thenReturn(Optional.of(father));

        FamilyBankingService.FatherSavingsAccountDetails details =
                service.lookupFatherSavingsAccount(" NEO-100 ");

        assertEquals("NEO-100", details.accountNumber());
        assertEquals("Ravi Kumar", details.name());
        assertEquals("Savings", details.accountType());
        assertEquals("ravi@example.com", details.email());
        assertEquals("ACTIVE", details.status());
    }

    @Test
    void adminCannotCreateMinorAccountForAdultOrWithoutForm60() {
        LocalDate adultDob = LocalDate.now().minusYears(18);
        assertThrows(IllegalArgumentException.class, () -> service.createMinorAccount("admin@neobank.local",
                "Asha", adultDob, "NEO-100", "123456789012", "9876543210",
                null, "Form 60", true));
        assertThrows(IllegalArgumentException.class, () -> service.createMinorAccount("admin@neobank.local",
                "Asha", LocalDate.now().minusYears(10), "NEO-100", "123456789012",
                "9876543210", null, " ", true));
        verifyNoInteractions(accountService, minorRepository);
    }
}
