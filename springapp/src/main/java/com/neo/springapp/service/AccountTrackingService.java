package com.neo.springapp.service;

import com.neo.springapp.model.AccountTracking;
import com.neo.springapp.model.User;
import com.neo.springapp.repository.AccountTrackingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AccountTrackingService {

    @Autowired
    private AccountTrackingRepository accountTrackingRepository;

    /**
     * Generate unique tracking ID
     */
    public String generateTrackingId() {
        // Generate a unique tracking ID using UUID and timestamp
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(7);
        return "TRK" + uuid + timestamp;
    }

    /**
     * Create tracking record for new account
     */
    public AccountTracking createTracking(User user, String aadharNumber, String mobileNumber) {
        String trackingId = generateTrackingId();
        
        AccountTracking tracking = new AccountTracking();
        tracking.setTrackingId(trackingId);
        tracking.setAadharNumber(aadharNumber);
        tracking.setMobileNumber(mobileNumber);
        tracking.setUser(user);
        tracking.setStatus("PENDING");
        tracking.setCreatedAt(LocalDateTime.now());
        tracking.setUpdatedAt(LocalDateTime.now());
        tracking.setStatusChangedAt(LocalDateTime.now());
        
        return accountTrackingRepository.save(tracking);
    }

    /**
     * Get tracking by ID
     */
    public Optional<AccountTracking> getTrackingById(Long id) {
        return accountTrackingRepository.findById(id);
    }

    /**
     * Get tracking by tracking ID
     */
    public Optional<AccountTracking> getTrackingByTrackingId(String trackingId) {
        return accountTrackingRepository.findByTrackingId(trackingId);
    }

    /**
     * Get tracking by Aadhar number
     */
    public Optional<AccountTracking> getTrackingByAadharNumber(String aadharNumber) {
        return accountTrackingRepository.findByAadharNumber(aadharNumber);
    }

    /**
     * Get tracking by mobile number
     */
    public Optional<AccountTracking> getTrackingByMobileNumber(String mobileNumber) {
        return accountTrackingRepository.findByMobileNumber(mobileNumber);
    }

    /**
     * Get tracking by Aadhar number and mobile number
     */
    public Optional<AccountTracking> getTrackingByAadharAndMobile(String aadharNumber, String mobileNumber) {
        return accountTrackingRepository.findByAadharNumberAndMobileNumber(aadharNumber, mobileNumber);
    }

    /**
     * Get tracking by user ID
     */
    public Optional<AccountTracking> getTrackingByUserId(Long userId) {
        return accountTrackingRepository.findByUserId(userId);
    }

    /**
     * Update tracking status
     */
    public AccountTracking updateTrackingStatus(Long id, String status, String updatedBy) {
        try {
            Optional<AccountTracking> trackingOpt = accountTrackingRepository.findById(id);
            if (trackingOpt.isPresent()) {
                AccountTracking tracking = trackingOpt.get();
                String oldStatus = tracking.getStatus();
                tracking.setStatus(status);
                tracking.setUpdatedAt(LocalDateTime.now());
                tracking.setStatusChangedAt(LocalDateTime.now());
                tracking.setUpdatedBy(updatedBy != null ? updatedBy : "Admin");
                
                System.out.println("Updating tracking ID: " + id + " from status: " + oldStatus + " to: " + status);
                AccountTracking saved = accountTrackingRepository.save(tracking);
                System.out.println("Tracking status updated successfully. New status: " + saved.getStatus());
                return saved;
            } else {
                System.out.println("Tracking record not found with ID: " + id);
                return null;
            }
        } catch (Exception e) {
            System.err.println("Error updating tracking status: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to update tracking status: " + e.getMessage(), e);
        }
    }

    /**
     * Update tracking status by tracking ID
     */
    public AccountTracking updateTrackingStatusByTrackingId(String trackingId, String status, String updatedBy) {
        Optional<AccountTracking> trackingOpt = accountTrackingRepository.findByTrackingId(trackingId);
        if (trackingOpt.isPresent()) {
            AccountTracking tracking = trackingOpt.get();
            tracking.setStatus(status);
            tracking.setUpdatedAt(LocalDateTime.now());
            tracking.setStatusChangedAt(LocalDateTime.now());
            tracking.setUpdatedBy(updatedBy);
            return accountTrackingRepository.save(tracking);
        }
        return null;
    }

    /**
     * Get all tracking records with pagination
     */
    public Page<AccountTracking> getAllTracking(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                   Sort.by(sortBy).descending() : 
                   Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return accountTrackingRepository.findAll(pageable);
    }

    /**
     * Get tracking by status
     */
    public List<AccountTracking> getTrackingByStatus(String status) {
        return accountTrackingRepository.findByStatus(status);
    }

    /**
     * Get tracking by status with pagination
     */
    public Page<AccountTracking> getTrackingByStatusWithPagination(String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return accountTrackingRepository.findByStatus(status, pageable);
    }

    /**
     * Search tracking records
     */
    public Page<AccountTracking> searchTracking(String searchTerm, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return accountTrackingRepository.searchTracking(searchTerm, pageable);
    }

    /**
     * Get recent tracking records
     */
    public List<AccountTracking> getRecentTracking(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by("createdAt").descending());
        return accountTrackingRepository.findRecentTracking(pageable);
    }
}

