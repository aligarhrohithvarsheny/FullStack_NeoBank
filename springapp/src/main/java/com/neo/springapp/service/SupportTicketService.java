package com.neo.springapp.service;

import com.neo.springapp.model.BillPayment;
import com.neo.springapp.model.CurrentAccountUpiPayment;
import com.neo.springapp.model.EmiPayment;
import com.neo.springapp.model.SavingsUpiTransaction;
import com.neo.springapp.model.SupportTicket;
import com.neo.springapp.model.Transaction;
import com.neo.springapp.model.TransferRecord;
import com.neo.springapp.repository.BillPaymentRepository;
import com.neo.springapp.repository.CurrentAccountUpiPaymentRepository;
import com.neo.springapp.repository.EmiPaymentRepository;
import com.neo.springapp.repository.SavingsUpiTransactionRepository;
import com.neo.springapp.repository.SupportTicketRepository;
import com.neo.springapp.repository.TransactionRepository;
import com.neo.springapp.repository.TransferRepository;
import com.neo.springapp.model.User;
import com.neo.springapp.repository.UserRepository;
import com.neo.springapp.service.UserSessionTokenService.SessionPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@SuppressWarnings("null")
public class SupportTicketService {

    @Autowired
    private SupportTicketRepository supportTicketRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private BillPaymentRepository billPaymentRepository;

    @Autowired
    private SavingsUpiTransactionRepository savingsUpiTransactionRepository;

    @Autowired
    private CurrentAccountUpiPaymentRepository currentAccountUpiPaymentRepository;

    @Autowired
    private TransferRepository transferRepository;

    @Autowired
    private EmiPaymentRepository emiPaymentRepository;

    @Autowired
    private UserRepository userRepository;

    public SupportTicket createTicket(SupportTicket ticket, SessionPrincipal principal) {
        User user = userRepository.findById(principal.userId())
                .orElseThrow(() -> new IllegalArgumentException("Signed-in user was not found"));
        String accountNumber = principal.accountNumber();
        if (accountNumber == null || accountNumber.isBlank()
                || !accountNumber.equals(user.getAccountNumber())) {
            throw new IllegalArgumentException("Signed-in account could not be verified");
        }
        ticket.setAccountNumber(accountNumber);
        ticket.setUserName(user.getName() == null || user.getName().isBlank()
                ? user.getUsername() : user.getName());
        ticket.setUserEmail(user.getEmail());
        ticket.setTicketId("TKT-" + System.currentTimeMillis());
        ticket.setStatus("OPEN");
        ticket.setAdminResponse(null);
        ticket.setAssignedTo(null);
        ticket.setCreatedAt(LocalDateTime.now());
        ticket.setUpdatedAt(LocalDateTime.now());
        ticket.setResolvedAt(null);
        ticket.setClosedAt(null);
        ticket.setTransactionAccountNumber(null);
        ticket.setTransactionAmount(null);
        ticket.setTransactionType(null);
        ticket.setTransactionStatus(null);
        ticket.setTransactionDate(null);
        ticket.setTransactionDescription(null);

        if (ticket.getTransactionId() != null && !ticket.getTransactionId().isBlank()) {
            VerifiedTransaction transaction = getOwnTransaction(accountNumber, ticket.getTransactionId())
                    .orElseThrow(() -> new IllegalArgumentException("Transaction ID was not found for this account"));
            copyTransactionDetails(ticket, transaction);
            ticket.setTransactionId(transaction.transactionId());
        }
        return supportTicketRepository.save(ticket);
    }

