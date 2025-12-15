package com.neo.springapp.service;

import com.neo.springapp.model.ChatMessage;
import com.neo.springapp.model.Account;
import com.neo.springapp.model.Loan;
import com.neo.springapp.model.GoldLoan;
import com.neo.springapp.model.Transaction;
import com.neo.springapp.repository.ChatMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ChatService {
    
    @Autowired
    private ChatMessageRepository chatMessageRepository;
    
    @Autowired
    private AccountService accountService;
    
    @Autowired
    private LoanService loanService;
    
    @Autowired
    private GoldLoanService goldLoanService;
    
    @Autowired
    private TransactionService transactionService;
    
    @Autowired
    private UserService userService;
    
    /**
     * Process user message and generate bot response
     */
    public ChatMessage processMessage(String sessionId, String userId, String userName, String message) {
        // Create and save user message FIRST - this ensures all user chats are saved
        ChatMessage userMessage = new ChatMessage();
        userMessage.setSessionId(sessionId);
        userMessage.setUserId(userId);
        userMessage.setUserName(userName);
        userMessage.setMessage(message != null ? message : "");
        userMessage.setSender("USER");
        userMessage.setStatus("PENDING");
        // Explicitly set timestamp to ensure it's saved
        userMessage.setTimestamp(java.time.LocalDateTime.now());
        
        // CRITICAL: Save user message immediately to ensure it's persisted
        ChatMessage savedUserMessage = chatMessageRepository.save(userMessage);
        System.out.println("✅ User message saved with ID: " + savedUserMessage.getId() + 
                         ", Session: " + sessionId + 
                         ", UserId: " + userId + 
                         ", Message: " + (message != null && message.length() > 0 ? message.substring(0, Math.min(50, message.length())) : "empty"));
        
        // Generate bot response
        String botResponse = generateBotResponse(userId, message != null ? message : "");
        boolean needsAdmin = botResponse.contains("ESCALATE_TO_ADMIN");
        
        ChatMessage botMessage = new ChatMessage();
        botMessage.setSessionId(sessionId);
        botMessage.setUserId(userId);
        botMessage.setUserName("Bot");
        botMessage.setMessage(needsAdmin ? 
            "I'm connecting you with a customer support representative. Please wait..." : 
            botResponse);
        botMessage.setSender("BOT");
        botMessage.setStatus(needsAdmin ? "ESCALATED" : "PENDING");
        // Explicitly set timestamp to ensure it's saved
        botMessage.setTimestamp(java.time.LocalDateTime.now());
        
        if (needsAdmin) {
            // Mark user message as escalated so it appears in escalated chats
            savedUserMessage.setStatus("ESCALATED");
            chatMessageRepository.save(savedUserMessage);
            System.out.println("✅ User message marked as ESCALATED - Session: " + sessionId);
        }
        
        // Save bot message - this ensures all bot chats are saved permanently
        ChatMessage savedBotMessage = chatMessageRepository.save(botMessage);
        System.out.println("✅ Bot message saved with ID: " + savedBotMessage.getId() + ", Session: " + sessionId);
        
        // Verify all messages are saved for this session
        List<ChatMessage> allSessionMessages = chatMessageRepository.findBySessionIdOrderByTimestampAsc(sessionId);
        System.out.println("📊 Total messages in session " + sessionId + ": " + allSessionMessages.size() + 
                         " (User: " + allSessionMessages.stream().filter(m -> "USER".equals(m.getSender())).count() + 
                         ", Bot: " + allSessionMessages.stream().filter(m -> "BOT".equals(m.getSender())).count() + ")");
        
        return savedBotMessage;
    }
    
    /**
     * Generate bot response based on user query
     */
    private String generateBotResponse(String userId, String message) {
        String lowerMessage = message.toLowerCase().trim();
        
        // Check balance
        if (matchesPattern(lowerMessage, "balance|account balance|current balance|how much|money")) {
            return getAccountBalance(userId);
        }
        
        // Check loans
        if (matchesPattern(lowerMessage, "loan|loans|emi|installment|personal loan|gold loan")) {
            return getLoanDetails(userId, lowerMessage);
        }
        
        // Check transactions
        if (matchesPattern(lowerMessage, "transaction|transactions|history|statement|recent")) {
            return getTransactionDetails(userId);
        }
        
        // Account information
        if (matchesPattern(lowerMessage, "account|account number|account details|my account")) {
            return getAccountDetails(userId);
        }
        
        // Greetings
        if (matchesPattern(lowerMessage, "hello|hi|hey|greetings|good morning|good afternoon|good evening")) {
            return "Hello! I'm NeoBank's virtual assistant. How can I help you today? " +
                   "I can help you with:\n" +
                   "• Check your account balance\n" +
                   "• View loan details\n" +
                   "• Check transaction history\n" +
                   "• Account information\n\n" +
                   "Just ask me anything!";
        }
        
        // Help
        if (matchesPattern(lowerMessage, "help|support|what can you do|commands")) {
            return "I can help you with:\n\n" +
                   "💰 **Account Balance** - Ask: 'What's my balance?' or 'Check balance'\n" +
                   "📋 **Loan Details** - Ask: 'My loans' or 'Loan information'\n" +
                   "💳 **Transactions** - Ask: 'Transaction history' or 'Recent transactions'\n" +
                   "🏦 **Account Info** - Ask: 'Account details' or 'My account'\n\n" +
                   "If you need more help, I'll connect you with a customer support representative.";
        }
        
        // Default response - escalate to admin
        return "ESCALATE_TO_ADMIN";
    }
    
    private boolean matchesPattern(String text, String patterns) {
        String[] patternArray = patterns.split("\\|");
        for (String pattern : patternArray) {
            if (text.contains(pattern.trim())) {
                return true;
            }
        }
        return false;
    }
    
    private String getAccountBalance(String userId) {
        try {
            Account account = accountService.getAccountByNumber(userId);
            if (account == null) {
                // Try to get by email
                var userOpt = userService.findByEmail(userId);
                if (userOpt.isPresent() && userOpt.get().getAccountNumber() != null) {
                    account = accountService.getAccountByNumber(userOpt.get().getAccountNumber());
                }
            }
            
            if (account != null) {
                Double balance = accountService.getBalanceByAccountNumber(account.getAccountNumber());
                return String.format("💰 Your current account balance is **₹%.2f**\n\n" +
                                   "Account Number: %s\n" +
                                   "Account Type: %s",
                                   balance != null ? balance : 0.0,
                                   account.getAccountNumber(),
                                   account.getAccountType() != null ? account.getAccountType() : "Savings");
            }
            return "I couldn't find your account information. Please contact customer support.";
        } catch (Exception e) {
            return "I'm having trouble retrieving your balance. Let me connect you with a support representative.";
        }
    }
    
    private String getLoanDetails(String userId, String message) {
        try {
            Account account = accountService.getAccountByNumber(userId);
            String accountNumber = userId;
            if (account == null) {
                var userOpt = userService.findByEmail(userId);
                if (userOpt.isPresent() && userOpt.get().getAccountNumber() != null) {
                    accountNumber = userOpt.get().getAccountNumber();
                }
            } else {
                accountNumber = account.getAccountNumber();
            }
            
            List<Loan> loans = loanService.getLoansByAccountNumber(accountNumber);
            List<GoldLoan> goldLoans = goldLoanService.getGoldLoansByAccountNumber(accountNumber);
            
            StringBuilder response = new StringBuilder();
            
            if (loans != null && !loans.isEmpty()) {
                response.append("📋 **Personal Loans:**\n\n");
                for (Loan loan : loans) {
                    if ("Approved".equals(loan.getStatus())) {
                        response.append(String.format("• Loan Account: %s\n", loan.getLoanAccountNumber()));
                        response.append(String.format("  Amount: ₹%.2f\n", loan.getAmount()));
                        response.append(String.format("  Interest Rate: %.2f%%\n", loan.getInterestRate()));
                        response.append(String.format("  Tenure: %d months\n", loan.getTenure()));
                        response.append(String.format("  Status: %s\n\n", loan.getStatus()));
                    }
                }
            }
            
            if (goldLoans != null && !goldLoans.isEmpty()) {
                response.append("🥇 **Gold Loans:**\n\n");
                for (GoldLoan goldLoan : goldLoans) {
                    if ("Approved".equals(goldLoan.getStatus())) {
                        response.append(String.format("• Loan Account: %s\n", goldLoan.getLoanAccountNumber()));
                        response.append(String.format("  Amount: ₹%.2f\n", goldLoan.getLoanAmount()));
                        response.append(String.format("  Gold Weight: %.2f grams\n", goldLoan.getGoldGrams()));
                        response.append(String.format("  Interest Rate: %.2f%%\n", goldLoan.getInterestRate()));
                        response.append(String.format("  Status: %s\n\n", goldLoan.getStatus()));
                    }
                }
            }
            
            if (response.length() == 0) {
                return "You don't have any active loans at the moment.";
            }
            
            return response.toString();
        } catch (Exception e) {
            return "I'm having trouble retrieving your loan information. Let me connect you with a support representative.";
        }
    }
    
    private String getTransactionDetails(String userId) {
        try {
            Account account = accountService.getAccountByNumber(userId);
            String accountNumber = userId;
            if (account == null) {
                var userOpt = userService.findByEmail(userId);
                if (userOpt.isPresent() && userOpt.get().getAccountNumber() != null) {
                    accountNumber = userOpt.get().getAccountNumber();
                }
            } else {
                accountNumber = account.getAccountNumber();
            }
            
            var transactionsPage = transactionService.getTransactionsByAccountNumber(accountNumber, 0, 5);
            List<Transaction> transactions = transactionsPage.getContent();
            
            if (transactions == null || transactions.isEmpty()) {
                return "You don't have any recent transactions.";
            }
            
            StringBuilder response = new StringBuilder("💳 **Recent Transactions:**\n\n");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
            
            for (Transaction txn : transactions) {
                response.append(String.format("• %s\n", 
                    txn.getDate() != null ? txn.getDate().format(formatter) : "N/A"));
                response.append(String.format("  %s\n", 
                    txn.getDescription() != null ? txn.getDescription() : 
                    (txn.getMerchant() != null ? txn.getMerchant() : "Transaction")));
                response.append(String.format("  %s: ₹%.2f\n", 
                    txn.getType() != null ? txn.getType() : "Transaction",
                    txn.getAmount() != null ? txn.getAmount() : 0.0));
                response.append("\n");
            }
            
            return response.toString();
        } catch (Exception e) {
            return "I'm having trouble retrieving your transaction history. Let me connect you with a support representative.";
        }
    }
    
    private String getAccountDetails(String userId) {
        try {
            Account account = accountService.getAccountByNumber(userId);
            if (account == null) {
                var userOpt = userService.findByEmail(userId);
                if (userOpt.isPresent() && userOpt.get().getAccountNumber() != null) {
                    account = accountService.getAccountByNumber(userOpt.get().getAccountNumber());
                }
            }
            
            if (account != null) {
                return String.format("🏦 **Account Details:**\n\n" +
                                   "Account Number: %s\n" +
                                   "Account Holder: %s\n" +
                                   "Account Type: %s\n" +
                                   "IFSC Code: NEO0008648\n" +
                                   "Branch: NEOBANK HEAD OFFICE",
                                   account.getAccountNumber(),
                                   account.getName() != null ? account.getName() : "N/A",
                                   account.getAccountType() != null ? account.getAccountType() : "Savings");
            }
            return "I couldn't find your account information. Please contact customer support.";
        } catch (Exception e) {
            return "I'm having trouble retrieving your account details. Let me connect you with a support representative.";
        }
    }
    
    public ChatMessage saveMessage(ChatMessage message) {
        return chatMessageRepository.save(message);
    }
    
    public List<ChatMessage> getChatHistory(String sessionId) {
        return chatMessageRepository.findBySessionIdOrderByTimestampAsc(sessionId);
    }
    
    public List<ChatMessage> getUserChats(String userId) {
        return chatMessageRepository.findByUserIdOrderByTimestampDesc(userId);
    }
    
    public List<ChatMessage> getEscalatedChats() {
        // Get all escalated messages - these are the ones that need admin attention
        List<ChatMessage> escalatedMessages = chatMessageRepository.findByStatusOrderByTimestampDesc("ESCALATED");
        
        // Log to verify user messages are being saved
        System.out.println("Found " + escalatedMessages.size() + " escalated messages");
        for (ChatMessage msg : escalatedMessages) {
            System.out.println("Escalated message - Session: " + msg.getSessionId() + 
                             ", Sender: " + msg.getSender() + 
                             ", UserId: " + msg.getUserId() + 
                             ", Message: " + (msg.getMessage() != null ? msg.getMessage().substring(0, Math.min(50, msg.getMessage().length())) : "null"));
        }
        
        return escalatedMessages;
    }
    
    public List<ChatMessage> getAdminChats(String adminId) {
        return chatMessageRepository.findByAdminIdOrderByTimestampDesc(adminId);
    }
}

