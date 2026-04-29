package com.neo.springapp.controller;

import com.neo.springapp.model.SupportTicket;
import com.neo.springapp.service.SupportTicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/support-tickets")
@CrossOrigin(origins = "*")
public class SupportTicketController {

    @Autowired
    private SupportTicketService supportTicketService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> createTicket(@RequestBody SupportTicket ticket) {
        Map<String, Object> response = new HashMap<>();
        try {
            SupportTicket created = supportTicketService.createTicket(ticket);
            response.put("success", true);
            response.put("message", "Support ticket created successfully");
            response.put("ticket", created);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error creating support ticket: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<Map<String, Object>> getByAccountNumber(@PathVariable String accountNumber) {
        Map<String, Object> response = new HashMap<>();
        List<SupportTicket> tickets = supportTicketService.getTicketsByAccountNumber(accountNumber);
        response.put("success", true);
        response.put("tickets", tickets);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        Optional<SupportTicket> ticket = supportTicketService.getTicketById(id);
        if (ticket.isPresent()) {
            response.put("success", true);
            response.put("ticket", ticket.get());
            return ResponseEntity.ok(response);
        }
        response.put("success", false);
        response.put("message", "Ticket not found");
        return ResponseEntity.badRequest().body(response);
    }

    @GetMapping("/ticket/{ticketId}")
    public ResponseEntity<Map<String, Object>> getByTicketId(@PathVariable String ticketId) {
        Map<String, Object> response = new HashMap<>();
        Optional<SupportTicket> ticket = supportTicketService.getTicketByTicketId(ticketId);
        if (ticket.isPresent()) {
            response.put("success", true);
            response.put("ticket", ticket.get());
            return ResponseEntity.ok(response);
        }
        response.put("success", false);
        response.put("message", "Ticket not found");
        return ResponseEntity.badRequest().body(response);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<Map<String, Object>> getByStatus(@PathVariable String status) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("tickets", supportTicketService.getTicketsByStatus(status));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAll() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("tickets", supportTicketService.getAllTickets());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Map<String, Object> response = new HashMap<>();
        String status = body.get("status");
        String adminResponse = body.get("adminResponse");
        SupportTicket updated = supportTicketService.updateTicketStatus(id, status, adminResponse);
        if (updated != null) {
            response.put("success", true);
            response.put("message", "Ticket updated successfully");
            response.put("ticket", updated);
            return ResponseEntity.ok(response);
        }
        response.put("success", false);
        response.put("message", "Ticket not found");
        return ResponseEntity.badRequest().body(response);
    }

    @PutMapping("/{id}/assign")
    public ResponseEntity<Map<String, Object>> assignTicket(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Map<String, Object> response = new HashMap<>();
        String assignedTo = body.get("assignedTo");
        SupportTicket updated = supportTicketService.assignTicket(id, assignedTo);
        if (updated != null) {
            response.put("success", true);
            response.put("message", "Ticket assigned successfully");
            response.put("ticket", updated);
            return ResponseEntity.ok(response);
        }
        response.put("success", false);
        response.put("message", "Ticket not found");
        return ResponseEntity.badRequest().body(response);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("openCount", supportTicketService.getOpenTicketsCount());
        response.put("inProgressCount", supportTicketService.getInProgressCount());
        response.put("resolvedCount", supportTicketService.getResolvedCount());
        response.put("totalCount", supportTicketService.getAllTickets().size());
        return ResponseEntity.ok(response);
    }
}
