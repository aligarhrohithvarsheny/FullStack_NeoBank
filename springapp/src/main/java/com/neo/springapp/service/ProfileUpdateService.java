package com.neo.springapp.service;

import com.neo.springapp.model.*;
import com.neo.springapp.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@SuppressWarnings("null")
public class ProfileUpdateService {

    @Autowired
    private ProfileUpdateRequestRepository requestRepository;

    @Autowired
    private ProfileUpdateHistoryRepository historyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountService accountService;

    @Autowired
    private OtpService otpService;

    @Autowired
    private EmailService emailService;

    /**
     * Request profile update (address or phone) with OTP verification
     */
    @Transactional
    public Map<String, Object> requestProfileUpdate(Long userId, String field, String newValue) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Validate field
            if (!"ADDRESS".equals(field) && !"PHONE".equals(field)) {
                response.put("success", false);
                response.put("message", "Invalid field. Only ADDRESS and PHONE can be updated.");
                return response;
            }

            // Get user
            Optional<User> userOpt = userRepository.findById(userId);
            if (!userOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "User not found");
                return response;
            }

            User user = userOpt.get();
            Account account = user.getAccount();
            if (account == null) {
                response.put("success", false);
                response.put("message", "Account not found");
                return response;
            }

            // Get current value
            String oldValue = null;
            if ("ADDRESS".equals(field)) {
                oldValue = account.getAddress();
            } else if ("PHONE".equals(field)) {
                oldValue = account.getPhone();
            }

            // Check if new value is different
            if (newValue != null && newValue.equals(oldValue)) {
                response.put("success", false);
                response.put("message", "New value is the same as current value");
                return response;
            }

            // Check if there's already a pending request
            List<ProfileUpdateRequest> pendingRequests = requestRepository
                .findByUserIdAndStatus(userId, "PENDING");
            if (!pendingRequests.isEmpty()) {
                response.put("success", false);
                response.put("message", "You already have a pending update request. Please wait for admin approval.");
                return response;
            }

            // Generate and send OTP
            String otp = otpService.generateOtp();
            String userEmail = user.getEmail();
            if (userEmail == null || userEmail.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "User email not found. Cannot send OTP.");
                return response;
            }

            otpService.storeOtp(userEmail, otp);
            boolean emailSent = emailService.sendOtpEmail(userEmail, otp);

            if (!emailSent) {
                response.put("success", false);
                response.put("message", "Failed to send OTP. Please try again.");
                return response;
            }

            // Create update request
            ProfileUpdateRequest request = new ProfileUpdateRequest();
            request.setUserId(userId);
            request.setAccountNumber(user.getAccountNumber());
            request.setUserName(account.getName());
            request.setUserEmail(userEmail);
            request.setFieldToUpdate(field);
            request.setOldValue(oldValue);
            request.setNewValue(newValue);
            request.setOtp(otp);
            request.setOtpSentAt(LocalDateTime.now());
            request.setOtpVerified(false);
            request.setStatus("PENDING");

            ProfileUpdateRequest savedRequest = requestRepository.save(request);

            response.put("success", true);
            response.put("message", "OTP has been sent to your email. Please verify to submit the update request.");
            response.put("requestId", savedRequest.getId());
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to create update request: " + e.getMessage());
            e.printStackTrace();
        }
        
        return response;
    }

    /**
     * Verify OTP and submit profile update request
     */
    @Transactional
    public Map<String, Object> verifyOtpAndSubmitRequest(Long requestId, String otp) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            ProfileUpdateRequest request = requestRepository.findById(requestId)
                .orElse(null);
            
            if (request == null) {
                response.put("success", false);
                response.put("message", "Update request not found");
                return response;
            }

            if (!"PENDING".equals(request.getStatus())) {
                response.put("success", false);
                response.put("message", "Request is not in pending status");
                return response;
            }

            // Verify OTP
            boolean otpValid = otpService.verifyOtp(request.getUserEmail(), otp);
            if (!otpValid) {
                response.put("success", false);
                response.put("message", "Invalid or expired OTP");
                return response;
            }

            // Update request status
            request.setOtpVerified(true);
            request.setStatus("OTP_VERIFIED");
            requestRepository.save(request);

            response.put("success", true);
            response.put("message", "OTP verified. Update request submitted for admin approval.");
            response.put("request", request);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to verify OTP: " + e.getMessage());
            e.printStackTrace();
        }
        
        return response;
    }

    /**
     * Resend OTP for profile update request
     */
    @Transactional
    public Map<String, Object> resendOtp(Long requestId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            ProfileUpdateRequest request = requestRepository.findById(requestId)
                .orElse(null);
            
            if (request == null) {
                response.put("success", false);
                response.put("message", "Update request not found");
                return response;
            }

            if (!"PENDING".equals(request.getStatus())) {
                response.put("success", false);
                response.put("message", "Cannot resend OTP. Request is not in pending status");
                return response;
            }

            // Generate and send new OTP
            String otp = otpService.generateOtp();
            otpService.storeOtp(request.getUserEmail(), otp);
            boolean emailSent = emailService.sendOtpEmail(request.getUserEmail(), otp);

            if (!emailSent) {
                response.put("success", false);
                response.put("message", "Failed to send OTP. Please try again.");
                return response;
            }

            // Update request with new OTP
            request.setOtp(otp);
            request.setOtpSentAt(LocalDateTime.now());
            requestRepository.save(request);

            response.put("success", true);
            response.put("message", "OTP has been resent to your email");
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to resend OTP: " + e.getMessage());
            e.printStackTrace();
        }
        
        return response;
    }

    /**
     * Approve profile update request
     */
    @Transactional
    public Map<String, Object> approveProfileUpdate(Long requestId, String approvedBy) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            ProfileUpdateRequest request = requestRepository.findById(requestId)
                .orElse(null);
            
            if (request == null) {
                response.put("success", false);
                response.put("message", "Update request not found");
                return response;
            }

            if (!"OTP_VERIFIED".equals(request.getStatus())) {
                response.put("success", false);
                response.put("message", "Request is not verified. OTP must be verified first.");
                return response;
            }

            // Get user and account
            Optional<User> userOpt = userRepository.findById(request.getUserId());
            if (!userOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "User not found");
                return response;
            }

            User user = userOpt.get();

            // Enforce digital signature: profile updates require an approved signature
            if (user.getSignatureStatus() == null || !"APPROVED".equalsIgnoreCase(user.getSignatureStatus())) {
                response.put("success", false);
                response.put("message", "Profile update cannot be approved. User must have an APPROVED digital signature before changes are applied.");
                return response;
            }

            Account account = user.getAccount();
            if (account == null) {
                response.put("success", false);
                response.put("message", "Account not found");
                return response;
            }

            // Update the field
            if ("ADDRESS".equals(request.getFieldToUpdate())) {
                account.setAddress(request.getNewValue());
            } else if ("PHONE".equals(request.getFieldToUpdate())) {
                account.setPhone(request.getNewValue());
            }

            account.setLastUpdated(LocalDateTime.now());
            accountService.saveAccount(account);

            // Create update history
            ProfileUpdateHistory history = new ProfileUpdateHistory();
            history.setUserId(request.getUserId());
            history.setAccountNumber(request.getAccountNumber());
            history.setUserName(request.getUserName());
            history.setUserEmail(request.getUserEmail());
            history.setFieldUpdated(request.getFieldToUpdate());
            history.setOldValue(request.getOldValue());
            history.setNewValue(request.getNewValue());
            history.setRequestId(requestId);
            history.setApprovedBy(approvedBy);
            history.setUpdateDate(LocalDateTime.now());
            historyRepository.save(history);

            // Update request status
            request.setStatus("COMPLETED");
            request.setApprovalDate(LocalDateTime.now());
            request.setApprovedBy(approvedBy);
            request.setCompletionDate(LocalDateTime.now());
            requestRepository.save(request);

            response.put("success", true);
            response.put("message", "Profile update approved and completed successfully");
            response.put("history", history);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to approve update: " + e.getMessage());
            e.printStackTrace();
        }
        
        return response;
    }

    /**
     * Reject profile update request
     */
    @Transactional
    public Map<String, Object> rejectProfileUpdate(Long requestId, String rejectedBy, String reason) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            ProfileUpdateRequest request = requestRepository.findById(requestId)
                .orElse(null);
            
            if (request == null) {
                response.put("success", false);
                response.put("message", "Update request not found");
                return response;
            }

            if (!"OTP_VERIFIED".equals(request.getStatus()) && !"PENDING".equals(request.getStatus())) {
                response.put("success", false);
                response.put("message", "Request cannot be rejected in current status");
                return response;
            }

            request.setStatus("REJECTED");
            request.setApprovalDate(LocalDateTime.now());
            request.setApprovedBy(rejectedBy);
            request.setRejectionReason(reason);
            requestRepository.save(request);

            response.put("success", true);
            response.put("message", "Profile update request rejected");
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to reject update: " + e.getMessage());
            e.printStackTrace();
        }
        
        return response;
    }

    /**
     * Get update requests by user ID
     */
    public List<ProfileUpdateRequest> getUpdateRequestsByUser(Long userId) {
        return requestRepository.findByUserId(userId);
    }

    /**
     * Get pending update requests (for admin)
     */
    public List<ProfileUpdateRequest> getPendingUpdateRequests() {
        return requestRepository.findByStatus("OTP_VERIFIED");
    }

    /**
     * Get all update requests (for admin)
     */
    public List<ProfileUpdateRequest> getAllUpdateRequests() {
        return requestRepository.findAll();
    }

    /**
     * Get update history by user ID
     */
    public List<ProfileUpdateHistory> getUpdateHistoryByUser(Long userId) {
        return historyRepository.findByUserIdOrderByUpdateDateDesc(userId);
    }

    /**
     * Get update history by account number
     */
    public List<ProfileUpdateHistory> getUpdateHistoryByAccount(String accountNumber) {
        return historyRepository.findByAccountNumberOrderByUpdateDateDesc(accountNumber);
    }

    /**
     * Get update request by ID
     */
    public Optional<ProfileUpdateRequest> getUpdateRequestById(Long id) {
        return requestRepository.findById(id);
    }
}
