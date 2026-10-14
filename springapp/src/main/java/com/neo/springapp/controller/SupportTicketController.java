package com.neo.springapp.controller;

import com.neo.springapp.model.SupportTicket;
import com.neo.springapp.service.SupportTicketService;
import com.neo.springapp.service.UserSessionTokenService.SessionPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public ResponseEntity<Map<String, Object>> createTicket(
            @RequestBody SupportTicket ticket,
            @AuthenticationPrincipal SessionPrincipal principal) {
        if (principal == null) return unauthorized();
        Map<String, Object> response = new HashMap<>();
        try {
            SupportTicket created = supportTicketService.createTicket(ticket, principal);
            response.put("success", true);
            response.put("message", "Support ticket created successfully");
            response.put("ticket", created);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/verify-transaction")
    public ResponseEntity<Map<String, Object>> verifyTransaction(
            @RequestParam String transactionId,
            @AuthenticationPrincipal SessionPrincipal principal) {
        if (principal == null) return unauthorized();
        Optional<SupportTicketService.VerifiedTransaction> transaction =
                supportTicketService.getOwnTransaction(principal.accountNumber(), transactionId);
        if (transaction.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false, "message", "No matching transaction was found for your signed-in account"));
        }
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("transaction", transaction.get());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<Map<String, Object>> getByAccountNumber(
            @PathVariable String accountNumber,
            @AuthenticationPrincipal SessionPrincipal principal) {
        if (principal == null) return unauthorized();
        if (!principal.accountNumber().equals(accountNumber)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "success", false, "message", "You can only view your own support requests"));
        }
        return getOwnTickets(principal);
    }

    @GetMapping("/mine")
    public ResponseEntity<Map<String, Object>> getMine(
            @AuthenticationPrincipal SessionPrincipal principal) {
        if (principal == null) return unauthorized();
        return getOwnTickets(principal);
    }

    private ResponseEntity<Map<String, Object>> getOwnTickets(SessionPrincipal principal) {
        Map<String, Object> response = new HashMap<>();
        List<SupportTicket> tickets = supportTicketService.getTicketsByAccountNumber(principal.accountNumber());
        response.put("success", true);
        response.put("tickets", tickets);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal SessionPrincipal principal) {
        if (principal == null) return unauthorized();
        Map<String, Object> response = new HashMap<>();
        Optional<SupportTicket> ticket = supportTicketService.getTicketByIdAndAccountNumber(id, principal.accountNumber());
        if (ticket.isPresent()) {
            response.put("success", true);
            response.put("ticket", ticket.get());
            return ResponseEntity.ok(response);
        }
        response.put("success", false);
        response.put("message", "Ticket not found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @GetMapping("/ticket/{ticketId}")
    public ResponseEntity<Map<String, Object>> getByTicketId(
            @PathVariable String ticketId,
            @AuthenticationPrincipal SessionPrincipal principal) {
        if (principal == null) return unauthorized();
        Map<String, Object> response = new HashMap<>();
        Optional<SupportTicket> ticket = supportTicketService.getTicketByTicketIdAndAccountNumber(ticketId, principal.accountNumber());
        if (ticket.isPresent()) {
            response.put("success", true);
            response.put("ticket", ticket.get());
            return ResponseEntity.ok(response);
        }
        response.put("success", false);
        response.put("message", "Ticket not found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<Map<String, Object>> getByStatus(
            @PathVariable String status,
            @AuthenticationPrincipal SessionPrincipal principal) {
        if (principal == null) return unauthorized();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("tickets", supportTicketService.getTicketsByAccountNumber(principal.accountNumber()).stream()
                .filter(ticket -> status.equalsIgnoreCase(ticket.getStatus()))
                .toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAll(
            @AuthenticationPrincipal SessionPrincipal principal) {
        if (principal == null) return unauthorized();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        if ("ADMIN".equals(principal.scope())) {
            response.put("tickets", supportTicketService.getAllTickets());
            return ResponseEntity.ok(response);
        }
        if ("USER".equals(principal.scope()) && principal.accountNumber() != null) {
            response.put("tickets", supportTicketService.getTicketsByAccountNumber(principal.accountNumber()));
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "success", false, "message", "Only admins can view all support tickets"));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal SessionPrincipal principal) {
        if (principal == null) return unauthorized();
        if (!"ADMIN".equals(principal.scope())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "success", false, "message", "Only admins can update support tickets"));
        }
        String status = body == null ? null : body.get("status");
        if (status == null || !List.of("OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED").contains(status)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false, "message", "A valid support ticket status is required"));
        }
        SupportTicket updated = supportTicketService.updateTicketStatus(
                id, status, body.get("adminResponse"));
        if (updated == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false, "message", "Ticket not found"));
        }
        return ResponseEntity.ok(Map.of(
                "success", true, "message", "Ticket updated successfully", "ticket", updated));
    }

    @PutMapping("/{id}/assign")
    public ResponseEntity<Map<String, Object>> assignTicket(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal SessionPrincipal principal) {
        if (principal == null) return unauthorized();
        if (!"ADMIN".equals(principal.scope())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "success", false, "message", "Only admins can assign support tickets"));
        }
        String assignedTo = body == null ? null : body.get("assignedTo");
        if (assignedTo == null || assignedTo.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false, "message", "An assignee is required"));
        }
        SupportTicket updated = supportTicketService.assignTicket(id, assignedTo);
        if (updated == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false, "message", "Ticket not found"));
        }
        return ResponseEntity.ok(Map.of(
                "success", true, "message", "Ticket assigned successfully", "ticket", updated));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats(
            @AuthenticationPrincipal SessionPrincipal principal) {
        if (principal == null) return unauthorized();
        if ("ADMIN".equals(principal.scope())) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("openCount", supportTicketService.getOpenTicketsCount());
            response.put("inProgressCount", supportTicketService.getInProgressCount());
            response.put("resolvedCount", supportTicketService.getResolvedCount());
            response.put("totalCount", supportTicketService.getTotalTicketsCount());
            return ResponseEntity.ok(response);
        }

        List<SupportTicket> tickets = supportTicketService.getTicketsByAccountNumber(principal.accountNumber());
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("openCount", tickets.stream().filter(ticket -> "OPEN".equals(ticket.getStatus())).count());
        response.put("inProgressCount", tickets.stream().filter(ticket -> "IN_PROGRESS".equals(ticket.getStatus())).count());
        response.put("resolvedCount", tickets.stream().filter(ticket -> "RESOLVED".equals(ticket.getStatus())).count());
        response.put("totalCount", tickets.size());
        return ResponseEntity.ok(response);
    }

    private ResponseEntity<Map<String, Object>> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "success", false, "message", "Please sign in to access your support requests"));
    }
}
