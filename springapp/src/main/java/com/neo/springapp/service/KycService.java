package com.neo.springapp.service;

import com.neo.springapp.model.KycRequest;
import com.neo.springapp.model.User;
import com.neo.springapp.model.Account;
import com.neo.springapp.repository.KycRepository;
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
public class KycService {

    @Autowired
    private KycRepository kycRepository;
    
    @Autowired
    private UserRepository userRepository;

    // Basic CRUD operations
    public KycRequest saveKycRequest(KycRequest kycRequest) {
        return kycRepository.save(kycRequest);
    }

    public Optional<KycRequest> getKycRequestById(Long id) {
        return kycRepository.findById(id);
    }

    public Optional<KycRequest> getKycRequestByPanNumber(String panNumber) {
        return kycRepository.findByPanNumber(panNumber);
    }

    public Optional<KycRequest> getKycRequestByUserId(String userId) {
        return kycRepository.findByUserId(userId);
    }

    public Optional<KycRequest> getKycRequestByUserAccountNumber(String userAccountNumber) {
        return kycRepository.findByUserAccountNumber(userAccountNumber);
    }

    public List<KycRequest> getAllKycRequests() {
        return kycRepository.findAll();
    }

    public Page<KycRequest> getAllKycRequestsWithPagination(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                   Sort.by(sortBy).descending() : 
                   Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return kycRepository.findAll(pageable);
    }

    // Status-based operations
    public List<KycRequest> getKycRequestsByStatus(String status) {
        return kycRepository.findByStatus(status);
    }

    public Page<KycRequest> getKycRequestsByStatusWithPagination(String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("submittedDate").descending());
        return kycRepository.findByStatus(status, pageable);
    }

    public List<KycRequest> getPendingKycRequestsForReview() {
        return kycRepository.findPendingRequestsForReview();
    }

    // Create new KYC request
    public KycRequest createKycRequest(String panNumber, String name, String userId, 
                                       String userName, String userEmail, String userAccountNumber) {
        // Check if KYC request already exists
        Optional<KycRequest> existing = kycRepository.findByPanNumber(panNumber);
        if (existing.isPresent()) {
            return existing.get(); // already exists
        }
        
        KycRequest request = new KycRequest(panNumber, name, userId, userName, userEmail, userAccountNumber);
        return kycRepository.save(request);
    }

    // Approve KYC and update user details across all systems
    public KycRequest approveKyc(Long kycRequestId, String adminName) {
        Optional<KycRequest> requestOpt = kycRepository.findById(kycRequestId);
        if (requestOpt.isPresent()) {
            KycRequest request = requestOpt.get();
            request.setStatus("Approved");
            request.setApprovedDate(LocalDateTime.now());
            request.setApprovedBy(adminName);
            
            // Save KYC request first
            KycRequest savedRequest = kycRepository.save(request);
            
            // Update user details across all systems
            updateUserDetailsFromKyc(request);
            
            System.out.println("✅ KYC approved and user details updated for: " + request.getUserAccountNumber());
            return savedRequest;
        }
        return null;
    }
    
    // Update user details from approved KYC
    private void updateUserDetailsFromKyc(KycRequest kycRequest) {
        try {
            // Find user by account number
            Optional<User> userOpt = userRepository.findByAccountNumber(kycRequest.getUserAccountNumber());
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                Account account = user.getAccount();
                
                if (account != null) {
                    // Update account details with KYC information
                    account.setName(kycRequest.getName());
                    account.setPan(kycRequest.getPanNumber());
                    account.setKycVerified(true);
                    account.setLastUpdated(LocalDateTime.now());
                    
                    // Save updated account
                    // Note: Account will be saved when user is saved due to cascade
                }
                
                // Save updated user
                userRepository.save(user);
                
                System.out.println("✅ Updated user account details from KYC:");
                System.out.println("   Account Number: " + kycRequest.getUserAccountNumber());
                System.out.println("   Name: " + kycRequest.getName());
                System.out.println("   PAN: " + kycRequest.getPanNumber());
                System.out.println("   KYC Verified: true");
            } else {
                System.out.println("❌ User not found for account number: " + kycRequest.getUserAccountNumber());
            }
        } catch (Exception e) {
            System.out.println("❌ Error updating user details from KYC: " + e.getMessage());
        }
    }

    // Reject KYC
    public KycRequest rejectKyc(Long kycRequestId, String adminName) {
        Optional<KycRequest> requestOpt = kycRepository.findById(kycRequestId);
        if (requestOpt.isPresent()) {
            KycRequest request = requestOpt.get();
            request.setStatus("Rejected");
            request.setApprovedDate(LocalDateTime.now());
            request.setApprovedBy(adminName);
            return kycRepository.save(request);
        }
        return null;
    }

    // Check KYC status by PAN number
    public Optional<KycRequest> getKycStatusByPanNumber(String panNumber) {
        return kycRepository.findByPanNumber(panNumber);
    }

    // Search operations
    public Page<KycRequest> searchKycRequests(String searchTerm, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("submittedDate").descending());
        return kycRepository.searchKycRequests(searchTerm, pageable);
    }

    // Statistics operations
    public Long getTotalKycRequestsCount() {
        return kycRepository.count();
    }

    public Long getKycRequestsCountByStatus(String status) {
        return kycRepository.countByStatus(status);
    }

    public List<KycRequest> getRecentKycRequests(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by("submittedDate").descending());
        return kycRepository.findRecentRequests(pageable);
    }

    // Admin operations
    public List<KycRequest> getKycRequestsByApprovedBy(String adminName) {
        return kycRepository.findByApprovedBy(adminName);
    }

    public void deleteKycRequest(Long id) {
        kycRepository.deleteById(id);
    }
}
