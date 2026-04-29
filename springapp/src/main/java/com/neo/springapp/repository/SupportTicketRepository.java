package com.neo.springapp.repository;

import com.neo.springapp.model.SupportTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {

    List<SupportTicket> findByAccountNumber(String accountNumber);

    List<SupportTicket> findByAccountNumberOrderByCreatedAtDesc(String accountNumber);

    Optional<SupportTicket> findByTicketId(String ticketId);

    List<SupportTicket> findByStatus(String status);

    List<SupportTicket> findByCategory(String category);

    List<SupportTicket> findByAssignedTo(String assignedTo);

    List<SupportTicket> findByPriority(String priority);

    long countByStatus(String status);
}
