package com.neo.springapp.service;

import com.neo.springapp.model.CreditCard;
import com.neo.springapp.repository.*;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CreditCardServicePinTest {

    @Test
    void setPinStoresHashThatCanBeVerified() {
        CreditCardRepository creditCardRepository = mock(CreditCardRepository.class);
        CreditCard card = new CreditCard();
        when(creditCardRepository.findById(1L)).thenReturn(Optional.of(card));

        PasswordService passwordService = new PasswordService();
        CreditCardService service = new CreditCardService(
                creditCardRepository,
                mock(CreditCardTransactionRepository.class),
                mock(CreditCardBillRepository.class),
                mock(AccountService.class),
                mock(TransactionService.class),
                mock(GlobalTransactionIdGenerator.class),
                mock(ChequeRepository.class),
                mock(BranchAccountService.class),
                mock(CreditCardSettingsRepository.class),
                mock(ChequeRequestRepository.class),
                mock(BusinessChequeRequestRepository.class),
                mock(SalaryAccountRepository.class),
                mock(CurrentAccountRepository.class),
                mock(CreditCardEmiPlanRepository.class),
                mock(CreditCardEmiInstallmentRepository.class),
                passwordService);

        assertTrue(service.setPin(1L, "1234"));
        assertNotEquals("1234", card.getPin());
        assertTrue(passwordService.verifyPin("1234", card.getPin()));
        assertTrue(card.isPinSet());
        verify(creditCardRepository).save(card);
    }
}
