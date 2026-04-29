package com.neo.springapp.service;

import com.neo.springapp.model.SupportTicket;
import com.neo.springapp.repository.SupportTicketRepository;
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

    public SupportTicket createTicket(SupportTicket ticket) {
        ticket.setTicketId("TKT-" + System.currentTimeMillis());
        ticket.setStatus("OPEN");
        ticket.setCreatedAt(LocalDateTime.now());
        return supportTicketRepository.save(ticket);
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
