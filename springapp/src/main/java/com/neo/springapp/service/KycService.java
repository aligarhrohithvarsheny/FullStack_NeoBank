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

    public List<KycRequest> getAllKycRequestsByUserAccountNumber(String userAccountNumber) {
        List<KycRequest> requests = kycRepository.findAllByUserAccountNumber(userAccountNumber);
        // Sort by submittedDate descending to get most recent first
        requests.sort((a, b) -> {
            if (a.getSubmittedDate() == null && b.getSubmittedDate() == null) return 0;
            if (a.getSubmittedDate() == null) return 1;
            if (b.getSubmittedDate() == null) return -1;
            return b.getSubmittedDate().compareTo(a.getSubmittedDate());
        });
        return requests;
    }

    public boolean hasExistingKycRequests(String userAccountNumber) {
        return kycRepository.countByUserAccountNumber(userAccountNumber) > 0;
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
    // This method updates the entire account when KYC is approved
    // PAN number updates will propagate to the entire account
    private void updateUserDetailsFromKyc(KycRequest kycRequest) {
        try {
            // Find user by account number
            Optional<User> userOpt = userRepository.findByAccountNumber(kycRequest.getUserAccountNumber());
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                Account account = user.getAccount();
                
                if (account != null) {
                    // Update account details with KYC information
                    // Name and PAN are updated from KYC request
                    account.setName(kycRequest.getName());
                    
                    // PAN number update - this updates the entire account
                    // When PAN is updated, it propagates to all account-related systems
                    String previousPan = account.getPan();
                    account.setPan(kycRequest.getPanNumber());
                    
                    // Mark KYC as verified and update timestamp
                    account.setKycVerified(true);
                    account.setLastUpdated(LocalDateTime.now());
                    
                    // Log PAN update if it changed
                    if (previousPan != null && !previousPan.equals(kycRequest.getPanNumber())) {
                        System.out.println("⚠️ PAN Number Updated:");
                        System.out.println("   Previous PAN: " + previousPan);
                        System.out.println("   New PAN: " + kycRequest.getPanNumber());
                        System.out.println("   This update will affect the entire account.");
                    }
                    
                    // Save updated account
                    // Note: Account will be saved when user is saved due to cascade
                } else {
                    System.out.println("⚠️ Account not found for user: " + kycRequest.getUserAccountNumber());
                }
                
                // Save updated user (this will cascade save the account)
                userRepository.save(user);
                
                System.out.println("✅ Updated user account details from KYC:");
                System.out.println("   Account Number: " + kycRequest.getUserAccountNumber());
                System.out.println("   Name: " + kycRequest.getName());
                System.out.println("   PAN: " + kycRequest.getPanNumber());
                System.out.println("   KYC Verified: true");
                System.out.println("   Account updated successfully - changes propagated to entire account");
            } else {
                System.out.println("❌ User not found for account number: " + kycRequest.getUserAccountNumber());
            }
        } catch (Exception e) {
            System.out.println("❌ Error updating user details from KYC: " + e.getMessage());
            e.printStackTrace();
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

    // Get latest pending KYC request for a user
    public Optional<KycRequest> getLatestPendingKycRequest(String userAccountNumber) {
        List<KycRequest> pendingRequests = kycRepository.findByUserAccountNumberAndStatusOrderBySubmittedDateDesc(
            userAccountNumber, "Pending");
        return pendingRequests.isEmpty() ? Optional.empty() : Optional.of(pendingRequests.get(0));
    }
}
