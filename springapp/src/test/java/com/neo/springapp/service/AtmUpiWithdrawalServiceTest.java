package com.neo.springapp.service;

import com.neo.springapp.model.Account;
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
}
