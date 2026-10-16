package com.neo.springapp.service;

import com.neo.springapp.model.BillPayment;
import com.neo.springapp.model.EmiPayment;
import com.neo.springapp.model.SavingsUpiTransaction;
import com.neo.springapp.model.Transaction;
import com.neo.springapp.model.TransferRecord;
import com.neo.springapp.repository.BillPaymentRepository;
import com.neo.springapp.repository.CurrentAccountUpiPaymentRepository;
import com.neo.springapp.repository.EmiPaymentRepository;
import com.neo.springapp.repository.SavingsUpiTransactionRepository;
import com.neo.springapp.repository.SupportTicketRepository;
import com.neo.springapp.repository.TransactionRepository;
import com.neo.springapp.repository.TransferRepository;
import com.neo.springapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupportTicketServiceTransactionVerificationTest {

    @Mock private SupportTicketRepository supportTicketRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private BillPaymentRepository billPaymentRepository;
    @Mock private SavingsUpiTransactionRepository savingsUpiTransactionRepository;
    @Mock private CurrentAccountUpiPaymentRepository currentAccountUpiPaymentRepository;
    @Mock private TransferRepository transferRepository;
    @Mock private EmiPaymentRepository emiPaymentRepository;
    @Mock private UserRepository userRepository;

    private SupportTicketService service;

    @BeforeEach
    void setUp() {
        service = new SupportTicketService();
        ReflectionTestUtils.setField(service, "supportTicketRepository", supportTicketRepository);
        ReflectionTestUtils.setField(service, "transactionRepository", transactionRepository);
        ReflectionTestUtils.setField(service, "billPaymentRepository", billPaymentRepository);
        ReflectionTestUtils.setField(service, "savingsUpiTransactionRepository", savingsUpiTransactionRepository);
        ReflectionTestUtils.setField(service, "currentAccountUpiPaymentRepository", currentAccountUpiPaymentRepository);
        ReflectionTestUtils.setField(service, "transferRepository", transferRepository);
        ReflectionTestUtils.setField(service, "emiPaymentRepository", emiPaymentRepository);
        ReflectionTestUtils.setField(service, "userRepository", userRepository);
    }

    @Test
    void verifiesSavingsUpiTransactionOnlyForAnInvolvedAccount() {
        SavingsUpiTransaction upi = new SavingsUpiTransaction();
        upi.setTransactionRef("UPI-123");
        upi.setSenderAccount("ACC-1");
        upi.setReceiverAccount("ACC-2");
        upi.setAmount(java.math.BigDecimal.valueOf(125.50));
        upi.setStatus("SUCCESS");
        upi.setCreatedAt(LocalDateTime.now());
        upi.setRemark("Lunch");
        when(savingsUpiTransactionRepository.findByTransactionRef("UPI-123")).thenReturn(Optional.of(upi));

        Optional<SupportTicketService.VerifiedTransaction> own =
                service.getOwnTransaction("ACC-2", "UPI-123");
        Optional<SupportTicketService.VerifiedTransaction> unrelated =
                service.getOwnTransaction("ACC-3", "UPI-123");

        assertThat(own).isPresent();
        assertThat(own.get().type()).isEqualTo("UPI CREDIT");
        assertThat(own.get().accountNumber()).isEqualTo("ACC-2");
        assertThat(unrelated).isEmpty();
    }

    @Test
    void verifiesTransferByIdOnlyForSenderOrRecipient() {
        TransferRecord transfer = new TransferRecord();
        transfer.setTransferId("TRF-123");
        transfer.setSenderAccountNumber("ACC-1");
        transfer.setRecipientAccountNumber("ACC-2");
        transfer.setAmount(80.0);
        transfer.setStatus("Pending");
        transfer.setDate(LocalDateTime.now());
        when(transferRepository.findByTransferId("TRF-123")).thenReturn(Optional.of(transfer));

        Optional<SupportTicketService.VerifiedTransaction> own =
                service.getOwnTransaction("ACC-1", "TRF-123");
        Optional<SupportTicketService.VerifiedTransaction> unrelated =
                service.getOwnTransaction("ACC-3", "TRF-123");

        assertThat(own).isPresent();
        assertThat(own.get().type()).startsWith("FUND TRANSFER");
        assertThat(own.get().status()).isEqualTo("Pending");
        assertThat(unrelated).isEmpty();
    }

    @Test
    void verifiesPendingEmiByItsDatabaseIdForItsAccount() {
        EmiPayment emi = new EmiPayment();
        emi.setId(42L);
        emi.setAccountNumber("ACC-1");
        emi.setLoanAccountNumber("LOAN-1");
        emi.setEmiNumber(3);
        emi.setTotalAmount(2500.0);
        emi.setStatus("Pending");
        emi.setDueDate(java.time.LocalDate.now());
        when(emiPaymentRepository.findByTransactionId("42")).thenReturn(List.of(emi));

        Optional<SupportTicketService.VerifiedTransaction> own =
                service.getOwnTransaction("ACC-1", "42");
        Optional<SupportTicketService.VerifiedTransaction> unrelated =
                service.getOwnTransaction("ACC-2", "42");

        assertThat(own).isPresent();
        assertThat(own.get().type()).isEqualTo("LOAN EMI");
        assertThat(own.get().status()).isEqualTo("Pending");
        assertThat(unrelated).isEmpty();
    }

    @Test
    void verifiesRegularLedgerTransactionAndDoesNotExposeAnotherAccountsRecord() {
        Transaction transaction = new Transaction();
        transaction.setTransactionId("TXN-123");
        transaction.setAccountNumber("ACC-1");
        transaction.setAmount(12.0);
        transaction.setType("Gold Loan Payment");
        transaction.setStatus("Completed");
        when(transactionRepository.findByTransactionId("TXN-123")).thenReturn(Optional.of(transaction));

        Optional<SupportTicketService.VerifiedTransaction> own =
                service.getOwnTransaction("ACC-1", "TXN-123");
        Optional<SupportTicketService.VerifiedTransaction> unrelated =
                service.getOwnTransaction("ACC-2", "TXN-123");

        assertThat(own).isPresent();
        assertThat(own.get().type()).isEqualTo("Gold Loan Payment");
        assertThat(unrelated).isEmpty();
    }

    @Test
    void verifiesBillPaymentByItsPaymentIdOnlyForItsAccount() {
        BillPayment billPayment = new BillPayment();
        billPayment.setTransactionId("BILL-123");
        billPayment.setAccountNumber("ACC-1");
        billPayment.setBillType("Mobile Bill");
        billPayment.setAmount(45.0);
        billPayment.setStatus("Completed");
        billPayment.setPaymentDate(LocalDateTime.now());
        billPayment.setDescription("Mobile bill payment");
        when(billPaymentRepository.findByTransactionId("BILL-123")).thenReturn(Optional.of(billPayment));

        Optional<SupportTicketService.VerifiedTransaction> own =
                service.getOwnTransaction("ACC-1", "BILL-123");
        Optional<SupportTicketService.VerifiedTransaction> unrelated =
                service.getOwnTransaction("ACC-2", "BILL-123");

        assertThat(own).isPresent();
        assertThat(own.get().type()).isEqualTo("Mobile Bill");
        assertThat(own.get().amount()).isEqualTo(45.0);
        assertThat(unrelated).isEmpty();
    }
}
