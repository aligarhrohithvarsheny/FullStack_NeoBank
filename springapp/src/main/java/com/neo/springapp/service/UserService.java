package com.neo.springapp.service;

import com.neo.springapp.model.User;
import com.neo.springapp.model.Account;
import com.neo.springapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private AccountService accountService;

    // Basic CRUD operations
    public User saveUser(User user) {
        // Don't generate account number automatically - only when admin approves
        // Account number will be generated in approveUser() method
        return userRepository.save(user);
    }

    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> getUserByAccountNumber(String accountNumber) {
        return userRepository.findByAccountNumber(accountNumber);
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Page<User> getAllUsersWithPagination(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                   Sort.by(sortBy).descending() : 
                   Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return userRepository.findAll(pageable);
    }

    // Status-based operations
    public List<User> getUsersByStatus(String status) {
        return userRepository.findByStatus(status);
    }

    public Page<User> getUsersByStatusWithPagination(String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("joinDate").descending());
        return userRepository.findByStatus(status, pageable);
    }

    public User approveUser(Long userId, String adminName) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            
            // Check if user is already approved
            if ("APPROVED".equals(user.getStatus())) {
                System.out.println("User already approved: " + user.getUsername());
                return user;
            }
            
            // Generate unique account number for approval
            user.setAccountNumber(generateAccountNumber());
            
            // Update user status
            user.setStatus("APPROVED");
            
            // Create or update account
            Account account = user.getAccount();
            if (account == null) {
                account = new Account();
                account.setAccountNumber(user.getAccountNumber());
                account.setName(user.getUsername());
                account.setStatus("ACTIVE");
                account.setCreatedAt(LocalDateTime.now());
                account.setLastUpdated(LocalDateTime.now());
                account.setBalance(0.0); // Initialize balance
                
                // Set required fields with unique values to avoid constraint violations
                // Generate unique Aadhar and PAN numbers based on user ID and timestamp
                String timestamp = String.valueOf(System.currentTimeMillis());
                account.setAadharNumber("AADHAR_" + user.getId() + "_" + timestamp);
                account.setPan("PAN" + user.getId() + timestamp.substring(timestamp.length() - 4));
                
                // Set other required fields with default values
                account.setDob("1990-01-01"); // Default DOB
                account.setAge(25); // Default age
                account.setOccupation("Employee"); // Default occupation
                account.setAccountType("Savings"); // Default account type
                account.setIncome(50000.0); // Default income
                account.setPhone("9999999999"); // Default phone
                account.setAddress("Default Address"); // Default address
            } else {
                account.setAccountNumber(user.getAccountNumber());
                account.setStatus("ACTIVE");
                account.setLastUpdated(LocalDateTime.now());
            }
            
            // Save account first
            Account savedAccount = accountService.saveAccount(account);
            user.setAccount(savedAccount);
            
            // Save updated user
            User approvedUser = userRepository.save(user);
            
            System.out.println("✅ User approved successfully: " + approvedUser.getUsername());
            System.out.println("Account Number: " + approvedUser.getAccountNumber());
            
            return approvedUser;
        } else {
            System.out.println("❌ User not found: " + userId);
            return null;
        }
    }

    public User closeUserAccount(Long userId, String adminName) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setStatus("CLOSED");
            return userRepository.save(user);
        }
        return null;
    }

    // Search operations
    public Page<User> searchUsers(String searchTerm, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("joinDate").descending());
        return userRepository.searchUsers(searchTerm, pageable);
    }

    // Filter operations
    public List<User> getUsersByIncomeRange(Double minIncome, Double maxIncome) {
        return userRepository.findByIncomeRange(minIncome, maxIncome);
    }

    public List<User> getUsersByOccupation(String occupation) {
        return userRepository.findByOccupation(occupation);
    }

    public List<User> getUsersByAccountType(String accountType) {
        return userRepository.findByAccountType(accountType);
    }

    public List<User> getUsersByJoinDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return userRepository.findByJoinDateRange(startDate, endDate);
    }

    // Statistics operations
    public Long getTotalUsersCount() {
        return userRepository.count();
    }

    public Long getUsersCountByStatus(String status) {
        return userRepository.countByStatus(status);
    }

    public Double getAverageIncome() {
        return userRepository.getAverageIncome();
    }

    public List<User> getRecentUsers(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by("joinDate").descending());
        return userRepository.findRecentUsers(pageable);
    }

    // Update operations
    public User updateUser(Long id, User userDetails) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            
            // Update only non-null fields
            if (userDetails.getEmail() != null) user.setEmail(userDetails.getEmail());
            if (userDetails.getStatus() != null) user.setStatus(userDetails.getStatus());
            
            // Update account fields if account exists
            if (user.getAccount() != null) {
                Account account = user.getAccount();
                if (userDetails.getName() != null) account.setName(userDetails.getName());
                if (userDetails.getPhone() != null) account.setPhone(userDetails.getPhone());
                if (userDetails.getAddress() != null) account.setAddress(userDetails.getAddress());
                if (userDetails.getOccupation() != null) account.setOccupation(userDetails.getOccupation());
                if (userDetails.getIncome() != null) account.setIncome(userDetails.getIncome());
                if (userDetails.getAccountType() != null) account.setAccountType(userDetails.getAccountType());
            }
            
            return userRepository.save(user);
        }
        return null;
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    // Utility methods
    private String generateAccountNumber() {
        String accountNumber;
        do {
            // Generate account number with timestamp and random number
            long timestamp = System.currentTimeMillis();
            int random = (int)(Math.random() * 10000); // Increased range for better uniqueness
            accountNumber = "ACC" + timestamp + random;
            
            // Check if this account number already exists
            Optional<User> existingUser = userRepository.findByAccountNumber(accountNumber);
            if (!existingUser.isPresent()) {
                break; // Account number is unique
            }
        } while (true);
        
        System.out.println("Generated unique account number: " + accountNumber);
        return accountNumber;
    }

    // Validation methods
    public boolean isEmailUnique(String email) {
        return !userRepository.findByEmail(email).isPresent();
    }

    public boolean isPanUnique(String pan) {
        return !userRepository.findByPan(pan).isPresent();
    }

    public boolean isAadharUnique(String aadhar) {
        return !userRepository.findByAadhar(aadhar).isPresent();
    }


    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}
