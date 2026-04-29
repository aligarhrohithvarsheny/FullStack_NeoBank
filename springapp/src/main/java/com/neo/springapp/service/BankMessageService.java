package com.neo.springapp.service;

import com.neo.springapp.model.BankMessage;
import com.neo.springapp.repository.BankMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@SuppressWarnings("null")
public class BankMessageService {

    @Autowired
    private BankMessageRepository bankMessageRepository;

    public BankMessage sendMessage(BankMessage message) {
        message.setCreatedAt(LocalDateTime.now());
        return bankMessageRepository.save(message);
    }

    public BankMessage sendSystemMessage(String accountNumber, String title, String content, String messageType) {
        BankMessage message = new BankMessage();
        message.setRecipientAccountNumber(accountNumber);
        message.setTitle(title);
        message.setContent(content);
        message.setMessageType(messageType);
        message.setSender("SYSTEM");
        return bankMessageRepository.save(message);
    }

    public List<BankMessage> getMessagesByAccountNumber(String accountNumber) {
        return bankMessageRepository.findByRecipientAccountNumberOrderByCreatedAtDesc(accountNumber);
    }

    public List<BankMessage> getUnreadMessages(String accountNumber) {
        return bankMessageRepository.findByRecipientAccountNumberAndIsReadFalseOrderByCreatedAtDesc(accountNumber);
    }

    public long getUnreadCount(String accountNumber) {
        return bankMessageRepository.countByRecipientAccountNumberAndIsReadFalse(accountNumber);
    }

    public BankMessage markAsRead(Long messageId) {
        Optional<BankMessage> opt = bankMessageRepository.findById(messageId);
        if (opt.isPresent()) {
            BankMessage msg = opt.get();
            msg.setRead(true);
            msg.setReadAt(LocalDateTime.now());
            return bankMessageRepository.save(msg);
        }
        return null;
    }

    public void markAllAsRead(String accountNumber) {
        List<BankMessage> unread = bankMessageRepository.findByRecipientAccountNumberAndIsReadFalseOrderByCreatedAtDesc(accountNumber);
        for (BankMessage msg : unread) {
            msg.setRead(true);
            msg.setReadAt(LocalDateTime.now());
        }
        bankMessageRepository.saveAll(unread);
    }

    public boolean deleteMessage(Long id) {
        if (bankMessageRepository.existsById(id)) {
            bankMessageRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public List<BankMessage> getAllMessages() {
        return bankMessageRepository.findAll();
    }
}
