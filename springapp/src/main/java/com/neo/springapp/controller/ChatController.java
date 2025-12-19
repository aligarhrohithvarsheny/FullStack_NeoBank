package com.neo.springapp.controller;

import com.neo.springapp.model.ChatMessage;
import com.neo.springapp.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;
    
    @Autowired
    private ApplicationContext applicationContext;
    
    // WebSocket messaging template - will be retrieved lazily if available
    private Object getMessagingTemplate() {
        try {
            // Try to get SimpMessagingTemplate bean by type name
            String[] beanNames = applicationContext.getBeanNamesForType(
                Class.forName("org.springframework.messaging.simp.SimpMessagingTemplate")
            );
            if (beanNames.length > 0) {
                return applicationContext.getBean(beanNames[0]);
            }
        } catch (Exception e) {
            // WebSocket not configured or class not available, return null
        }
        return null;
    }

    // Health check endpoint
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "ok");
        response.put("service", "chat");
        return ResponseEntity.ok(response);
    }

    // Admin sends message
    @PostMapping("/admin/send")
    public ResponseEntity<Map<String, Object>> adminSendMessage(
            @RequestParam("sessionId") String sessionId,
            @RequestParam("adminId") String adminId,
            @RequestParam(value = "message", required = false) String message,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Get user info from session
            List<ChatMessage> chatHistory = chatService.getChatHistory(sessionId);
            if (chatHistory.isEmpty()) {
                response.put("success", false);
                response.put("message", "Chat session not found");
                return ResponseEntity.badRequest().body(response);
            }
            
            ChatMessage firstMessage = chatHistory.get(0);
            String userId = firstMessage.getUserId();
            
            // Create admin message
            ChatMessage adminMessage = new ChatMessage();
            adminMessage.setSessionId(sessionId);
            adminMessage.setUserId(userId);
            adminMessage.setUserName("Admin");
            adminMessage.setMessage(message != null ? message : "");
            adminMessage.setSender("ADMIN");
            adminMessage.setAdminId(adminId);
            adminMessage.setStatus("RESOLVED");
            adminMessage.setIsRead(true);
            // Explicitly set timestamp to ensure it's saved
            adminMessage.setTimestamp(java.time.LocalDateTime.now());
            
            // Handle file upload if present
            if (file != null && !file.isEmpty()) {
                String attachmentUrl = saveChatAttachment(file, sessionId);
                adminMessage.setAttachmentUrl(attachmentUrl);
                adminMessage.setAttachmentName(file.getOriginalFilename());
                String contentType = file.getContentType();
                if (contentType != null) {
                    if (contentType.startsWith("image/")) {
                        adminMessage.setAttachmentType("IMAGE");
                    } else if (contentType.equals("application/pdf")) {
                        adminMessage.setAttachmentType("PDF");
                    } else {
                        adminMessage.setAttachmentType("FILE");
                    }
                }
            }
            
            ChatMessage savedMessage = chatService.saveMessage(adminMessage);
            
            // Send to WebSocket topic (if WebSocket is available)
            sendWebSocketMessage("/topic/chat/" + sessionId, savedMessage);
            
            response.put("success", true);
            response.put("message", savedMessage);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Error sending admin message: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    // User sends message with optional file
    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendMessage(
            @RequestParam("sessionId") String sessionId,
            @RequestParam("userId") String userId,
            @RequestParam("userName") String userName,
            @RequestParam(value = "message", required = false) String message,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (sessionId == null || sessionId.isEmpty()) {
                sessionId = UUID.randomUUID().toString();
            }
            
            // Process message and get bot response
            ChatMessage botResponse = chatService.processMessage(sessionId, userId, userName, message != null ? message : "");
            
            // Handle file upload if present
            if (file != null && !file.isEmpty()) {
                String attachmentUrl = saveChatAttachment(file, sessionId);
                // Update the last user message with attachment
                List<ChatMessage> recentMessages = chatService.getChatHistory(sessionId);
                if (!recentMessages.isEmpty()) {
                    ChatMessage lastUserMessage = recentMessages.stream()
                        .filter(m -> "USER".equals(m.getSender()))
                        .reduce((first, second) -> second)
                        .orElse(null);
                    if (lastUserMessage != null) {
                        lastUserMessage.setAttachmentUrl(attachmentUrl);
                        lastUserMessage.setAttachmentName(file.getOriginalFilename());
                        String contentType = file.getContentType();
                        if (contentType != null) {
                            if (contentType.startsWith("image/")) {
                                lastUserMessage.setAttachmentType("IMAGE");
                            } else if (contentType.equals("application/pdf")) {
                                lastUserMessage.setAttachmentType("PDF");
                            } else {
                                lastUserMessage.setAttachmentType("FILE");
                            }
                        }
                        chatService.saveMessage(lastUserMessage);
                    }
                }
            }
            
            // Send to WebSocket topic for real-time updates (if WebSocket is available)
            sendWebSocketMessage("/topic/chat/" + sessionId, botResponse);
            // If escalated, notify admin
            if ("ESCALATED".equals(botResponse.getStatus())) {
                sendWebSocketMessage("/topic/admin/chat", botResponse);
            }
            
            response.put("success", true);
            response.put("sessionId", sessionId);
            response.put("botResponse", botResponse);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error in sendMessage: " + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Error sending message: " + e.getMessage());
            response.put("error", e.getClass().getSimpleName());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    // Helper method to save chat attachments
    private String saveChatAttachment(MultipartFile file, String sessionId) throws IOException {
        // Create uploads directory if it doesn't exist
        String uploadDir = "uploads/chat";
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String filename = "chat_" + sessionId + "_" + System.currentTimeMillis() + extension;
        
        // Save file
        Path filePath = Paths.get(uploadDir, filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        // Return relative URL
        return "/api/chat/attachment/" + filename;
    }
    
    // Serve chat attachments
    @GetMapping("/attachment/{filename}")
    public ResponseEntity<byte[]> getAttachment(@PathVariable String filename) {
        try {
            Path filePath = Paths.get("uploads/chat", filename);
            byte[] fileContent = Files.readAllBytes(filePath);
            
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            
            return ResponseEntity.ok()
                .header("Content-Type", contentType)
                .body(fileContent);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Get chat history
    @GetMapping("/history/{sessionId}")
    public ResponseEntity<List<ChatMessage>> getChatHistory(@PathVariable String sessionId) {
        List<ChatMessage> messages = chatService.getChatHistory(sessionId);
        return ResponseEntity.ok(messages);
    }

    // Get escalated chats for admin
    @GetMapping("/admin/escalated")
    public ResponseEntity<List<ChatMessage>> getEscalatedChats() {
        List<ChatMessage> chats = chatService.getEscalatedChats();
        return ResponseEntity.ok(chats);
    }

    // Get all chats for admin dashboard
    @GetMapping("/admin/all")
    public ResponseEntity<Map<String, Object>> getAllChats() {
        Map<String, Object> response = new HashMap<>();
        List<ChatMessage> escalated = chatService.getEscalatedChats();
        response.put("escalated", escalated);
        response.put("totalEscalated", escalated.size());
        return ResponseEntity.ok(response);
    }

    // Admin takes over a chat
    @PostMapping("/admin/takeover")
    public ResponseEntity<Map<String, Object>> adminTakeover(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String sessionId = request.get("sessionId");
            String adminId = request.get("adminId");
            
            // Update all messages in session to assign admin
            List<ChatMessage> messages = chatService.getChatHistory(sessionId);
            for (ChatMessage msg : messages) {
                if ("ESCALATED".equals(msg.getStatus())) {
                    msg.setAdminId(adminId);
                    msg.setStatus("IN_PROGRESS");
                    chatService.saveMessage(msg);
                }
            }
            
            // Notify user that admin has joined
            ChatMessage adminJoined = new ChatMessage();
            adminJoined.setSessionId(sessionId);
            adminJoined.setUserId(messages.get(0).getUserId());
            adminJoined.setUserName("System");
            adminJoined.setMessage("A customer support representative has joined the chat.");
            adminJoined.setSender("SYSTEM");
            adminJoined.setAdminId(adminId);
            // Explicitly set timestamp to ensure it's saved
            adminJoined.setTimestamp(java.time.LocalDateTime.now());
            chatService.saveMessage(adminJoined);
            
            // Send to WebSocket topic (if WebSocket is available)
            sendWebSocketMessage("/topic/chat/" + sessionId, adminJoined);
            
            response.put("success", true);
            response.put("message", "Admin takeover successful");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error taking over chat: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    // Helper method to send WebSocket messages safely
    private void sendWebSocketMessage(String destination, Object message) {
        Object messagingTemplate = getMessagingTemplate();
        if (messagingTemplate != null) {
            try {
                // Use reflection to call convertAndSend to avoid compile-time dependency
                java.lang.reflect.Method method = messagingTemplate.getClass()
                    .getMethod("convertAndSend", String.class, Object.class);
                method.invoke(messagingTemplate, destination, message);
            } catch (Exception e) {
                // WebSocket not available or error - silently continue
                // Chat will work via HTTP polling instead
            }
        }
    }
}

