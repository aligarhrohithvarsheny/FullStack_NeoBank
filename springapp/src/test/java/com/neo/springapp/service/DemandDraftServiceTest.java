package com.neo.springapp.service;

import com.neo.springapp.model.Account;
import com.neo.springapp.model.DemandDraft;
import com.neo.springapp.model.PositivePayRequest;
import com.neo.springapp.model.PositivePayStatus;
import com.neo.springapp.model.SavingsChequeRequest;
import com.neo.springapp.repository.AccountRepository;
import com.neo.springapp.repository.ChequeRepository;
import com.neo.springapp.repository.DemandDraftRepository;
import com.neo.springapp.repository.PositivePayRequestRepository;
import com.neo.springapp.repository.SavingsChequeRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DemandDraftServiceTest {
    @Mock private DemandDraftRepository draftRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private TransactionService transactionService;
    @Mock private ChequeRepository chequeRepository;
    @Mock private SavingsChequeRequestRepository savingsChequeRepository;
    @Mock private PositivePayRequestRepository positivePayRequestRepository;
    @Mock private AccountService accountService;

    private DemandDraftService service;
    private Account account;

    @BeforeEach
    void setUp() {
        service = new DemandDraftService(draftRepository, accountRepository, transactionService, chequeRepository,
                savingsChequeRepository, positivePayRequestRepository, accountService);
        account = new Account();
        account.setId(81L);
        account.setAccountNumber("SAV-1001");
        account.setBalance(10000.0);
        when(accountRepository.findByAccountNumber("SAV-1001")).thenReturn(account);
    }

    @Test
    void verifiesNewSavingsChequeNumbersOwnedByTheAccount() {
        SavingsChequeRequest draw = new SavingsChequeRequest();
        draw.setAccountId(account.getId());
        draw.setChequeNumber("SCHQ-81-0001");
        draw.setStatus("AWAITING_POSITIVE_PAY");
        when(savingsChequeRepository.findAllByChequeNumber("SCHQ-81-0001")).thenReturn(List.of(draw));

        assertSame(account, service.verifyCheque("SAV-1001", " SCHQ-81-0001 "));
    }

    @Test
    void rejectsSavingsChequeNumbersOwnedByAnotherAccount() {
        SavingsChequeRequest draw = new SavingsChequeRequest();
        draw.setAccountId(82L);
        draw.setChequeNumber("SCHQ-82-0001");
        draw.setStatus("APPROVED");
        when(savingsChequeRepository.findAllByChequeNumber("SCHQ-82-0001")).thenReturn(List.of(draw));

        assertNull(service.verifyCheque("SAV-1001", "SCHQ-82-0001"));
    }

    @Test
    void rejectsSavingsChequeNumbersThatAreNoLongerAvailable() {
        SavingsChequeRequest draw = new SavingsChequeRequest();
        draw.setAccountId(account.getId());
        draw.setChequeNumber("SCHQ-81-0002");
        draw.setStatus("REJECTED");
        when(savingsChequeRepository.findAllByChequeNumber("SCHQ-81-0002")).thenReturn(List.of(draw));

        assertNull(service.verifyCheque("SAV-1001", "SCHQ-81-0002"));
    }

    @Test
    void createsDemandDraftFromAnAiPassedSavingsChequeNumber() {
        SavingsChequeRequest draw = new SavingsChequeRequest();
        draw.setAccountId(account.getId());
        draw.setChequeNumber("SCHQ-81-0003");
        draw.setStatus("PENDING");
        when(savingsChequeRepository.findAllByChequeNumber("SCHQ-81-0003")).thenReturn(List.of(draw));
        when(draftRepository.findByChequeNumberAndAccountNumber("SCHQ-81-0003", "SAV-1001"))
                .thenReturn(Optional.empty());
        PositivePayRequest registration = new PositivePayRequest();
        registration.setStatus(PositivePayStatus.APPROVED);
        registration.setAmount(new BigDecimal("500.00"));
        registration.setPayeeName("Payee");
        when(positivePayRequestRepository.findFirstByAccountNumberAndChequeNumberAndStatusIn(
                org.mockito.ArgumentMatchers.eq("SAV-1001"),
                org.mockito.ArgumentMatchers.eq("SCHQ-81-0003"),
                org.mockito.ArgumentMatchers.anyList())).thenReturn(Optional.of(registration));
        when(draftRepository.save(any(DemandDraft.class))).thenAnswer(invocation -> invocation.getArgument(0));
        DemandDraft request = new DemandDraft();
        request.setChequeNumber("SCHQ-81-0003");
        request.setPayeeName("Payee");
        request.setPayeeAccountNumber("SAV-2002");
        request.setAmount(new BigDecimal("500.00"));
        request.setDraftDate(LocalDate.now());

        DemandDraft saved = service.create("SAV-1001", request);

        assertEquals("SCHQ-81-0003", saved.getChequeNumber());
        assertEquals("PENDING", saved.getStatus());
        assertEquals("SAV-1001", saved.getAccountNumber());
    }
}
