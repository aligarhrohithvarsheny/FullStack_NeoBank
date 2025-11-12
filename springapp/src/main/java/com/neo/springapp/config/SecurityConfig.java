package com.neo.springapp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.util.List;
import java.util.Map;


@Configuration
public class SecurityConfig implements WebMvcConfigurer {

    @PersistenceContext
    private EntityManager entityManager;

    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:4200", "http://127.0.0.1:4200", "http://localhost:3000", "http://frontend:80")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    
    public static class SecurityHeaders {
        
        public static final String X_CONTENT_TYPE_OPTIONS = "X-Content-Type-Options";
        public static final String X_FRAME_OPTIONS = "X-Frame-Options";
        public static final String X_XSS_PROTECTION = "X-XSS-Protection";
        public static final String STRICT_TRANSPORT_SECURITY = "Strict-Transport-Security";
        
        public static final String NOSNIFF = "nosniff";
        public static final String DENY = "DENY";
        public static final String XSS_PROTECTION_VALUE = "1; mode=block";
        public static final String HSTS_VALUE = "max-age=31536000; includeSubDomains";
    }

    
    public static class ValidationConstants {
        
        // Account number validation
        public static final int MIN_ACCOUNT_NUMBER_LENGTH = 8;
        public static final int MAX_ACCOUNT_NUMBER_LENGTH = 16;
        
        // Password validation
        public static final int MIN_PASSWORD_LENGTH = 8;
        public static final int MAX_PASSWORD_LENGTH = 50;
        
        // Name validation
        public static final int MIN_NAME_LENGTH = 2;
        public static final int MAX_NAME_LENGTH = 50;
        
        // Email validation
        public static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$";
        
        // Phone validation
        public static final String PHONE_REGEX = "^[6-9]\\d{9}$";
        
        // PAN validation
        public static final String PAN_REGEX = "^[A-Z]{5}[0-9]{4}[A-Z]{1}$";
        
        
        public static final String AADHAR_REGEX = "^[2-9]{1}[0-9]{11}$";
        
        
        public static final double MIN_AMOUNT = 1.0;
        public static final double MAX_AMOUNT = 1000000.0;
    }

    
    public static class SecurityUtils {
        
        
        public static String sanitizeInput(String input) {
            if (input == null) {
                return null;
            }
            return input.replaceAll("[<>\"'&]", "")
                       .trim();
        }
        
        
        public static boolean isValidEmail(String email) {
            return email != null && email.matches(ValidationConstants.EMAIL_REGEX);
        }
        
        
        public static boolean isValidPhone(String phone) {
            return phone != null && phone.matches(ValidationConstants.PHONE_REGEX);
        }
        
        /**
         * Validate PAN format
         */
        public static boolean isValidPAN(String pan) {
            return pan != null && pan.matches(ValidationConstants.PAN_REGEX);
        }
        
        
        public static boolean isValidAadhar(String aadhar) {
            return aadhar != null && aadhar.matches(ValidationConstants.AADHAR_REGEX);
        }
        

        public static boolean isValidAmount(double amount) {
            return amount >= ValidationConstants.MIN_AMOUNT && 
                   amount <= ValidationConstants.MAX_AMOUNT;
        }
        
        
        public static String generateSecureTransactionId() {
            return "TXN" + System.currentTimeMillis() + 
                   String.valueOf((int)(Math.random() * 1000));
        }
        
        
        public static String generateSecureAccountNumber() {
            return "ACC" + System.currentTimeMillis() + 
                   String.valueOf((int)(Math.random() * 100));
        }
        
        
        public static String maskSensitiveData(String data) {
            if (data == null || data.length() < 4) {
                return "****";
            }
            return data.substring(0, 2) + "****" + data.substring(data.length() - 2);
        }
    }

    
    public static class EnvironmentConfig {
        
