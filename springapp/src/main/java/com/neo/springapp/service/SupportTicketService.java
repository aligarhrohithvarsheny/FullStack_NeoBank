package com.neo.springapp.service;

import com.neo.springapp.model.SupportTicket;
import com.neo.springapp.repository.SupportTicketRepository;
import com.neo.springapp.model.Transaction;
import com.neo.springapp.repository.TransactionRepository;
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
            Transaction transaction = transactionRepository.findByTransactionId(ticket.getTransactionId().trim())
                    .orElseThrow(() -> new IllegalArgumentException("Transaction ID was not found for this account"));
            if (!accountNumber.equals(transaction.getAccountNumber())) {
                throw new IllegalArgumentException("You can only raise a ticket for your own transaction");
            }
            copyTransactionDetails(ticket, transaction);
            ticket.setTransactionId(transaction.getTransactionId());
        }
        return supportTicketRepository.save(ticket);
    }

    public Transaction getOwnTransaction(String accountNumber, String transactionId) {
        if (accountNumber == null || transactionId == null || transactionId.isBlank()) return null;
        return transactionRepository.findByTransactionId(transactionId.trim())
                .filter(transaction -> accountNumber.equals(transaction.getAccountNumber()))
                .orElse(null);
    }

    private void copyTransactionDetails(SupportTicket ticket, Transaction transaction) {
        ticket.setTransactionAccountNumber(transaction.getAccountNumber());
        ticket.setTransactionAmount(transaction.getAmount());
        ticket.setTransactionType(transaction.getType());
        ticket.setTransactionStatus(transaction.getStatus());
        ticket.setTransactionDate(transaction.getDate() == null ? null : transaction.getDate().toString());
        ticket.setTransactionDescription(transaction.getDescription());
    }

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
}