    public Optional<VerifiedTransaction> getOwnTransaction(String accountNumber, String transactionId) {
        if (accountNumber == null || accountNumber.isBlank() || transactionId == null || transactionId.isBlank()) {
            return Optional.empty();
        }
        String id = transactionId.trim();

        Optional<Transaction> transaction = transactionRepository.findByTransactionId(id)
                .filter(item -> accountNumber.equals(item.getAccountNumber()));
        if (transaction.isEmpty()) {
            try {
                transaction = transactionRepository.findByGlobalTransactionSequence(Long.valueOf(id))
                        .filter(item -> accountNumber.equals(item.getAccountNumber()));
            } catch (NumberFormatException ignored) {
                // Non-numeric transaction IDs do not have a global sequence representation.
            }
        }
        if (transaction.isPresent()) {
            return transaction.map(item -> new VerifiedTransaction(
                    item.getTransactionId() == null ? id : item.getTransactionId(),
                    item.getAccountNumber(),
                    item.getAmount(),
                    item.getType(),
                    item.getStatus(),
                    item.getDate(),
                    item.getDescription() == null ? item.getMerchant() : item.getDescription()));
        }

        Optional<BillPayment> billPayment = billPaymentRepository.findByTransactionId(id)
                .filter(item -> accountNumber.equals(item.getAccountNumber()));
        if (billPayment.isPresent()) {
            BillPayment item = billPayment.get();
            return Optional.of(new VerifiedTransaction(
                    item.getTransactionId(),
                    item.getAccountNumber(),
                    item.getAmount(),
                    item.getBillType() == null ? "BILL PAYMENT" : item.getBillType(),
                    item.getStatus(),
                    item.getPaymentDate(),
                    item.getDescription()));
        }

        Optional<SavingsUpiTransaction> savingsUpi = savingsUpiTransactionRepository.findByTransactionRef(id)
                .filter(item -> accountNumber.equals(item.getSenderAccount())
                        || accountNumber.equals(item.getReceiverAccount()));
        if (savingsUpi.isPresent()) {
            SavingsUpiTransaction item = savingsUpi.get();
            boolean sent = accountNumber.equals(item.getSenderAccount());
            return Optional.of(new VerifiedTransaction(
                    item.getTransactionRef(),
                    accountNumber,
                    item.getAmount() == null ? null : item.getAmount().doubleValue(),
                    sent ? "UPI DEBIT" : "UPI CREDIT",
                    item.getStatus(),
                    item.getCreatedAt(),
                    item.getRemark()));
        }

        Optional<CurrentAccountUpiPayment> currentUpi = currentAccountUpiPaymentRepository.findByTxnId(id)
                .filter(item -> accountNumber.equals(item.getAccountNumber()));
        if (currentUpi.isPresent()) {
            CurrentAccountUpiPayment item = currentUpi.get();
            return Optional.of(new VerifiedTransaction(
                    item.getTxnId(),
                    item.getAccountNumber(),
                    item.getAmount(),
                    "UPI " + item.getTxnType(),
                    item.getStatus(),
                    item.getCreatedAt(),
                    item.getNote()));
        }

        Optional<TransferRecord> transfer = transferRepository.findByTransferId(id)
                .filter(item -> accountNumber.equals(item.getSenderAccountNumber())
                        || accountNumber.equals(item.getRecipientAccountNumber()));
        if (transfer.isEmpty()) {
            try {
                transfer = transferRepository.findByGlobalTransactionSequence(Long.valueOf(id))
                        .filter(item -> accountNumber.equals(item.getSenderAccountNumber())
                                || accountNumber.equals(item.getRecipientAccountNumber()));
            } catch (NumberFormatException ignored) {
                // Non-numeric transaction IDs do not have a global sequence representation.
            }
        }
        if (transfer.isPresent()) {
            TransferRecord item = transfer.get();
            boolean sent = accountNumber.equals(item.getSenderAccountNumber());
            return Optional.of(new VerifiedTransaction(
                    item.getTransferId() == null ? id : item.getTransferId(),
                    accountNumber,
                    item.getAmount(),
                    "FUND TRANSFER " + (item.getTransferType() == null ? "" : item.getTransferType().name()),
                    item.getStatus(),
                    item.getDate(),
                    sent ? "Transfer to " + item.getRecipientName() : "Transfer from " + item.getSenderName()));
        }

        Optional<EmiPayment> emi = emiPaymentRepository.findByTransactionId(id).stream()
                .filter(item -> accountNumber.equals(item.getAccountNumber()))
                .findFirst();
        if (emi.isEmpty()) {
            try {
                emi = emiPaymentRepository.findById(Long.valueOf(id))
                        .filter(item -> accountNumber.equals(item.getAccountNumber()));
            } catch (NumberFormatException ignored) {
                // Non-numeric transaction IDs are not EMI database IDs.
            }
        }
        return emi.map(item -> new VerifiedTransaction(
                id,
                item.getAccountNumber(),
                item.getTotalAmount(),
                "LOAN EMI",
                item.getStatus(),
                item.getPaymentDate() != null ? item.getPaymentDate() : item.getDueDate().atStartOfDay(),
                "EMI " + item.getEmiNumber() + " for loan account " + item.getLoanAccountNumber()));
    }