        public static final String DEVELOPMENT = "development";
        public static final String PRODUCTION = "production";
        public static final String TESTING = "testing";
        
        
        public static String[] getCorsOrigins(String environment) {
            switch (environment.toLowerCase()) {
                case DEVELOPMENT:
                    return new String[]{
                        "http://localhost:4200",
                        "http://127.0.0.1:4200",
                        "http://localhost:3000"
                    };
                case PRODUCTION:
                    return new String[]{
                        "https://yourdomain.com",
                        "https://www.yourdomain.com"
                    };
                case TESTING:
                    return new String[]{
                        "http://localhost:4200",
                        "http://localhost:8080"
                    };
                default:
                    return new String[]{"http://localhost:4200"};
            }
        }
    }

    
    public static class SecurityAudit {
        
        
        public static void logSecurityEvent(String event, String details) {
            System.out.println("SECURITY_EVENT: " + event + " - " + details + 
                             " - Timestamp: " + System.currentTimeMillis());
        }
        
        
        public static void logAuthenticationAttempt(String username, boolean success) {
            String status = success ? "SUCCESS" : "FAILED";
            logSecurityEvent("AUTH_ATTEMPT", "User: " + username + " - Status: " + status);
        }
        
        
        public static void logTransactionEvent(String transactionId, String event) {
            logSecurityEvent("TRANSACTION", "ID: " + transactionId + " - Event: " + event);
        }
        
        
        public static void logAdminAction(String adminId, String action, String target) {
            logSecurityEvent("ADMIN_ACTION", "Admin: " + adminId + " - Action: " + action + " - Target: " + target);
        }
    }

    /**
     * JPQL Security utilities for safe database operations
     */
    public static class JPQLSecurityUtils {
        
        /**
         * Sanitize JPQL query parameters to prevent injection attacks
         */
        public static String sanitizeJPQLParameter(String parameter) {
            if (parameter == null) {
                return null;
            }
            // Remove dangerous characters that could be used in JPQL injection
            return parameter.replaceAll("[';\"\\\\]", "")
                           .replaceAll("(?i)(union|select|insert|update|delete|drop|create|alter)", "")
                           .trim();
        }
        
        /**
         * Validate JPQL query for security
         */
        public static boolean isValidJPQLQuery(String query) {
            if (query == null || query.trim().isEmpty()) {
                return false;
            }
            
            String lowerQuery = query.toLowerCase().trim();
            
            // Check for dangerous keywords
            String[] dangerousKeywords = {
                "drop", "delete", "truncate", "alter", "create", "insert", "update",
                "exec", "execute", "sp_", "xp_", "cmdshell", "bulk", "openrowset"
            };
            
            for (String keyword : dangerousKeywords) {
                if (lowerQuery.contains(keyword)) {
                    return false;
                }
            }
            
            return true;
        }
        
        /**
         * Create safe JPQL query with parameter binding
         */
        public static Query createSafeJPQLQuery(EntityManager entityManager, String jpql, Map<String, Object> parameters) {
            if (!isValidJPQLQuery(jpql)) {
                throw new SecurityException("Invalid JPQL query detected");
            }
            
            Query query = entityManager.createQuery(jpql);
            
            // Bind parameters safely
            if (parameters != null) {
                for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    
                    // Sanitize string parameters
                    if (value instanceof String) {
                        value = sanitizeJPQLParameter((String) value);
                    }
                    
                    query.setParameter(key, value);
                }
            }
            
            return query;
        }
        
        /**
         * Execute safe JPQL query with result limit
         */
        public static List<?> executeSafeJPQLQuery(EntityManager entityManager, String jpql, 
                                                   Map<String, Object> parameters, int maxResults) {
            Query query = createSafeJPQLQuery(entityManager, jpql, parameters);
            query.setMaxResults(maxResults);
            return query.getResultList();
        }
        
