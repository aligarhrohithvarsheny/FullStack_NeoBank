package com.neo.springapp.controller;

import com.neo.springapp.model.BankMessage;
import com.neo.springapp.service.BankMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bank-messages")
@CrossOrigin(origins = "*")
public class BankMessageController {

    @Autowired
    private BankMessageService bankMessageService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> sendMessage(@RequestBody BankMessage message) {
        Map<String, Object> response = new HashMap<>();
        try {
            BankMessage sent = bankMessageService.sendMessage(message);
            response.put("success", true);
            response.put("message", "Message sent successfully");
            response.put("data", sent);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error sending message: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/system")
    public ResponseEntity<Map<String, Object>> sendSystemMessage(@RequestBody Map<String, String> body) {
        Map<String, Object> response = new HashMap<>();
        try {
            BankMessage sent = bankMessageService.sendSystemMessage(
                body.get("accountNumber"),
                body.get("title"),
                body.get("content"),
                body.get("messageType")
            );
            response.put("success", true);
            response.put("data", sent);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<Map<String, Object>> getByAccountNumber(@PathVariable String accountNumber) {
        Map<String, Object> response = new HashMap<>();
        List<BankMessage> messages = bankMessageService.getMessagesByAccountNumber(accountNumber);
        response.put("success", true);
        response.put("messages", messages);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/account/{accountNumber}/unread")
    public ResponseEntity<Map<String, Object>> getUnread(@PathVariable String accountNumber) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("messages", bankMessageService.getUnreadMessages(accountNumber));
        response.put("unreadCount", bankMessageService.getUnreadCount(accountNumber));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/account/{accountNumber}/unread-count")
    public ResponseEntity<Map<String, Object>> getUnreadCount(@PathVariable String accountNumber) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("unreadCount", bankMessageService.getUnreadCount(accountNumber));
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Map<String, Object>> markAsRead(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        BankMessage msg = bankMessageService.markAsRead(id);
        if (msg != null) {
            response.put("success", true);
            response.put("data", msg);
            return ResponseEntity.ok(response);
        }
        response.put("success", false);
        response.put("message", "Message not found");
        return ResponseEntity.badRequest().body(response);
    }

    @PutMapping("/account/{accountNumber}/read-all")
    public ResponseEntity<Map<String, Object>> markAllAsRead(@PathVariable String accountNumber) {
        Map<String, Object> response = new HashMap<>();
        bankMessageService.markAllAsRead(accountNumber);
        response.put("success", true);
        response.put("message", "All messages marked as read");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        if (bankMessageService.deleteMessage(id)) {
            response.put("success", true);
            response.put("message", "Message deleted");
            return ResponseEntity.ok(response);
        }
        response.put("success", false);
        response.put("message", "Message not found");
        return ResponseEntity.badRequest().body(response);
    }

    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAll() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("messages", bankMessageService.getAllMessages());
        return ResponseEntity.ok(response);
    }
}
