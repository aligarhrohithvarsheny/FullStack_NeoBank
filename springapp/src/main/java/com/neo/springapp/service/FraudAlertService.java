package com.neo.springapp.service;

import com.neo.springapp.model.FraudAlert;
import com.neo.springapp.repository.FraudAlertRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@SuppressWarnings("null")
public class FraudAlertService {

    private final FraudAlertRepository fraudAlertRepository;

    public FraudAlertService(FraudAlertRepository fraudAlertRepository) {
        this.fraudAlertRepository = fraudAlertRepository;
    }

    public FraudAlert save(FraudAlert alert) {
        return fraudAlertRepository.save(alert);
    }

    public Optional<FraudAlert> findById(Long id) {
        return fraudAlertRepository.findById(id);
    }

    public List<FraudAlert> findAllPendingReview() {
        return fraudAlertRepository.findByStatusOrderByCreatedAtDesc(FraudAlert.Status.PENDING_REVIEW);
    }

    public Page<FraudAlert> findAllPendingReview(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return fraudAlertRepository.findByStatusOrderByCreatedAtDesc(FraudAlert.Status.PENDING_REVIEW, pageable);
    }

    public Page<FraudAlert> findWithFilters(FraudAlert.Status status, FraudAlert.AlertType alertType,
                                            FraudAlert.SourceType sourceType, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return fraudAlertRepository.findWithFilters(status, alertType, sourceType, pageable);
    }

    public List<FraudAlert> findBySourceEntity(String entityId) {
        return fraudAlertRepository.findBySourceEntityId(entityId);
    }

    public long countPendingReview() {
        return fraudAlertRepository.countPendingReview();
    }

    public FraudAlert updateStatus(Long id, FraudAlert.Status status, String reviewedBy, String reviewNotes) {
        Optional<FraudAlert> opt = fraudAlertRepository.findById(id);
        if (opt.isEmpty()) return null;
        FraudAlert a = opt.get();
        a.setStatus(status);
        a.setReviewedAt(LocalDateTime.now());
        a.setReviewedBy(reviewedBy);
        a.setReviewNotes(reviewNotes);
        return fraudAlertRepository.save(a);
    }

    /** Create alert for 3 failed login attempts (user or admin) */
    public FraudAlert recordLoginFraud(String email, String name, FraudAlert.SourceType sourceType,
                                       String clientIp, String location, String deviceInfo) {
        FraudAlert a = new FraudAlert();
        a.setAlertType(FraudAlert.AlertType.LOGIN_FRAUD);
        a.setSourceType(sourceType);
        a.setSourceEntityId(email);
        a.setSourceEntityName(name != null ? name : email);
        a.setTitle("Multiple failed login attempts");
        a.setDescription("Account locked after 3 failed password attempts. Possible brute force or credential stuffing.");
        a.setClientIp(clientIp);
        a.setLocation(location);
        a.setDeviceInfo(deviceInfo);
        a.setSeverity("HIGH");
        return save(a);
    }

    /** Create alert for suspicious transaction */
    public FraudAlert recordTransactionAnomaly(String accountNumber, String userName, String title, String description,
                                               String detailsJson, String clientIp, String location, String deviceInfo) {
        FraudAlert a = new FraudAlert();
        a.setAlertType(FraudAlert.AlertType.TRANSACTION_ANOMALY);
        a.setSourceType(FraudAlert.SourceType.USER);
        a.setSourceEntityId(accountNumber);
        a.setSourceEntityName(userName);
        a.setTitle(title);
        a.setDescription(description);
        a.setDetailsJson(detailsJson);
        a.setClientIp(clientIp);
        a.setLocation(location);
        a.setDeviceInfo(deviceInfo);
        a.setSeverity("MEDIUM");
        return save(a);
    }

    /** Create alert for behavioral biometric deviation */
    public FraudAlert recordBehavioralAnomaly(String email, String title, String description, String detailsJson,
                                              String clientIp, String deviceInfo) {
        FraudAlert a = new FraudAlert();
        a.setAlertType(FraudAlert.AlertType.BEHAVIORAL_BIOMETRIC);
        a.setSourceType(FraudAlert.SourceType.USER);
        a.setSourceEntityId(email);
        a.setSourceEntityName(email);
        a.setTitle(title);
        a.setDescription(description);
        a.setDetailsJson(detailsJson);
        a.setClientIp(clientIp);
        a.setDeviceInfo(deviceInfo);
        a.setSeverity("MEDIUM");
        return save(a);
    }

    /** Create alert for suspected phishing content */
    public FraudAlert recordPhishingSuspect(String sourceId, String title, String description, String detailsJson,
                                            String clientIp) {
        FraudAlert a = new FraudAlert();
        a.setAlertType(FraudAlert.AlertType.PHISHING_SUSPECT);
        a.setSourceType(FraudAlert.SourceType.USER);
        a.setSourceEntityId(sourceId);
        a.setTitle(title);
        a.setDescription(description);
        a.setDetailsJson(detailsJson);
        a.setClientIp(clientIp);
        a.setSeverity("HIGH");
        return save(a);
    }

    /** Create alert for KYC document fraud */
    public FraudAlert recordKycDocumentFraud(String userAccountNumber, String userName, String title, String description,
                                             String detailsJson) {
        FraudAlert a = new FraudAlert();
        a.setAlertType(FraudAlert.AlertType.KYC_DOCUMENT_FRAUD);
        a.setSourceType(FraudAlert.SourceType.USER);
        a.setSourceEntityId(userAccountNumber);
        a.setSourceEntityName(userName);
        a.setTitle(title);
        a.setDescription(description);
        a.setDetailsJson(detailsJson);
        a.setSeverity("HIGH");
        return save(a);
    }
}
