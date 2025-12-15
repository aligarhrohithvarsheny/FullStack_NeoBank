package com.neo.springapp.service;

import com.neo.springapp.model.*;
import com.neo.springapp.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AdminSearchService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private ChequeRepository chequeRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CardRepository cardRepository;

    /**
     * Comprehensive search across all entities
     */
    public Map<String, Object> searchAll(String searchTerm) {
        Map<String, Object> results = new HashMap<>();
        
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            results.put("success", false);
            results.put("message", "Search term cannot be empty");
            return results;
        }

        String term = searchTerm.trim();
        results.put("searchTerm", term);
        results.put("success", true);

        // Search Accounts
        List<Map<String, Object>> accounts = searchAccounts(term);
        results.put("accounts", accounts);
        results.put("accountCount", accounts.size());

        // Search Users
        List<Map<String, Object>> users = searchUsers(term);
        results.put("users", users);
        results.put("userCount", users.size());

        // Search Loans
        List<Map<String, Object>> loans = searchLoans(term);
        results.put("loans", loans);
        results.put("loanCount", loans.size());

        // Search Cheques
        List<Map<String, Object>> cheques = searchCheques(term);
        results.put("cheques", cheques);
        results.put("chequeCount", cheques.size());

        // Search Transactions
        List<Map<String, Object>> transactions = searchTransactions(term);
        results.put("transactions", transactions);
        results.put("transactionCount", transactions.size());

        // Search Cards
        List<Map<String, Object>> cards = searchCards(term);
        results.put("cards", cards);
        results.put("cardCount", cards.size());

        // Total results
        int totalCount = accounts.size() + users.size() + loans.size() + 
                        cheques.size() + transactions.size() + cards.size();
        results.put("totalCount", totalCount);

        return results;
    }

    /**
     * Search accounts by name, phone, aadhar, PAN, account number
     */
    private List<Map<String, Object>> searchAccounts(String term) {
        List<Map<String, Object>> results = new ArrayList<>();
        
        // Search by account number
        Account accountByNumber = accountRepository.findByAccountNumber(term);
        if (accountByNumber != null) {
            results.add(createAccountResult(accountByNumber, "Account Number"));
        }

        // Search by name using search query
        Pageable pageable = PageRequest.of(0, 100);
        Page<Account> accountsByName = accountRepository.searchAccounts(term, pageable);
        for (Account account : accountsByName.getContent()) {
            if (!results.stream().anyMatch(r -> r.get("accountNumber").equals(account.getAccountNumber()))) {
                results.add(createAccountResult(account, "Name"));
            }
        }

        // Search by phone
        List<Account> accountsByPhone = accountRepository.findByPhone(term);
        for (Account account : accountsByPhone) {
            if (!results.stream().anyMatch(r -> r.get("accountNumber").equals(account.getAccountNumber()))) {
                results.add(createAccountResult(account, "Phone"));
            }
        }

        // Search by Aadhar
        Account accountByAadhar = accountRepository.findByAadharNumber(term);
        if (accountByAadhar != null) {
            if (!results.stream().anyMatch(r -> r.get("accountNumber").equals(accountByAadhar.getAccountNumber()))) {
                results.add(createAccountResult(accountByAadhar, "Aadhar Number"));
            }
        }

        // Search by PAN
        Account accountByPan = accountRepository.findByPan(term);
        if (accountByPan != null) {
            if (!results.stream().anyMatch(r -> r.get("accountNumber").equals(accountByPan.getAccountNumber()))) {
                results.add(createAccountResult(accountByPan, "PAN Number"));
            }
        }

        // Search by partial matches
        if (term.length() >= 3) {
            List<Account> allAccounts = accountRepository.findAll();
            for (Account account : allAccounts) {
                if (results.stream().anyMatch(r -> r.get("accountNumber").equals(account.getAccountNumber()))) {
                    continue;
                }
                
                boolean matches = false;
                String matchType = "";
                
                if (account.getName() != null && account.getName().toLowerCase().contains(term.toLowerCase())) {
                    matches = true;
                    matchType = "Name (Partial)";
                } else if (account.getPhone() != null && account.getPhone().contains(term)) {
                    matches = true;
                    matchType = "Phone (Partial)";
                } else if (account.getAadharNumber() != null && account.getAadharNumber().contains(term)) {
                    matches = true;
                    matchType = "Aadhar (Partial)";
                } else if (account.getPan() != null && account.getPan().toLowerCase().contains(term.toLowerCase())) {
                    matches = true;
                    matchType = "PAN (Partial)";
                } else if (account.getAccountNumber() != null && account.getAccountNumber().contains(term)) {
                    matches = true;
                    matchType = "Account Number (Partial)";
                }
                
                if (matches) {
                    results.add(createAccountResult(account, matchType));
                }
            }
        }

        return results;
    }

    private Map<String, Object> createAccountResult(Account account, String matchType) {
        Map<String, Object> result = new HashMap<>();
        result.put("type", "Account");
        result.put("accountNumber", account.getAccountNumber());
        result.put("name", account.getName());
        result.put("phone", account.getPhone());
        result.put("aadharNumber", account.getAadharNumber());
        result.put("pan", account.getPan());
        result.put("balance", account.getBalance());
        result.put("accountType", account.getAccountType());
        result.put("status", account.getStatus());
        result.put("matchType", matchType);
        return result;
    }

    /**
     * Search users by email, username, account number
     */
    private List<Map<String, Object>> searchUsers(String term) {
        List<Map<String, Object>> results = new ArrayList<>();
        
        // Search by account number
        Optional<User> userByAccount = userRepository.findByAccountNumber(term);
        if (userByAccount.isPresent()) {
            results.add(createUserResult(userByAccount.get(), "Account Number"));
        }

        // Search by email
        Optional<User> userByEmail = userRepository.findByEmail(term);
        if (userByEmail.isPresent()) {
            if (!results.stream().anyMatch(r -> r.get("userId").equals(userByEmail.get().getId()))) {
                results.add(createUserResult(userByEmail.get(), "Email"));
            }
        }

        // Search by username
        Optional<User> userByUsername = userRepository.findByUsername(term);
        if (userByUsername.isPresent()) {
            if (!results.stream().anyMatch(r -> r.get("userId").equals(userByUsername.get().getId()))) {
                results.add(createUserResult(userByUsername.get(), "Username"));
            }
        }

        // Search by PAN (through account)
        Optional<User> userByPan = userRepository.findByPan(term);
        if (userByPan.isPresent()) {
            if (!results.stream().anyMatch(r -> r.get("userId").equals(userByPan.get().getId()))) {
                results.add(createUserResult(userByPan.get(), "PAN Number"));
            }
        }

        // Search by Aadhar (through account)
        Optional<User> userByAadhar = userRepository.findByAadhar(term);
        if (userByAadhar.isPresent()) {
            if (!results.stream().anyMatch(r -> r.get("userId").equals(userByAadhar.get().getId()))) {
                results.add(createUserResult(userByAadhar.get(), "Aadhar Number"));
            }
        }

        return results;
    }

    private Map<String, Object> createUserResult(User user, String matchType) {
        Map<String, Object> result = new HashMap<>();
        result.put("type", "User");
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("email", user.getEmail());
        result.put("accountNumber", user.getAccountNumber());
        result.put("status", user.getStatus());
        if (user.getAccount() != null) {
            result.put("name", user.getAccount().getName());
            result.put("phone", user.getAccount().getPhone());
            result.put("balance", user.getAccount().getBalance());
        }
        result.put("matchType", matchType);
        return result;
    }

    /**
     * Search loans by loan account number, loan ID, account number
     */
    private List<Map<String, Object>> searchLoans(String term) {
        List<Map<String, Object>> results = new ArrayList<>();
        
        // Search by loan account number
        Optional<Loan> loanByAccountNumber = loanRepository.findByLoanAccountNumber(term);
        if (loanByAccountNumber.isPresent()) {
            results.add(createLoanResult(loanByAccountNumber.get(), "Loan Account Number"));
        }

        // Search by loan ID (if term is numeric)
        try {
            Long loanId = Long.parseLong(term);
            Optional<Loan> loanById = loanRepository.findById(loanId);
            if (loanById.isPresent()) {
                if (!results.stream().anyMatch(r -> r.get("loanId").equals(loanId))) {
                    results.add(createLoanResult(loanById.get(), "Loan ID"));
                }
            }
        } catch (NumberFormatException e) {
            // Not a number, skip
        }

        // Search by account number
        List<Loan> loansByAccount = loanRepository.findByAccountNumber(term);
        for (Loan loan : loansByAccount) {
            if (!results.stream().anyMatch(r -> r.get("loanId").equals(loan.getId()))) {
                results.add(createLoanResult(loan, "Account Number"));
            }
        }

        // Search by child account number (for education loans)
        List<Loan> loansByChildAccount = loanRepository.findByChildAccountNumber(term);
        for (Loan loan : loansByChildAccount) {
            if (!results.stream().anyMatch(r -> r.get("loanId").equals(loan.getId()))) {
                results.add(createLoanResult(loan, "Child Account Number"));
            }
        }

        // Partial search on loan account number
        if (term.length() >= 4) {
            List<Loan> allLoans = loanRepository.findAll();
            for (Loan loan : allLoans) {
                if (results.stream().anyMatch(r -> r.get("loanId").equals(loan.getId()))) {
                    continue;
                }
                
                if (loan.getLoanAccountNumber() != null && 
                    loan.getLoanAccountNumber().toLowerCase().contains(term.toLowerCase())) {
                    results.add(createLoanResult(loan, "Loan Account Number (Partial)"));
                }
            }
        }

        return results;
    }

    private Map<String, Object> createLoanResult(Loan loan, String matchType) {
        Map<String, Object> result = new HashMap<>();
        result.put("type", "Loan");
        result.put("loanId", loan.getId());
        result.put("loanAccountNumber", loan.getLoanAccountNumber());
        result.put("accountNumber", loan.getAccountNumber());
        result.put("childAccountNumber", loan.getChildAccountNumber());
        result.put("userName", loan.getUserName());
        result.put("userEmail", loan.getUserEmail());
        result.put("loanType", loan.getType());
        result.put("amount", loan.getAmount());
        result.put("status", loan.getStatus());
        result.put("tenure", loan.getTenure());
        result.put("interestRate", loan.getInterestRate());
        result.put("applicationDate", loan.getApplicationDate());
        result.put("matchType", matchType);
        return result;
    }

    /**
     * Search cheques by cheque number, account number
     */
    private List<Map<String, Object>> searchCheques(String term) {
        List<Map<String, Object>> results = new ArrayList<>();
        
        // Search by cheque number
        Optional<Cheque> chequeByNumber = chequeRepository.findByChequeNumber(term);
        if (chequeByNumber.isPresent()) {
            results.add(createChequeResult(chequeByNumber.get(), "Cheque Number"));
        }

        // Search by account number
        List<Cheque> chequesByAccount = chequeRepository.findByAccountNumber(term);
        for (Cheque cheque : chequesByAccount) {
            if (!results.stream().anyMatch(r -> r.get("chequeId").equals(cheque.getId()))) {
                results.add(createChequeResult(cheque, "Account Number"));
            }
        }

        // Partial search on cheque number
        if (term.length() >= 4) {
            Pageable pageable = PageRequest.of(0, 50);
            Page<Cheque> cheques = chequeRepository.findByChequeNumberContaining(term, pageable);
            for (Cheque cheque : cheques.getContent()) {
                if (!results.stream().anyMatch(r -> r.get("chequeId").equals(cheque.getId()))) {
                    results.add(createChequeResult(cheque, "Cheque Number (Partial)"));
                }
            }
        }

        return results;
    }

    private Map<String, Object> createChequeResult(Cheque cheque, String matchType) {
        Map<String, Object> result = new HashMap<>();
        result.put("type", "Cheque");
        result.put("chequeId", cheque.getId());
        result.put("chequeNumber", cheque.getChequeNumber());
        result.put("accountNumber", cheque.getAccountNumber());
        result.put("accountHolderName", cheque.getAccountHolderName());
        result.put("amount", cheque.getAmount());
        result.put("status", cheque.getStatus());
        result.put("createdAt", cheque.getCreatedAt());
        result.put("drawnDate", cheque.getDrawnDate());
        result.put("matchType", matchType);
        return result;
    }

    /**
     * Search transactions by transaction ID, account number, description
     */
    private List<Map<String, Object>> searchTransactions(String term) {
        List<Map<String, Object>> results = new ArrayList<>();
        
        // Search by transaction ID (if term is numeric)
        try {
            Long transactionId = Long.parseLong(term);
            Optional<Transaction> transactionById = transactionRepository.findById(transactionId);
            if (transactionById.isPresent()) {
                results.add(createTransactionResult(transactionById.get(), "Transaction ID"));
            }
        } catch (NumberFormatException e) {
            // Not a number, skip
        }

        // Search by account number
        Pageable txPageable = PageRequest.of(0, 50);
        Page<Transaction> transactionsByAccount = transactionRepository.findByAccountNumberOrderByDateDesc(term, txPageable);
        for (Transaction transaction : transactionsByAccount.getContent()) {
            if (!results.stream().anyMatch(r -> r.get("transactionId").equals(transaction.getId()))) {
                results.add(createTransactionResult(transaction, "Account Number"));
            }
        }

        // Search by description (partial match)
        if (term.length() >= 3) {
            List<Transaction> allTransactions = transactionRepository.findAll();
            for (Transaction transaction : allTransactions) {
                if (results.stream().anyMatch(r -> r.get("transactionId").equals(transaction.getId()))) {
                    continue;
                }
                
                if (transaction.getDescription() != null && 
                    transaction.getDescription().toLowerCase().contains(term.toLowerCase())) {
                    results.add(createTransactionResult(transaction, "Description"));
                }
            }
        }

        // Limit to 50 most recent
        if (results.size() > 50) {
            results = results.subList(0, 50);
        }

        return results;
    }

    private Map<String, Object> createTransactionResult(Transaction transaction, String matchType) {
        Map<String, Object> result = new HashMap<>();
        result.put("type", "Transaction");
        result.put("transactionId", transaction.getId());
        result.put("accountNumber", transaction.getAccountNumber());
        result.put("amount", transaction.getAmount());
        result.put("transactionType", transaction.getType());
        result.put("merchant", transaction.getMerchant());
        result.put("description", transaction.getDescription());
        result.put("status", transaction.getStatus());
        result.put("date", transaction.getDate());
        result.put("balance", transaction.getBalance());
        result.put("matchType", matchType);
        return result;
    }

    /**
     * Search cards by card number, account number
     */
    private List<Map<String, Object>> searchCards(String term) {
        List<Map<String, Object>> results = new ArrayList<>();
        
        // Search by card number (last 4 digits or full)
        List<Card> allCards = cardRepository.findAll();
        for (Card card : allCards) {
            if (card.getCardNumber() != null) {
                String cardNumber = card.getCardNumber();
                // Check if term matches last 4 digits or full card number
                if (cardNumber.endsWith(term) || cardNumber.contains(term)) {
                    results.add(createCardResult(card, "Card Number"));
                }
            }
        }

        // Search by account number
        List<Card> cardsByAccount = cardRepository.findByAccountNumber(term);
        for (Card card : cardsByAccount) {
            if (!results.stream().anyMatch(r -> r.get("cardId").equals(card.getId()))) {
                results.add(createCardResult(card, "Account Number"));
            }
        }

        return results;
    }

    private Map<String, Object> createCardResult(Card card, String matchType) {
        Map<String, Object> result = new HashMap<>();
        result.put("type", "Card");
        result.put("cardId", card.getId());
        result.put("cardNumber", card.getCardNumber());
        result.put("accountNumber", card.getAccountNumber());
        result.put("cardHolderName", card.getUserName());
        result.put("cardType", card.getCardType());
        result.put("status", card.getStatus());
        result.put("expiryDate", card.getExpiryDate());
        result.put("expiryDateTime", card.getExpiryDateTime());
        result.put("matchType", matchType);
        return result;
    }
}

