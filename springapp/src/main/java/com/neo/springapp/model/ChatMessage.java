package com.neo.springapp.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "chat_messages")
public class ChatMessage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String sessionId; // Unique session ID for each chat conversation
    private String userId; // User account number or email
    private String userName; // User name
    private String message; // Message content
    private String sender; // "USER", "BOT", or "ADMIN"
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
    
    private String status; // "PENDING", "RESOLVED", "ESCALATED"
    private String adminId; // Admin ID if escalated
    private Boolean isRead; // Whether admin has read the message
    private String attachmentUrl; // URL or path to uploaded file
    private String attachmentType; // "IMAGE", "PDF", etc.
    private String attachmentName; // Original file name
    
    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
        if (isRead == null) {
            isRead = false;
        }
        if (status == null) {
            status = "PENDING";
        }
    }
}