    private void copyTransactionDetails(SupportTicket ticket, VerifiedTransaction transaction) {
        ticket.setTransactionAccountNumber(transaction.accountNumber());
        ticket.setTransactionAmount(transaction.amount());
        ticket.setTransactionType(transaction.type());
        ticket.setTransactionStatus(transaction.status());
        ticket.setTransactionDate(transaction.date() == null ? null : transaction.date().toString());
        ticket.setTransactionDescription(transaction.description());
    }

    public record VerifiedTransaction(
            String transactionId,
            String accountNumber,
            Double amount,
            String type,
            String status,
            LocalDateTime date,
            String description) {}

    public List<SupportTicket> getTicketsByAccountNumber(String accountNumber) {
        return supportTicketRepository.findByAccountNumberOrderByCreatedAtDesc(accountNumber);
    }

    public Optional<SupportTicket> getTicketById(Long id) {
        return supportTicketRepository.findById(id);
    }

    public Optional<SupportTicket> getTicketByTicketId(String ticketId) {
        return supportTicketRepository.findByTicketId(ticketId);
    }

    public Optional<SupportTicket> getTicketByIdAndAccountNumber(Long id, String accountNumber) {
        return supportTicketRepository.findByIdAndAccountNumber(id, accountNumber);
    }

    public Optional<SupportTicket> getTicketByTicketIdAndAccountNumber(String ticketId, String accountNumber) {
        return supportTicketRepository.findByTicketIdAndAccountNumber(ticketId, accountNumber);
    }

    public List<SupportTicket> getTicketsByStatus(String status) {
        return supportTicketRepository.findByStatus(status);
    }

    public List<SupportTicket> getAllTickets() {
        return supportTicketRepository.findAll();
    }

    public SupportTicket updateTicketStatus(Long id, String status, String adminResponse) {
        Optional<SupportTicket> opt = supportTicketRepository.findById(id);
        if (opt.isPresent()) {
            SupportTicket ticket = opt.get();
            ticket.setStatus(status);
            if (adminResponse != null) {
                ticket.setAdminResponse(adminResponse);
            }
            if ("RESOLVED".equals(status)) {
                ticket.setResolvedAt(LocalDateTime.now());
            } else if ("CLOSED".equals(status)) {
                ticket.setClosedAt(LocalDateTime.now());
            }
            return supportTicketRepository.save(ticket);
        }
        return null;
    }

    public SupportTicket assignTicket(Long id, String assignedTo) {
        Optional<SupportTicket> opt = supportTicketRepository.findById(id);
        if (opt.isPresent()) {
            SupportTicket ticket = opt.get();
            ticket.setAssignedTo(assignedTo);
            ticket.setStatus("IN_PROGRESS");
            return supportTicketRepository.save(ticket);
        }
        return null;
    }

    public long getOpenTicketsCount() {
        return supportTicketRepository.countByStatus("OPEN");
    }

    public long getInProgressCount() {
        return supportTicketRepository.countByStatus("IN_PROGRESS");
    }

    public long getResolvedCount() {
        return supportTicketRepository.countByStatus("RESOLVED");
    }

    public long getTotalTicketsCount() {
        return supportTicketRepository.count();
    }
}