        /**
         * Validate search term for JPQL LIKE operations
         */
        public static String sanitizeSearchTerm(String searchTerm) {
            if (searchTerm == null) {
                return null;
            }
            
            // Escape special characters for LIKE operations
            return searchTerm.replaceAll("[%_\\\\]", "\\\\$0")
                            .replaceAll("[';\"\\\\]", "")
                            .trim();
        }
        
        /**
         * Create safe LIKE query parameter
         */
        public static String createSafeLikeParameter(String searchTerm) {
            String sanitized = sanitizeSearchTerm(searchTerm);
            return "%" + sanitized + "%";
        }
    }

    /**
     * JPQL Query Templates for common operations
     */
    public static class JPQLQueryTemplates {
        
        // User queries
        public static final String FIND_USER_BY_EMAIL = 
            "SELECT u FROM User u WHERE u.email = :email";
        
        public static final String FIND_USER_BY_ACCOUNT_NUMBER = 
            "SELECT u FROM User u WHERE u.accountNumber = :accountNumber";
        
        public static final String FIND_USERS_BY_STATUS = 
            "SELECT u FROM User u WHERE u.status = :status ORDER BY u.joinDate DESC";
        
        public static final String SEARCH_USERS_BY_NAME = 
            "SELECT u FROM User u WHERE LOWER(u.account.name) LIKE LOWER(:searchTerm) ORDER BY u.account.name";
        
        public static final String FIND_USERS_BY_INCOME_RANGE = 
            "SELECT u FROM User u WHERE u.account.income BETWEEN :minIncome AND :maxIncome ORDER BY u.account.income DESC";
        
        // Account queries
        public static final String FIND_ACCOUNT_BY_NUMBER = 
            "SELECT a FROM Account a WHERE a.accountNumber = :accountNumber";
        
        public static final String FIND_ACCOUNTS_BY_TYPE = 
            "SELECT a FROM Account a WHERE a.accountType = :accountType ORDER BY a.createdAt DESC";
        
        public static final String FIND_ACCOUNTS_BY_BALANCE_RANGE = 
            "SELECT a FROM Account a WHERE a.balance BETWEEN :minBalance AND :maxBalance ORDER BY a.balance DESC";
        
        // Transaction queries
        public static final String FIND_TRANSACTIONS_BY_ACCOUNT = 
            "SELECT t FROM Transaction t WHERE t.accountNumber = :accountNumber ORDER BY t.date DESC";
        
        public static final String FIND_TRANSACTIONS_BY_TYPE = 
            "SELECT t FROM Transaction t WHERE t.type = :type ORDER BY t.date DESC";
        
        public static final String FIND_TRANSACTIONS_BY_DATE_RANGE = 
            "SELECT t FROM Transaction t WHERE t.date BETWEEN :startDate AND :endDate ORDER BY t.date DESC";
        
        public static final String SEARCH_TRANSACTIONS_BY_DESCRIPTION = 
            "SELECT t FROM Transaction t WHERE LOWER(t.description) LIKE LOWER(:searchTerm) ORDER BY t.date DESC";
        
        // Transfer queries
        public static final String FIND_TRANSFERS_BY_SENDER = 
            "SELECT tr FROM TransferRecord tr WHERE tr.senderAccountNumber = :accountNumber ORDER BY tr.date DESC";
        
        public static final String FIND_TRANSFERS_BY_RECIPIENT = 
            "SELECT tr FROM TransferRecord tr WHERE tr.recipientAccountNumber = :accountNumber ORDER BY tr.date DESC";
        
        public static final String FIND_TRANSFERS_BY_STATUS = 
            "SELECT tr FROM TransferRecord tr WHERE tr.status = :status ORDER BY tr.date DESC";
        
        public static final String SEARCH_TRANSFERS = 
            "SELECT tr FROM TransferRecord tr WHERE " +
            "LOWER(tr.senderName) LIKE LOWER(:searchTerm) OR " +
            "LOWER(tr.recipientName) LIKE LOWER(:searchTerm) OR " +
            "tr.senderAccountNumber LIKE :searchTerm OR " +
            "tr.recipientAccountNumber LIKE :searchTerm " +
            "ORDER BY tr.date DESC";
        
        // Loan queries
        public static final String FIND_LOANS_BY_USER = 
            "SELECT l FROM Loan l WHERE l.userEmail = :email ORDER BY l.applicationDate DESC";
        
        public static final String FIND_LOANS_BY_STATUS = 
            "SELECT l FROM Loan l WHERE l.status = :status ORDER BY l.applicationDate DESC";
        
        public static final String FIND_LOANS_BY_AMOUNT_RANGE = 
            "SELECT l FROM Loan l WHERE l.amount BETWEEN :minAmount AND :maxAmount ORDER BY l.amount DESC";
        
        public static final String SEARCH_LOANS = 
            "SELECT l FROM Loan l WHERE " +
            "LOWER(l.userEmail) LIKE LOWER(:searchTerm) OR " +
            "LOWER(l.purpose) LIKE LOWER(:searchTerm) OR " +
            "LOWER(l.type) LIKE LOWER(:searchTerm) " +
            "ORDER BY l.applicationDate DESC";
        
        // KYC queries
        public static final String FIND_KYC_BY_USER = 
            "SELECT k FROM KycRequest k WHERE k.userId = :userId ORDER BY k.submittedDate DESC";
        
        public static final String FIND_KYC_BY_STATUS = 
            "SELECT k FROM KycRequest k WHERE k.status = :status ORDER BY k.submittedDate DESC";
        
        public static final String SEARCH_KYC_REQUESTS = 
            "SELECT k FROM KycRequest k WHERE " +
            "LOWER(k.userName) LIKE LOWER(:searchTerm) OR " +
            "LOWER(k.userEmail) LIKE LOWER(:searchTerm) OR " +
            "k.userAccountNumber LIKE :searchTerm " +
            "ORDER BY k.submittedDate DESC";
        
        // Card queries
        public static final String FIND_CARDS_BY_USER = 
            "SELECT c FROM Card c WHERE c.userId = :userId ORDER BY c.issueDate DESC";
        
        public static final String FIND_CARDS_BY_STATUS = 
            "SELECT c FROM Card c WHERE c.status = :status ORDER BY c.issueDate DESC";
        
        public static final String SEARCH_CARDS = 
            "SELECT c FROM Card c WHERE " +
            "LOWER(c.cardNumber) LIKE LOWER(:searchTerm) OR " +
            "LOWER(c.cardHolderName) LIKE LOWER(:searchTerm) OR " +
            "LOWER(c.cardType) LIKE LOWER(:searchTerm) " +
            "ORDER BY c.issueDate DESC";
        
        // Statistics queries
        public static final String COUNT_USERS_BY_STATUS = 
            "SELECT COUNT(u) FROM User u WHERE u.status = :status";
        
        public static final String COUNT_TRANSACTIONS_BY_TYPE = 
            "SELECT COUNT(t) FROM Transaction t WHERE t.type = :type";
        
        public static final String SUM_TRANSACTION_AMOUNTS = 
            "SELECT SUM(t.amount) FROM Transaction t WHERE t.type = :type AND t.date BETWEEN :startDate AND :endDate";
        
        public static final String AVG_ACCOUNT_BALANCE = 
            "SELECT AVG(a.balance) FROM Account a WHERE a.status = :status";
        
        public static final String TOTAL_LOAN_AMOUNT = 
            "SELECT SUM(l.amount) FROM Loan l WHERE l.status = :status";
        
        public static final String COUNT_PENDING_KYC = 
            "SELECT COUNT(k) FROM KycRequest k WHERE k.status = 'PENDING'";
    }

    /**
     * JPQL Security Service for executing safe queries
     */
    public static class JPQLSecurityService {
        
        private final EntityManager entityManager;
        
        public JPQLSecurityService(EntityManager entityManager) {
            this.entityManager = entityManager;
        }
        
        /**
         * Execute safe user search
         */
        public List<?> searchUsers(String searchTerm, int maxResults) {
            String jpql = JPQLQueryTemplates.SEARCH_USERS_BY_NAME;
            Map<String, Object> parameters = Map.of(
                "searchTerm", JPQLSecurityUtils.createSafeLikeParameter(searchTerm)
            );
            return JPQLSecurityUtils.executeSafeJPQLQuery(entityManager, jpql, parameters, maxResults);
        }
        
        /**
         * Execute safe transaction search
         */
        public List<?> searchTransactions(String searchTerm, int maxResults) {
            String jpql = JPQLQueryTemplates.SEARCH_TRANSACTIONS_BY_DESCRIPTION;
            Map<String, Object> parameters = Map.of(
                "searchTerm", JPQLSecurityUtils.createSafeLikeParameter(searchTerm)
            );
            return JPQLSecurityUtils.executeSafeJPQLQuery(entityManager, jpql, parameters, maxResults);
        }
        
        /**
         * Execute safe transfer search
         */
        public List<?> searchTransfers(String searchTerm, int maxResults) {
            String jpql = JPQLQueryTemplates.SEARCH_TRANSFERS;
            Map<String, Object> parameters = Map.of(
                "searchTerm", JPQLSecurityUtils.createSafeLikeParameter(searchTerm)
            );
            return JPQLSecurityUtils.executeSafeJPQLQuery(entityManager, jpql, parameters, maxResults);
        }
        
        /**
         * Execute safe loan search
         */
        public List<?> searchLoans(String searchTerm, int maxResults) {
            String jpql = JPQLQueryTemplates.SEARCH_LOANS;
            Map<String, Object> parameters = Map.of(
                "searchTerm", JPQLSecurityUtils.createSafeLikeParameter(searchTerm)
            );
            return JPQLSecurityUtils.executeSafeJPQLQuery(entityManager, jpql, parameters, maxResults);
        }
        
        /**
         * Execute safe KYC search
         */
        public List<?> searchKycRequests(String searchTerm, int maxResults) {
            String jpql = JPQLQueryTemplates.SEARCH_KYC_REQUESTS;
            Map<String, Object> parameters = Map.of(
                "searchTerm", JPQLSecurityUtils.createSafeLikeParameter(searchTerm)
            );
            return JPQLSecurityUtils.executeSafeJPQLQuery(entityManager, jpql, parameters, maxResults);
        }
        
        /**
         * Execute safe card search
         */
        public List<?> searchCards(String searchTerm, int maxResults) {
            String jpql = JPQLQueryTemplates.SEARCH_CARDS;
            Map<String, Object> parameters = Map.of(
                "searchTerm", JPQLSecurityUtils.createSafeLikeParameter(searchTerm)
            );
            return JPQLSecurityUtils.executeSafeJPQLQuery(entityManager, jpql, parameters, maxResults);
        }
        
        /**
         * Get user statistics safely
         */
        public Long getUserCountByStatus(String status) {
            String jpql = JPQLQueryTemplates.COUNT_USERS_BY_STATUS;
            Map<String, Object> parameters = Map.of("status", status);
            Query query = JPQLSecurityUtils.createSafeJPQLQuery(entityManager, jpql, parameters);
            return (Long) query.getSingleResult();
        }
        
        /**
         * Get transaction statistics safely
         */
        public Double getTransactionSumByType(String type, String startDate, String endDate) {
            String jpql = JPQLQueryTemplates.SUM_TRANSACTION_AMOUNTS;
            Map<String, Object> parameters = Map.of(
                "type", type,
                "startDate", startDate,
                "endDate", endDate
            );
            Query query = JPQLSecurityUtils.createSafeJPQLQuery(entityManager, jpql, parameters);
            Object result = query.getSingleResult();
            return result != null ? (Double) result : 0.0;
        }
    }
}
