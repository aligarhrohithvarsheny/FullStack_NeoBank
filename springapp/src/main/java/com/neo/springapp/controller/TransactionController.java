package com.neo.springapp.controller;

import com.neo.springapp.model.Transaction;
import com.neo.springapp.model.User;
import com.neo.springapp.service.TransactionService;
import com.neo.springapp.service.UserService;
import com.neo.springapp.service.AccountService;
import com.neo.springapp.service.PdfService;
import com.neo.springapp.service.EmailService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final UserService userService;
    private final AccountService accountService;
    private final PdfService pdfService;
    private final EmailService emailService;

    public TransactionController(TransactionService transactionService, UserService userService, 
                                AccountService accountService, PdfService pdfService, EmailService emailService) {
        this.transactionService = transactionService;
        this.userService = userService;
        this.accountService = accountService;
        this.pdfService = pdfService;
        this.emailService = emailService;
    }

    // Add new transaction
    @PostMapping
    public Transaction createTransaction(@RequestBody Transaction transaction) {
        transaction.setDate(LocalDateTime.now());
        return transactionService.saveTransaction(transaction);
    }

    // Get all transactions with pagination and sorting
    @GetMapping
    public Page<Transaction> getTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "date") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return transactionService.getAllTransactions(page, size, sortBy, sortDir);
    }

    // Get transactions by account number with pagination
    @GetMapping("/account/{accountNumber}")
    public Page<Transaction> getTransactionsByAccount(
            @PathVariable String accountNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return transactionService.getTransactionsByAccountNumber(accountNumber, page, size);
    }

    // Get transactions by user name with pagination
    @GetMapping("/user/{userName}")
    public Page<Transaction> getTransactionsByUser(
            @PathVariable String userName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return transactionService.getTransactionsByUserName(userName, page, size);
    }

    // Get transactions by type with pagination
    @GetMapping("/type/{type}")
    public Page<Transaction> getTransactionsByType(
            @PathVariable String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return transactionService.getTransactionsByType(type, page, size);
    }

    // Get transactions by status with pagination
    @GetMapping("/status/{status}")
    public Page<Transaction> getTransactionsByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return transactionService.getTransactionsByStatus(status, page, size);
    }

    // Get transactions by date range with pagination
    @GetMapping("/date-range")
    public Page<Transaction> getTransactionsByDateRange(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        LocalDateTime start = LocalDateTime.parse(startDate);
        LocalDateTime end = LocalDateTime.parse(endDate);
        return transactionService.getTransactionsByDateRange(start, end, page, size);
    }

    // Get transactions by merchant with pagination
    @GetMapping("/merchant/{merchant}")
    public Page<Transaction> getTransactionsByMerchant(
            @PathVariable String merchant,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return transactionService.getTransactionsByMerchant(merchant, page, size);
    }

    // Get mini statement (recent 5 transactions)
    @GetMapping("/mini-statement/{accountNumber}")
    public List<Transaction> getMiniStatement(@PathVariable String accountNumber) {
        return transactionService.getMiniStatement(accountNumber);
    }

    // Get transaction summary
    @GetMapping("/summary/{accountNumber}/{type}")
    public Object[] getTransactionSummary(@PathVariable String accountNumber, @PathVariable String type) {
        return transactionService.getTransactionSummary(accountNumber, type);
    }

    // Search transactions with multiple criteria
    @GetMapping("/search")
    public Page<Transaction> searchTransactions(
            @RequestParam(required = false) String accountNumber,
            @RequestParam(required = false) String merchant,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "date") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        LocalDateTime start = startDate != null ? LocalDateTime.parse(startDate) : null;
        LocalDateTime end = endDate != null ? LocalDateTime.parse(endDate) : null;
        
        return transactionService.searchTransactions(accountNumber, merchant, type, status, 
                                                    start, end, page, size, sortBy, sortDir);
    }

    // Send bank statement via email
    @PostMapping("/send-statement/{accountNumber}")
    public ResponseEntity<Map<String, Object>> sendBankStatementByEmail(@PathVariable String accountNumber) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Get user by account number
            Optional<User> userOptional = userService.getUserByAccountNumber(accountNumber);
            if (!userOptional.isPresent()) {
                response.put("success", false);
                response.put("message", "User not found for account number: " + accountNumber);
                return ResponseEntity.badRequest().body(response);
            }
            
            User user = userOptional.get();
            String userEmail = user.getEmail();
            String userName = user.getName();
            
            if (userEmail == null || userEmail.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Email address not found for this account. Please update your email address.");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Get current balance
            Double currentBalance = accountService.getBalanceByAccountNumber(accountNumber);
            if (currentBalance == null) {
                currentBalance = 0.0;
            }
            
            // Get all transactions for the account (get a large number to include all)
            Page<Transaction> transactionPage = transactionService.getTransactionsByAccountNumber(accountNumber, 0, 1000);
            List<Transaction> transactions = transactionPage.getContent();
            
            // Generate PDF
            byte[] pdfBytes;
            try {
                pdfBytes = pdfService.generateBankStatement(accountNumber, userName, userEmail, currentBalance, transactions);
            } catch (IOException e) {
                response.put("success", false);
                response.put("message", "Failed to generate PDF: " + e.getMessage());
                return ResponseEntity.internalServerError().body(response);
            }
            
            // Send email with PDF attachment
            boolean emailSent = false;
            String emailError = null;
            
            try {
                emailSent = emailService.sendBankStatementEmail(userEmail, accountNumber, userName, pdfBytes);
            } catch (Exception emailException) {
                emailError = emailException.getMessage();
                System.err.println("Email sending exception caught in controller: " + emailError);
                if (emailException.getCause() != null) {
                    emailError = emailException.getCause().getMessage();
                    System.err.println("Email error cause: " + emailError);
                }
            }
            
            if (emailSent) {
                response.put("success", true);
                response.put("message", "Bank statement has been sent successfully to " + userEmail);
                response.put("email", userEmail);
                response.put("note", "Please check your inbox and spam folder. If you don't receive the email within a few minutes, check the server logs for email configuration issues.");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Failed to send email. " + (emailError != null ? "Error: " + emailError : "Please check email configuration in application.properties."));
                response.put("email", userEmail);
                response.put("error", emailError);
                response.put("note", "Please check: 1) Gmail App Password is correct, 2) Email configuration in application.properties, 3) Server console logs for detailed error messages.");
                return ResponseEntity.internalServerError().body(response);
            }
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error sending bank statement: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // Test email configuration endpoint
    @PostMapping("/test-email/{accountNumber}")
    public ResponseEntity<Map<String, Object>> testEmailConfiguration(@PathVariable String accountNumber) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Get user by account number
            Optional<User> userOptional = userService.getUserByAccountNumber(accountNumber);
            if (!userOptional.isPresent()) {
                response.put("success", false);
                response.put("message", "User not found for account number: " + accountNumber);
                return ResponseEntity.badRequest().body(response);
            }
            
            User user = userOptional.get();
            String userEmail = user.getEmail();
            
            if (userEmail == null || userEmail.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Email address not found for this account.");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Try to send a simple test email
            // We'll use the email service's OTP method as a test
            String testOtp = "TEST123";
            boolean emailSent = emailService.sendOtpEmail(userEmail, testOtp);
            
            response.put("success", emailSent);
            response.put("message", emailSent ? 
                "Test email sent successfully! Check your inbox at " + userEmail : 
                "Failed to send test email. Check server logs for details.");
            response.put("email", userEmail);
            response.put("note", "This is a test email. Check your inbox and spam folder.");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error testing email: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
