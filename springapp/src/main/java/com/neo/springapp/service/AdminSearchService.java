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

    @Autowired(required = false)
    private CreditCardRepository creditCardRepository;

    @Autowired(required = false)
    private FixedDepositRepository fixedDepositRepository;

    @Autowired(required = false)
    private InvestmentRepository investmentRepository;

    @Autowired(required = false)
    private EmiPaymentRepository emiPaymentRepository;

    @Autowired(required = false)
    private InsuranceApplicationRepository insuranceApplicationRepository;

    @Autowired(required = false)
    private FasttagRepository fasttagRepository;

    @Autowired(required = false)
    private SalaryAccountRepository salaryAccountRepository;

    @Autowired(required = false)
    private CurrentAccountRepository currentAccountRepository;

    @Autowired(required = false)
    private SoundboxDeviceRepository soundboxDeviceRepository;

    @Autowired(required = false)
    private VideoKycSessionRepository videoKycSessionRepository;

    @Autowired(required = false)
    private MerchantRepository merchantRepository;

    @Autowired(required = false)
    private AgentRepository agentRepository;

    @Autowired(required = false)
    private GoldLoanRepository goldLoanRepository;

    @Autowired(required = false)
    private EducationLoanApplicationRepository educationLoanApplicationRepository;

    @Autowired(required = false)
    private EducationLoanSubsidyClaimRepository educationLoanSubsidyClaimRepository;

    @Autowired(required = false)
    private KycRepository kycRepository;

    @Autowired(required = false)
    private SupportTicketRepository supportTicketRepository;

    @Autowired(required = false)
    private MerchantApplicationRepository merchantApplicationRepository;

    /**
     * Comprehensive search across ALL entities - A to Z
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

        // Search Cards (Debit)
        List<Map<String, Object>> cards = searchCards(term);
        results.put("cards", cards);
        results.put("cardCount", cards.size());

        // Search Credit Cards
        List<Map<String, Object>> creditCards = searchCreditCards(term);
        results.put("creditCards", creditCards);
        results.put("creditCardCount", creditCards.size());

        // Search Fixed Deposits
        List<Map<String, Object>> fixedDeposits = searchFixedDeposits(term);
        results.put("fixedDeposits", fixedDeposits);
        results.put("fixedDepositCount", fixedDeposits.size());

        // Search Investments
        List<Map<String, Object>> investments = searchInvestments(term);
        results.put("investments", investments);
        results.put("investmentCount", investments.size());

        // Search EMIs
        List<Map<String, Object>> emis = searchEmis(term);
        results.put("emis", emis);
        results.put("emiCount", emis.size());

        // Search Insurance
        List<Map<String, Object>> insurance = searchInsurance(term);
        results.put("insurance", insurance);
        results.put("insuranceCount", insurance.size());

        // Search FASTag
        List<Map<String, Object>> fastags = searchFastags(term);
        results.put("fastags", fastags);
        results.put("fastagCount", fastags.size());

        // Search Salary Accounts
        List<Map<String, Object>> salaryAccounts = searchSalaryAccounts(term);
        results.put("salaryAccounts", salaryAccounts);
        results.put("salaryAccountCount", salaryAccounts.size());

        // Search Current Accounts
        List<Map<String, Object>> currentAccounts = searchCurrentAccounts(term);
        results.put("currentAccounts", currentAccounts);
        results.put("currentAccountCount", currentAccounts.size());

        // Search Soundbox Devices
        List<Map<String, Object>> soundboxes = searchSoundboxDevices(term);
        results.put("soundboxes", soundboxes);
        results.put("soundboxCount", soundboxes.size());

        // Search Video KYC
        List<Map<String, Object>> videoKyc = searchVideoKyc(term);
        results.put("videoKyc", videoKyc);
        results.put("videoKycCount", videoKyc.size());

        // Search Merchants
        List<Map<String, Object>> merchants = searchMerchants(term);
        results.put("merchants", merchants);
        results.put("merchantCount", merchants.size());

        // Search Agents
        List<Map<String, Object>> agents = searchAgents(term);
        results.put("agents", agents);
        results.put("agentCount", agents.size());

        // Search Gold Loans
        List<Map<String, Object>> goldLoans = searchGoldLoans(term);
        results.put("goldLoans", goldLoans);
        results.put("goldLoanCount", goldLoans.size());

        // Search Education Loans
        List<Map<String, Object>> educationLoans = searchEducationLoans(term);
        results.put("educationLoans", educationLoans);
        results.put("educationLoanCount", educationLoans.size());

        // Search Subsidy Claims
        List<Map<String, Object>> subsidyClaims = searchSubsidyClaims(term);
        results.put("subsidyClaims", subsidyClaims);
        results.put("subsidyClaimCount", subsidyClaims.size());

        // Search KYC
        List<Map<String, Object>> kycRequests = searchKyc(term);
        results.put("kycRequests", kycRequests);
        results.put("kycCount", kycRequests.size());

        // Search Support Tickets
        List<Map<String, Object>> supportTickets = searchSupportTickets(term);
        results.put("supportTickets", supportTickets);
        results.put("supportTicketCount", supportTickets.size());

        // Search Merchant Onboarding Applications
        List<Map<String, Object>> onboardingApps = searchMerchantOnboarding(term);
        results.put("onboardingApplications", onboardingApps);
        results.put("onboardingCount", onboardingApps.size());

        // Total results
        int totalCount = accounts.size() + users.size() + loans.size() + 
                        cheques.size() + transactions.size() + cards.size() +
                        creditCards.size() + fixedDeposits.size() + investments.size() +
                        emis.size() + insurance.size() + fastags.size() +
                        salaryAccounts.size() + currentAccounts.size() + soundboxes.size() +
                        videoKyc.size() + merchants.size() + agents.size() +
                        goldLoans.size() + educationLoans.size() + subsidyClaims.size() +
                        kycRequests.size() + supportTickets.size() + onboardingApps.size();
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

    // ==================== CREDIT CARDS ====================
    private List<Map<String, Object>> searchCreditCards(String term) {
        List<Map<String, Object>> results = new ArrayList<>();
        if (creditCardRepository == null) return results;
        try {
            Optional<CreditCard> byCardNumber = creditCardRepository.findByCardNumber(term);
            byCardNumber.ifPresent(cc -> results.add(createCreditCardResult(cc, "Card Number")));

            List<CreditCard> byAccount = creditCardRepository.findByAccountNumber(term);
            for (CreditCard cc : byAccount) {
                if (results.stream().noneMatch(r -> r.get("cardNumber").equals(cc.getCardNumber()))) {
                    results.add(createCreditCardResult(cc, "Account Number"));
                }
            }
            List<CreditCard> byName = creditCardRepository.findByUserNameContainingIgnoreCase(term);
            for (CreditCard cc : byName) {
                if (results.stream().noneMatch(r -> r.get("cardNumber").equals(cc.getCardNumber()))) {
                    results.add(createCreditCardResult(cc, "Name"));
                }
            }
        } catch (Exception e) { /* skip on error */ }
        return results;
    }

    private Map<String, Object> createCreditCardResult(CreditCard cc, String matchType) {
        Map<String, Object> result = new HashMap<>();
        result.put("type", "CreditCard");
        result.put("cardNumber", cc.getCardNumber());
        result.put("accountNumber", cc.getAccountNumber());
        result.put("userName", cc.getUserName());
        result.put("status", cc.getStatus());
        result.put("creditLimit", cc.getApprovedLimit());
        result.put("availableLimit", cc.getAvailableLimit());
        result.put("matchType", matchType);
        return result;
    }

    // ==================== FIXED DEPOSITS ====================
    private List<Map<String, Object>> searchFixedDeposits(String term) {
        List<Map<String, Object>> results = new ArrayList<>();
        if (fixedDepositRepository == null) return results;
        try {
            Optional<FixedDeposit> byFdAccount = fixedDepositRepository.findByFdAccountNumber(term);
            byFdAccount.ifPresent(fd -> results.add(createFixedDepositResult(fd, "FD Account Number")));

            List<FixedDeposit> byAccount = fixedDepositRepository.findByAccountNumber(term);
            for (FixedDeposit fd : byAccount) {
                if (results.stream().noneMatch(r -> r.get("id").equals(fd.getId()))) {
                    results.add(createFixedDepositResult(fd, "Account Number"));
                }
            }
            try {
                Long fdId = Long.parseLong(term);
                Optional<FixedDeposit> byId = fixedDepositRepository.findById(fdId);
                byId.ifPresent(fd -> {
                    if (results.stream().noneMatch(r -> r.get("id").equals(fd.getId()))) {
                        results.add(createFixedDepositResult(fd, "FD ID"));
                    }
                });
            } catch (NumberFormatException e) { }
        } catch (Exception e) { /* skip on error */ }
        return results;
    }

    private Map<String, Object> createFixedDepositResult(FixedDeposit fd, String matchType) {
        Map<String, Object> result = new HashMap<>();
        result.put("type", "FixedDeposit");
        result.put("id", fd.getId());
        result.put("fdAccountNumber", fd.getFdAccountNumber());
        result.put("accountNumber", fd.getAccountNumber());
        result.put("amount", fd.getPrincipalAmount());
        result.put("status", fd.getStatus());
        result.put("interestRate", fd.getInterestRate());
        result.put("tenure", fd.getTenure());
        result.put("matchType", matchType);
        return result;
    }

    // ==================== INVESTMENTS ====================
    private List<Map<String, Object>> searchInvestments(String term) {
        List<Map<String, Object>> results = new ArrayList<>();
        if (investmentRepository == null) return results;
        try {
            List<Investment> byAccount = investmentRepository.findByAccountNumber(term);
            for (Investment inv : byAccount) {
                results.add(createInvestmentResult(inv, "Account Number"));
            }
            try {
                Long invId = Long.parseLong(term);
                Optional<Investment> byId = investmentRepository.findById(invId);
                byId.ifPresent(inv -> {
                    if (results.stream().noneMatch(r -> r.get("id").equals(inv.getId()))) {
                        results.add(createInvestmentResult(inv, "Investment ID"));
                    }
                });
            } catch (NumberFormatException e) { }
        } catch (Exception e) { /* skip on error */ }
        return results;
    }

    private Map<String, Object> createInvestmentResult(Investment inv, String matchType) {
        Map<String, Object> result = new HashMap<>();
        result.put("type", "Investment");
        result.put("id", inv.getId());
        result.put("accountNumber", inv.getAccountNumber());
        result.put("investmentType", inv.getInvestmentType());
        result.put("amount", inv.getInvestmentAmount());
        result.put("status", inv.getStatus());
        result.put("matchType", matchType);
        return result;
    }

    // ==================== EMI PAYMENTS ====================
    private List<Map<String, Object>> searchEmis(String term) {
        List<Map<String, Object>> results = new ArrayList<>();
        if (emiPaymentRepository == null) return results;
        try {
            List<EmiPayment> byLoanAccount = emiPaymentRepository.findByLoanAccountNumberOrderByEmiNumberAsc(term);
            for (EmiPayment emi : byLoanAccount) {
                results.add(createEmiResult(emi, "Loan Account Number"));
            }
            List<EmiPayment> byAccount = emiPaymentRepository.findByAccountNumberOrderByDueDateDesc(term);
            for (EmiPayment emi : byAccount) {
                if (results.stream().noneMatch(r -> r.get("id").equals(emi.getId()))) {
                    results.add(createEmiResult(emi, "Account Number"));
                }
            }
            try {
                Long emiId = Long.parseLong(term);
                Optional<EmiPayment> byId = emiPaymentRepository.findById(emiId);
                byId.ifPresent(emi -> {
                    if (results.stream().noneMatch(r -> r.get("id").equals(emi.getId()))) {
                        results.add(createEmiResult(emi, "EMI ID"));
                    }
                });
            } catch (NumberFormatException e) { }
        } catch (Exception e) { /* skip on error */ }
        if (results.size() > 50) return new ArrayList<>(results.subList(0, 50));
        return results;
    }

    private Map<String, Object> createEmiResult(EmiPayment emi, String matchType) {
        Map<String, Object> result = new HashMap<>();
        result.put("type", "EMI");
        result.put("id", emi.getId());
        result.put("loanAccountNumber", emi.getLoanAccountNumber());
        result.put("accountNumber", emi.getAccountNumber());
        result.put("emiNumber", emi.getEmiNumber());
        result.put("amount", emi.getTotalAmount());
        result.put("status", emi.getStatus());
        result.put("dueDate", emi.getDueDate());
        result.put("matchType", matchType);
        return result;
    }

    // ==================== INSURANCE ====================
    private List<Map<String, Object>> searchInsurance(String term) {
        List<Map<String, Object>> results = new ArrayList<>();
        if (insuranceApplicationRepository == null) return results;
        try {
            Optional<InsuranceApplication> byAppNumber = insuranceApplicationRepository.findByApplicationNumber(term);
            byAppNumber.ifPresent(ins -> results.add(createInsuranceResult(ins, "Application Number")));

            List<InsuranceApplication> byAccount = insuranceApplicationRepository.findByAccountNumber(term);
            for (InsuranceApplication ins : byAccount) {
                if (results.stream().noneMatch(r -> r.get("id").equals(ins.getId()))) {
                    results.add(createInsuranceResult(ins, "Account Number"));
                }
            }
            try {
                Long insId = Long.parseLong(term);
                Optional<InsuranceApplication> byId = insuranceApplicationRepository.findById(insId);
                byId.ifPresent(ins -> {
                    if (results.stream().noneMatch(r -> r.get("id").equals(ins.getId()))) {
                        results.add(createInsuranceResult(ins, "Insurance ID"));
                    }
                });
            } catch (NumberFormatException e) { }
        } catch (Exception e) { /* skip on error */ }
        return results;
    }

    private Map<String, Object> createInsuranceResult(InsuranceApplication ins, String matchType) {
        Map<String, Object> result = new HashMap<>();
        result.put("type", "Insurance");
        result.put("id", ins.getId());
        result.put("applicationNumber", ins.getApplicationNumber());
        result.put("accountNumber", ins.getAccountNumber());
        result.put("status", ins.getStatus());
        result.put("matchType", matchType);
        return result;
    }

    // ==================== FASTAG ====================
    private List<Map<String, Object>> searchFastags(String term) {
        List<Map<String, Object>> results = new ArrayList<>();
        if (fasttagRepository == null) return results;
        try {
            Fasttag byNumber = fasttagRepository.findByFasttagNumber(term);
            if (byNumber != null) results.add(createFastagResult(byNumber, "FASTag Number"));

            Fasttag byVehicle = fasttagRepository.findByVehicleNumber(term);
            if (byVehicle != null && results.stream().noneMatch(r -> r.get("id").equals(byVehicle.getId()))) {
                results.add(createFastagResult(byVehicle, "Vehicle Number"));
            }
            List<Fasttag> byEmail = fasttagRepository.findByEmail(term);
            for (Fasttag ft : byEmail) {
                if (results.stream().noneMatch(r -> r.get("id").equals(ft.getId()))) {
                    results.add(createFastagResult(ft, "Email"));
                }
            }
        } catch (Exception e) { /* skip on error */ }
        return results;
    }

    private Map<String, Object> createFastagResult(Fasttag ft, String matchType) {
        Map<String, Object> result = new HashMap<>();
        result.put("type", "FASTag");
        result.put("id", ft.getId());
        result.put("fasttagNumber", ft.getFasttagNumber());
        result.put("vehicleNumber", ft.getVehicleNumber());
        result.put("status", ft.getStatus());
        result.put("matchType", matchType);
        return result;
    }

    // ==================== SALARY ACCOUNTS ====================
    private List<Map<String, Object>> searchSalaryAccounts(String term) {
        List<Map<String, Object>> results = new ArrayList<>();
        if (salaryAccountRepository == null) return results;
        try {
            SalaryAccount byAccount = salaryAccountRepository.findByAccountNumber(term);
            if (byAccount != null) results.add(createSalaryAccountResult(byAccount, "Account Number"));

            SalaryAccount byAadhar = salaryAccountRepository.findByAadharNumber(term);
            if (byAadhar != null && results.stream().noneMatch(r -> r.get("id").equals(byAadhar.getId()))) {
                results.add(createSalaryAccountResult(byAadhar, "Aadhar Number"));
            }
            SalaryAccount byPan = salaryAccountRepository.findByPanNumber(term);
            if (byPan != null && results.stream().noneMatch(r -> r.get("id").equals(byPan.getId()))) {
                results.add(createSalaryAccountResult(byPan, "PAN Number"));
            }
            SalaryAccount byEmpId = salaryAccountRepository.findByEmployeeId(term);
            if (byEmpId != null && results.stream().noneMatch(r -> r.get("id").equals(byEmpId.getId()))) {
                results.add(createSalaryAccountResult(byEmpId, "Employee ID"));
            }
            SalaryAccount byEmail = salaryAccountRepository.findByEmail(term);
            if (byEmail != null && results.stream().noneMatch(r -> r.get("id").equals(byEmail.getId()))) {
                results.add(createSalaryAccountResult(byEmail, "Email"));
            }
            SalaryAccount byCustomerId = salaryAccountRepository.findByCustomerId(term);
            if (byCustomerId != null && results.stream().noneMatch(r -> r.get("id").equals(byCustomerId.getId()))) {
                results.add(createSalaryAccountResult(byCustomerId, "Customer ID"));
            }
        } catch (Exception e) { /* skip on error */ }
        return results;
    }

    private Map<String, Object> createSalaryAccountResult(SalaryAccount sa, String matchType) {
        Map<String, Object> result = new HashMap<>();
        result.put("type", "SalaryAccount");
        result.put("id", sa.getId());
        result.put("accountNumber", sa.getAccountNumber());
        result.put("customerName", sa.getEmployeeName());
        result.put("companyName", sa.getCompanyName());
        result.put("status", sa.getStatus());
        result.put("matchType", matchType);
        return result;
    }

    // ==================== CURRENT ACCOUNTS ====================
    private List<Map<String, Object>> searchCurrentAccounts(String term) {
        List<Map<String, Object>> results = new ArrayList<>();
        if (currentAccountRepository == null) return results;
        try {
            Optional<CurrentAccount> byAccount = currentAccountRepository.findByAccountNumber(term);
            byAccount.ifPresent(ca -> results.add(createCurrentAccountResult(ca, "Account Number")));

            Optional<CurrentAccount> byCustomerId = currentAccountRepository.findByCustomerId(term);
            byCustomerId.ifPresent(ca -> {
                if (results.stream().noneMatch(r -> r.get("id").equals(ca.getId()))) {
                    results.add(createCurrentAccountResult(ca, "Customer ID"));
                }
            });
            Optional<CurrentAccount> byGst = currentAccountRepository.findByGstNumber(term);
            byGst.ifPresent(ca -> {
                if (results.stream().noneMatch(r -> r.get("id").equals(ca.getId()))) {
                    results.add(createCurrentAccountResult(ca, "GST Number"));
                }
            });
            Optional<CurrentAccount> byPan = currentAccountRepository.findByPanNumber(term);
            byPan.ifPresent(ca -> {
                if (results.stream().noneMatch(r -> r.get("id").equals(ca.getId()))) {
                    results.add(createCurrentAccountResult(ca, "PAN Number"));
                }
            });
        } catch (Exception e) { /* skip on error */ }
        return results;
    }

    private Map<String, Object> createCurrentAccountResult(CurrentAccount ca, String matchType) {
        Map<String, Object> result = new HashMap<>();
        result.put("type", "CurrentAccount");
        result.put("id", ca.getId());
        result.put("accountNumber", ca.getAccountNumber());
        result.put("businessName", ca.getBusinessName());
        result.put("businessType", ca.getBusinessType());
        result.put("status", ca.getStatus());
        result.put("balance", ca.getBalance());
        result.put("matchType", matchType);
        return result;
    }

    // ==================== SOUNDBOX DEVICES ====================
    private List<Map<String, Object>> searchSoundboxDevices(String term) {
        List<Map<String, Object>> results = new ArrayList<>();
        if (soundboxDeviceRepository == null) return results;
        try {
            Optional<SoundboxDevice> byDeviceId = soundboxDeviceRepository.findByDeviceId(term);
            byDeviceId.ifPresent(sb -> results.add(createSoundboxResult(sb, "Device ID")));

            Optional<SoundboxDevice> byAccount = soundboxDeviceRepository.findByAccountNumber(term);
            byAccount.ifPresent(sb -> {
                if (results.stream().noneMatch(r -> r.get("id").equals(sb.getId()))) {
                    results.add(createSoundboxResult(sb, "Account Number"));
                }
            });
        } catch (Exception e) { /* skip on error */ }
        return results;
    }

    private Map<String, Object> createSoundboxResult(SoundboxDevice sb, String matchType) {
        Map<String, Object> result = new HashMap<>();
        result.put("type", "Soundbox");
        result.put("id", sb.getId());
        result.put("deviceId", sb.getDeviceId());
        result.put("accountNumber", sb.getAccountNumber());
        result.put("status", sb.getStatus());
        result.put("matchType", matchType);
        return result;
    }

    // ==================== VIDEO KYC ====================
    private List<Map<String, Object>> searchVideoKyc(String term) {
        List<Map<String, Object>> results = new ArrayList<>();
        if (videoKycSessionRepository == null) return results;
        try {
            Optional<VideoKycSession> byVerification = videoKycSessionRepository.findByVerificationNumber(term);
            byVerification.ifPresent(vk -> results.add(createVideoKycResult(vk, "Verification Number")));

            Optional<VideoKycSession> byTempAccount = videoKycSessionRepository.findByTemporaryAccountNumber(term);
            byTempAccount.ifPresent(vk -> {
                if (results.stream().noneMatch(r -> r.get("id").equals(vk.getId()))) {
                    results.add(createVideoKycResult(vk, "Account Number"));
                }
            });
            Optional<VideoKycSession> byMobile = videoKycSessionRepository.findByMobileNumber(term);
            byMobile.ifPresent(vk -> {
                if (results.stream().noneMatch(r -> r.get("id").equals(vk.getId()))) {
                    results.add(createVideoKycResult(vk, "Mobile Number"));
                }
            });
            Optional<VideoKycSession> byEmail = videoKycSessionRepository.findByEmail(term);
            byEmail.ifPresent(vk -> {
                if (results.stream().noneMatch(r -> r.get("id").equals(vk.getId()))) {
                    results.add(createVideoKycResult(vk, "Email"));
                }
            });
            try {
                Long vkId = Long.parseLong(term);
                Optional<VideoKycSession> byId = videoKycSessionRepository.findById(vkId);
                byId.ifPresent(vk -> {
                    if (results.stream().noneMatch(r -> r.get("id").equals(vk.getId()))) {
                        results.add(createVideoKycResult(vk, "Video KYC ID"));
                    }
                });
            } catch (NumberFormatException e) { }
        } catch (Exception e) { /* skip on error */ }
        return results;
    }

    private Map<String, Object> createVideoKycResult(VideoKycSession vk, String matchType) {
        Map<String, Object> result = new HashMap<>();
        result.put("type", "VideoKYC");
        result.put("id", vk.getId());
        result.put("verificationNumber", vk.getVerificationNumber());
        result.put("kycStatus", vk.getKycStatus());
        result.put("matchType", matchType);
        return result;
    }

    // ==================== MERCHANTS ====================
    private List<Map<String, Object>> searchMerchants(String term) {
        List<Map<String, Object>> results = new ArrayList<>();
        if (merchantRepository == null) return results;
        try {
            Optional<Merchant> byMerchantId = merchantRepository.findByMerchantId(term);
            byMerchantId.ifPresent(m -> results.add(createMerchantResult(m, "Merchant ID")));

            Optional<Merchant> byMobile = merchantRepository.findByMobile(term);
            byMobile.ifPresent(m -> {
                if (results.stream().noneMatch(r -> r.get("merchantId").equals(m.getMerchantId()))) {
                    results.add(createMerchantResult(m, "Mobile"));
                }
            });
            Optional<Merchant> byEmail = merchantRepository.findByEmail(term);
            byEmail.ifPresent(m -> {
                if (results.stream().noneMatch(r -> r.get("merchantId").equals(m.getMerchantId()))) {
                    results.add(createMerchantResult(m, "Email"));
                }
            });
        } catch (Exception e) { /* skip on error */ }
        return results;
    }

    private Map<String, Object> createMerchantResult(Merchant m, String matchType) {
        Map<String, Object> result = new HashMap<>();
        result.put("type", "Merchant");
        result.put("id", m.getId());
        result.put("merchantId", m.getMerchantId());
        result.put("businessName", m.getBusinessName());
        result.put("status", m.getStatus());
        result.put("matchType", matchType);
        return result;
    }

    // ==================== AGENTS ====================
    private List<Map<String, Object>> searchAgents(String term) {
        List<Map<String, Object>> results = new ArrayList<>();
        if (agentRepository == null) return results;
        try {
            Optional<Agent> byAgentId = agentRepository.findByAgentId(term);
            byAgentId.ifPresent(a -> results.add(createAgentResult(a, "Agent ID")));

            Optional<Agent> byEmail = agentRepository.findByEmail(term);
            byEmail.ifPresent(a -> {
                if (results.stream().noneMatch(r -> r.get("id").equals(a.getId()))) {
                    results.add(createAgentResult(a, "Email"));
                }
            });
            Optional<Agent> byMobile = agentRepository.findByMobile(term);
            byMobile.ifPresent(a -> {
                if (results.stream().noneMatch(r -> r.get("id").equals(a.getId()))) {
                    results.add(createAgentResult(a, "Mobile"));
                }
            });
        } catch (Exception e) { /* skip on error */ }
        return results;
    }

    private Map<String, Object> createAgentResult(Agent a, String matchType) {
        Map<String, Object> result = new HashMap<>();
        result.put("type", "Agent");
        result.put("id", a.getId());
        result.put("agentId", a.getAgentId());
        result.put("name", a.getName());
        result.put("email", a.getEmail());
        result.put("status", a.getStatus());
        result.put("matchType", matchType);
        return result;
    }

    // ==================== GOLD LOANS ====================
    private List<Map<String, Object>> searchGoldLoans(String term) {
        List<Map<String, Object>> results = new ArrayList<>();
        if (goldLoanRepository == null) return results;
        try {
            Optional<GoldLoan> byLoanAccount = goldLoanRepository.findByLoanAccountNumber(term);
            byLoanAccount.ifPresent(gl -> results.add(createGoldLoanResult(gl, "Loan Account Number")));

            List<GoldLoan> byAccount = goldLoanRepository.findByAccountNumber(term);
            for (GoldLoan gl : byAccount) {
                if (results.stream().noneMatch(r -> r.get("id").equals(gl.getId()))) {
                    results.add(createGoldLoanResult(gl, "Account Number"));
                }
            }
            try {
                Long glId = Long.parseLong(term);
                Optional<GoldLoan> byId = goldLoanRepository.findById(glId);
                byId.ifPresent(gl -> {
                    if (results.stream().noneMatch(r -> r.get("id").equals(gl.getId()))) {
                        results.add(createGoldLoanResult(gl, "Gold Loan ID"));
                    }
                });
            } catch (NumberFormatException e) { }
        } catch (Exception e) { /* skip on error */ }
        return results;
    }

    private Map<String, Object> createGoldLoanResult(GoldLoan gl, String matchType) {
        Map<String, Object> result = new HashMap<>();
        result.put("type", "GoldLoan");
        result.put("id", gl.getId());
        result.put("loanAccountNumber", gl.getLoanAccountNumber());
        result.put("accountNumber", gl.getAccountNumber());
        result.put("status", gl.getStatus());
        result.put("matchType", matchType);
        return result;
    }

    // ==================== EDUCATION LOANS ====================
    private List<Map<String, Object>> searchEducationLoans(String term) {
        List<Map<String, Object>> results = new ArrayList<>();
        if (educationLoanApplicationRepository == null) return results;
        try {
            Optional<EducationLoanApplication> byLoanAccount = educationLoanApplicationRepository.findByLoanAccountNumber(term);
            byLoanAccount.ifPresent(el -> results.add(createEducationLoanResult(el, "Loan Account Number")));

            List<EducationLoanApplication> byAccount = educationLoanApplicationRepository.findByApplicantAccountNumber(term);
            for (EducationLoanApplication el : byAccount) {
                if (results.stream().noneMatch(r -> r.get("id").equals(el.getId()))) {
                    results.add(createEducationLoanResult(el, "Account Number"));
                }
            }
            try {
                Long loanIdLong = Long.parseLong(term);
                Optional<EducationLoanApplication> byLoanId = educationLoanApplicationRepository.findByLoanId(loanIdLong);
            byLoanId.ifPresent(el -> {
                if (results.stream().noneMatch(r -> r.get("id").equals(el.getId()))) {
                    results.add(createEducationLoanResult(el, "Loan ID"));
                }
            });
            } catch (NumberFormatException e) { }
            List<EducationLoanApplication> byChildName = educationLoanApplicationRepository.findByChildNameContainingIgnoreCase(term);
            for (EducationLoanApplication el : byChildName) {
                if (results.stream().noneMatch(r -> r.get("id").equals(el.getId()))) {
                    results.add(createEducationLoanResult(el, "Child Name"));
                }
            }
        } catch (Exception e) { /* skip on error */ }
        return results;
    }

    private Map<String, Object> createEducationLoanResult(EducationLoanApplication el, String matchType) {
        Map<String, Object> result = new HashMap<>();
        result.put("type", "EducationLoan");
        result.put("id", el.getId());
        result.put("loanAccountNumber", el.getLoanAccountNumber());
        result.put("applicantAccountNumber", el.getApplicantAccountNumber());
        result.put("childName", el.getChildName());
        result.put("applicationStatus", el.getApplicationStatus());
        result.put("matchType", matchType);
        return result;
    }

    // ==================== SUBSIDY CLAIMS ====================
    private List<Map<String, Object>> searchSubsidyClaims(String term) {
        List<Map<String, Object>> results = new ArrayList<>();
        if (educationLoanSubsidyClaimRepository == null) return results;
        try {
            List<EducationLoanSubsidyClaim> byAccount = educationLoanSubsidyClaimRepository.findByAccountNumber(term);
            for (EducationLoanSubsidyClaim sc : byAccount) {
                results.add(createSubsidyClaimResult(sc, "Account Number"));
            }
            List<EducationLoanSubsidyClaim> byLoanAccount = educationLoanSubsidyClaimRepository.findByLoanAccountNumber(term);
            for (EducationLoanSubsidyClaim sc : byLoanAccount) {
                if (results.stream().noneMatch(r -> r.get("id").equals(sc.getId()))) {
                    results.add(createSubsidyClaimResult(sc, "Loan Account Number"));
                }
            }
            try {
                Long scId = Long.parseLong(term);
                Optional<EducationLoanSubsidyClaim> byId = educationLoanSubsidyClaimRepository.findById(scId);
                byId.ifPresent(sc -> {
                    if (results.stream().noneMatch(r -> r.get("id").equals(sc.getId()))) {
                        results.add(createSubsidyClaimResult(sc, "Subsidy ID"));
                    }
                });
            } catch (NumberFormatException e) { }
        } catch (Exception e) { /* skip on error */ }
        return results;
    }

    private Map<String, Object> createSubsidyClaimResult(EducationLoanSubsidyClaim sc, String matchType) {
        Map<String, Object> result = new HashMap<>();
        result.put("type", "SubsidyClaim");
        result.put("id", sc.getId());
        result.put("accountNumber", sc.getAccountNumber());
        result.put("loanAccountNumber", sc.getLoanAccountNumber());
        result.put("status", sc.getStatus());
        result.put("matchType", matchType);
        return result;
    }

    // ==================== KYC REQUESTS ====================
    private List<Map<String, Object>> searchKyc(String term) {
        List<Map<String, Object>> results = new ArrayList<>();
        if (kycRepository == null) return results;
        try {
            Optional<KycRequest> byPan = kycRepository.findByPanNumber(term);
            byPan.ifPresent(kyc -> results.add(createKycResult(kyc, "PAN Number")));
            List<KycRequest> byAccount = kycRepository.findAllByUserAccountNumber(term);
            for (KycRequest kyc : byAccount) {
                if (results.stream().noneMatch(r -> r.get("id").equals(kyc.getId()))) {
                    results.add(createKycResult(kyc, "Account Number"));
                }
            }
            try {
                Long kycId = Long.parseLong(term);
                Optional<KycRequest> byId = kycRepository.findById(kycId);
                byId.ifPresent(kyc -> {
                    if (results.stream().noneMatch(r -> r.get("id").equals(kyc.getId()))) {
                        results.add(createKycResult(kyc, "KYC ID"));
                    }
                });
            } catch (NumberFormatException e) { }
        } catch (Exception e) { /* skip on error */ }
        return results;
    }

    private Map<String, Object> createKycResult(KycRequest kyc, String matchType) {
        Map<String, Object> result = new HashMap<>();
        result.put("type", "KYC");
        result.put("id", kyc.getId());
        result.put("panNumber", kyc.getPanNumber());
        result.put("status", kyc.getStatus());
        result.put("matchType", matchType);
        return result;
    }

    // ==================== SUPPORT TICKETS ====================
    private List<Map<String, Object>> searchSupportTickets(String term) {
        List<Map<String, Object>> results = new ArrayList<>();
        if (supportTicketRepository == null) return results;
        try {
            Optional<SupportTicket> byTicketId = supportTicketRepository.findByTicketId(term);
            byTicketId.ifPresent(st -> results.add(createSupportTicketResult(st, "Ticket ID")));

            List<SupportTicket> byAccount = supportTicketRepository.findByAccountNumber(term);
            for (SupportTicket st : byAccount) {
                if (results.stream().noneMatch(r -> r.get("id").equals(st.getId()))) {
                    results.add(createSupportTicketResult(st, "Account Number"));
                }
            }
        } catch (Exception e) { /* skip on error */ }
        return results;
    }

    private Map<String, Object> createSupportTicketResult(SupportTicket st, String matchType) {
        Map<String, Object> result = new HashMap<>();
        result.put("type", "SupportTicket");
        result.put("id", st.getId());
        result.put("ticketId", st.getTicketId());
        result.put("accountNumber", st.getAccountNumber());
        result.put("status", st.getStatus());
        result.put("category", st.getCategory());
        result.put("matchType", matchType);
        return result;
    }

    // ==================== MERCHANT ONBOARDING ====================
    private List<Map<String, Object>> searchMerchantOnboarding(String term) {
        List<Map<String, Object>> results = new ArrayList<>();
        if (merchantApplicationRepository == null) return results;
        try {
            Optional<MerchantApplication> byAppId = merchantApplicationRepository.findByApplicationId(term);
            byAppId.ifPresent(ma -> results.add(createOnboardingResult(ma, "Application ID")));

            try {
                Long maId = Long.parseLong(term);
                Optional<MerchantApplication> byId = merchantApplicationRepository.findById(maId);
                byId.ifPresent(ma -> {
                    if (results.stream().noneMatch(r -> r.get("id").equals(ma.getId()))) {
                        results.add(createOnboardingResult(ma, "Onboarding ID"));
                    }
                });
            } catch (NumberFormatException e) { }
        } catch (Exception e) { /* skip on error */ }
        return results;
    }

    private Map<String, Object> createOnboardingResult(MerchantApplication ma, String matchType) {
        Map<String, Object> result = new HashMap<>();
        result.put("type", "MerchantOnboarding");
        result.put("id", ma.getId());
        result.put("applicationId", ma.getApplicationId());
        result.put("status", ma.getStatus());
        result.put("matchType", matchType);
        return result;
    }
}

