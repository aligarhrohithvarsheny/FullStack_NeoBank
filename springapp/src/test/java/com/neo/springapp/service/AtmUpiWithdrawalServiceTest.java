package com.neo.springapp.service;

import com.neo.springapp.model.Account;
import com.neo.springapp.model.AtmMachine;
import com.neo.springapp.model.AtmUpiWithdrawalRequest;
import com.neo.springapp.model.User;
import com.neo.springapp.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AtmUpiWithdrawalServiceTest {

    @Mock private AtmUpiWithdrawalRequestRepository requestRepository;
    @Mock private AtmMachineRepository atmMachineRepository;
    @Mock private AtmTransactionRepository atmTransactionRepository;
    @Mock private UserRepository userRepository;
    @Mock private SalaryAccountRepository salaryAccountRepository;
    @Mock private SavingsUpiTransactionRepository savingsUpiTransactionRepository;
    @Mock private SalaryUpiTransactionRepository salaryUpiTransactionRepository;
    @Mock private AtmManagementService atmManagementService;

    @InjectMocks private AtmUpiWithdrawalService service;

    @Test
    void rejectsAmountsNotInHundredRupeeIncrements() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createRequest("ATM001", "ACC001", "SAVINGS", 550.50));
        verifyNoInteractions(requestRepository, atmMachineRepository, userRepository);
    }

    @Test
    void appliesAdminConfiguredAtmLimitInsteadOfHardcodedUpiLimit() {
        String accountNumber = "ACC001";
        when(userRepository.findByAccountNumber(accountNumber))
                .thenReturn(Optional.of(approvedSavingsUser("acc001@neobank")));
        when(atmMachineRepository.findByAtmId("ATM001")).thenReturn(Optional.of(activeAtm(40_000.0)));
        when(requestRepository.save(any(AtmUpiWithdrawalRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> response = service.createRequest("ATM001", accountNumber, "SAVINGS", 30_000.0);

        assertTrue((Boolean) response.get("success"));
        assertTrue(((String) response.get("qrPayload")).contains("pa=" + "atmwithdrawal@neobank"));
        assertEquals("acc001@neobank", response.get("senderUpiId"));
        assertEquals("atmwithdrawal@neobank", response.get("receiverUpiId"));
        assertEquals("SAVINGS", response.get("accountType"));
        assertEquals("NeoBank ATM", response.get("atmName"));
        assertEquals("Main Street", response.get("atmLocation"));
        verify(requestRepository).save(any(AtmUpiWithdrawalRequest.class));
    }

    @Test
    void rejectsWithdrawalsAboveAdminConfiguredAtmLimit() {
        String accountNumber = "ACC001";
        when(userRepository.findByAccountNumber(accountNumber))
                .thenReturn(Optional.of(approvedSavingsUser("acc001@neobank")));
        when(atmMachineRepository.findByAtmId("ATM001")).thenReturn(Optional.of(activeAtm(20_000.0)));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.createRequest("ATM001", accountNumber, "SAVINGS", 30_000.0));

        assertTrue(error.getMessage().contains("admin-set per-withdrawal limit"));
        verify(requestRepository, never()).save(any(AtmUpiWithdrawalRequest.class));
    }

    @Test
    void rejectsUnverifiedUpiDomainsForAtmWithdrawals() {
        String accountNumber = "ACC001";
        when(userRepository.findByAccountNumber(accountNumber))
                .thenReturn(Optional.of(approvedSavingsUser("acc001@externalbank")));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.createRequest("ATM001", accountNumber, "SAVINGS", 500.0));

        assertTrue(error.getMessage().contains("verified NeoBank UPI ID"));
        verifyNoInteractions(requestRepository, atmMachineRepository);
    }

    @Test
    void doesNotAllowUnverifiedAccountsToOpenAtmQrRequests() {
        String accountNumber = "ACC001";
        AtmUpiWithdrawalRequest request = new AtmUpiWithdrawalRequest();
        request.setRequestToken("request-token");
        request.setAccountNumber(accountNumber);
        request.setAccountType("SAVINGS");
        request.setStatus("PENDING");
        request.setExpiresAt(LocalDateTime.now().plusMinutes(2));

        when(requestRepository.findByRequestToken("request-token")).thenReturn(Optional.of(request));
        when(userRepository.findByAccountNumber(accountNumber))
                .thenReturn(Optional.of(approvedSavingsUser("acc001@externalbank")));

        assertThrows(IllegalArgumentException.class, () -> service.getRequest("request-token", accountNumber));
    }

    @Test
    void incorrectSavingsUpiPinDoesNotStartAtmWithdrawal() {
        String accountNumber = "ACC001";
        AtmUpiWithdrawalRequest request = new AtmUpiWithdrawalRequest();
        request.setRequestToken("request-token");
        request.setAccountNumber(accountNumber);
        request.setAccountType("SAVINGS");
        request.setStatus("PENDING");
        request.setExpiresAt(LocalDateTime.now().plusMinutes(2));
        request.setPinAttempts(0);

        User user = new User();
        user.setStatus("APPROVED");
        user.setUpiEnabled(true);
        user.setTransactionPinSet(true);
        user.setTransactionPin(new BCryptPasswordEncoder(10).encode("123456"));
        user.setUpiId("acc001@neobank");
        user.setAccount(new Account());

        when(requestRepository.findByRequestTokenForUpdate("request-token")).thenReturn(Optional.of(request));
        when(userRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(user));
        when(requestRepository.save(any(AtmUpiWithdrawalRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> response = service.confirmRequest("request-token", accountNumber, "000000");

        assertFalse((Boolean) response.get("success"));
        assertEquals("PENDING", request.getStatus());
        assertEquals(1, request.getPinAttempts());
        verifyNoInteractions(atmManagementService, savingsUpiTransactionRepository, salaryUpiTransactionRepository);
    }

    private User approvedSavingsUser(String upiId) {
        User user = new User();
        user.setStatus("APPROVED");
        user.setUpiEnabled(true);
        user.setTransactionPinSet(true);
        user.setTransactionPin(new BCryptPasswordEncoder(10).encode("123456"));
        user.setUpiId(upiId);
        user.setAccount(new Account());
        return user;
    }

    private AtmMachine activeAtm(Double maxWithdrawalLimit) {
        AtmMachine atm = new AtmMachine();
        atm.setAtmId("ATM001");
        atm.setAtmName("NeoBank ATM");
        atm.setLocation("Main Street");
        atm.setStatus("ACTIVE");
        atm.setMaxWithdrawalLimit(maxWithdrawalLimit);
        atm.setCashAvailable(50_000.0);
        return atm;
    }
}
